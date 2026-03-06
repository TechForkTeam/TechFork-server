/**
 * 통합 시나리오 테스트 (실제 트래픽 비율 시뮬레이션)
 *
 * 목적: 튜닝 완료 후 실제 유저 행동 비율로 서버 자원 분배 확인
 * 비율: 목록/상세 조회 70% / 검색 20% / 추천 10%
 *
 * 실행:
 *   k6 run --env BASE_URL=http://<SERVER> --env AUTH_TOKEN=<JWT> load-test.js
 */

import { searchAnonymousTest } from './scenarios/search-anonymous.js';
import { searchPersonalizedTest } from './scenarios/search-personalized.js';
import { recommendReadTest } from './scenarios/recommendation-read.js';
import { crudTest } from './scenarios/crud.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// 총 45 VU 기준
// crud: 31 VU (69%) / search_anonymous: 7 VU (16%) / search_personalized: 3 VU (7%) / recommendation_read: 4 VU (9%)
export const options = {
    scenarios: {
        crud: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 31 },
                { duration: '2m',  target: 31 },
                { duration: '30s', target: 0  },
            ],
            exec: 'crudTest',
        },

        search_anonymous: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 7 },
                { duration: '2m',  target: 7 },
                { duration: '30s', target: 0 },
            ],
            exec: 'searchAnonymousTest',
        },

        search_personalized: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 3 },
                { duration: '2m',  target: 3 },
                { duration: '30s', target: 0 },
            ],
            exec: 'searchPersonalizedTest',
        },

        recommendation_read: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 5 },
                { duration: '2m',  target: 5 },
                { duration: '30s', target: 0 },
            ],
            exec: 'recommendReadTest',
        },
    },

    thresholds: {
        'http_req_duration{scenario:crud}':                ['p(95)<200',  'p(99)<400'],
        'http_req_duration{scenario:search_anonymous}':    ['p(95)<500',  'p(99)<1000'],
        'http_req_duration{scenario:search_personalized}': ['p(95)<800',  'p(99)<1500'],
        'http_req_duration{scenario:recommendation_read}': ['p(95)<200',  'p(99)<400'],
        'http_req_failed':                                 ['rate<0.01'],
    },
};

export { crudTest, searchAnonymousTest, searchPersonalizedTest, recommendReadTest };

export function handleSummary(data) {
    return {
        'summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}