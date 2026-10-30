package com.ticketrush.gateway.web.rest;

import com.ticketrush.gateway.security.SecurityUtils;
import com.ticketrush.gateway.service.waitingroom.WaitingRoomService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * The waiting room for hot sales. Buyers join, then poll every few seconds until admitted; organisers watch the
 * queue length ({@code /api/admin/**} is admins only).
 */
@RestController
@RequestMapping("/api")
public class WaitingRoomResource {

    private final WaitingRoomService waitingRoomService;

    public WaitingRoomResource(WaitingRoomService waitingRoomService) {
        this.waitingRoomService = waitingRoomService;
    }

    /**
     * {@code POST /api/waiting-room/{eventId}} : join the queue (idempotent: keeps your place).
     */
    @PostMapping("/waiting-room/{eventId}")
    public Mono<WaitingRoomService.QueueStatus> join(@PathVariable long eventId) {
        return currentUser().flatMap(user -> waitingRoomService.join(eventId, user));
    }

    /**
     * {@code GET /api/waiting-room/{eventId}} : your place, and the pass once admitted.
     */
    @GetMapping("/waiting-room/{eventId}")
    public Mono<WaitingRoomService.QueueStatus> status(@PathVariable long eventId) {
        return currentUser().flatMap(user -> waitingRoomService.status(eventId, user));
    }

    @GetMapping("/admin/waiting-room/{eventId}")
    public Mono<WaitingRoomService.QueueStats> stats(@PathVariable long eventId) {
        return waitingRoomService.stats(eventId);
    }

    private static Mono<String> currentUser() {
        return SecurityUtils.getCurrentUserLogin().switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
    }
}
