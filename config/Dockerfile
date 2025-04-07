FROM gradle:7.4.1-jdk11-alpine as builder
WORKDIR /
COPY . /
RUN gradle clean build --no-daemon

FROM openjdk:11-jre-slim
WORKDIR /
COPY --from=builder build/libs/*.jar ./config.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "config.jar"]
