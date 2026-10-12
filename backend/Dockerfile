FROM maven:3.10-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/edupulse-backend.jar app.jar
ENV PORT=5050
EXPOSE 5050
ENTRYPOINT ["java","-jar","/app/app.jar"]
