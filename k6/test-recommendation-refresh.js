import { recommendRefreshTest } from './scenarios/recommendation-refresh.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// AUTH_TOKEN 필수: k6 run --env AUTH_TOKEN=<JWT> --env BASE_URL=http://... test-recommendation-refresh.js

const TARGET_VU = parseInt(__ENV.VU || '5');

export const options = {
    summaryTrendStats: ['med', 'p(95)', 'p(99)'],
    scenarios: {
        recommendation_refresh: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: TARGET_VU },
                { duration: '2m',  target: TARGET_VU },
                { duration: '30s', target: 0          },
            ],
            exec: 'recommendRefreshTest',
        },
    },

    thresholds: {
        'http_req_duration{scenario:recommendation_refresh}': ['p(95)<2000', 'p(99)<4000'],
        'http_req_failed':                                    ['rate<0.01'],
    },
};

export { recommendRefreshTest };

export function handleSummary(data) {
    return {
        'summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}