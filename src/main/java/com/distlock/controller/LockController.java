package com.distlock.controller;

import com.distlock.service.LockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/lock")
public class LockController {

    private final LockService lockService;

    @Autowired
    public LockController(LockService lockService) {
        this.lockService = lockService;
    }

    @PostMapping("/acquire/{lockKey}")
    public ResponseEntity<Map<String, Object>> acquireLock(
            @PathVariable String lockKey,
            @RequestParam(required = false, defaultValue = "30000") Long timeoutMs) {

        log.info("Acquiring lock: {}, timeout: {}", lockKey, timeoutMs);
        boolean acquired = lockService.acquireLock(lockKey, timeoutMs);

        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("acquired", acquired);
        response.put("timestamp", System.currentTimeMillis());

        if (acquired) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(409).body(response); // 409 Conflict
        }
    }

    @PostMapping("/release/{lockKey}")
    public ResponseEntity<Map<String, Object>> releaseLock(@PathVariable String lockKey) {
        log.info("Releasing lock: {}", lockKey);
        boolean released = lockService.releaseLock(lockKey);

        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("released", released);
        response.put("timestamp", System.currentTimeMillis());

        if (released) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(400).body(response); // 400 Bad Request
        }
    }

    @GetMapping("/status/{lockKey}")
    public ResponseEntity<Map<String, Object>> getLockStatus(@PathVariable String lockKey) {
        log.info("Checking lock status: {}", lockKey);
        boolean locked = lockService.isLocked(lockKey);

        Map<String, Object> response = new HashMap<>();
        response.put("lockKey", lockKey);
        response.put("locked", locked);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getLockInfo() {
        log.info("Getting lock system info");
        Map<String, Object> info = lockService.getLockInfo();
        info.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(info);
    }
}