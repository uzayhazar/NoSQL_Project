import provided_classes.VersionList;

import java.util.Random;
public class FrugalSkipList<P>  implements VersionList<P> {

    private FSListNode head;
    private final Random random = new Random();
    class FSListNode {
        long timestamp;
        P payload;
        FSListNode next;
        FSListNode ridgy;
        int level;

        FSListNode(long timestamp, P payload) {
            this.timestamp = timestamp;
            this.payload = payload;
            this.level = 0;
        }
    }
    @Override
    public void append(P o, long timestamp) {
        FSListNode newVersion = new FSListNode(timestamp, o);
        newVersion.next = head;
        if(head != null && random.nextBoolean()) {
            newVersion.level = head.level + 1;
        }
        else {
            newVersion.level = 0;
        }

        FSListNode current = head;
        while (current != null && current.level < newVersion.level) {
            current = current.ridgy;
        }
        newVersion.ridgy = current;
        head = newVersion;
    }

    @Override
    public P findVisible(long timestamp) {
        FSListNode current = head;
        while (current != null && current.timestamp > timestamp) {

            if (current.ridgy != null && current.ridgy.timestamp > timestamp) {
                current = current.ridgy;
            } else {
                current = current.next;
            }
        }
        return current != null ? current.payload : null;
    }
}
