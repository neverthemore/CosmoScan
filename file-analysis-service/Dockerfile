FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline --quiet
COPY src ./src
RUN mvn package -DskipTests --quiet

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN mkdir -p /app/reports
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8002
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "app.jar"]
