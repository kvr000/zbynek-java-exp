FROM openjdk:17-jdk-slim
ARG JAR_FILE spatial-rest-jpa.jar
COPY ${JAR_FILE} /
ENTRYPOINT ["java", "-jar", "/spatial-rest-jpa.jar"]
#ENTRYPOINT ["jar", "-t", "-f", "spatial-rest-jpa.jar"]
#ENTRYPOINT ["ls", "-la"]
