package com.omarelassal.sdgnotification.service;

import com.omarelassal.sdgnotification.dto.RegisterRequest;
import com.omarelassal.sdgnotification.dto.SubscriberInfoDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface SubscriberService {

    SubscriberInfoDto register(RegisterRequest request);

    void unsubscribe(String subscriberId);

    SseEmitter createEmitter(String subscriberId);

    Map<String, SseEmitter> getActiveEmitters();

    List<SubscriberInfoDto> getAllSubscribers();

    SubscriberInfoDto getSubscriber(String subscriberId);

    int activeEmitterCount();
}
