package com.bobkevic.cqrs.publisher;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.bobkevic.cqrs.publisher.dtos.Greeting;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PublisherIT {


  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  public void createClient() {
    final ResponseEntity<Greeting> responseEntity =
        restTemplate.getForEntity("/greeting?name=bogus-name", Greeting.class);
    final Greeting responseObject = responseEntity.getBody();
    assertThat(responseEntity.getStatusCode(), is(HttpStatus.OK));
    assertThat(responseObject.content(), is("Hello, bogus-name!"));
  }

}