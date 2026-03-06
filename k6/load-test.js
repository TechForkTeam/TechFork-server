import { searchAnonymousTest } from './scenarios/search-anonymous.js';
import { searchPersonalizedTest } from './scenarios/search-personalized.js';
import { recommendReadTest } from './scenarios/recommendation-read.js';
import { recommendRefreshTest } from './scenarios/recommendation-refresh.js';
import { crudTest } from './scenarios/crud.js';

export const options = {
    scenarios: {
        search_anonymous: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },
                { duration: '2m', target: 10 },
                { duration: '30s', target: 0 },
            ],
            exec: 'searchAnonymousTest',
        },

        search_personalized: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 5 },
                { duration: '2m', target: 5 },
                { duration: '30s', target: 0 },
            ],
            exec: 'searchPersonalizedTest',
        },

        recommendation_read: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },
                { duration: '2m', target: 10 },
                { duration: '30s', target: 0 },
            ],
            exec: 'recommendReadTest',
        },

        recommendation_refresh: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 3 },
                { duration: '2m', target: 3 },
                { duration: '30s', target: 0 },
            ],
            exec: 'recommendRefreshTest',
        },

        crud: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },
                { duration: '2m', target: 20 },
                { duration: '30s', target: 0 },
            ],
            exec: 'crudTest',
        },
    },

    thresholds: {
        'http_req_duration{scenario:search_anonymous}':       ['p(95)<500',  'p(99)<1000'],
        'http_req_duration{scenario:search_personalized}':    ['p(95)<800',  'p(99)<1500'],
        'http_req_duration{scenario:recommendation_read}':    ['p(95)<300',  'p(99)<600'],
        'http_req_duration{scenario:recommendation_refresh}': ['p(95)<2000', 'p(99)<4000'],
        'http_req_duration{scenario:crud}':                   ['p(95)<200',  'p(99)<400'],
        'http_req_failed':                                    ['rate<0.01'],
    },
};

export { searchAnonymousTest } from './scenarios/search-anonymous.js';
export { searchPersonalizedTest } from './scenarios/search-personalized.js';
export { recommendReadTest } from './scenarios/recommendation-read.js';
export { recommendRefreshTest } from './scenarios/recommendation-refresh.js';
export { crudTest } from './scenarios/crud.js';
export { handleSummary } from './summarize.js';