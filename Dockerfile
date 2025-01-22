# Use a Maven image to build the application
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy only the pom.xml and source code
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src

# Build the application
RUN mvn package -DskipTests

RUN ls -la /app/target/

# Use a lightweight OpenJDK image to run the application
FROM openjdk:17-jdk-slim
WORKDIR /app
VOLUME /tmp

# Copy the built JAR file from the builder stage
COPY --from=builder /app/target/KlikSigurnost-*.jar app.jar

# Ensure app.jar is executable
RUN chmod +x /app/app.jar

# Run the app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
