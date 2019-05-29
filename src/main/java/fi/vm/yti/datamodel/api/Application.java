package fi.vm.yti.datamodel.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan({ "fi.vm.yti.datamodel.api" })
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}