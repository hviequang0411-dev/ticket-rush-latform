package com.ticketrush.order.service.checkout;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(String code) {
        super("Order " + code + " not found");
    }
}
