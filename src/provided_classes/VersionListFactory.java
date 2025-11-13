package provided_classes;

public interface VersionListFactory<P> {
    VersionList<P> create(KVStore store, Serializer<P> serializer);
}
