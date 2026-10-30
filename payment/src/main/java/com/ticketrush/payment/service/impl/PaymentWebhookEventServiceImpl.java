package com.ticketrush.payment.service.impl;

import com.ticketrush.payment.domain.PaymentWebhookEvent;
import com.ticketrush.payment.repository.PaymentWebhookEventRepository;
import com.ticketrush.payment.service.PaymentWebhookEventService;
import com.ticketrush.payment.service.dto.PaymentWebhookEventDTO;
import com.ticketrush.payment.service.mapper.PaymentWebhookEventMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.ticketrush.payment.domain.PaymentWebhookEvent}.
 */
@Service
@Transactional
public class PaymentWebhookEventServiceImpl implements PaymentWebhookEventService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentWebhookEventServiceImpl.class);

    private final PaymentWebhookEventRepository paymentWebhookEventRepository;

    private final PaymentWebhookEventMapper paymentWebhookEventMapper;

    public PaymentWebhookEventServiceImpl(
        PaymentWebhookEventRepository paymentWebhookEventRepository,
        PaymentWebhookEventMapper paymentWebhookEventMapper
    ) {
        this.paymentWebhookEventRepository = paymentWebhookEventRepository;
        this.paymentWebhookEventMapper = paymentWebhookEventMapper;
    }

    @Override
    public PaymentWebhookEventDTO save(PaymentWebhookEventDTO paymentWebhookEventDTO) {
        LOG.debug("Request to save PaymentWebhookEvent : {}", paymentWebhookEventDTO);
        PaymentWebhookEvent paymentWebhookEvent = paymentWebhookEventMapper.toEntity(paymentWebhookEventDTO);
        paymentWebhookEvent = paymentWebhookEventRepository.save(paymentWebhookEvent);
        return paymentWebhookEventMapper.toDto(paymentWebhookEvent);
    }

    @Override
    public PaymentWebhookEventDTO update(PaymentWebhookEventDTO paymentWebhookEventDTO) {
        LOG.debug("Request to update PaymentWebhookEvent : {}", paymentWebhookEventDTO);
        PaymentWebhookEvent paymentWebhookEvent = paymentWebhookEventMapper.toEntity(paymentWebhookEventDTO);
        paymentWebhookEvent = paymentWebhookEventRepository.save(paymentWebhookEvent);
        return paymentWebhookEventMapper.toDto(paymentWebhookEvent);
    }

    @Override
    public Optional<PaymentWebhookEventDTO> partialUpdate(PaymentWebhookEventDTO paymentWebhookEventDTO) {
        LOG.debug("Request to partially update PaymentWebhookEvent : {}", paymentWebhookEventDTO);

        return paymentWebhookEventRepository
            .findById(paymentWebhookEventDTO.getId())
            .map(existingPaymentWebhookEvent -> {
                paymentWebhookEventMapper.partialUpdate(existingPaymentWebhookEvent, paymentWebhookEventDTO);

                return existingPaymentWebhookEvent;
            })
            .map(paymentWebhookEventRepository::save)
            .map(paymentWebhookEventMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentWebhookEventDTO> findAll() {
        LOG.debug("Request to get all PaymentWebhookEvents");
        return paymentWebhookEventRepository
            .findAll()
            .stream()
            .map(paymentWebhookEventMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentWebhookEventDTO> findOne(Long id) {
        LOG.debug("Request to get PaymentWebhookEvent : {}", id);
        return paymentWebhookEventRepository.findById(id).map(paymentWebhookEventMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete PaymentWebhookEvent : {}", id);
        paymentWebhookEventRepository.deleteById(id);
    }
}
