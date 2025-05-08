package com.example.cachecoordinator.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.cachecoordinator.node.NodeRegistry;

/**
 * Controller for node management operations.
 */
@RestController
@RequestMapping("/api/nodes")
public class NodeController {

    private final NodeRegistry nodeRegistry;
    
    @Autowired
    public NodeController(NodeRegistry nodeRegistry) {
        this.nodeRegistry = nodeRegistry;
    }
    
    /**
     * Gets a list of all active nodes.
     * 
     * @return List of node IDs
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getNodes() {
        Map<String, Object> response = new HashMap<>();
        response.put("activeNodes", nodeRegistry.getActiveNodes());
        response.put("totalNodeCount", nodeRegistry.getTotalNodeCount());
        response.put("activeNodeCount", nodeRegistry.getActiveNodeCount());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Registers a new node.
     * 
     * @param nodeRequest Node registration request
     * @return HTTP 201 if created, 409 if already exists
     */
    @PostMapping
    public ResponseEntity<Void> registerNode(@RequestBody NodeRegistrationRequest nodeRequest) {
        boolean added = nodeRegistry.addNode(
            nodeRequest.getId(), 
            nodeRequest.getEndpoint(), 
            nodeRequest.getCapacity()
        );
        
        if (added) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
    
    /**
     * Removes a node from the registry.
     * 
     * @param nodeId ID of the node to remove
     * @return HTTP 200 if removed, 404 if not found
     */
    @DeleteMapping("/{nodeId}")
    public ResponseEntity<Void> unregisterNode(@PathVariable String nodeId) {
        boolean removed = nodeRegistry.removeNode(nodeId);
        
        if (removed) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Request body for node registration.
     */
    public static class NodeRegistrationRequest {
        private String id;
        private String endpoint;
        private int capacity;
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public int getCapacity() {
            return capacity;
        }
        
        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }
    }
} 