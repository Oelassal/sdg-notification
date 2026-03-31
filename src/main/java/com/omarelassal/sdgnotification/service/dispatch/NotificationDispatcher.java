package com.omarelassal.sdgnotification.service.dispatch;


import com.omarelassal.sdgnotification.dto.NotificationRequest;

public interface NotificationDispatcher {

//    return number of subscribers reached, or -1 if dispatch is async (broker mode)
    int dispatch(NotificationRequest request);

    String mode();
}
