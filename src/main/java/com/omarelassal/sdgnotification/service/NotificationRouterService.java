package com.omarelassal.sdgnotification.service;

import com.omarelassal.sdgnotification.dto.NotificationRequest;

public interface NotificationRouterService {

    /**
     * Broadcasts a notification. Returns number of subscribers reached,
     * or -1 if delivery is async (broker mode).
     */
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
