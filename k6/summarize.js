import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

/**
 * handleSummary 공통 헬퍼
 * 각 test-*.js 파일에서 import하여 사용:
 *   export { handleSummary } from './summarize.js';
 *
 * 실행 후 summary.json 파일로 튜닝 전후 수치(p50/p95/p99) 비교 가능
 */
export function handleSummary(data) {
    return {
        'summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
