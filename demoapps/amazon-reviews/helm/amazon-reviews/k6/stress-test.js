import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { SharedArray } from 'k6/data';

const TARGET_URL = __ENV.TARGET_URL || 'http://localhost:8080/graphql';

export const options = {
  scenarios: {
    sustained_load: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.K6_RAMP_UP || '5m', target: parseInt(__ENV.K6_VUS) || 50 },
        { duration: __ENV.K6_DURATION || '3h50m', target: parseInt(__ENV.K6_VUS) || 50 },
        { duration: '5m', target: 0 },
      ],
    },
    spike_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      startTime: '1h',
      stages: [
        { duration: '30s', target: 200 },
        { duration: '2m', target: 200 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

function graphql(query, variables = {}) {
  const payload = JSON.stringify({ query, variables });
  const params = { headers: { 'Content-Type': 'application/json' } };
  return http.post(TARGET_URL, payload, params);
}

// Scenario weights
const scenarios = [
  { weight: 30, fn: simpleRead },
  { weight: 20, fn: listRead },
  { weight: 15, fn: nestedRead },
  { weight: 10, fn: batchComputed },
  { weight: 10, fn: deepTraversal },
  { weight: 10, fn: searchQuery },
  { weight: 5,  fn: mutationQuery },
];

// Build cumulative weights for selection
const cumulativeWeights = [];
let cumulative = 0;
for (const s of scenarios) {
  cumulative += s.weight;
  cumulativeWeights.push({ cumWeight: cumulative, fn: s.fn });
}

function selectScenario() {
  const r = Math.random() * 100;
  for (const s of cumulativeWeights) {
    if (r < s.cumWeight) return s.fn;
  }
  return simpleRead;
}

export default function () {
  const scenario = selectScenario();
  scenario();
  sleep(randomIntBetween(1, 3) / 10); // 0.1-0.3s between requests
}

// --- Scenario implementations ---

function simpleRead() {
  const id = btoa(`Product:${randomIntBetween(1, 100000)}`);
  const res = graphql(`
    query SimpleRead($id: ID!) {
      product(id: $id) {
        title
        price
        averageRating
      }
    }
  `, { id });
  check(res, {
    'simple_read: status 200': (r) => r.status === 200,
    'simple_read: no errors': (r) => !JSON.parse(r.body).errors,
  });
}

function listRead() {
  const res = graphql(`
    query ListRead {
      allProducts(first: 50) {
        edges {
          node {
            title
            price
          }
        }
        pageInfo {
          hasNextPage
          endCursor
        }
        totalCount
      }
    }
  `);
  check(res, {
    'list_read: status 200': (r) => r.status === 200,
    'list_read: has edges': (r) => {
      const body = JSON.parse(r.body);
      return body.data && body.data.allProducts && body.data.allProducts.edges;
    },
  });
}

function nestedRead() {
  const id = btoa(`Product:${randomIntBetween(1, 100000)}`);
  const res = graphql(`
    query NestedRead($id: ID!) {
      product(id: $id) {
        category {
          name
          parent {
            name
          }
        }
        reviews(first: 10) {
          edges {
            node {
              rating
              text
              author {
                displayName
              }
            }
          }
        }
      }
    }
  `, { id });
  check(res, {
    'nested_read: status 200': (r) => r.status === 200,
  });
}

function batchComputed() {
  const id = btoa(`Product:${randomIntBetween(1, 100000)}`);
  const res = graphql(`
    query BatchComputed($id: ID!) {
      product(id: $id) {
        ratingSummary {
          average
          distribution
        }
        boughtTogether {
          title
          price
        }
      }
    }
  `, { id });
  check(res, {
    'batch_computed: status 200': (r) => r.status === 200,
  });
}

function deepTraversal() {
  const id = btoa(`Category:${randomIntBetween(1, 1000)}`);
  const res = graphql(`
    query DeepTraversal($id: ID!) {
      category(id: $id) {
        products(first: 20) {
          edges {
            node {
              reviews(first: 5) {
                edges {
                  node {
                    author {
                      reviews(first: 3) {
                        edges {
                          node {
                            product {
                              title
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  `, { id });
  check(res, {
    'deep_traversal: status 200': (r) => r.status === 200,
  });
}

function searchQuery() {
  const terms = ['wireless', 'headphones', 'charger', 'cable', 'laptop', 'mouse', 'keyboard', 'monitor', 'speaker', 'camera'];
  const term = terms[randomIntBetween(0, terms.length - 1)];
  const res = graphql(`
    query Search($query: String!) {
      searchProducts(query: $query, limit: 20) {
        title
        price
        reviews(first: 3) {
          edges {
            node {
              rating
            }
          }
        }
      }
    }
  `, { query: term });
  check(res, {
    'search: status 200': (r) => r.status === 200,
  });
}

function mutationQuery() {
  const productId = btoa(`Product:${randomIntBetween(1, 100000)}`);
  const res = graphql(`
    mutation CreateReview($input: CreateReviewInput!) {
      createReview(input: $input) {
        id
        product {
          averageRating
        }
      }
    }
  `, {
    input: {
      productId: productId,
      rating: randomIntBetween(1, 5),
      title: 'k6 stress test review',
      text: 'This is an automated review from the k6 stress test.',
      verified: false,
    },
  });
  check(res, {
    'mutation: status 200': (r) => r.status === 200,
  });
}
