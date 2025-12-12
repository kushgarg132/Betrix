# Use lightweight OpenJDK image
FROM eclipse-temurin:17-jdk

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
