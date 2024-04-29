package com.rabbitmq.example.Rabbitmq_Learning.config;



import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.util.ErrorHandler;

/**
 * Key-points of Configuration Class
 * In the above class, we have configured Beans as per our RabbitMQ properties defined in the application.properties class.
 *
 * Use @ EnableRabbit to enable support for Rabbit Listener.
 * @Value injects properties from the resource file into the Component classes.
 * Define a bean of Queue with a name and mark it as non-durable. Non-durable means that the queue and any messages on it will be removed when RabbitMQ is stopped. On the other hand, restarting the application won’t have any effect on the queue.
 * For the exchange, we have four different types, but in our example, we have registered a bean of the DirectExchange type. It routes messages to a queue by matching a complete routing key.
 * Then we created a bean for Binding to tie an exchange with the queue.
 * To ensure that messages are delivered in JSON format, create a bean for MessageConverter
 * Register ConnectionFactory bean to make a connection to RabbitMQ.
 * Bean of rabbitTemplate serves the purpose of sending messages to the queue.
 * To define RabbitAdmin, declare AmqpAdmin bean or define it in the ApplicationContext. It’s useful if we need queues to be automatically declared and bounded.
 * For the container factory bean, we have used SimpleRabbitListenerContainerFactory. It is required since the infrastructure looks for a bean rabbitListenerContainerFactory as the factory’s source for creating message listener containers by default.
 * The errorHandler is used to return some user-friendly Error Object when an exception is thrown by the listener.
 */
@EnableRabbit
@Configuration
public class RabbitMQConfig {


//    Message: A message is a form of data that is shared from the producer to the consumer. The data can hold requests, information, meta-data, etc.
//            Producer: A producer is a user program that is responsible to send or produce messages.
//    Exchange: Exchanges are message routing agents, it is responsible to route the messages to different queues with the help of header attributes,
//              bindings, and routing keys. Binding is the link between a queue and the exchange. Whereas, the Routing Key is the address that the
//              exchange uses to determine which queue the message should be sent to.
//    Queue: A queue is a mailbox or a buffer that stores messages. It has certain limitations like disk space and memory.
//            Consumer: A consumer is a user program that is responsible to receive messages.



    @Value("${rabbitmq.queue}")
    private String queueName;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routingkey}")
    private String routingkey;

    @Value("${rabbitmq.username}")
    private String username;

    @Value("${rabbitmq.password}")
    private String password;

    @Value("${rabbitmq.host}")
    private String host;

    @Value("${rabbitmq.virtualhost}")
    private String virtualHost;

    @Value("${rabbitmq.reply.timeout}")
    private Integer replyTimeout;

    @Value("${rabbitmq.concurrent.consumers}")
    private Integer concurrentConsumers;

    @Value("${rabbitmq.max.concurrent.consumers}")
    private Integer maxConcurrentConsumers;

    /**
     * Non-durable means that the queue and any messages on it will be removed when RabbitMQ is stopped.
     *     On the other hand, restarting the application won’t have any effect on the queue.
     * @return
     */
    @Bean
    public Queue queue() {
        return new Queue(queueName, false);
    }

    /**
     * we have four different types, but in our example, we have registered a bean of the DirectExchange type.
     *   It routes messages to a queue by matching a complete routing key.
     * @return
     */
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchange);
    }

    /**
     * we created a bean for Binding to tie an exchange with the queue.
     * @param queue
     * @param exchange
     * @return
     */
    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {

        return BindingBuilder.bind(queue).to(exchange).with(routingkey);
    }

    /**
     * To ensure that messages are delivered in JSON format, create a bean for MessageConverter
     * @return
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Register ConnectionFactory bean to make a connection to RabbitMQ.
     * @return
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setVirtualHost(virtualHost);
        connectionFactory.setHost(host);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    /**
     * Bean of rabbitTemplate serves the purpose of sending messages to the queue
     * @param connectionFactory
     * @return
     */
    @Bean
    public AmqpTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setDefaultReceiveQueue(queueName);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        rabbitTemplate.setReplyAddress(queue().getName());
        rabbitTemplate.setReplyTimeout(replyTimeout);
        rabbitTemplate.setUseDirectReplyToContainer(false);
        return rabbitTemplate;
    }

    /**
     * To define RabbitAdmin, declare AmqpAdmin bean or define it in the ApplicationContext.
     * It’s useful if we need queues to be automatically declared and bounded.
     * @return
     */
    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    /***
     * For the container factory bean, we have used SimpleRabbitListenerContainerFactory. It is required since the infrastructure looks for a bean rabbitListenerContainerFactory as the
     * factory’s source for creating message listener containers by default.
     * @return
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        final SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(concurrentConsumers);
        factory.setMaxConcurrentConsumers(maxConcurrentConsumers);
        factory.setErrorHandler(errorHandler());
        return factory;
    }

    /**
     * The errorHandler is used to return some user-friendly Error Object when an exception is thrown by the listener.
     * @return
     */
    @Bean
    public ErrorHandler errorHandler() {
        return new ConditionalRejectingErrorHandler(new MyFatalExceptionStrategy());
    }

    public static class MyFatalExceptionStrategy extends ConditionalRejectingErrorHandler.DefaultExceptionStrategy {

        private final Logger logger = LogManager.getLogger(getClass());

        @Override
        public boolean isFatal(Throwable t) {
            if (t instanceof ListenerExecutionFailedException) {
                ListenerExecutionFailedException lefe = (ListenerExecutionFailedException) t;
                logger.error("Failed to process inbound message from queue "
                        + lefe.getFailedMessage().getMessageProperties().getConsumerQueue()
                        + "; failed message: " + lefe.getFailedMessage(), t);
            }
            return super.isFatal(t);
        }

    }
}
