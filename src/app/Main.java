package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"app", "config", "controllers", "data", "dtos", "exceptions", "services", "utils"})
@EnableMongoRepositories(basePackages = "data.repositories")
@EnableMongoAuditing
public class Main {
    public static void main(String... args) {
        SpringApplication.run(Main.class, args);
    }
}