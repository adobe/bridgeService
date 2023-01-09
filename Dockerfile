FROM --platform=linux/x86_64 maven:3.8.7-ibmjava-8 AS build

ARG ARTIFACTORY_USER
ARG ARTIFACTORY_API_TOKEN

COPY .mvn /home/app/.mvn
COPY src /home/app/src
COPY pom.xml /home/app
COPY src/main/resources/conf /home/app/conf

RUN mvn -f /home/app/pom.xml --settings /home/app/.mvn/settings.xml dependency:resolve
RUN mvn -f /home/app/pom.xml --settings /home/app/.mvn/settings.xml verify
#RUN mvn -f /home/app/pom.xml --settings /home/app/.mvn/settings.xml package

#
# Package stage
#
#FROM openjdk:11-jre-slim
#COPY --from=build /home/app/target/integroBridgeService-jar-with-dependencies.jar /usr/local/lib/integroBridgeService.jar
EXPOSE 4567
ENTRYPOINT ["mvn","-f","/home/app/pom.xml","--settings","/home/app/.mvn/settings.xml","exec:java"]
