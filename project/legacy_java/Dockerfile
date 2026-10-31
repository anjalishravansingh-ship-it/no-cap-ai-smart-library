FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN mkdir -p out && javac -d out $(find src -name '*.java')
EXPOSE 8080
CMD ["java","-cp","out","com.nocapai.web.WebServer"]
