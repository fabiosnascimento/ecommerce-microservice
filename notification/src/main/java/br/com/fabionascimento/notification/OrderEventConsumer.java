package br.com.fabionascimento.notification;

import br.com.fabionascimento.notification.payload.OrderCreatedEvent;
import br.com.fabionascimento.notification.payload.OrderStatus;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrderEventConsumer {
    @RabbitListener(queues = "${rabbitmq.queue.name}")
    public void handleOrderEvent(OrderCreatedEvent orderEvent) {
        System.out.println("Received Order Event: " + orderEvent);

        long orderId = orderEvent.getOrderId();
        OrderStatus orderStatus = orderEvent.getOrderStatus();

        System.out.println("Order ID: " + orderId);
        System.out.println("Order Status: " + orderStatus);
    }
}
