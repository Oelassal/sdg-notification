package com.omarelassal.sdgnotification.service.dispatch.impl;

import com.omarelassal.sdgnotification.dto.NotificationRequest;
import com.omarelassal.sdgnotification.service.SubscriberService;
import com.omarelassal.sdgnotification.service.dispatch.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectDispatcherImpl implements NotificationDispatcher {

    private final SubscriberService subscriberService;

    @Override
    public int dispatch(NotificationRequest request) {
        Map<String, SseEmitter> emitters = subscriberService.getActiveEmitters();

        if (emitters.isEmpty()) {
            log.info("[DIRECT] No active streams — notification not delivered");
            return 0;
        }

        Map<String, Object> payload = buildPayload(request);
        AtomicInteger delivered = new AtomicInteger(0);

        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
                delivered.incrementAndGet();
                log.debug("[DIRECT] Delivered to subscriberId={}", id);
            } catch (IOException e) {
                log.warn("[DIRECT] Delivery failed for subscriberId={} — {}", id, e.getMessage());
                emitter.completeWithError(e);
            }
        });

        log.info("[DIRECT] Broadcast complete — delivered: {}/{}", delivered.get(), emitters.size());
        return delivered.get();
    }

    @Override
    public String mode() {
        return "DIRECT";
    }

    public static Map<String, Object> buildPayload(NotificationRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", request.getTitle());
        payload.put("message", request.getMessage());
        payload.put("sentAt", Instant.now().toString());
        return payload;
    }
}
