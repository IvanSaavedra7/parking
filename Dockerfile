FROM openjdk:17-jdk-slim

WORKDIR /app

# Copie o arquivo fat JAR corretamente - ajuste o nome conforme necessário
COPY build/libs/*-all.jar app.jar

EXPOSE 3003

# Execute o JAR
ENTRYPOINT ["java", "-jar", "app.jar"]