package br.com.flowlinkerAPI.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.Declarables;

@Configuration
public class RabbitMQConfig {
    
    public static final String QUEUE_NAME = "stripe-events-queue";
    public static final String EXCHANGE_NAME = "stripe-exchange";
    public static final String ROUTING_KEY = "stripe.event.#";

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public TopicExchange exchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean 
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean 
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory); // usa SimpleMessageConverter (texto puro)
    }

    // Declara explicitamente a topologia do Stripe no broker ao subir a aplicação
    @Bean
    public Declarables stripeTopology(TopicExchange exchange, Queue queue, Binding binding) {
        return new Declarables(exchange, queue, binding);
    }
}
