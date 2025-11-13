import provided_classes.VersionList;

public class FrugalSkipList<P>  implements VersionList<P> {

    class FSListNode {
        long timestamp;
        Object payload;
        FSListNode next;
        FSListNode ridgy;
        int level;

        FSListNode(long timestamp, Object payload) {
            this.timestamp = timestamp;
            this.payload = payload;
            this.level = 0;
        }
    }
    @Override
    public void append(Object o, long timestamp) {

    }

    @Override
    public P findVisible(long timestamp) {
        return null;
    }
}
