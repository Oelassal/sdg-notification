package com.omarelassal.sdgnotification.controller;

import com.omarelassal.sdgnotification.dto.ApiResponse;
import com.omarelassal.sdgnotification.dto.RegisterRequest;
import com.omarelassal.sdgnotification.dto.SubscriberInfoDto;
import com.omarelassal.sdgnotification.service.SubscriberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/subscribers")
@RequiredArgsConstructor
@Tag(name = "Subscriber", description = "Subscriber registration and real-time stream endpoints")
public class SubscriberController {

    private final SubscriberService subscriberService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register as a subscriber",
            description = "Creates a subscription and returns a subscriber ID. Use this ID to open the SSE stream."
    )
    public ApiResponse<SubscriberInfoDto> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request — email={}", request.getEmail());
        SubscriberInfoDto subscriber = subscriberService.register(request);
        return ApiResponse.ok("Registered successfully. Use your ID to open the notification stream.", subscriber);
    }

    @GetMapping(value = "/{subscriberId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Open SSE notification stream",
            description = "Opens a persistent SSE connection. Keep this open to receive real-time notifications."
    )
    public SseEmitter stream(
            @Parameter(description = "Subscriber ID returned from /register")
            @PathVariable String subscriberId) {
        log.info("SSE stream request — subscriberId={}", subscriberId);
        return subscriberService.createEmitter(subscriberId);
    }

    @DeleteMapping("/{subscriberId}/unsubscribe")
    @Operation(
            summary = "Unsubscribe",
            description = "Removes the subscriber and closes their active stream if open."
    )
    public ApiResponse<Void> unsubscribe(
            @Parameter(description = "Subscriber ID to remove")
            @PathVariable String subscriberId) {
        log.info("Unsubscribe request — subscriberId={}", subscriberId);
        subscriberService.unsubscribe(subscriberId);
        return ApiResponse.ok("Unsubscribed successfully");
    }
}
