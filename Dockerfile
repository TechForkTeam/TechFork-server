# Java 17 기반 슬림 이미지 사용
FROM openjdk:17-slim

ARG JAR_FILE=build/libs/*.jar

# jar 파일 복사
COPY ${JAR_FILE} app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]