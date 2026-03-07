import { searchPersonalizedTest } from './scenarios/search-personalized.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// AUTH_TOKEN 필수: k6 run --env AUTH_TOKEN=<JWT> --env BASE_URL=http://... test-search-personalized.js

const TARGET_VU = parseInt(__ENV.VU || '10');

export const options = {
    summaryTrendStats: ['med', 'p(95)', 'p(99)'],
    scenarios: {
        search_personalized: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: TARGET_VU },
                { duration: '2m',  target: TARGET_VU },
                { duration: '30s', target: 0          },
            ],
            exec: 'searchPersonalizedTest',
        },
    },

    thresholds: {
        'http_req_duration{scenario:search_personalized}': ['p(95)<800', 'p(99)<1500'],
        'http_req_failed':                                 ['rate<0.01'],
    },
};

export { searchPersonalizedTest };

export function handleSummary(data) {
    return {
        'summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}