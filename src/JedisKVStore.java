import provided_classes.FlushableKVStore;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class JedisKVStore implements FlushableKVStore {
    private final JedisPool jedisPool;

    public JedisKVStore(String host, int port) {
        this.jedisPool = new JedisPool(host, port);
    }

    public JedisKVStore() {
        this("localhost", 6379);
    }

    @Override
    public void put(String storeKey, String storeValue) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(storeKey, storeValue);
        }
    }

    @Override
    public String get(String storeKey) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(storeKey);
        }
    }

    @Override
    public void flushDB() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushDB();
        }
    }

    public void close() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}

