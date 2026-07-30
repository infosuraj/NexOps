package com.nexops.gateway.controller;

import com.nexops.gateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody Map<String, String> creds) {
        String username = creds.get("username");
        String password = creds.get("password");

        // hardcoded for demo - real project would check a user store
        if ("admin".equals(username) && "nexops123".equals(password)) {
            String token = jwtUtil.generateToken(username);
            return Mono.just(ResponseEntity.ok(Map.of(
                "token", token,
                "type", "Bearer",
                "username", username,
                "message", "Login successful"
            )));
        }

        return Mono.just(ResponseEntity.status(401).body(Map.of(
            "error", "Invalid credentials"
        )));
    }

    // quick demo token without needing credentials
    @GetMapping("/token")
    public Mono<ResponseEntity<Map<String, Object>>> demoToken() {
        String token = jwtUtil.generateToken("demo-user");
        return Mono.just(ResponseEntity.ok(Map.of(
            "token", token,
            "type", "Bearer",
            "note", "demo token - expires in 24h"
        )));
    }
}
