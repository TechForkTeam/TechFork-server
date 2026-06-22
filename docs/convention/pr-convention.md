### 형식
```
[타입/#이슈번호] 작업 내용
```

### 예시
```bash
[feat/#23] 게시글 스크랩 기능 구현
[fix/#17] RDS 접근을 public access 대신 ec2에서 접속하도록 변경
[improve/#45] 게시글 검색에 카테고리 필터 추가
[refactor/#40] GlobalExceptionHandler 중복 코드 제거
[debug/#11] 헬스 체크 경로에 누락된 헤더 정보 추가
[chore/#60] Spring Boot 3.5.7로 업데이트
[docs/#70] README API 문서 링크 추가
[deploy/#80] RDS 스토리지 타입 gp3→gp2 변경
```

### 타입
- **feat**: 새로운 기능 추가
- **improve**: 기존 기능 개선/변경
- **refactor**: 기능 변경 없는 코드 리팩토링
- **fix**: 버그 수정
- **debug**: 디버깅 및 오류 해결
- **chore**: 기타 수정 (설정, 의존성 등)
- **docs**: 문서 작업
- **deploy**: 배포/인프라 관련