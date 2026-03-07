import { searchAnonymousTest } from './scenarios/search-anonymous.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const TARGET_VU = parseInt(__ENV.VU || '15');

export const options = {
    summaryTrendStats: ['med', 'p(95)', 'p(99)'],
    scenarios: {
        search_anonymous: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: TARGET_VU },
                { duration: '2m',  target: TARGET_VU },
                { duration: '30s', target: 0          },
            ],
            exec: 'searchAnonymousTest',
        },
    },

    thresholds: {
        'http_req_duration{scenario:search_anonymous}': ['p(95)<500', 'p(99)<1000'],
        'http_req_failed':                              ['rate<0.01'],
    },
};

export { searchAnonymousTest };

export function handleSummary(data) {
    return {
        'summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}