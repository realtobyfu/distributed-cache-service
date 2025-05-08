package com.example.cachecoordinator.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.example.cachecoordinator.node.NodeRegistry;
import com.example.cachecoordinator.partition.PartitionStrategy;

/**
 * Implementation of CacheService that distributes cache operations
 * across multiple nodes using the configured partitioning strategy.
 * 
 * For demonstration purposes, this is just simulating distributed behavior
 * without actually making HTTP calls to real nodes.
 */
@Service
@Primary
public class DistributedCacheService implements CacheService {

    // This map simulates data stored across multiple nodes for demo purposes
    private final Map<String, Map<String, Object>> nodeDataMap = new ConcurrentHashMap<>();
    
    private final NodeRegistry nodeRegistry;
    private final PartitionStrategy partitionStrategy;
    
    // Metrics
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong puts = new AtomicLong(0);
    private final AtomicLong deletes = new AtomicLong(0);

    @Autowired
    public DistributedCacheService(NodeRegistry nodeRegistry, PartitionStrategy partitionStrategy) {
        this.nodeRegistry = nodeRegistry;
        this.partitionStrategy = partitionStrategy;
    }

    @Override
    public Object get(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        List<String> activeNodes = nodeRegistry.getActiveNodes();
        if (activeNodes.isEmpty()) {
            misses.incrementAndGet();
            return null;
        }
        
        String targetNodeId = partitionStrategy.getNodeForKey(key, activeNodes);
        Map<String, Object> nodeData = getOrCreateNodeData(targetNodeId);
        
        Object value = nodeData.get(key);
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
        
        List<String> activeNodes = nodeRegistry.getActiveNodes();
        if (activeNodes.isEmpty()) {
            throw new IllegalStateException("No active nodes available for cache operation");
        }
        
        // Determine the primary node for this key
        String primaryNodeId = partitionStrategy.getNodeForKey(key, activeNodes);
        Map<String, Object> primaryNodeData = getOrCreateNodeData(primaryNodeId);
        primaryNodeData.put(key, value);
        
        // Handle replication to backup nodes if we have enough nodes
        if (activeNodes.size() > 1) {
            int replicaCount = Math.min(2, activeNodes.size() - 1); // At most 2 replicas
            List<String> backupNodeIds = partitionStrategy.getBackupNodesForKey(
                key, primaryNodeId, activeNodes, replicaCount);
            
            // Store replicas on backup nodes
            for (String backupNodeId : backupNodeIds) {
                Map<String, Object> backupNodeData = getOrCreateNodeData(backupNodeId);
                backupNodeData.put(key, value);
            }
        }
        
        puts.incrementAndGet();
    }

    @Override
    public boolean delete(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        
        List<String> activeNodes = nodeRegistry.getActiveNodes();
        if (activeNodes.isEmpty()) {
            return false;
        }
        
        String targetNodeId = partitionStrategy.getNodeForKey(key, activeNodes);
        Map<String, Object> nodeData = nodeDataMap.get(targetNodeId);
        
        boolean removed = false;
        if (nodeData != null) {
            removed = nodeData.remove(key) != null;
        }
        
        if (removed) {
            // Also remove from backup nodes
            if (activeNodes.size() > 1) {
                int replicaCount = Math.min(2, activeNodes.size() - 1);
                List<String> backupNodeIds = partitionStrategy.getBackupNodesForKey(
                    key, targetNodeId, activeNodes, replicaCount);
                
                for (String backupNodeId : backupNodeIds) {
                    Map<String, Object> backupNodeData = nodeDataMap.get(backupNodeId);
                    if (backupNodeData != null) {
                        backupNodeData.remove(key);
                    }
                }
            }
            
            deletes.incrementAndGet();
        }
        
        return removed;
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalItems = 0;
        for (Map<String, Object> nodeData : nodeDataMap.values()) {
            totalItems += nodeData.size();
        }
        
        long totalRequests = hits.get() + misses.get();
        double hitRatio = totalRequests > 0 ? (double) hits.get() / totalRequests : 0.0;
        
        stats.put("totalNodes", nodeRegistry.getTotalNodeCount());
        stats.put("activeNodes", nodeRegistry.getActiveNodeCount());
        stats.put("totalItems", totalItems);
        stats.put("hits", hits.get());
        stats.put("misses", misses.get());
        stats.put("puts", puts.get());
        stats.put("deletes", deletes.get());
        stats.put("hitRatio", hitRatio);
        
        return stats;
    }
    
    /**
     * Gets or creates the data map for a specific node.
     * 
     * @param nodeId The node ID
     * @return The data map for that node
     */
    private Map<String, Object> getOrCreateNodeData(String nodeId) {
        return nodeDataMap.computeIfAbsent(nodeId, k -> new ConcurrentHashMap<>());
    }
} 