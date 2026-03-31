package com.omarelassal.sdgnotification.service;

import com.omarelassal.sdgnotification.dto.NotificationRequest;

public interface NotificationRouterService {

    int broadcast(NotificationRequest request);

    RouterStatus getStatus();

    record RouterStatus(
            String mode,
            int activeConnections,
            int totalSubscribers,
            int switchUpAt,
            int switchDownAt
    ) {}
}
