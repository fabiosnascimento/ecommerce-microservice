package br.com.fabionascimento.order.clients;

import br.com.fabionascimento.order.dto.UserResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface UserServiceClient {

    @GetExchange("/api/users/{id}")
    UserResponse getUserDetail(@PathVariable String id);
}
