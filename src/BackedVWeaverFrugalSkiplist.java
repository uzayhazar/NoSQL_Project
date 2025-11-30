import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import provided_classes.KVStore;
import provided_classes.Serializer;
import provided_classes.VersionList;

/**
 * Backed version of VWeaver Frugal Skiplist with Redis persistence.
 * Nodes are serialized and stored in KVStore using their timestamp as key.
 */
public class BackedVWeaverFrugalSkiplist<P> implements VersionList<P>, Serializer<P> {
    private final VWeaverFrugalSkiplist<P> list;
    private final KVStore store;
    private final ObjectMapper objectMapper;

    public BackedVWeaverFrugalSkiplist(KVStore store) {
        this.store = store;
        this.list = new VWeaverFrugalSkiplist<>();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void append(P p, long timestamp) {
        list.append(p, timestamp);
        // Store serialized payload in backing store
        store.put(String.valueOf(timestamp), serialize(p));
    }

    @Override
    public P findVisible(long timestamp) {
        // Return payload from in-memory structure
        // Backing store is for persistence/durability
        return list.findVisible(timestamp);
    }

    /**
     * Gets the head node for kRidgy pointer management.
     */
    public VWeaverFrugalSkiplist<P>.FSListNode getHead() {
        return list.getHead();
    }

    /**
     * Finds the visible node at given timestamp.
     * Used for setting kRidgy pointers.
     */
    public VWeaverFrugalSkiplist<P>.FSListNode findVisibleNode(long timestamp) {
        return list.findVisibleNode(timestamp);
    }

    @Override
    public String serialize(P p) {
        if (p != null) {
            try {
                return objectMapper.writeValueAsString(p);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return "";
    }

    @Override
    public P deSerialize(String serializedT) {
        if (serializedT != null && !serializedT.isEmpty()) {
            try {
                return objectMapper.readValue(serializedT, (Class<P>) Object.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
