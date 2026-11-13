package com.petstore.order.internal;

import com.petstore.order.internal.handoff.LoggingOrderHandoffPublisher;
import com.petstore.order.internal.handoff.OrderHandoffPublisher;
import com.petstore.shared.StoreMigration;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Wiring of the order module. */
@Configuration(proxyBeanMethods = false)
class OrderConfiguration {

  /**
   * Scheduling, which drives the hand-off retry run. Switched off in the job that copies data to
   * another store: that job must move orders, not deliver them.
   */
  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  @Profile("!" + StoreMigration.PROFILE)
  static class HandoffRetrySchedule {}

  /** The clock orders are dated with. A bean, so that tests can fix the time. */
  @Bean
  @ConditionalOnMissingBean
  Clock clock() {
    return Clock.systemDefaultZone();
  }

  /** Hand-off without a message broker: {@code petstore.order.handoff.transport=log}. */
  @Bean
  @ConditionalOnProperty(name = "petstore.order.handoff.transport", havingValue = "log")
  OrderHandoffPublisher loggingOrderHandoffPublisher(Clock clock) {
    return new LoggingOrderHandoffPublisher(clock);
  }
}
