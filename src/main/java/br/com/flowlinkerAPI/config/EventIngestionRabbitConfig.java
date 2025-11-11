package br.com.flowlinkerAPI.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.QueueBuilder;

@Configuration
public class EventIngestionRabbitConfig {

    public static final String EXCHANGE_EVENTS = "events.exchange";
	public static final String QUEUE_ACTIVITY = "events.activity";
	public static final String QUEUE_CAMPAIGN = "events.campaign";
	public static final String QUEUE_SECURITY = "events.security";
    

    @Bean
	public TopicExchange eventsExchange() {
		return ExchangeBuilder.topicExchange(EXCHANGE_EVENTS).durable(true).build();
	}

	@Bean
	public Queue activityQueue() {
		return QueueBuilder.durable(QUEUE_ACTIVITY).build();
	}

	@Bean
	public Queue campaignQueue() {
		return QueueBuilder.durable(QUEUE_CAMPAIGN).build();
	}

	@Bean
	public Queue securityQueue() {
		return QueueBuilder.durable(QUEUE_SECURITY).build();
	}

	@Bean
	public Binding bindActivity(Queue activityQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(activityQueue).to(eventsExchange).with("#.activity.#");
	}

	@Bean
	public Binding bindCampaign(Queue campaignQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(campaignQueue).to(eventsExchange).with("#.campaign.#");
	}

	@Bean
	public Binding bindSecurity(Queue securityQueue, TopicExchange eventsExchange) {
		return BindingBuilder.bind(securityQueue).to(eventsExchange).with("#.security.#");
	}

	@Bean
	public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate eventsRabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
		RabbitTemplate tpl = new RabbitTemplate(cf);
		tpl.setMessageConverter(converter);
		return tpl;
	}
}
