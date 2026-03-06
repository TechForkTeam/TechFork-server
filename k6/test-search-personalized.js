import { searchPersonalizedTest } from './scenarios/search-personalized.js';
import { handleSummary } from './summarize.js';

// AUTH_TOKEN 필수: k6 run --env AUTH_TOKEN=<JWT> --env BASE_URL=http://... test-search-personalized.js

export const options = {
    scenarios: {
        search_personalized: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },
                { duration: '2m',  target: 10 },
                { duration: '30s', target: 0  },
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
export { handleSummary };