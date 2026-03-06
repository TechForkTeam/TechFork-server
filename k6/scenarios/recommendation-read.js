import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, AUTH_HEADERS } from '../config.js';

export function recommendReadTest() {
  const res = http.get(`${BASE_URL}/api/v1/recommendations`, AUTH_HEADERS);

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has recommendations': (r) => {
      try {
        const body = JSON.parse(r.body);
        return Array.isArray(body.data) || Array.isArray(body.content);
      } catch {
        return false;
      }
    },
  });

  sleep(Math.random() * 2 + 1);
}
