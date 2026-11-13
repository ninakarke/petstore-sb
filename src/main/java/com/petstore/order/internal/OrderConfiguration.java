package com.petstore.order.internal;

import com.petstore.order.internal.handoff.LoggingOrderHandoffPublisher;
import com.petstore.order.internal.handoff.OrderHandoffPublisher;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Wiring of the order module. Scheduling is switched on for the hand-off retry run. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class OrderConfiguration {

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
