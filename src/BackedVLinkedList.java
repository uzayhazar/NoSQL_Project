import com.fasterxml.jackson.databind.ObjectMapper;
import  provided_classes.KVStore;
import  provided_classes.VersionList;
import  provided_classes.Serializer;

public class BackedVLinkedList <P> implements VersionList<P>, Serializer<P>{
    private final VLinkedList<P> list;
    private final KVStore store;
    private final ObjectMapper objectMapper;

    public BackedVLinkedList(KVStore store) {
        this.store = store;
        this.list = new VLinkedList<>();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void append(P p, long timestamp) {
        list.append(p, timestamp);
        store.put(String.valueOf(timestamp), serialize(p));
    }

    @Override
    public P findVisible(long timestamp) {
        // VLinkedList already stores the payload in memory
        // The backing store is for persistence/durability
        return list.findVisible(timestamp);
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
