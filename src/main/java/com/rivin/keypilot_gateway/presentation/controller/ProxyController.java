package com.rivin.keypilot_gateway.presentation.controller;


import com.rivin.keypilot_gateway.application.service.ProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProxyController {

    private final ProxyService proxyService;

    // Provider is hardcoded to "openai" for now.
    // Lap 5 will make this dynamic via config/request header.
    private static final String DEFAULT_PROVIDER = "openai";

    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    // HttpServletRequest request = This will include URL, header and the method of the request.
    // String body = This will get the data inside the request, also what we called as Body of a request.
    //  @RequestMapping("/**")  = Catch Everything (Any URL, Any request, any method)
    @RequestMapping("/**")
    public ResponseEntity<String> proxy(HttpServletRequest request, @RequestBody(required = false) String body) {
        return proxyService.forward(DEFAULT_PROVIDER, request, body);
    }
}