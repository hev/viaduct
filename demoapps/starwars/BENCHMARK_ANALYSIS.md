# Star Wars Benchmark Analysis: Cache Hits Flatten the Tail

## TL;DR

The Star Wars benchmark has one clear result: **fan-out under memory pressure creates the latency tail, and response-cache hits remove that tail for repeat queries.**

With 50,000 in-memory entities and a 128MB JVM heap, Viaduct handles shallow reads well, but a 500-character nested read reaches **184ms mean / 215ms P99** and only **5.4 ops/s**. The JVM spends **8.1% of wall time in GC**, including 34 old-gen collections.

With the NVMe-backed query-result cache in [hev/mesh](https://github.com/hev/mesh) `layer-gateway`, repeat queries return in about **1ms regardless of GraphQL depth or fan-out**. The measured fan-out case drops from **28.3ms direct to 1.1ms from cache** in the cache benchmark, a **24x speedup**. Compared with the constrained 128MB JVM run, the same shape maps to roughly **170x** faster repeat reads.

The practical conclusion is:

- Keep Viaduct healthy on cache misses by bounding expensive fan-out.
- Send repeat read traffic through a persistent response cache.
- Treat mutation-aware invalidation as the correctness requirement that makes long-lived cache entries safe.

## Benchmark Context

There are two related benchmark runs:

| Run | Purpose | Environment |
|-----|---------|-------------|
| JVM pressure benchmark | Shows where Viaduct's latency tail comes from | Star Wars demo, 50K synthetic characters, JDK 21, G1, `-Xmx128m -Xms64m`, 5 warmup + 50 measured iterations |
| Cache benchmark | Measures the pull-through response cache | Star Wars demo with 50,005 characters, `layer-gateway` Rust/Axum proxy, Aerospike Enterprise, Docker, 20 measured cache-hit iterations |

The absolute "direct Viaduct" numbers differ between the two runs because the JVM pressure benchmark deliberately constrains heap to expose GC behavior, while the cache benchmark measures proxy/cache overhead in Docker. The shape of the result is consistent: nested fan-out is expensive on the direct path, while cache hits stay near 1ms.

## Lead Result: Cache Hit Latency Is Flat

The cache key is a SHA-256 hash of the GraphQL request body. On a hit, the gateway returns serialized response bytes from Aerospike instead of invoking Viaduct resolvers.

```
---------------------------------------------------------------------------------------------------------
GRAPHQL CACHE HIT BENCHMARK (50K dataset, Aerospike NVMe cache)
---------------------------------------------------------------------------------------------------------
Query                                        Direct (Viaduct)    Cache Hit    Speedup
---------------------------------------------------------------------------------------------------------
flat: 100 chars, name                                  6.3ms        932us         6x
flat: 100 chars, all scalars                           6.7ms        989us         6x
depth-1: 100 + homeworld                              22.1ms        1.0ms        21x
depth-1: 100 + homeworld + species                    14.7ms        923us        16x
depth-2: 100 + species.homeworld                      10.6ms        979us        10x
depth-2: 100 + homeworld.residents(5)                 12.0ms        1.0ms        11x
depth-3: 100 + hw.residents.species                   11.2ms        985us        11x
fanout: 500 + homeworld + species                     28.3ms        1.1ms        24x
kitchen sink: 50 + all resolvers                       6.4ms        1.0ms         5x
---------------------------------------------------------------------------------------------------------
```

Key observations:

- Cache hits land between **923us and 1.1ms** across flat, nested, and fan-out queries.
- Query depth stops mattering on the hit path because no resolver tree is built.
- Cache misses still pay the direct Viaduct cost plus the proxy/Aerospike write path; the table focuses on the steady-state hit path.
- Cached responses avoid resolver invocation, transient object allocation, batch lookup work, and object-tree-to-JSON serialization.

## Why The Direct Path Tails

Under a 128MB heap, direct Viaduct latency grows with resolver fan-out and allocation pressure:

| Query Shape | Mean | P99 | Ops/s |
|-------------|------|-----|-------|
| flat: list(100) name | 2.84ms | 5.55ms | 352.5 |
| flat: list(100) all scalars | 6.83ms | 14.72ms | 146.5 |
| depth1: +homeworld | 13.85ms | 19.75ms | 72.2 |
| depth1: +homeworld +species | 22.00ms | 28.18ms | 45.5 |
| depth2: +species.homeworld | 13.34ms | 21.54ms | 75.0 |
| depth2: +homeworld.residents | 13.49ms | 18.21ms | 74.1 |
| depth3: hw.residents.species | 14.76ms | 19.79ms | 67.8 |
| depth3: films.chars.hw.res | 8.71ms | 15.29ms | 114.8 |
| fanout: 500 + depth1 | 183.61ms | 215.21ms | 5.4 |
| computed: filmCount + richSummary | 18.40ms | 25.45ms | 54.3 |
| kitchen sink: all resolvers | 25.53ms | 33.91ms | 39.2 |

Fan-out dominates. A 500-item listing query that resolves two related entities per row is enough to push direct execution to **184ms mean** and **5.4 ops/s** on the constrained heap. This is the important miss-path risk.

GC explains the tail:

```
----------------------------------------------------------------------
MEMORY & GC REPORT
----------------------------------------------------------------------
  Heap used:          78.5 MB
  Heap committed:    128.0 MB
  Heap max:          128.0 MB
----------------------------------------------------------------------
  GC Collectors:
    G1 Young Generation             collections:  719   time:   1105 ms
    G1 Concurrent GC                collections:  300   time:    486 ms
    G1 Old Generation               collections:   34   time:   1029 ms
----------------------------------------------------------------------
  Memory Snapshots (during scaled tests):
    After 50,000 insert: heap delta = 14.7 MB (60.4 -> 75.0 MB)
    After 50K benchmarks: heap = 69.9 MB
----------------------------------------------------------------------
```

Across 32 seconds of benchmark execution, GC consumed **2.6 seconds**. The stored data is not the main problem: 50K characters add only **14.7 MB**, or about **290 bytes per entity**. The tail comes from transient per-request allocation: resolver objects, intermediate collections, lookup results, and serialization buffers.

Even the flat `name` query shows the effect: **2.68ms P50 to 5.55ms P99**, despite doing almost no resolver work.

## What The Cache Changes

The cache changes repeat reads from "execute the resolver graph again" to "read serialized bytes by key":

| Cost Driver | Direct Viaduct | Cache Hit |
|-------------|----------------|-----------|
| Resolver invocations | Hundreds to thousands per request | 0 |
| Transient JVM allocation | Resolver objects, collections, serialization buffers | Near zero in Viaduct |
| Query depth impact | Increases latency and GC pressure | Mostly irrelevant |
| Fan-out impact | Can dominate latency and throughput | Mostly payload-size bound |
| JVM old-gen pressure | Visible under load | Avoided for cached reads |

This does not make cache misses free. It changes the steady-state read path when requests repeat, which is exactly where listing pages, search results, detail pages, and common persisted queries spend most of their volume.

## Why NVMe Fits Better Than RAM-Only Cache

The queries worth caching are the nested and fan-out queries, and those produce a large key space. A 50KB average response across 40M active query/entity combinations is roughly 2TB of response data. That is not a good fit for an in-process LRU or a memory-only cache.

An NVMe-backed cache can hold the long tail on one node with about 1ms reads. Persistence also changes warming behavior: deploys and process restarts do not erase the working set, so warming is cumulative instead of starting over after each rollout.

For stable, read-heavy data:

- Pull-through fills entries on first miss.
- Query-history replay can warm a new region or first deploy.
- Tiered warming can prioritize the top query shapes and entities before broad traffic.
- Mutation-aware invalidation can purge only entries affected by changed types or entities.

The last point is the correctness boundary. Long-lived entries are safe only when writes invalidate the affected responses.

### Cache Invalidation: Current Status

Neither the demo apps nor the layer-gateway implement cache invalidation today. Mutations bypass the cache and always hit Viaduct directly, but cached read responses can go stale after a mutation lands. Several invalidation strategies are apparent:

1. **Schema-static invalidation.** The GraphQL schema statically declares which types a mutation can affect (via its return type and reachable types). On any mutation, the cache layer can invalidate all entries whose query text references those types. Over-invalidates, but correct and requires no Viaduct engine changes.

2. **Response-derived invalidation.** The mutation response contains the affected entity IDs (e.g., `Review:42`, `Product:7`). The layer-gateway already sees the response on its way back through the proxy — it can extract IDs and purge matching cache entries. More precise than schema-static; still requires no engine changes.

3. **Engine-emitted invalidation metadata.** A new SPI (e.g., `MutationObserver`) on the Viaduct instrumentation interface could emit the set of types and entity IDs resolved during mutation execution. This gives the cache layer exact invalidation signals without parsing response JSON.

All three approaches benefit from a **reverse index** in the cache layer mapping `(type, entity_id) → Set<cache_key>`, built at cache-write time by extracting entity IDs from miss responses. This makes invalidation O(affected entities) rather than O(cache size).

## Rollout Shape

Because the cache layer is a transparent proxy, cutover can be gradual:

1. Run in shadow mode and mirror live reads to warm the cache.
2. Watch hit rate and correctness before serving responses.
3. Shift read traffic gradually through the cache.
4. Keep Viaduct as the miss path and mutation handler.

This avoids a user-visible cold start. Once warm, the NVMe working set survives cache-layer deploys and restarts.

## Reproducing The Cache Benchmark

```bash
# 1. Start Viaduct with 50K dataset
docker run --rm -d \
  -v $(pwd)/../..:/app \
  -v viaduct-m2:/root/.m2 \
  -w /app/demoapps/starwars \
  -p 8082:8080 \
  -e USE_MAVEN_LOCAL=true \
  -e SEED_CHARACTERS=50000 \
  --name viaduct \
  eclipse-temurin:21-jdk \
  sh -c "./gradlew run --no-daemon"

# 2. Start mesh cache layer (from hev/mesh repo)
docker compose --profile graphql-cache up -d

# 3. Run benchmark
./scripts/bench-graphql-cache.sh
```

## Reproducing The JVM-Only Benchmark

```bash
cd demoapps/starwars
export JAVA_HOME=/path/to/jdk21

# From the Viaduct root:
./gradlew --include-build demoapps/starwars :starwars:test --tests '*ReadWriteBenchmarkTest*'

# Docker constrained runs (512MB / 256MB / 128MB containers):
cd demoapps/starwars
docker compose -f docker-compose.benchmark.yml up bench-512m
docker compose -f docker-compose.benchmark.yml up bench-256m
docker compose -f docker-compose.benchmark.yml up bench-128m
```
