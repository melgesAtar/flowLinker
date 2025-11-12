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
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

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

	// Garante que exchange/queues/bindings sejam declarados no broker ao subir a aplicação
	@Bean
	public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
		RabbitAdmin admin = new RabbitAdmin(connectionFactory);
		admin.setAutoStartup(true);
		return admin;
	}

	@Bean
	public Declarables eventTopology(TopicExchange eventsExchange,
	                                 Queue activityQueue,
	                                 Queue campaignQueue,
	                                 Queue securityQueue,
	                                 Binding bindActivity,
	                                 Binding bindCampaign,
	                                 Binding bindSecurity) {
		return new Declarables(
				eventsExchange,
				activityQueue, campaignQueue, securityQueue,
				bindActivity, bindCampaign, bindSecurity
		);
	}
}
