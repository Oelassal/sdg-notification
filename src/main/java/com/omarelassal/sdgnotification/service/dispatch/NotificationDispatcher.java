package com.omarelassal.sdgnotification.service.dispatch;


import com.omarelassal.sdgnotification.dto.NotificationRequest;

public interface NotificationDispatcher {

    /**
     * Dispatches a notification to subscribers.
     *
     * @param request the notification payload
     * @return number of subscribers reached, or -1 if dispatch is async (broker mode)
     */
    int dispatch(NotificationRequest request);

    /**
     * Human-readable mode label used in logs and monitoring endpoints.
     */
    String mode();
}
