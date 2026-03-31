package com.omarelassal.sdgnotification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SubscriberInfoDto {
    private String id;
    private String name;
    private String email;
    private Instant subscribedAt;
    private boolean isActive;
}
