package tractors.retail.payments.service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

  @Value("${rabbitmq.email.exchange}")
  private String emailExchange;

  @Bean
  public TopicExchange emailExchange() {
    return new TopicExchange(emailExchange, true, false);
  }
}
