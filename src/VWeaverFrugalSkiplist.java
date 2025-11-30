import provided_classes.VersionList;

import java.util.Random;

/**
 * VWeaver Frugal Skiplist with kRidgy pointers for cross-list navigation.
 * Extends the Frugal Skiplist concept by adding kRidgy pointers that point
 * to nodes in the next key's version list, enabling efficient range queries.
 */
public class VWeaverFrugalSkiplist<P> implements VersionList<P> {
    private FSListNode head;
    private final Random random = new Random();
    
    /**
     * Node in the VWeaver Frugal Skiplist.
     * Contains:
     * - next: pointer to next node in same list (by timestamp)
     * - vRidgy: pointer for skipping within same list (Frugal Skiplist feature)
     * - kRidgy: pointer to node in next key's version list (VWeaver feature)
     * - level: determines vRidgy pointer behavior
     */
    class FSListNode {
        long timestamp;
        P payload;
        FSListNode next;      // Next node in same version list
        FSListNode vRidgy;     // Pointer for skipping within same list (renamed from ridgy for clarity)
        FSListNode kRidgy;    // Pointer to node in next key's version list
        int level;            // Level for vRidgy pointer management

        FSListNode(long timestamp, P payload) {
            this.timestamp = timestamp;
            this.payload = payload;
            this.level = 0;
            this.next = null;
            this.vRidgy = null;
            this.kRidgy = null;
        }
    }

    public VWeaverFrugalSkiplist() {
        this.head = null;
    }

    /**
     * Appends a new version to the head of the list.
     * Sets vRidgy pointer according to Frugal Skiplist algorithm.
     * kRidgy pointer should be set externally by the MultiVersionMap.
     */
    @Override
    public void append(P payload, long timestamp) {
        FSListNode newVersion = new FSListNode(timestamp, payload);
        newVersion.next = head;
        
        // Determine level for vRidgy pointer (Frugal Skiplist algorithm)
        if (head != null && random.nextBoolean()) {
            newVersion.level = head.level + 1;
        } else {
            newVersion.level = 0;
        }

        // Set vRidgy pointer: traverse until we find a node with level >= newVersion.level
        FSListNode current = head;
        while (current != null && current.level < newVersion.level) {
            current = current.vRidgy;
            if (current == null) break;
        }
        newVersion.vRidgy = current;
        
        head = newVersion;
    }

    /**
     * Finds the visible version at the given timestamp.
     * Uses vRidgy pointers to skip nodes efficiently.
     */
    @Override
    public P findVisible(long timestamp) {
        FSListNode current = head;
        
        while (current != null && current.timestamp > timestamp) {
            // Use vRidgy if it points to a node with timestamp > target
            if (current.vRidgy != null && current.vRidgy.timestamp > timestamp) {
                current = current.vRidgy;
            } else {
                // Otherwise, follow next pointer
                current = current.next;
            }
        }
        
        return current != null ? current.payload : null;
    }

    /**
     * Gets the head node (newest version).
     * Used by MultiVersionMap to set kRidgy pointers.
     */
    public FSListNode getHead() {
        return head;
    }

    /**
     * Finds the node that would be visible at the given timestamp.
     * Returns the node itself (not just the payload).
     * Used for setting kRidgy pointers.
     */
    public FSListNode findVisibleNode(long timestamp) {
        FSListNode current = head;
        
        while (current != null && current.timestamp > timestamp) {
            if (current.vRidgy != null && current.vRidgy.timestamp > timestamp) {
                current = current.vRidgy;
            } else {
                current = current.next;
            }
        }
        
        return current;
    }
}
