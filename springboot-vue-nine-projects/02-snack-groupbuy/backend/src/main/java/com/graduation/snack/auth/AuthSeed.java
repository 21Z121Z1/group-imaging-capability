package com.graduation.snack.auth;
import org.springframework.boot.CommandLineRunner; import org.springframework.core.annotation.Order; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
@org.springframework.context.annotation.Profile({"dev","test","ci"}) @Configuration public class AuthSeed { @Bean @Order(0) CommandLineRunner seedAuth(AuthService auth) { return args -> { auth.createSeedUser("admin","Admin123!Demo","系统管理员","ADMIN"); auth.createSeedUser("student","Student123!Demo","演示用户","USER"); }; } }
