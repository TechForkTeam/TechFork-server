import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, DEFAULT_HEADERS } from '../config.js';

export function crudTest() {
  // 1) 글 목록 조회 (cursor-based pagination)
  const listRes = http.get(
    `${BASE_URL}/api/v1/posts/recent?size=20`,
    DEFAULT_HEADERS
  );

  check(listRes, {
    'list: status 200': (r) => r.status === 200,
  });

  // 2) 목록에서 랜덤으로 하나 골라 상세 조회
  try {
    const body = JSON.parse(listRes.body);
    const posts = body.data?.posts || [];

    if (posts.length > 0) {
      const randomPost = posts[Math.floor(Math.random() * posts.length)];
      const detailRes = http.get(
        `${BASE_URL}/api/v1/posts/${randomPost.id}`,
        DEFAULT_HEADERS
      );

      check(detailRes, {
        'detail: status 200': (r) => r.status === 200,
      });
    }
  } catch (e) {
    // 파싱 실패 시 무시 — 목록 조회 자체의 성능 데이터는 이미 수집됨
  }

  sleep(Math.random() * 1 + 0.5); // 0.5~1.5초 간격
}
