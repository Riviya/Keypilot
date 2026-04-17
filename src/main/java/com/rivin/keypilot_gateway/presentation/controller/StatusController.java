package com.rivin.keypilot_gateway.presentation.controller;

import com.rivin.keypilot_gateway.application.dto.GatewayStatusResponse;
import com.rivin.keypilot_gateway.application.service.GatewayStatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final GatewayStatusService gatewayStatusService;

    public StatusController(GatewayStatusService gatewayStatusService) {
        this.gatewayStatusService = gatewayStatusService;
    }

    @GetMapping
    public ResponseEntity<GatewayStatusResponse> getStatus() {
        GatewayStatusResponse status = gatewayStatusService.getStatus();

        HttpStatus httpStatus = status.healthy()
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus).body(status);
    }
}
