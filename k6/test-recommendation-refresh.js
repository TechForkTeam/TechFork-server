import { recommendRefreshTest } from './scenarios/recommendation-refresh.js';
import { handleSummary } from './summarize.js';

// AUTH_TOKEN 필수: k6 run --env AUTH_TOKEN=<JWT> --env BASE_URL=http://... test-recommendation-refresh.js

export const options = {
    scenarios: {
        recommendation_refresh: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 5 },
                { duration: '2m',  target: 5 },
                { duration: '30s', target: 0  },
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
export { handleSummary };