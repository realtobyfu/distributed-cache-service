package com.example.cachecoordinator.partition;

import java.util.List;

/**
 * Interface for cache data partitioning strategies.
 */
public interface PartitionStrategy {
    
    /**
     * Calculates which node should handle a specific key.
     * 
     * @param key The key to map to a node
     * @param availableNodes List of available node identifiers
     * @return The node identifier that should handle this key
     */
    String getNodeForKey(String key, List<String> availableNodes);
    
    /**
     * Recalculates the key distribution after node changes.
     * 
     * @param currentNodes Current list of available node identifiers
     * @param previousNodes Previous list of node identifiers before change
     * @return A mapping of keys that need to be redistributed from their previous node to their new node
     */
    default void rebalance(List<String> currentNodes, List<String> previousNodes) {
        // Default implementation does nothing
    }
    
    /**
     * Gets a list of backup nodes for a key for redundancy.
     * 
     * @param key The key to find backup nodes for
     * @param primaryNode The primary node for this key
     * @param availableNodes List of all available nodes
     * @param replicaCount Number of replicas to maintain
     * @return List of node identifiers that should maintain replicas
     */
    List<String> getBackupNodesForKey(String key, String primaryNode, 
                                      List<String> availableNodes, int replicaCount);
} 