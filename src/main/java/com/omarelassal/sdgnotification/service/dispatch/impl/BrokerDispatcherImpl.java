package com.omarelassal.sdgnotification.service.dispatch.impl;

import com.omarelassal.sdgnotification.config.RabbitMQConfig;
import com.omarelassal.sdgnotification.dto.NotificationRequest;
import com.omarelassal.sdgnotification.service.dispatch.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrokerDispatcherImpl implements NotificationDispatcher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public int dispatch(NotificationRequest request) {
        Map<String, Object> payload = buildPayload(request);

        rabbitTemplate.convertAndSend(RabbitMQConfig.FANOUT_EXCHANGE, "", payload);

        log.info("[BROKER] Published to '{}' — title='{}'", RabbitMQConfig.FANOUT_EXCHANGE, request.getTitle());

        return -1;
    }

    @Override
    public String mode() {
        return "BROKER";
    }

    private Map<String, Object> buildPayload(NotificationRequest request) {
        return DirectDispatcherImpl.buildPayload(request);
    }
}
