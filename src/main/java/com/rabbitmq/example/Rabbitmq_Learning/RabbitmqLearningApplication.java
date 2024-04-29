package com.rabbitmq.example.Rabbitmq_Learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration(exclude={org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration.class})
public class RabbitmqLearningApplication {

	public static void main(String[] args) {

		System.out.println("Hello");
		SpringApplication.run(RabbitmqLearningApplication.class, args);
	}

}
