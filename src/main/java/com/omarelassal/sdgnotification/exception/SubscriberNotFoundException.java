package com.omarelassal.sdgnotification.exception;

public class SubscriberNotFoundException extends RuntimeException {

    public SubscriberNotFoundException(String subscriberId) {
        super("Subscriber not found: " + subscriberId);
    }
}
