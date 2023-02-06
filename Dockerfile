FROM --platform=linux/x86_64 maven:3.8.7-ibmjava-8 AS build
#FOR ARM Environments
#FROM maven:3-eclipse-temurin-8 AS build

ARG ARTIFACTORY_USER
ARG ARTIFACTORY_API_TOKEN

COPY .mvn /home/app/.mvn
COPY src /home/app/src
COPY pom.xml /home/app
COPY src/main/resources/conf /home/app/conf

RUN mvn -f /home/app/pom.xml --settings /home/app/.mvn/settings.xml dependency:resolve
RUN mvn -f /home/app/pom.xml --settings /home/app/.mvn/settings.xml verify
#RUN mvn -f /home/app/pom.xml --settings /home/app/.mvn/settings.xml package

EXPOSE 4567

## Non SSL  + ARM
#ENTRYPOINT ["mvn","-f","/home/app/pom.xml","--settings","/home/app/.mvn/settings.xml","exec:java","-Dexec.args='test'"]

## SSL
EXPOSE 443
ENTRYPOINT ["mvn","-f","/home/app/pom.xml","--settings","/home/app/.mvn/settings.xml","exec:java"]
