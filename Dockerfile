# Stage 1 - Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2 - Run
FROM eclipse-temurin:17-jdk
WORKDIR /app
RUN apt-get update && apt-get install -y tzdata
ENV TZ=Asia/Ho_Chi_Minh
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Duser.timezone=Asia/Ho_Chi_Minh","-jar","app.jar","--spring.profiles.active=prod"]
