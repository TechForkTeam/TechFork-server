import { recommendReadTest } from './scenarios/recommendation-read.js';
import { handleSummary } from './summarize.js';

// AUTH_TOKEN 필수: k6 run --env AUTH_TOKEN=<JWT> --env BASE_URL=http://... test-recommendation-read.js

export const options = {
    scenarios: {
        recommendation_read: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },
                { duration: '2m',  target: 20 },
                { duration: '30s', target: 0  },
            ],
            exec: 'recommendReadTest',
        },
    },

    thresholds: {
        'http_req_duration{scenario:recommendation_read}': ['p(95)<200', 'p(99)<400'],
        'http_req_failed':                                 ['rate<0.01'],
    },
};

export { recommendReadTest };
export { handleSummary };