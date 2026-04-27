package com.example.starwars.service.test

import com.example.starwars.modules.filmography.characters.models.Character
import com.example.starwars.modules.filmography.characters.models.CharacterRepository
import com.fasterxml.jackson.databind.JsonNode
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadLocalRandom
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.lang.management.ManagementFactory

/**
 * GraphQL read/write speed test harness.
 *
 * Measures throughput and latency for reads (queries) and writes (mutations)
 * at different complexity levels. Boots the Star Wars Micronaut server in-process
 * and runs timed benchmarks across multiple scenarios.
 *
 * Run with: ./gradlew :starwars:test --tests '*ReadWriteBenchmarkTest*'
 */
@MicronautTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ReadWriteBenchmarkTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Inject
    lateinit var characterRepository: CharacterRepository

    companion object {
        private const val WARMUP_ITERATIONS = 5
        private const val MEASUREMENT_ITERATIONS = 50
        private const val CONCURRENCY_LEVEL = 10

        private val results = mutableListOf<BenchmarkResult>()
        private val scaledResults = mutableListOf<BenchmarkResult>()
        private val cachedResults = mutableListOf<Pair<BenchmarkResult, BenchmarkResult>>() // uncached, cached
        private val memorySnapshots = mutableListOf<String>()

        data class BenchmarkResult(
            val name: String,
            val measurements: List<Double>, // latencies in ms
        ) {
            val min get() = measurements.min()
            val max get() = measurements.max()
            val mean get() = measurements.average()
            val p50 get() = percentile(50.0)
            val p95 get() = percentile(95.0)
            val p99 get() = percentile(99.0)
            val opsPerSecond get() = 1000.0 / mean

            private fun percentile(p: Double): Double {
                val sorted = measurements.sorted()
                val index = (p / 100.0 * (sorted.size - 1))
                val lower = index.toInt()
                val upper = lower + 1
                if (upper >= sorted.size) return sorted.last()
                val fraction = index - lower
                return sorted[lower] + fraction * (sorted[upper] - sorted[lower])
            }
        }

        fun printResults() {
            val header = String.format(
                "%-30s %8s %8s %8s %8s %8s %8s %10s",
                "Benchmark", "Min", "Max", "Mean", "P50", "P95", "P99", "Ops/s"
            )
            val separator = "-".repeat(header.length)
            println("\n$separator")
            println("GRAPHQL BENCHMARK RESULTS ($WARMUP_ITERATIONS warmup, $MEASUREMENT_ITERATIONS measured)")
            println(separator)
            println(header)
            println(separator)
            for (result in results) {
                println(
                    String.format(
                        "%-30s %7.2fms %7.2fms %7.2fms %7.2fms %7.2fms %7.2fms %9.1f",
                        result.name, result.min, result.max, result.mean,
                        result.p50, result.p95, result.p99, result.opsPerSecond
                    )
                )
            }
            println(separator)
        }

        fun printScaledResults() {
            if (scaledResults.isEmpty()) return
            val header = String.format(
                "%-40s %8s %8s %8s %8s %8s %8s %10s",
                "Scaled Benchmark", "Min", "Max", "Mean", "P50", "P95", "P99", "Ops/s"
            )
            val separator = "-".repeat(header.length)
            println("\n$separator")
            println("SCALED DATASET BENCHMARK RESULTS ($WARMUP_ITERATIONS warmup, $MEASUREMENT_ITERATIONS measured)")
            println(separator)
            println(header)
            println(separator)
            for (result in scaledResults) {
                println(
                    String.format(
                        "%-40s %7.2fms %7.2fms %7.2fms %7.2fms %7.2fms %7.2fms %9.1f",
                        result.name, result.min, result.max, result.mean,
                        result.p50, result.p95, result.p99, result.opsPerSecond
                    )
                )
            }
            println(separator)
        }

        fun printCachedComparison() {
            if (cachedResults.isEmpty()) return
            val header = String.format(
                "%-34s %10s %10s %10s %10s %10s",
                "Query", "Uncached", "Cached", "Speedup", "P99 Unc.", "P99 Cache"
            )
            val separator = "-".repeat(header.length)
            println("\n$separator")
            println("CACHE SIMULATION: UNCACHED vs CACHED (50K dataset, 128MB heap)")
            println(separator)
            println(header)
            println(separator)
            for ((uncached, cached) in cachedResults) {
                val speedup = uncached.mean / cached.mean
                val cachedUs = cached.mean * 1000.0
                val cachedP99Us = cached.p99 * 1000.0
                println(
                    String.format(
                        "%-34s %9.2fms %8.0f\u00B5s %9.0fx %9.2fms %8.0f\u00B5s",
                        uncached.name, uncached.mean, cachedUs, speedup,
                        uncached.p99, cachedP99Us
                    )
                )
            }
            println(separator)
        }

        fun printMemoryAndGcReport() {
            val runtime = Runtime.getRuntime()
            val mb = 1024.0 * 1024.0
            val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / mb
            val committedMb = runtime.totalMemory() / mb
            val maxMb = runtime.maxMemory() / mb

            val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()

            val separator = "-".repeat(70)
            println("\n$separator")
            println("MEMORY & GC REPORT")
            println(separator)
            println(String.format("  Heap used:      %8.1f MB", usedMb))
            println(String.format("  Heap committed: %8.1f MB", committedMb))
            println(String.format("  Heap max:       %8.1f MB", maxMb))
            println(separator)
            println("  GC Collectors:")
            for (gc in gcBeans) {
                println(String.format("    %-30s  collections: %4d   time: %6d ms",
                    gc.name, gc.collectionCount, gc.collectionTime))
            }
            println(separator)

            if (memorySnapshots.isNotEmpty()) {
                println("\n  Memory Snapshots (during scaled tests):")
                for (snapshot in memorySnapshots) {
                    println("    $snapshot")
                }
                println(separator)
            }
        }

        fun captureHeapUsedMb(): Double {
            System.gc() // best-effort to get a cleaner reading
            val runtime = Runtime.getRuntime()
            return (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
        }
    }

    private fun benchmark(name: String, action: () -> Unit) {
        // Warmup
        repeat(WARMUP_ITERATIONS) { action() }

        // Measurement
        val measurements = mutableListOf<Double>()
        repeat(MEASUREMENT_ITERATIONS) {
            val start = System.nanoTime()
            action()
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            measurements.add(elapsed)
        }

        val result = BenchmarkResult(name, measurements)
        results.add(result)
    }

    private fun scaledBenchmark(name: String, action: () -> Unit) {
        // Warmup
        repeat(WARMUP_ITERATIONS) { action() }

        // Measurement
        val measurements = mutableListOf<Double>()
        repeat(MEASUREMENT_ITERATIONS) {
            val start = System.nanoTime()
            action()
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            measurements.add(elapsed)
        }

        val result = BenchmarkResult(name, measurements)
        scaledResults.add(result)
    }

    // --- Read Benchmarks ---

    @Test
    @Order(1)
    fun `read - simple single character`() {
        benchmark("Simple read (1 char)") {
            val response = client.executeGraphQLQuery("""{ allCharacters(limit: 1) { name } }""")
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }
    }

    @Test
    @Order(2)
    fun `read - list of characters`() {
        benchmark("List read (10 chars)") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 10) { name birthYear } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }
    }

    @Test
    @Order(3)
    fun `read - nested with homeworld and species`() {
        benchmark("Nested read (5 chars)") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 5) { name homeworld { name } species { name } } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }
    }

    @Test
    @Order(4)
    fun `read - batch resolver fields`() {
        benchmark("Batch resolver read") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 5) { name filmCount richSummary } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }
    }

    @Test
    @Order(5)
    fun `read - deep nested films with characters and homeworld`() {
        benchmark("Deep nested read") {
            val response = client.executeGraphQLQuery(
                """{ allFilms(limit: 3) { title mainCharacters { name homeworld { name } } } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }
    }

    // --- Write Benchmarks ---

    @Test
    @Order(6)
    fun `write - create character`() {
        benchmark("Create mutation") {
            val response = client.executeGraphQLQueryWithAdminAccess("""
                mutation {
                    createCharacter(input: { name: "BenchChar", birthYear: "100BBY" }) {
                        id name
                    }
                }
            """.trimIndent())
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }
    }

    @Test
    @Order(7)
    fun `write - update character name`() {
        // Create a character to update repeatedly
        val createResponse = client.executeGraphQLQueryWithAdminAccess("""
            mutation {
                createCharacter(input: { name: "UpdateTarget", birthYear: "50BBY" }) {
                    id
                }
            }
        """.trimIndent())
        val charId = createResponse.path("data").path("createCharacter").path("id").asText()

        var counter = 0
        benchmark("Update mutation") {
            counter++
            val response = client.executeGraphQLQueryWithAdminAccess("""
                mutation {
                    updateCharacterName(id: "$charId", name: "Updated-$counter") {
                        id name
                    }
                }
            """.trimIndent())
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }
    }

    @Test
    @Order(8)
    fun `write - delete character`() {
        benchmark("Delete mutation") {
            // Create then delete each iteration
            val createResponse = client.executeGraphQLQueryWithAdminAccess("""
                mutation {
                    createCharacter(input: { name: "DeleteTarget" }) {
                        id
                    }
                }
            """.trimIndent())
            val charId = createResponse.path("data").path("createCharacter").path("id").asText()

            val response = client.executeGraphQLQueryWithAdminAccess("""
                mutation { deleteCharacter(id: "$charId") }
            """.trimIndent())
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }
    }

    @Test
    @Order(9)
    fun `write - mixed read after write`() {
        benchmark("Read-after-write") {
            // Create a character
            val createResponse = client.executeGraphQLQueryWithAdminAccess("""
                mutation {
                    createCharacter(input: { name: "ReadAfterWrite", birthYear: "30ABY" }) {
                        id
                    }
                }
            """.trimIndent())
            val charId = createResponse.path("data").path("createCharacter").path("id").asText()

            // Immediately query it back
            val readResponse = client.executeGraphQLQuery("""
                { searchCharacter(search: { byId: "$charId" }) { name birthYear } }
            """.trimIndent())
            assert(readResponse.path("errors").isMissingNode || readResponse.path("errors").isNull)
        }
    }

    // --- Concurrency Benchmark ---

    @Test
    @Order(10)
    fun `concurrent - parallel reads`() {
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            client.executeGraphQLQuery("""{ allCharacters(limit: 1) { name } }""")
        }

        val measurements = mutableListOf<Double>()
        repeat(MEASUREMENT_ITERATIONS) {
            val start = System.nanoTime()
            runBlocking {
                val jobs = (1..CONCURRENCY_LEVEL).map {
                    async(Dispatchers.IO) {
                        client.executeGraphQLQuery("""{ allCharacters(limit: 5) { name birthYear } }""")
                    }
                }
                jobs.awaitAll()
            }
            val elapsed = (System.nanoTime() - start) / 1_000_000.0
            measurements.add(elapsed)
        }

        val result = BenchmarkResult("Concurrent reads (${CONCURRENCY_LEVEL}x)", measurements)
        results.add(result)
    }

    // --- Scaled Dataset Benchmarks ---

    @Test
    @Order(20)
    fun `scaled - 50K dataset with nesting depth ladder`() {
        val tier = 50_000
        val baselineHeap = captureHeapUsedMb()

        val syntheticIds = bulkInsertCharacters(tier)

        val afterInsertHeap = captureHeapUsedMb()
        val delta = afterInsertHeap - baselineHeap
        memorySnapshots.add(String.format("After %,d insert: heap delta = %.1f MB (%.1f -> %.1f MB)",
            tier, delta, baselineHeap, afterInsertHeap))

        // --- Flat baseline (depth 0) ---
        scaledBenchmark("flat: list(100) name") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 100) { name } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        scaledBenchmark("flat: list(100) all scalars") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 100) { name birthYear eyeColor gender hairColor height mass created edited } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        // --- Depth 1: character → homeworld/species ---
        scaledBenchmark("depth1: +homeworld") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 100) { name homeworld { name } } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        scaledBenchmark("depth1: +homeworld +species") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 100) { name homeworld { name } species { name } } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        // --- Depth 2: character → species → homeworld ---
        scaledBenchmark("depth2: +species.homeworld") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 100) { name species { name homeworld { name } } } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        scaledBenchmark("depth2: +homeworld.residents") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 100) { name homeworld { name residents(limit: 5) { name } } } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        // --- Depth 3: character → homeworld → residents → species ---
        scaledBenchmark("depth3: hw.residents.species") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 100) { name homeworld { name residents(limit: 5) { name species { name } } } } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        // --- Depth 3 via films: film → characters → homeworld → residents ---
        scaledBenchmark("depth3: films.chars.hw.res") {
            val response = client.executeGraphQLQuery(
                """{ allFilms(limit: 3) { title characters(limit: 20) { name homeworld { name residents(limit: 5) { name } } } } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        // --- Fan-out stress: large limit × deep nesting ---
        scaledBenchmark("fanout: 500 × depth1") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 500) { name homeworld { name } species { name } } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        // --- Computed fields at scale (batch resolvers generate extra objects) ---
        scaledBenchmark("computed: filmCount+richSummary") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 100) { name filmCount richSummary } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        // --- Kitchen sink: every resolver layer at once ---
        scaledBenchmark("kitchen sink: all resolvers") {
            val response = client.executeGraphQLQuery(
                """{ allCharacters(limit: 50) { name birthYear displayName displaySummary appearanceDescription filmCount richSummary homeworld { name diameter population } species { name classification homeworld { name } } } }"""
            )
            assert(response.path("errors").isMissingNode || response.path("errors").isNull)
        }

        // GC snapshot after the beating
        val postBenchHeap = captureHeapUsedMb()
        memorySnapshots.add(String.format("After 50K benchmarks: heap = %.1f MB", postBenchHeap))

        // Cleanup
        bulkDeleteCharacters(syntheticIds)
    }

    // --- Cache Simulation Benchmarks ---

    @Test
    @Order(30)
    fun `cached - simulated query cache vs uncached at 50K`() {
        val tier = 50_000
        val syntheticIds = bulkInsertCharacters(tier)

        // Simulated cache: query string → serialized response
        val cache = ConcurrentHashMap<String, JsonNode>()

        // Each query is benchmarked uncached (first), then cached (second)
        val queries = listOf(
            "flat(100)" to
                """{ allCharacters(limit: 100) { name } }""",
            "all scalars(100)" to
                """{ allCharacters(limit: 100) { name birthYear eyeColor gender hairColor height mass created edited } }""",
            "depth1: hw+species" to
                """{ allCharacters(limit: 100) { name homeworld { name } species { name } } }""",
            "depth2: sp.homeworld" to
                """{ allCharacters(limit: 100) { name species { name homeworld { name } } } }""",
            "depth3: hw.res.species" to
                """{ allCharacters(limit: 100) { name homeworld { name residents(limit: 5) { name species { name } } } } }""",
            "fanout: 500 × depth1" to
                """{ allCharacters(limit: 500) { name homeworld { name } species { name } } }""",
            "kitchen sink" to
                """{ allCharacters(limit: 50) { name birthYear displayName displaySummary appearanceDescription filmCount richSummary homeworld { name diameter population } species { name classification homeworld { name } } } }""",
        )

        for ((name, query) in queries) {
            // --- Uncached: every request hits Viaduct ---
            cache.clear()
            val uncachedMeasurements = mutableListOf<Double>()
            // Warmup (uncached)
            repeat(WARMUP_ITERATIONS) {
                client.executeGraphQLQuery(query)
            }
            repeat(MEASUREMENT_ITERATIONS) {
                val start = System.nanoTime()
                val response = client.executeGraphQLQuery(query)
                val elapsed = (System.nanoTime() - start) / 1_000_000.0
                uncachedMeasurements.add(elapsed)
                assert(response.path("errors").isMissingNode || response.path("errors").isNull)
            }

            // --- Cached: first request warms, rest simulate NVMe read ---
            cache.clear()
            val cachedMeasurements = mutableListOf<Double>()
            // Warm the cache with one real request
            val warmResponse = client.executeGraphQLQuery(query)
            cache[query] = warmResponse
            // Warmup (cached path with simulated NVMe latency)
            repeat(WARMUP_ITERATIONS) {
                simulateNvmeRead()
                cache[query]!!
            }
            repeat(MEASUREMENT_ITERATIONS) {
                val start = System.nanoTime()
                simulateNvmeRead()
                val response = cache[query]!!
                val elapsed = (System.nanoTime() - start) / 1_000_000.0
                cachedMeasurements.add(elapsed)
                assert(response.path("errors").isMissingNode || response.path("errors").isNull)
            }

            val uncached = BenchmarkResult(name, uncachedMeasurements)
            val cached = BenchmarkResult(name, cachedMeasurements)
            cachedResults.add(uncached to cached)
        }

        bulkDeleteCharacters(syntheticIds)
    }

    // --- Helpers ---

    /**
     * Simulate NVMe read latency: ~50-150µs with jitter.
     * Real-world NVMe random 4K reads land in this range.
     */
    private fun simulateNvmeRead() {
        val baseNanos = 50_000L  // 50µs base
        val jitterNanos = ThreadLocalRandom.current().nextLong(0, 100_000) // 0-100µs jitter
        val targetNanos = baseNanos + jitterNanos
        val deadline = System.nanoTime() + targetNanos
        @Suppress("ControlFlowWithEmptyBody")
        while (System.nanoTime() < deadline) { /* spin-wait for sub-ms accuracy */ }
    }

    // --- Helpers for scaled tests ---

    private fun bulkInsertCharacters(count: Int): List<String> {
        val ids = mutableListOf<String>()
        val homeworldIds = listOf("1", "2", "3", "4") // existing planet IDs
        val speciesIds = listOf("1", "2")               // existing species IDs

        for (i in 1..count) {
            val char = Character(
                id = "", // will be assigned by repository
                name = "Synth-$i",
                birthYear = "${(i % 100)}BBY",
                eyeColor = "brown",
                gender = if (i % 2 == 0) "male" else "female",
                hairColor = "black",
                height = 150 + (i % 50),
                mass = 60f + (i % 40),
                homeworldId = homeworldIds[i % homeworldIds.size],
                speciesId = speciesIds[i % speciesIds.size]
            )
            val added = characterRepository.add(char)
            ids.add(added.id)
        }
        return ids
    }

    private fun bulkDeleteCharacters(ids: List<String>) {
        for (id in ids) {
            characterRepository.delete(id)
        }
    }

    // --- Print Summary ---

    @Test
    @Order(99)
    fun `print benchmark results`() {
        printResults()
        printScaledResults()
        printCachedComparison()
        printMemoryAndGcReport()
    }
}
