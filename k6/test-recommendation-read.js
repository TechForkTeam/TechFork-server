import { recommendReadTest } from './scenarios/recommendation-read.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// AUTH_TOKEN 필수: k6 run --env AUTH_TOKEN=<JWT> --env BASE_URL=http://... test-recommendation-read.js

const TARGET_VU = parseInt(__ENV.VU || '20');

export const options = {
    summaryTrendStats: ['med', 'p(95)', 'p(99)'],
    scenarios: {
        recommendation_read: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: TARGET_VU },
                { duration: '2m',  target: TARGET_VU },
                { duration: '30s', target: 0          },
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

export function handleSummary(data) {
    return {
        'summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}