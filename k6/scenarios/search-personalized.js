import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, AUTH_HEADERS, SEARCH_KEYWORDS } from '../config.js';

export function searchPersonalizedTest() {
  const keyword = SEARCH_KEYWORDS[Math.floor(Math.random() * SEARCH_KEYWORDS.length)];

  const res = http.get(
    `${BASE_URL}/api/v1/search?query=${encodeURIComponent(keyword)}`,
    AUTH_HEADERS
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has results': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data !== undefined || body.content !== undefined;
      } catch {
        return false;
      }
    },
  });

  sleep(Math.random() * 2 + 1);
}
