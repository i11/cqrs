package com.bobkevic.cqrs.publisher.configurations;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

import com.google.api.gax.retrying.RetrySettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.threeten.bp.Duration;

@Configuration
public class PubsubConfiguration {

  @Bean
  @Scope(SCOPE_SINGLETON)
  @Qualifier("pubsubRetry")
  public RetrySettings retrySettings() {
    return RetrySettings.newBuilder()
        .setInitialRpcTimeout(Duration.ofMillis(20))
        .setMaxRpcTimeout(Duration.ofSeconds(10))
        .setInitialRetryDelay(Duration.ofMillis(10))
        .setRetryDelayMultiplier(2.0)
        .setMaxRetryDelay(Duration.ofSeconds(5))
        .setTotalTimeout(Duration.ofSeconds(20))
        .build();
  }

}
