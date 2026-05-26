FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/vrtech-1.0.0.jar app.jar
EXPOSE 8080
CMD ["java", "-Xmx450m", "-jar", "app.jar"]
