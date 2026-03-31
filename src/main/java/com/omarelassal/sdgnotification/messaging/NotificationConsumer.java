package com.omarelassal.sdgnotification.messaging;

import com.omarelassal.sdgnotification.service.SubscriberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SubscriberService subscriberService;

    @RabbitListener(queues = "#{notificationQueue.name}")
    public void onNotification(Map<String, Object> payload) {
        Map<String, SseEmitter> emitters = subscriberService.getActiveEmitters();

        if (emitters.isEmpty()) {
            log.debug("[CONSUMER] Message received but no active streams on this node");
            return;
        }

        int delivered = 0;

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().name("notification").data(payload));
                delivered++;
                log.debug("[CONSUMER] Delivered to subscriberId={}", entry.getKey());
            } catch (IOException e) {
                log.warn("[CONSUMER] Delivery failed for subscriberId={} — {}", entry.getKey(), e.getMessage());
                entry.getValue().completeWithError(e);
            }
        }

        log.info("[CONSUMER] Broker message delivered — reached {}/{} streams", delivered, emitters.size());
    }
}
