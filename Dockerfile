# Builds Quizzmosis and runs it on Tomcat. Usually started through docker-compose.yml,
# which also starts the MySQL database: docker compose up --build

# ---- 1. Build: compile the code and package target/quizzmosis.war with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Download the dependencies first, so Docker can reuse this layer while only the code changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# The tests run in CI (see .github/workflows/ci.yml), so the image build skips them.
RUN mvn -B -q package -DskipTests

# ---- 2. Run: a plain Tomcat with just the WAR ----
FROM tomcat:10.1-jdk21-temurin

# Deployed as ROOT.war, so the site is at http://localhost:8080/ instead of /quizzmosis/.
COPY --from=build /build/target/quizzmosis.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
