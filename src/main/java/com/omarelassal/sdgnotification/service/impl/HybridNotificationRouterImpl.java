package com.omarelassal.sdgnotification.service.impl;

import com.omarelassal.sdgnotification.dto.NotificationRequest;
import com.omarelassal.sdgnotification.service.NotificationRouterService;
import com.omarelassal.sdgnotification.service.SubscriberService;
import com.omarelassal.sdgnotification.service.dispatch.NotificationDispatcher;
import com.omarelassal.sdgnotification.service.dispatch.impl.BrokerDispatcherImpl;
import com.omarelassal.sdgnotification.service.dispatch.impl.DirectDispatcherImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HybridNotificationRouterImpl implements NotificationRouterService {

    @Value("${hybrid.dispatcher.broker-threshold:100}")
    private int threshold;

    @Value("${hybrid.dispatcher.hysteresis:10}")
    private int hysteresis;

    @Value("${hybrid.dispatcher.transition-buffer-ms:50}")
    private long transitionMs;

    private final DirectDispatcherImpl directDispatcher;
    private final BrokerDispatcherImpl brokerDispatcher;
    private final SubscriberService subscriberService;

    // volatile ensures every thread always sees the latest dispatcher without locking
    private volatile NotificationDispatcher dispatcher;

    public HybridNotificationRouterImpl(DirectDispatcherImpl directDispatcher,
                                        BrokerDispatcherImpl brokerDispatcher,
                                        SubscriberService subscriberService) {
        this.directDispatcher = directDispatcher;
        this.brokerDispatcher = brokerDispatcher;
        this.subscriberService = subscriberService;
        this.dispatcher = directDispatcher;
    }

    @Override
    public int broadcast(NotificationRequest request) {
        adapt();

        NotificationDispatcher current = dispatcher;
        log.info("Broadcasting via [{}] | active: {} | threshold: {}/{}",
                current.mode(), subscriberService.activeEmitterCount(), threshold, threshold - hysteresis);

        try {
            return current.dispatch(request);
        } catch (Exception ex) {
            return handleFailure(ex, request, current);
        }
    }

    @Override
    public RouterStatus getStatus() {
        return new RouterStatus(
                dispatcher.mode(),
                subscriberService.activeEmitterCount(),
                subscriberService.getAllSubscribers().size(),
                threshold,
                threshold - hysteresis
        );
    }

    private void adapt() {
        int active = subscriberService.activeEmitterCount();
        int switchDownAt = threshold - hysteresis;

        if (dispatcher == directDispatcher && active >= threshold) {
            switchTo(brokerDispatcher, active,
                    String.format("connections (%d) >= threshold (%d)", active, threshold));
        } else if (dispatcher == brokerDispatcher && active < switchDownAt) {
            switchTo(directDispatcher, active,
                    String.format("connections (%d) < switch-down band (%d)", active, switchDownAt));
        }
    }

    private void switchTo(NotificationDispatcher next, int count, String reason) {
        String from = dispatcher.mode();
        applyTransitionBuffer();
        dispatcher = next;
        log.warn("Dispatcher switched: {} → {} | connections={} | reason={}", from, next.mode(), count, reason);
    }

    private void applyTransitionBuffer() {
        if (transitionMs > 0) {
            try {
                Thread.sleep(transitionMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Transition buffer interrupted");
            }
        }
    }

    private int handleFailure(Exception ex, NotificationRequest request, NotificationDispatcher failed) {
        if (failed == brokerDispatcher) {
            log.error("Broker dispatch failed — falling back to DIRECT. Reason: {}", ex.getMessage());
            return directDispatcher.dispatch(request);
        }
        log.error("Direct dispatch failed unexpectedly", ex);
        throw new RuntimeException("Notification dispatch failed", ex);
    }
}
