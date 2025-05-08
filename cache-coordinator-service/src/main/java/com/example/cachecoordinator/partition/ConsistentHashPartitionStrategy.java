package com.example.cachecoordinator.partition;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementation of PartitionStrategy using consistent hashing.
 * Consistent hashing minimizes the number of keys that need to be remapped
 * when nodes are added or removed from the cache cluster.
 */
@Component
public class ConsistentHashPartitionStrategy implements PartitionStrategy {

    private final NavigableMap<Long, String> ring = new ConcurrentSkipListMap<>();
    private final int virtualNodeCount;
    
    /**
     * Constructor with configurable virtual node count.
     * 
     * @param virtualNodeCount Number of virtual nodes per physical node
     */
    public ConsistentHashPartitionStrategy(@Value("${cache.virtualNodeCount:100}") int virtualNodeCount) {
        this.virtualNodeCount = virtualNodeCount;
    }
    
    @Override
    public String getNodeForKey(String key, List<String> availableNodes) {
        if (availableNodes == null || availableNodes.isEmpty()) {
            throw new IllegalStateException("No available nodes to handle request");
        }
        
        // Make sure the ring is up-to-date with the available nodes
        updateRing(availableNodes);
        
        // If the ring is still empty, default to the first node
        if (ring.isEmpty()) {
            return availableNodes.get(0);
        }
        
        // Find the node for this key on the ring
        long hash = hash(key);
        SortedMap<Long, String> tailMap = ring.tailMap(hash);
        Long nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        
        return ring.get(nodeHash);
    }
    
    @Override
    public List<String> getBackupNodesForKey(String key, String primaryNode, 
                                           List<String> availableNodes, int replicaCount) {
        List<String> backupNodes = new ArrayList<>();
        if (availableNodes.size() <= 1) {
            return backupNodes; // No backup nodes available
        }
        
        // Make sure the ring is up-to-date with the available nodes
        updateRing(availableNodes);
        
        // Find the node position on the ring
        long hash = hash(key);
        SortedMap<Long, String> tailMap = ring.tailMap(hash, false); // Exclude the exact match
        
        int count = 0;
        // Collect backup nodes clockwise around the ring
        for (String node : tailMap.values()) {
            if (!node.equals(primaryNode) && !backupNodes.contains(node)) {
                backupNodes.add(node);
                count++;
                if (count >= replicaCount) break;
            }
        }
        
        // If we need more nodes, wrap around to the beginning of the ring
        if (count < replicaCount) {
            for (String node : ring.values()) {
                if (!node.equals(primaryNode) && !backupNodes.contains(node)) {
                    backupNodes.add(node);
                    count++;
                    if (count >= replicaCount) break;
                }
            }
        }
        
        return backupNodes;
    }
    
    /**
     * Updates the hash ring to reflect the current available nodes.
     * 
     * @param availableNodes List of currently available nodes
     */
    private void updateRing(List<String> availableNodes) {
        // Clear and rebuild the ring for simplicity
        // In a production implementation, you would want to only add/remove changed nodes
        ring.clear();
        
        for (String node : availableNodes) {
            for (int i = 0; i < virtualNodeCount; i++) {
                String virtualNode = node + "-" + i;
                ring.put(hash(virtualNode), node);
            }
        }
    }
    
    /**
     * Hashes a key to determine its position on the consistent hash ring.
     * 
     * @param key The key to hash
     * @return The hash value
     */
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return ((long) (digest[3] & 0xFF) << 24) |
                   ((long) (digest[2] & 0xFF) << 16) |
                   ((long) (digest[1] & 0xFF) << 8) |
                   ((long) (digest[0] & 0xFF));
        } catch (NoSuchAlgorithmException e) {
            // Fallback to a simpler hash function
            return key.hashCode() & 0xFFFFFFFFL;
        }
    }
} 