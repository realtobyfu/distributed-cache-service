package com.example.cachecoordinator.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.cachecoordinator.service.CacheService;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private final CacheService cacheService;

    @Autowired
    public CacheController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/{key}")
    public ResponseEntity<Object> getValue(@PathVariable String key) {
        Object value = cacheService.get(key);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(value);
    }

    @PutMapping("/{key}")
    public ResponseEntity<Void> putValue(@PathVariable String key, @RequestBody Object value) {
        cacheService.put(key, value);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> deleteValue(@PathVariable String key) {
        boolean removed = cacheService.delete(key);
        if (removed) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping
    public ResponseEntity<Object> getStats() {
        return ResponseEntity.ok(cacheService.getStats());
    }
} 