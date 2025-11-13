import provided_classes.KVStore;
import provided_classes.MultiVersionMap;
import provided_classes.VersionList;
import provided_classes.VersionListFactory;

import java.util.*;

public class BackedSimpleMVM<K extends Comparable<? super K>, P> implements MultiVersionMap<K, P> {
    private final TreeMap<K, VersionList<P>> treeMap;
    private final VersionListFactory<P> versionListFactory;
    private final KVStore store;
    private long versionCounter;

    public BackedSimpleMVM(VersionListFactory<P> versionListFactory, KVStore store) {
        this.treeMap = new TreeMap<>();
        this.versionListFactory = versionListFactory;
        this.store = store;
        this.versionCounter = 1;
    }

    @Override
    public Map.Entry<K, P> get(K k, long t) {
        VersionList<P> versionList = treeMap.get(k);
        if (versionList == null) {
            return null;
        }
        P payload = versionList.findVisible(t);
        if (payload == null) {
            return null;
        }
        return new AbstractMap.SimpleEntry<>(k, payload);
    }

    @Override
    public long append(K k, P p) {
        VersionList<P> versionList = treeMap.get(k);
        if (versionList == null) {
            versionList = versionListFactory.create(store, null);
            treeMap.put(k, versionList);
        }
        long version = versionCounter++;
        versionList.append(p, version);
        return version;
    }

    @Override
    public Iterator<Map.Entry<K, P>> rangeSnapshot(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive, long timestamp) {
        NavigableMap<K, VersionList<P>> subMap = treeMap.subMap(fromKey, fromInclusive, toKey, toInclusive);
        List<Map.Entry<K, P>> result = new ArrayList<>();
        
        for (Map.Entry<K, VersionList<P>> entry : subMap.entrySet()) {
            P payload = entry.getValue().findVisible(timestamp);
            if (payload != null) {
                result.add(new AbstractMap.SimpleEntry<>(entry.getKey(), payload));
            }
        }
        
        return result.iterator();
    }

    @Override
    public Iterator<Map.Entry<K, P>> snapshot(long timestamp) {
        List<Map.Entry<K, P>> result = new ArrayList<>();
        
        for (Map.Entry<K, VersionList<P>> entry : treeMap.entrySet()) {
            P payload = entry.getValue().findVisible(timestamp);
            if (payload != null) {
                result.add(new AbstractMap.SimpleEntry<>(entry.getKey(), payload));
            }
        }
        
        return result.iterator();
    }
}

