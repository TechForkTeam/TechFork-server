import { crudTest } from './scenarios/crud.js';
import { handleSummary } from './summarize.js';

export const options = {
    scenarios: {
        crud: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 30 },
                { duration: '2m',  target: 30 },
                { duration: '30s', target: 0  },
            ],
            exec: 'crudTest',
        },
    },

    thresholds: {
        'http_req_duration{scenario:crud}': ['p(95)<200', 'p(99)<400'],
        'http_req_failed':                  ['rate<0.01'],
    },
};

export { crudTest };
export { handleSummary };