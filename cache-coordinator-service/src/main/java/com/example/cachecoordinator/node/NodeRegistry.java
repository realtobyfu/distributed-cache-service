package com.example.cachecoordinator.node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Registry for managing cache nodes in the cluster.
 * It maintains a list of active nodes and periodically checks their health.
 */
@Component
public class NodeRegistry {
    
    private final Map<String, NodeInfo> nodes = new ConcurrentHashMap<>();
    private final ScheduledExecutorService healthCheckScheduler = Executors.newSingleThreadScheduledExecutor();
    
    @PostConstruct
    public void init() {
        // For demo purposes, add a local simulated node
        addNode("local-node-1", "http://localhost:8081", 100);
        
        // Start health check thread
        healthCheckScheduler.scheduleAtFixedRate(this::checkNodesHealth, 0, 10, TimeUnit.SECONDS);
    }
    
    @PreDestroy
    public void shutdown() {
        healthCheckScheduler.shutdown();
        try {
            if (!healthCheckScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                healthCheckScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Adds a new node to the registry.
     * 
     * @param nodeId Unique identifier for the node
     * @param endpoint HTTP endpoint for the node
     * @param capacity Maximum number of items the node can handle
     * @return true if the node was added, false if it already existed
     */
    public boolean addNode(String nodeId, String endpoint, int capacity) {
        if (nodes.containsKey(nodeId)) {
            return false;
        }
        
        NodeInfo nodeInfo = new NodeInfo(nodeId, endpoint, capacity);
        nodes.put(nodeId, nodeInfo);
        
        return true;
    }
    
    /**
     * Removes a node from the registry.
     * 
     * @param nodeId ID of the node to remove
     * @return true if the node was removed, false if it didn't exist
     */
    public boolean removeNode(String nodeId) {
        return nodes.remove(nodeId) != null;
    }
    
    /**
     * Gets all active nodes in the registry.
     * 
     * @return List of active node IDs
     */
    public List<String> getActiveNodes() {
        List<String> activeNodes = new ArrayList<>();
        
        for (Map.Entry<String, NodeInfo> entry : nodes.entrySet()) {
            if (entry.getValue().isActive()) {
                activeNodes.add(entry.getKey());
            }
        }
        
        return Collections.unmodifiableList(activeNodes);
    }
    
    /**
     * Gets information about a specific node.
     * 
     * @param nodeId ID of the node to get info for
     * @return NodeInfo if the node exists, null otherwise
     */
    public NodeInfo getNodeInfo(String nodeId) {
        return nodes.get(nodeId);
    }
    
    /**
     * Gets the total number of registered nodes.
     * 
     * @return Total node count
     */
    public int getTotalNodeCount() {
        return nodes.size();
    }
    
    /**
     * Gets the number of active nodes.
     * 
     * @return Active node count
     */
    public int getActiveNodeCount() {
        return (int) nodes.values().stream().filter(NodeInfo::isActive).count();
    }
    
    /**
     * Periodically checks the health of all registered nodes.
     * In a real implementation, this would make HTTP calls to each node's health endpoint.
     */
    private void checkNodesHealth() {
        for (NodeInfo node : nodes.values()) {
            try {
                // In a real implementation, this would be an HTTP call to the node's health endpoint
                // For demo purposes, we'll just assume the node is active
                node.setActive(true);
                node.setLastHealthCheckTime(System.currentTimeMillis());
            } catch (Exception e) {
                node.setActive(false);
                // Log the error in a real implementation
            }
        }
    }
    
    /**
     * Class representing information about a cache node.
     */
    public static class NodeInfo {
        private final String id;
        private final String endpoint;
        private final int capacity;
        private boolean active;
        private long lastHealthCheckTime;
        
        public NodeInfo(String id, String endpoint, int capacity) {
            this.id = id;
            this.endpoint = endpoint;
            this.capacity = capacity;
            this.active = true;
            this.lastHealthCheckTime = System.currentTimeMillis();
        }
        
        public String getId() {
            return id;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public int getCapacity() {
            return capacity;
        }
        
        public boolean isActive() {
            return active;
        }
        
        public void setActive(boolean active) {
            this.active = active;
        }
        
        public long getLastHealthCheckTime() {
            return lastHealthCheckTime;
        }
        
        public void setLastHealthCheckTime(long lastHealthCheckTime) {
            this.lastHealthCheckTime = lastHealthCheckTime;
        }
    }
} 