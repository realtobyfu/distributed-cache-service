package com.example.cachecoordinator.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.cachecoordinator.partition.PartitionStrategy;

/**
 * Simple in-memory implementation of CacheService for development and testing.
 * This implementation is not distributed and just stores data in a local map.
 */
@Service
public class InMemoryCacheService implements CacheService {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong puts = new AtomicLong(0);
    private final AtomicLong deletes = new AtomicLong(0);
    
    private final PartitionStrategy partitionStrategy;
    
    @Autowired
    public InMemoryCacheService(PartitionStrategy partitionStrategy) {
        this.partitionStrategy = partitionStrategy;
    }

    @Override
    public Object get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        Object value = cache.get(key);
        if (value != null) {
            hits.incrementAndGet();
        } else {
            misses.incrementAndGet();
        }
        
        return value;
    }

    @Override
    public void put(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
        
        cache.put(key, value);
        puts.incrementAndGet();
    }

    @Override
    public boolean delete(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        boolean removed = cache.remove(key) != null;
        if (removed) {
            deletes.incrementAndGet();
        }
        
        return removed;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalRequests = hits.get() + misses.get();
        double hitRatio = totalRequests > 0 ? (double) hits.get() / totalRequests : 0.0;
        
        stats.put("itemCount", cache.size());
        stats.put("hits", hits.get());
        stats.put("misses", misses.get());
        stats.put("puts", puts.get());
        stats.put("deletes", deletes.get());
        stats.put("hitRatio", hitRatio);
        
        return stats;
    }
} 