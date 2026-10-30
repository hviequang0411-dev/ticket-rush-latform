package com.ticketrush.payment.service.mapper;

import com.ticketrush.payment.domain.Payment;
import com.ticketrush.payment.domain.PaymentWebhookEvent;
import com.ticketrush.payment.service.dto.PaymentDTO;
import com.ticketrush.payment.service.dto.PaymentWebhookEventDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link PaymentWebhookEvent} and its DTO {@link PaymentWebhookEventDTO}.
 */
@Mapper(componentModel = "spring")
public interface PaymentWebhookEventMapper extends EntityMapper<PaymentWebhookEventDTO, PaymentWebhookEvent> {
    @Mapping(target = "payment", source = "payment", qualifiedByName = "paymentId")
    PaymentWebhookEventDTO toDto(PaymentWebhookEvent s);

    @Named("paymentId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PaymentDTO toDtoPaymentId(Payment payment);
}
