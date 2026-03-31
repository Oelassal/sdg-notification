package com.omarelassal.sdgnotification.controller;

import com.omarelassal.sdgnotification.dto.ApiResponse;
import com.omarelassal.sdgnotification.dto.NotificationRequest;
import com.omarelassal.sdgnotification.dto.SubscriberInfoDto;
import com.omarelassal.sdgnotification.service.NotificationRouterService;
import com.omarelassal.sdgnotification.service.SubscriberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/publisher")
@RequiredArgsConstructor
@Tag(name = "Publisher", description = "Publisher endpoints — send notifications and inspect subscribers")
public class PublisherController {

    private final NotificationRouterService router;
    private final SubscriberService subscriberService;

    @PostMapping("/notify")
    @Operation(
            summary = "Send a notification",
            description = "Broadcasts to all active SSE streams. Route is chosen adaptively (DIRECT or BROKER)."
    )
    public ApiResponse<Map<String, Object>> sendNotification(@Valid @RequestBody NotificationRequest request) {
        log.info("Publisher sending notification — title='{}'", request.getTitle());
        int delivered = router.broadcast(request);

        String note = delivered == -1
                ? "Dispatched via broker — delivery is async"
                : "Delivered to " + delivered + " active subscriber(s)";

        return ApiResponse.ok(note, Map.of(
                "deliveredTo", delivered == -1 ? "async" : delivered,
                "mode", router.getStatus().mode()
        ));
    }

    @GetMapping("/subscribers")
    @Operation(
            summary = "Get all subscribers",
            description = "Returns all registered subscribers with their current stream status."
    )
    public ApiResponse<List<SubscriberInfoDto>> getSubscribers() {
        List<SubscriberInfoDto> subscribers = subscriberService.getAllSubscribers();
        log.info("Publisher fetched subscriber list — count={}", subscribers.size());
        return ApiResponse.ok("Subscribers retrieved", subscribers);
    }

    @GetMapping("/router/status")
    @Operation(
            summary = "Router status",
            description = "Shows current dispatch mode, active connection count, and switching thresholds."
    )
    public ApiResponse<NotificationRouterService.RouterStatus> routerStatus() {
        NotificationRouterService.RouterStatus status = router.getStatus();
        log.debug("Router status requested — mode={}, active={}", status.mode(), status.activeConnections());
        return ApiResponse.ok("Router status retrieved", status);
    }
}
