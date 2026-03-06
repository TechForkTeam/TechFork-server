// ============================================
// 환경변수로 주입하거나 여기서 기본값 설정
// 실행 예: k6 run --env BASE_URL=http://your-server:8080 --env AUTH_TOKEN=xxx
// ============================================

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const AUTH_HEADERS = {
    headers: {
        'Authorization': `Bearer ${__ENV.AUTH_TOKEN || ''}`,
        'Content-Type': 'application/json',
    },
};

export const DEFAULT_HEADERS = {
    headers: {
        'Content-Type': 'application/json',
    },
};

// 검색 테스트용 키워드 목록
// 실제 크롤링된 블로그에서 자주 등장하는 키워드로 교체 권장
export const SEARCH_KEYWORDS = [
    '스프링',
    'kubernetes',
    '카프카',
    'Redis',
    'JPA',
    '데이터베이스',
    'MSA',
    '모니터링',
    'CI/CD',
    '성능',
];