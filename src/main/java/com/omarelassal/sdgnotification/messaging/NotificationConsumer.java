package com.omarelassal.sdgnotification.messaging;

import com.omarelassal.sdgnotification.service.SubscriberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumes notification messages from this node's anonymous queue.
 *
 * In BROKER mode, BrokerDispatcherImpl publishes to the fanout exchange.
 * RabbitMQ delivers a copy to every bound queue (one per server node).
 * This consumer pushes the message to all local SSE streams on this node.
 */
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

        AtomicInteger delivered = new AtomicInteger(0);

        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
                delivered.incrementAndGet();
                log.debug("[CONSUMER] Delivered to subscriberId={}", id);
            } catch (IOException e) {
                log.warn("[CONSUMER] Delivery failed for subscriberId={} — {}", id, e.getMessage());
                emitter.completeWithError(e);
            }
        });

        log.info("[CONSUMER] Broker message delivered — reached {}/{} streams", delivered.get(), emitters.size());
    }
}
