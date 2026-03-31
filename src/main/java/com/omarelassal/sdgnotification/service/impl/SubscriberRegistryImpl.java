package com.omarelassal.sdgnotification.service.impl;

import com.omarelassal.sdgnotification.dto.RegisterRequest;
import com.omarelassal.sdgnotification.dto.SubscriberInfoDto;
import com.omarelassal.sdgnotification.exception.SubscriberNotFoundException;
import com.omarelassal.sdgnotification.service.SubscriberService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SubscriberRegistryImpl implements SubscriberService {

    @Value("${sse.emitter.timeout-ms:300000}")
    private long emitterTimeout;

    private final Map<String, SubscriberInfoDto> subscribers = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SubscriberInfoDto register(RegisterRequest request) {
        String id = UUID.randomUUID().toString();

        SubscriberInfoDto subscriber = SubscriberInfoDto.builder()
                .id(id)
                .name(request.getName())
                .email(request.getEmail())
                .subscribedAt(Instant.now())
                .isActive(false)
                .build();

        subscribers.put(id, subscriber);
        log.info("Subscriber registered — id={}, email={}", id, request.getEmail());
        return subscriber;
    }

    @Override
    public void unsubscribe(String subscriberId) {
        if (!subscribers.containsKey(subscriberId)) {
            throw new SubscriberNotFoundException(subscriberId);
        }
        closeEmitter(subscriberId);
        subscribers.remove(subscriberId);
        log.info("Subscriber unsubscribed — id={}", subscriberId);
    }

    @Override
    public SseEmitter createEmitter(String subscriberId) {
        if (!subscribers.containsKey(subscriberId)) {
            throw new SubscriberNotFoundException(subscriberId);
        }

        closeEmitter(subscriberId);

        SseEmitter emitter = new SseEmitter(emitterTimeout);
        emitters.put(subscriberId, emitter);
        subscribers.get(subscriberId).setActive(true);

        emitter.onCompletion(() -> cleanupEmitter(subscriberId));
        emitter.onTimeout(() -> cleanupEmitter(subscriberId));
        emitter.onError(ex -> cleanupEmitter(subscriberId));

        log.info("SSE stream opened — subscriberId={} | active: {}", subscriberId, emitters.size() + 1);
        return emitter;
    }

    @Override
    public Map<String, SseEmitter> getActiveEmitters() {
        return Map.copyOf(emitters);
    }

    @Override
    public List<SubscriberInfoDto> getAllSubscribers() {
        return List.copyOf(subscribers.values());
    }

    @Override
    public SubscriberInfoDto getSubscriber(String subscriberId) {
        SubscriberInfoDto subscriber = subscribers.get(subscriberId);
        if (subscriber == null) {
            throw new SubscriberNotFoundException(subscriberId);
        }
        return subscriber;
    }

    @Override
    public int activeEmitterCount() {
        return emitters.size();
    }

    private void cleanupEmitter(String subscriberId) {
        emitters.remove(subscriberId);
        SubscriberInfoDto sub = subscribers.get(subscriberId);
        if (sub != null) {
            sub.setActive(false);
        }
        log.debug("Emitter cleaned up — subscriberId={} | remaining: {}", subscriberId, emitters.size());
    }

    private void closeEmitter(String subscriberId) {
        SseEmitter existing = emitters.remove(subscriberId);
        if (existing != null) {
            existing.complete();
        }
        SubscriberInfoDto sub = subscribers.get(subscriberId);
        if (sub != null) {
            sub.setActive(false);
        }
    }
}
