import provided_classes.VersionList;

public class VLinkedList<P> implements VersionList<P> {
    private Node head;
      private class Node {
        P payload;
        long timeStamp;
        Node next;

        Node(P payload, long timeStamp) {
            this.payload = payload;
            this.timeStamp = timeStamp;
            this.next = null;
        }
    }

    public VLinkedList() {
        head = null;
    }

    @Override
    public void append(P payload, long timestamp) {
        Node newNode = new Node(payload, timestamp);
        if( head != null)
            newNode.next = head;
        head = newNode;
    }

    @Override
    public P findVisible(long timestamp) {
        Node current = head;
        if (current == null) return null;
        
        // Find the newest version with timestamp <= requested timestamp
        // The list is ordered newest first (head has highest timestamp)
        while (current != null && current.timeStamp > timestamp) {
            current = current.next;
        }
        
        return current != null ? current.payload : null;
    }

}
