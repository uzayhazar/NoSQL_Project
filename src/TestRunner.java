import provided_classes.Test;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("  Multi-Version Map Test & Benchmark Runner");
        System.out.println("=======================================================\n");
        
        if (args.length == 0) {
            System.out.println("Usage: java TestRunner [test|benchmark|both]");
            System.out.println();
            System.out.println("Commands:");
            System.out.println("  test      - Run test with test_data.csv");
            System.out.println("  benchmark - Run benchmark with benchmark_data.csv");
            System.out.println("  both      - Run both test and benchmark");
            System.out.println();
            System.out.println("Note: Ensure Redis server is running on localhost:6379");
            System.out.println();
            return;
        }
        
        String command = args[0].toLowerCase();
        
        try {
            switch (command) {
                case "test":
                    Test.runTest();
                    break;
                case "benchmark":
                    Test.runBenchmark();
                    break;
                case "both":
                    Test.runTest();
                    System.out.println("\n\n");
                    Test.runBenchmark();
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Use: java TestRunner [test|benchmark|both]");
            }
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
            System.err.println("\nMake sure:");
            System.err.println("1. Redis server is running on localhost:6379");
            System.err.println("2. All required classes are compiled");
            System.err.println("3. Data files are in the 'data/' directory");
            e.printStackTrace();
        }
    }
}

