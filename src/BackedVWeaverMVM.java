import provided_classes.KVStore;
import provided_classes.MultiVersionMap;
import provided_classes.VersionList;
import provided_classes.VersionListFactory;

import java.util.*;

/**
 * VWeaver Multi-Version Map implementation.
 * Uses kRidgy pointers in Frugal Skiplists to enable efficient range queries
 * by allowing traversal between keys' version lists.
 */
public class BackedVWeaverMVM<K extends Comparable<? super K>, P> implements MultiVersionMap<K, P> {
    private final TreeMap<K, VersionList<P>> treeMap;
    private final VersionListFactory<P> versionListFactory;
    private final KVStore store;
    private long versionCounter;

    public BackedVWeaverMVM(VersionListFactory<P> versionListFactory, KVStore store) {
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
        boolean isNewKey = (versionList == null);
        
        if (isNewKey) {
            versionList = versionListFactory.create(store, null);
            treeMap.put(k, versionList);
        }
        
        long version = versionCounter++;
        versionList.append(p, version);
        
        // Set kRidgy pointer: point from current key's head to next key's visible node
        setKRidgyPointers(k, version);
        
        return version;
    }

    /**
     * Sets kRidgy pointers after appending a new version.
     * The kRidgy pointer from the current key's head should point to
     * the visible node in the next key at the same timestamp.
     */
    private void setKRidgyPointers(K currentKey, long timestamp) {
        // Get the current key's version list (must be BackedVWeaverFrugalSkiplist)
        VersionList<P> currentList = treeMap.get(currentKey);
        if (!(currentList instanceof BackedVWeaverFrugalSkiplist)) {
            return; // Not a VWeaver list, skip kRidgy setting
        }
        
        BackedVWeaverFrugalSkiplist<P> currentVWeaverList = (BackedVWeaverFrugalSkiplist<P>) currentList;
        VWeaverFrugalSkiplist<P>.FSListNode currentHead = currentVWeaverList.getHead();
        
        if (currentHead == null) {
            return;
        }
        
        // Find the next key in sorted order
        K nextKey = treeMap.higherKey(currentKey);
        
        if (nextKey != null) {
            // Get the next key's version list
            VersionList<P> nextList = treeMap.get(nextKey);
            if (nextList instanceof BackedVWeaverFrugalSkiplist) {
                BackedVWeaverFrugalSkiplist<P> nextVWeaverList = (BackedVWeaverFrugalSkiplist<P>) nextList;
                // Find the visible node in the next key at the current timestamp
                VWeaverFrugalSkiplist<P>.FSListNode visibleNode = nextVWeaverList.findVisibleNode(timestamp);
                // Set kRidgy pointer
                currentHead.kRidgy = visibleNode;
            }
        } else {
            // No next key, set kRidgy to null
            currentHead.kRidgy = null;
        }
        
        // Also update kRidgy for the previous key if it exists
        K prevKey = treeMap.lowerKey(currentKey);
        if (prevKey != null) {
            VersionList<P> prevList = treeMap.get(prevKey);
            if (prevList instanceof BackedVWeaverFrugalSkiplist) {
                BackedVWeaverFrugalSkiplist<P> prevVWeaverList = (BackedVWeaverFrugalSkiplist<P>) prevList;
                VWeaverFrugalSkiplist<P>.FSListNode prevHead = prevVWeaverList.getHead();
                if (prevHead != null) {
                    // Update previous key's kRidgy to point to current key's visible node
                    VWeaverFrugalSkiplist<P>.FSListNode currentVisible = currentVWeaverList.findVisibleNode(timestamp);
                    prevHead.kRidgy = currentVisible;
                }
            }
        }
    }

    @Override
    public Iterator<Map.Entry<K, P>> rangeSnapshot(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive, long timestamp) {
        List<Map.Entry<K, P>> result = new ArrayList<>();
        
        // Get the submap of keys in range
        NavigableMap<K, VersionList<P>> subMap = treeMap.subMap(fromKey, fromInclusive, toKey, toInclusive);
        
        if (subMap.isEmpty()) {
            return result.iterator();
        }
        
        // Start with the first key
        K currentKey = subMap.firstKey();
        
        // Try to use kRidgy-based traversal if all lists are VWeaver lists
        boolean useKRidgy = true;
        for (VersionList<P> list : subMap.values()) {
            if (!(list instanceof BackedVWeaverFrugalSkiplist)) {
                useKRidgy = false;
                break;
            }
        }
        
        if (useKRidgy) {
            // VWeaver optimization: use kRidgy pointers to jump between keys
            BackedVWeaverFrugalSkiplist<P> firstList = (BackedVWeaverFrugalSkiplist<P>) subMap.get(currentKey);
            VWeaverFrugalSkiplist<P>.FSListNode currentNode = firstList.findVisibleNode(timestamp);
            
            // Traverse keys using kRidgy pointers
            while (currentKey != null && currentKey.compareTo(toKey) <= 0) {
                if (currentNode != null && currentNode.payload != null) {
                    result.add(new AbstractMap.SimpleEntry<>(currentKey, currentNode.payload));
                }
                
                // Check if we can follow kRidgy to next key
                if (currentNode != null && currentNode.kRidgy != null) {
                    // Follow kRidgy pointer
                    currentNode = currentNode.kRidgy;
                    // Move to next key
                    currentKey = subMap.higherKey(currentKey);
                    // Verify that currentNode belongs to currentKey
                    if (currentKey != null) {
                        BackedVWeaverFrugalSkiplist<P> currentList = (BackedVWeaverFrugalSkiplist<P>) subMap.get(currentKey);
                        VWeaverFrugalSkiplist<P>.FSListNode expectedNode = currentList.findVisibleNode(timestamp);
                        // If kRidgy doesn't match, use expected node
                        if (expectedNode != currentNode) {
                            currentNode = expectedNode;
                        }
                    }
                } else {
                    // No kRidgy, move to next key and find visible node
                    currentKey = subMap.higherKey(currentKey);
                    if (currentKey != null) {
                        BackedVWeaverFrugalSkiplist<P> nextList = (BackedVWeaverFrugalSkiplist<P>) subMap.get(currentKey);
                        currentNode = nextList.findVisibleNode(timestamp);
                    } else {
                        currentNode = null;
                    }
                }
            }
        } else {
            // Fallback: regular traversal without kRidgy
            for (Map.Entry<K, VersionList<P>> entry : subMap.entrySet()) {
                P payload = entry.getValue().findVisible(timestamp);
                if (payload != null) {
                    result.add(new AbstractMap.SimpleEntry<>(entry.getKey(), payload));
                }
            }
        }
        
        return result.iterator();
    }

    @Override
    public Iterator<Map.Entry<K, P>> snapshot(long timestamp) {
        // Full snapshot: query all keys
        if (treeMap.isEmpty()) {
            return Collections.emptyIterator();
        }
        return rangeSnapshot(
            treeMap.firstKey(), true,
            treeMap.lastKey(), true,
            timestamp
        );
    }
}
