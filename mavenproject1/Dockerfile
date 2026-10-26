# Stage 1: Build the Maven application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/mavenproject1-1.0-SNAPSHOT.jar app.jar
EXPOSE 7070
CMD ["java", "-jar", "app.jar"]
