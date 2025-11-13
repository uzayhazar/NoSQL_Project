import com.fasterxml.jackson.databind.ObjectMapper;
import provided_classes.KVStore;
import provided_classes.Serializer;
import provided_classes.VersionList;

import java.util.Random;

public class BackedFrugalSkiplist<P> implements VersionList<P>, Serializer<P> {
    private static final int MAX_LEVEL = 16;
    private static final double P = 0.5;
    
    private final SkipListNode head;
    private final KVStore store;
    private final ObjectMapper objectMapper;
    private final Random random;
    private int level;

    private class SkipListNode {
        long timestamp;
        P payload;
        SkipListNode[] forward;

        @SuppressWarnings("unchecked")
        SkipListNode(int level, long timestamp, P payload) {
            this.timestamp = timestamp;
            this.payload = payload;
            this.forward = new SkipListNode[level + 1];
        }
    }

    public BackedFrugalSkiplist(KVStore store) {
        this.store = store;
        this.objectMapper = new ObjectMapper();
        this.random = new Random();
        this.level = 0;
        this.head = new SkipListNode(MAX_LEVEL, -1, null);
    }

    private int randomLevel() {
        int lvl = 0;
        while (random.nextDouble() < P && lvl < MAX_LEVEL) {
            lvl++;
        }
        return lvl;
    }

    @Override
    public void append(P p, long timestamp) {
        @SuppressWarnings("unchecked")
        SkipListNode[] update = new SkipListNode[MAX_LEVEL + 1];
        SkipListNode current = head;

        // Find position to insert
        for (int i = level; i >= 0; i--) {
            while (current.forward[i] != null && current.forward[i].timestamp < timestamp) {
                current = current.forward[i];
            }
            update[i] = current;
        }

        // Insert new node
        int newLevel = randomLevel();
        if (newLevel > level) {
            for (int i = level + 1; i <= newLevel; i++) {
                update[i] = head;
            }
            level = newLevel;
        }

        SkipListNode newNode = new SkipListNode(newLevel, timestamp, p);
        for (int i = 0; i <= newLevel; i++) {
            newNode.forward[i] = update[i].forward[i];
            update[i].forward[i] = newNode;
        }

        // Store in backing store
        store.put(String.valueOf(timestamp), serialize(p));
    }

    @Override
    public P findVisible(long timestamp) {
        SkipListNode current = head;

        // Search for the largest timestamp <= given timestamp
        for (int i = level; i >= 0; i--) {
            while (current.forward[i] != null && current.forward[i].timestamp <= timestamp) {
                current = current.forward[i];
            }
        }

        // current now points to the node with largest timestamp <= given timestamp
        // Return the payload stored in the node (backing store is for persistence)
        if (current != head && current.timestamp <= timestamp) {
            return current.payload;
        }

        return null;
    }

    @Override
    public String serialize(P p) {
        if (p != null) {
            try {
                return objectMapper.writeValueAsString(p);
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }

    @Override
    public P deSerialize(String serializedT) {
        if (serializedT != null && !serializedT.isEmpty()) {
            try {
                return objectMapper.readValue(serializedT, (Class<P>) Object.class);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}

