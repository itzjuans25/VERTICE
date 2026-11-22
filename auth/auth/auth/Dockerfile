FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar

USER spring:spring
ENV PORT=9090
EXPOSE 9090
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s CMD wget -q --spider "http://127.0.0.1:$PORT/health" || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=55", "-jar", "/app/app.jar"]
