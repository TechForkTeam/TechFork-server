# Java 17
FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=build/libs/*.jar

# jar 파일 복사
COPY ${JAR_FILE} app.jar

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]