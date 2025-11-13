package provided_classes;

public interface Serializer<T> {

    String serialize(T t);
    T deSerialize(String serializedT);
}
