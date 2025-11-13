import com.fasterxml.jackson.databind.ObjectMapper;
import  provided_classes.KVStore;

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
        P result = list.findVisible(timestamp);
        if (result != null) {
            return deSerialize(store.get(String.valueOf(timestamp)));
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
