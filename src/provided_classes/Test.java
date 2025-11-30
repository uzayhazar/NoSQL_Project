package provided_classes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Test {
    public record Payload(String title, String comment, String timestamp){};
    
    public static void main(String[] args) {
        // Uncomment the test or benchmark you want to run
        
        // runTest();
        // runBenchmark();
    }

    public static void runTest() {
        System.out.println("=== Running Test with test_data.csv ===\n");
        
        // Read test data
        List<Map.Entry<String, Payload>> data = readData("data/test_data.csv");
        System.out.println("Loaded " + data.size() + " entries from test_data.csv\n");

        // Test with BackedVLinkedList
        System.out.println("--- Testing with BackedVLinkedList ---");
        testWithImplementation(data, "BackedVLinkedList");

        // Test with BackedFrugalSkiplist
        System.out.println("\n--- Testing with BackedFrugalSkiplist ---");
        testWithImplementation(data, "BackedFrugalSkiplist");
        
        // Test with BackedVWeaverFrugalSkiplist (Task 1.4)
        System.out.println("\n--- Testing with BackedVWeaverFrugalSkiplist (VWeaver) ---");
        testVWeaver(data);
    }

    private static void testWithImplementation(List<Map.Entry<String, Payload>> data, String implName) {
        try {
            // Create JedisKVStore
            Class<?> jedisKVStoreClass = Class.forName("JedisKVStore");
            FlushableKVStore store = (FlushableKVStore) jedisKVStoreClass.getDeclaredConstructor().newInstance();
            
            // Flush DB before test
            store.flushDB();
            
            // Create factory
            Class<?> factoryClass = Class.forName(implName + "Factory");
            VersionListFactory<Payload> factory = (VersionListFactory<Payload>) factoryClass.getDeclaredConstructor().newInstance();
            
            // Create MultiVersionMap
            Class<?> mvmClass = Class.forName("BackedSimpleMVM");
            MultiVersionMap<String, Payload> mvm = (MultiVersionMap<String, Payload>) 
                mvmClass.getDeclaredConstructor(VersionListFactory.class, KVStore.class).newInstance(factory, store);
            
            // Insert data
            long startInsert = System.nanoTime();
            for (Map.Entry<String, Payload> entry : data) {
                mvm.append(entry.getKey(), entry.getValue());
            }
            long endInsert = System.nanoTime();
            System.out.println("Insertion time: " + (endInsert - startInsert) / 1_000_000.0 + " ms");
            
            // Query range snapshot
            System.out.println("\nRange snapshot [KEY002, KEY004] at timestamp 20:");
            Iterator<Map.Entry<String, Payload>> snapshot = mvm.rangeSnapshot("KEY002", true, "KEY004", true, 20);
            while (snapshot.hasNext()) {
                Map.Entry<String, Payload> entry = snapshot.next();
                System.out.println(entry.getKey() + "=" + entry.getValue());
            }
            
            // Expected output (for verification):
            // KEY002=Payload[title=Some Title for KEY002, comment=Change 3 for key KEY002, timestamp=19]
            // KEY003=Payload[title=Some Title for KEY003, comment=Change 4 for key KEY003, timestamp=20]
            // KEY004=Payload[title=Some Title for KEY004, comment=Change 3 for key KEY004, timestamp=13]
            
            store.flushDB();
            
        } catch (Exception e) {
            System.err.println("Error testing " + implName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void runBenchmark() {
        System.out.println("=== Running Benchmark with benchmark_data.csv ===\n");
        
        // Read benchmark data
        List<Map.Entry<String, Payload>> data = readData("data/benchmark_data.csv");
        System.out.println("Loaded " + data.size() + " entries from benchmark_data.csv\n");
        
        // Timestamps to query
        long[] timestamps = {10, 100, 500, 1_000, 5_000, 10_000, 50_000, 100_000, 500_000};
        int warmupRuns = 3;
        int benchmarkRuns = 5;
        
        System.out.println("Warmup runs: " + warmupRuns);
        System.out.println("Benchmark runs: " + benchmarkRuns);
        System.out.println();

        // Benchmark BackedVLinkedList
        System.out.println("=== Benchmarking BackedVLinkedList ===");
        benchmarkImplementation(data, "BackedVLinkedList", timestamps, warmupRuns, benchmarkRuns);

        // Benchmark BackedFrugalSkiplist
        System.out.println("\n=== Benchmarking BackedFrugalSkiplist ===");
        benchmarkImplementation(data, "BackedFrugalSkiplist", timestamps, warmupRuns, benchmarkRuns);
        
        // Benchmark BackedVWeaverMVM (Task 1.4)
        System.out.println("\n=== Benchmarking BackedVWeaverMVM (VWeaver) ===");
        benchmarkVWeaver(data, timestamps, warmupRuns, benchmarkRuns);
    }

    private static void benchmarkImplementation(List<Map.Entry<String, Payload>> data, String implName, 
                                                  long[] timestamps, int warmupRuns, int benchmarkRuns) {
        try {
            // Warmup
            for (int i = 0; i < warmupRuns; i++) {
                runBenchmarkIteration(data, implName, timestamps, false);
            }
            
            // Actual benchmark
            List<Long> insertionTimes = new ArrayList<>();
            List<List<Long>> queryTimes = new ArrayList<>();
            for (int i = 0; i < timestamps.length; i++) {
                queryTimes.add(new ArrayList<>());
            }
            
            for (int i = 0; i < benchmarkRuns; i++) {
                BenchmarkResult result = runBenchmarkIteration(data, implName, timestamps, true);
                insertionTimes.add(result.insertionTime);
                for (int j = 0; j < timestamps.length; j++) {
                    queryTimes.get(j).add(result.queryTimes.get(j));
                }
            }
            
            // Calculate and print averages
            System.out.println("\n--- Results (average of " + benchmarkRuns + " runs) ---");
            System.out.printf("Average insertion time: %.2f ms\n", average(insertionTimes) / 1_000_000.0);
            System.out.println("\nFull-range snapshot query times:");
            for (int i = 0; i < timestamps.length; i++) {
                System.out.printf("  Timestamp %,d: %.3f ms\n", timestamps[i], average(queryTimes.get(i)) / 1_000_000.0);
            }
            
        } catch (Exception e) {
            System.err.println("Error benchmarking " + implName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class BenchmarkResult {
        long insertionTime;
        List<Long> queryTimes;
        
        BenchmarkResult(long insertionTime, List<Long> queryTimes) {
            this.insertionTime = insertionTime;
            this.queryTimes = queryTimes;
        }
    }

    private static BenchmarkResult runBenchmarkIteration(List<Map.Entry<String, Payload>> data, 
                                                          String implName, long[] timestamps, boolean verbose) {
        try {
            // Create JedisKVStore
            Class<?> jedisKVStoreClass = Class.forName("JedisKVStore");
            FlushableKVStore store = (FlushableKVStore) jedisKVStoreClass.getDeclaredConstructor().newInstance();
            
            // Flush DB before benchmark
            store.flushDB();
            
            // Create factory
            Class<?> factoryClass = Class.forName(implName + "Factory");
            VersionListFactory<Payload> factory = (VersionListFactory<Payload>) factoryClass.getDeclaredConstructor().newInstance();
            
            // Create MultiVersionMap
            Class<?> mvmClass = Class.forName("BackedSimpleMVM");
            MultiVersionMap<String, Payload> mvm = (MultiVersionMap<String, Payload>) 
                mvmClass.getDeclaredConstructor(VersionListFactory.class, KVStore.class).newInstance(factory, store);
            
            // Measure insertion time
            long startInsert = System.nanoTime();
            for (Map.Entry<String, Payload> entry : data) {
                mvm.append(entry.getKey(), entry.getValue());
            }
            long endInsert = System.nanoTime();
            long insertionTime = endInsert - startInsert;
            
            // Measure query times
            List<Long> queryTimes = new ArrayList<>();
            for (long timestamp : timestamps) {
                long startQuery = System.nanoTime();
                Iterator<Map.Entry<String, Payload>> snapshot = mvm.snapshot(timestamp);
                int count = 0;
                while (snapshot.hasNext()) {
                    snapshot.next();
                    count++;
                }
                long endQuery = System.nanoTime();
                queryTimes.add(endQuery - startQuery);
                
                if (verbose) {
                    System.out.printf("Snapshot at %,d returned %d entries in %.3f ms\n", 
                        timestamp, count, (endQuery - startQuery) / 1_000_000.0);
                }
            }
            
            store.flushDB();
            
            return new BenchmarkResult(insertionTime, queryTimes);
            
        } catch (Exception e) {
            throw new RuntimeException("Error in benchmark iteration", e);
        }
    }

    /**
     * Test VWeaver implementation (Task 1.4)
     */
    private static void testVWeaver(List<Map.Entry<String, Payload>> data) {
        try {
            // Create JedisKVStore
            Class<?> jedisKVStoreClass = Class.forName("JedisKVStore");
            FlushableKVStore store = (FlushableKVStore) jedisKVStoreClass.getDeclaredConstructor().newInstance();
            
            // Flush DB before test
            store.flushDB();
            
            // Create factory for VWeaver Frugal Skiplist
            Class<?> factoryClass = Class.forName("BackedVWeaverFrugalSkiplistFactory");
            VersionListFactory<Payload> factory = (VersionListFactory<Payload>) factoryClass.getDeclaredConstructor().newInstance();
            
            // Create BackedVWeaverMVM
            Class<?> mvmClass = Class.forName("BackedVWeaverMVM");
            MultiVersionMap<String, Payload> mvm = (MultiVersionMap<String, Payload>) 
                mvmClass.getDeclaredConstructor(VersionListFactory.class, KVStore.class).newInstance(factory, store);
            
            // Insert data
            long startInsert = System.nanoTime();
            for (Map.Entry<String, Payload> entry : data) {
                mvm.append(entry.getKey(), entry.getValue());
            }
            long endInsert = System.nanoTime();
            System.out.println("Insertion time: " + (endInsert - startInsert) / 1_000_000.0 + " ms");
            
            // Query range snapshot
            System.out.println("\nRange snapshot [KEY002, KEY004] at timestamp 20:");
            Iterator<Map.Entry<String, Payload>> snapshot = mvm.rangeSnapshot("KEY002", true, "KEY004", true, 20);
            while (snapshot.hasNext()) {
                Map.Entry<String, Payload> entry = snapshot.next();
                System.out.println(entry.getKey() + "=" + entry.getValue());
            }
            
            // Expected output (should match other implementations):
            // KEY002=Payload[title=Some Title for KEY002, comment=Change 3 for key KEY002, timestamp=19]
            // KEY003=Payload[title=Some Title for KEY003, comment=Change 4 for key KEY003, timestamp=20]
            // KEY004=Payload[title=Some Title for KEY004, comment=Change 3 for key KEY004, timestamp=13]
            
            store.flushDB();
            
        } catch (Exception e) {
            System.err.println("Error testing VWeaver: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Benchmark VWeaver implementation (Task 1.4)
     */
    private static void benchmarkVWeaver(List<Map.Entry<String, Payload>> data, 
                                          long[] timestamps, int warmupRuns, int benchmarkRuns) {
        try {
            // Warmup
            for (int i = 0; i < warmupRuns; i++) {
                runVWeaverBenchmarkIteration(data, timestamps, false);
            }
            
            // Actual benchmark
            List<Long> insertionTimes = new ArrayList<>();
            List<List<Long>> queryTimes = new ArrayList<>();
            List<List<Long>> rangeQueryTimes = new ArrayList<>();
            for (int i = 0; i < timestamps.length; i++) {
                queryTimes.add(new ArrayList<>());
                rangeQueryTimes.add(new ArrayList<>());
            }
            
            for (int i = 0; i < benchmarkRuns; i++) {
                VWeaverBenchmarkResult result = runVWeaverBenchmarkIteration(data, timestamps, false);
                insertionTimes.add(result.insertionTime);
                for (int j = 0; j < timestamps.length; j++) {
                    queryTimes.get(j).add(result.queryTimes.get(j));
                    rangeQueryTimes.get(j).add(result.rangeQueryTimes.get(j));
                }
            }
            
            // Calculate and print averages
            System.out.println("\n--- Results (average of " + benchmarkRuns + " runs) ---");
            System.out.printf("Average insertion time: %.2f ms\n", average(insertionTimes) / 1_000_000.0);
            System.out.println("\nFull-range snapshot query times:");
            for (int i = 0; i < timestamps.length; i++) {
                System.out.printf("  Timestamp %,d: %.3f ms\n", timestamps[i], average(queryTimes.get(i)) / 1_000_000.0);
            }
            System.out.println("\nRange snapshot query times (first 10% of keys):");
            for (int i = 0; i < timestamps.length; i++) {
                System.out.printf("  Timestamp %,d: %.3f ms\n", timestamps[i], average(rangeQueryTimes.get(i)) / 1_000_000.0);
            }
            
        } catch (Exception e) {
            System.err.println("Error benchmarking VWeaver: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class VWeaverBenchmarkResult {
        long insertionTime;
        List<Long> queryTimes;
        List<Long> rangeQueryTimes;
        
        VWeaverBenchmarkResult(long insertionTime, List<Long> queryTimes, List<Long> rangeQueryTimes) {
            this.insertionTime = insertionTime;
            this.queryTimes = queryTimes;
            this.rangeQueryTimes = rangeQueryTimes;
        }
    }

    private static VWeaverBenchmarkResult runVWeaverBenchmarkIteration(List<Map.Entry<String, Payload>> data, 
                                                                      long[] timestamps, boolean verbose) {
        try {
            // Create JedisKVStore
            Class<?> jedisKVStoreClass = Class.forName("JedisKVStore");
            FlushableKVStore store = (FlushableKVStore) jedisKVStoreClass.getDeclaredConstructor().newInstance();
            
            // Flush DB before benchmark
            store.flushDB();
            
            // Create factory for VWeaver Frugal Skiplist
            Class<?> factoryClass = Class.forName("BackedVWeaverFrugalSkiplistFactory");
            VersionListFactory<Payload> factory = (VersionListFactory<Payload>) factoryClass.getDeclaredConstructor().newInstance();
            
            // Create BackedVWeaverMVM
            Class<?> mvmClass = Class.forName("BackedVWeaverMVM");
            MultiVersionMap<String, Payload> mvm = (MultiVersionMap<String, Payload>) 
                mvmClass.getDeclaredConstructor(VersionListFactory.class, KVStore.class).newInstance(factory, store);
            
            // Measure insertion time
            long startInsert = System.nanoTime();
            for (Map.Entry<String, Payload> entry : data) {
                mvm.append(entry.getKey(), entry.getValue());
            }
            long endInsert = System.nanoTime();
            long insertionTime = endInsert - startInsert;
            
            // Measure query times (full snapshot)
            List<Long> queryTimes = new ArrayList<>();
            for (long timestamp : timestamps) {
                long startQuery = System.nanoTime();
                Iterator<Map.Entry<String, Payload>> snapshot = mvm.snapshot(timestamp);
                int count = 0;
                while (snapshot.hasNext()) {
                    snapshot.next();
                    count++;
                }
                long endQuery = System.nanoTime();
                queryTimes.add(endQuery - startQuery);
                
                if (verbose) {
                    System.out.printf("Full snapshot at %,d returned %d entries in %.3f ms\n", 
                        timestamp, count, (endQuery - startQuery) / 1_000_000.0);
                }
            }
            
            // Measure range query times (to test kRidgy optimization)
            List<Long> rangeQueryTimes = new ArrayList<>();
            // Find first and last keys for range query
            String firstKey = data.get(0).getKey();
            String lastKey = data.get(data.size() - 1).getKey();
            // Use a subset (first 10% of keys) for range query
            int rangeSize = Math.max(1, data.size() / 10);
            String rangeEndKey = data.get(Math.min(rangeSize - 1, data.size() - 1)).getKey();
            
            for (long timestamp : timestamps) {
                long startQuery = System.nanoTime();
                Iterator<Map.Entry<String, Payload>> rangeSnapshot = mvm.rangeSnapshot(firstKey, true, rangeEndKey, true, timestamp);
                int count = 0;
                while (rangeSnapshot.hasNext()) {
                    rangeSnapshot.next();
                    count++;
                }
                long endQuery = System.nanoTime();
                rangeQueryTimes.add(endQuery - startQuery);
                
                if (verbose) {
                    System.out.printf("Range snapshot [%s, %s] at %,d returned %d entries in %.3f ms\n", 
                        firstKey, rangeEndKey, timestamp, count, (endQuery - startQuery) / 1_000_000.0);
                }
            }
            
            store.flushDB();
            
            return new VWeaverBenchmarkResult(insertionTime, queryTimes, rangeQueryTimes);
            
        } catch (Exception e) {
            throw new RuntimeException("Error in VWeaver benchmark iteration", e);
        }
    }

    private static double average(List<Long> values) {
        return values.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }

    public static List<Map.Entry<String, Payload>> readData(String path) {
        List<Map.Entry<String, Payload>> l = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            // Skip header
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", 4); // key,title,comment,"version"
                String key = values[0];
                String title = values[1];
                String comment = values[2];
                String timestamp = values[3];

                l.add(new AbstractMap.SimpleEntry<>(key, new Payload(title, comment, timestamp)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return l;
    }
}
