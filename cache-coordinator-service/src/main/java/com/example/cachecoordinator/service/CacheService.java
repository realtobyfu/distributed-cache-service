package com.example.cachecoordinator.service;

import java.util.Map;

/**
 * Service interface for cache operations.
 */
public interface CacheService {
    
    /**
     * Retrieves a value from the cache.
     * @param key The key to look up
     * @return The value associated with the key, or null if not found
     */
    Object get(String key);
    
    /**
     * Stores a value in the cache.
     * @param key The key under which to store the value
     * @param value The value to store
     */
    void put(String key, Object value);
    
    /**
     * Removes a value from the cache.
     * @param key The key to remove
     * @return true if the key was found and removed, false otherwise
     */
    boolean delete(String key);
    
    /**
     * Returns statistics about the cache.
     * @return A map containing cache statistics
     */
    Map<String, Object> getStats();
} 