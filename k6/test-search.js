import { searchAnonymousTest } from './scenarios/search-anonymous.js';
import { searchPersonalizedTest } from './scenarios/search-personalized.js';
import { handleSummary } from './summarize.js';

export const options = {
    scenarios: {
        search_anonymous: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },
                { duration: '2m',  target: 10 },
                { duration: '30s', target: 0  },
            ],
            exec: 'searchAnonymousTest',
        },

        search_personalized: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 5 },
                { duration: '2m',  target: 5 },
                { duration: '30s', target: 0 },
            ],
            exec: 'searchPersonalizedTest',
        },
    },

    thresholds: {
        'http_req_duration{scenario:search_anonymous}':    ['p(95)<500',  'p(99)<1000'],
        'http_req_duration{scenario:search_personalized}': ['p(95)<800',  'p(99)<1500'],
        'http_req_failed':                                 ['rate<0.01'],
    },
};

export { searchAnonymousTest, searchPersonalizedTest };
export { handleSummary };
