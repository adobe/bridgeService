FROM --platform=linux/x86_64 maven:3.8.7-ibmjava-8 AS build

COPY src/main/resources/conf /home/app/conf
COPY target/integroBridgeService-jar-with-dependencies.jar integroBridgeService-jar-with-dependencies.jar

EXPOSE 8080
ENTRYPOINT java -cp integroBridgeService-jar-with-dependencies.jar MainContainer "test"

#EXPOSE 443
#ENTRYPOINT java -cp integroBridgeService-jar-with-dependencies.jar MainContainer
