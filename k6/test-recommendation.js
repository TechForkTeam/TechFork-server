import { recommendReadTest } from './scenarios/recommendation-read.js';
import { recommendRefreshTest } from './scenarios/recommendation-refresh.js';
import { handleSummary } from './summarize.js';

export const options = {
    scenarios: {
        recommendation_read: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 10 },
                { duration: '2m',  target: 10 },
                { duration: '30s', target: 0  },
            ],
            exec: 'recommendReadTest',
        },

        recommendation_refresh: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 3 },
                { duration: '2m',  target: 3 },
                { duration: '30s', target: 0 },
            ],
            exec: 'recommendRefreshTest',
        },
    },

    thresholds: {
        'http_req_duration{scenario:recommendation_read}':    ['p(95)<300',  'p(99)<600'],
        'http_req_duration{scenario:recommendation_refresh}': ['p(95)<2000', 'p(99)<4000'],
        'http_req_failed':                                    ['rate<0.01'],
    },
};

export { recommendReadTest, recommendRefreshTest };
export { handleSummary };
