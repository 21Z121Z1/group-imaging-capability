package com.graduation.rental;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class RentalApplication {
  public static void main(String[] args) { SpringApplication.run(RentalApplication.class, args); }
}
