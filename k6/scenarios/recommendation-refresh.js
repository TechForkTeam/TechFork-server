import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, AUTH_HEADERS } from '../config.js';

export function recommendRefreshTest() {
  const res = http.post(
    `${BASE_URL}/api/v1/recommendations/regenerate`,
    null,
    AUTH_HEADERS
  );

  check(res, {
    'status is 200 or 201': (r) => r.status === 200 || r.status === 201,
  });

  // 재생성은 무거운 작업이므로 호출 간격을 길게
  sleep(Math.random() * 3 + 2); // 2~5초 간격
}
