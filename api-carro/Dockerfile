# Etapa de build
# Estágio 1: Construir a aplicação Java
# Usar uma imagem base oficial do OpenJDK com o Maven instalado
FROM maven:3.9.6-eclipse-temurin-17-focal AS build

# Define o diretório de trabalho no contêiner
WORKDIR /app

# Copia os arquivos de configuração do Maven (pom.xml) para acelerar o build
COPY pom.xml .

# Copia o código-fonte
COPY src ./src

# Executa o build do projeto e empacota-o em um JAR
RUN mvn clean package -DskipTests


# Estágio 2: Criar a imagem final de produção
# Usar uma imagem base menor, otimizada para execução (JRE sem o Maven)
FROM eclipse-temurin:17-jdk

# Define o diretório de trabalho para a imagem final
WORKDIR /app

# Copia o arquivo JAR gerado no estágio de "build"
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta que o seu aplicativo Spring Boot usa
EXPOSE 8082

# Define o comando para executar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]