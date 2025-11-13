# ---- Stage 1: Build ---------------------------------------------------------
FROM maven:3.9.7-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copia POM e código (build simples; para cache mais fino, poderia copiar só o pom.xml e baixar dependências antes)
COPY pom.xml .
COPY src ./src

# Compila sem testes
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package


# ---- Stage 2: Runtime -------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Configs básicas
ENV TZ=Etc/UTC
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Usuário não-root
RUN useradd -ms /bin/bash appuser
USER appuser

WORKDIR /app

# Copia o jar gerado (qualquer nome gerado pelo Spring Boot)
ARG JAR_FILE=/workspace/target/*.jar
COPY --from=build ${JAR_FILE} /app/app.jar

# Porta padrão (pode ser sobrescrita com env SERVER_PORT ou -p do Docker)
EXPOSE 8080

# Healthcheck simples (pode customizar via HEALTHCHECK_PATH)
ENV HEALTHCHECK_PATH="/actuator/health"
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:${SERVER_PORT:-8080}${HEALTHCHECK_PATH} || exit 1

# Executa a aplicação (JAVA_OPTS pode ser customizado via env)
ENTRYPOINT ["/bin/sh","-c","exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]


