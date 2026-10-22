package br.com.fabionascimento.notification.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private String userId;
    private OrderStatus orderStatus;
    private List<OrderItemDTO> items;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
