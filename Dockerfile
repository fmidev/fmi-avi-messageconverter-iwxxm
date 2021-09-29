FROM maven:3.6.3-openjdk-11

WORKDIR /fmi-avi-messageconverter-iwxxm
COPY pom.xml .
COPY ci_settings.xml .
COPY fmidev-settings.xml .
COPY src src

ENV MAVEN_CLI_OPTS="-P fmidev -s fmidev-settings.xml  --batch-mode"
ENV MAVEN_OPTS="-Dmaven.repo.local=.m2/repository"
RUN mvn $MAVEN_CLI_OPTS install
RUN mvn $MAVEN_CLI_OPTS test
RUN mvn $MAVEN_CLI_OPTS package
RUN ls ./target/
