package com.ticketrush.payment.service.mapper;

import com.ticketrush.payment.domain.Payment;
import com.ticketrush.payment.service.dto.PaymentDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Payment} and its DTO {@link PaymentDTO}.
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper extends EntityMapper<PaymentDTO, Payment> {}
