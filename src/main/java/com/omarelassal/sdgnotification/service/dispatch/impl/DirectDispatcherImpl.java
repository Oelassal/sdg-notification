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
        int delivered = 0;

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event().name("notification").data(payload));
                delivered++;
                log.debug("[DIRECT] Delivered to subscriberId={}", entry.getKey());
            } catch (IOException e) {
                log.warn("[DIRECT] Delivery failed for subscriberId={} — {}", entry.getKey(), e.getMessage());
                entry.getValue().completeWithError(e);
            }
        }

        log.info("[DIRECT] Broadcast complete — delivered: {}/{}", delivered, emitters.size());
        return delivered;
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
