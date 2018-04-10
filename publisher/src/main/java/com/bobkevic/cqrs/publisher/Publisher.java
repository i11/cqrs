package com.bobkevic.cqrs.publisher;

import java.lang.invoke.MethodHandles;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

@SpringBootApplication
@EnableOAuth2Client
public class Publisher {

  public static void main(String[] args) {
    SpringApplication.run(MethodHandles.lookup().lookupClass(), args);
  }

}
