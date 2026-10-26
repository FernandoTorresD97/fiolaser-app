FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S fiolaser && adduser -S fiolaser -G fiolaser
COPY --from=build /app/target/fiolaser-app.jar app.jar
USER fiolaser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
