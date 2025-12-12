# Use lightweight OpenJDK image
FROM openjdk:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy project files
COPY . .

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the application
RUN ./gradlew build

# Expose port (optional)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "build/libs/backend-0.0.1-SNAPSHOT.jar"]
