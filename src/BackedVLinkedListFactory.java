import provided_classes.KVStore;
import provided_classes.Serializer;
import provided_classes.VersionList;
import provided_classes.VersionListFactory;

public class BackedVLinkedListFactory<P> implements VersionListFactory<P> {
    @Override
    public VersionList<P> create(KVStore store, Serializer<P> serializer) {
        return new BackedVLinkedList<>(store);
    }
}

