package tractors.retail.payments.service.rabbitmq;

import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailEventPublisher {

  private final RabbitTemplate rabbitTemplate;

  @Value("${rabbitmq.email.exchange}")
  private String exchange;

  @Value("${rabbitmq.email.routing-key}")
  private String routingKey;

  public EmailEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(Map<String, Object> event) {
    rabbitTemplate.convertAndSend(exchange, routingKey, event);
  }
}
