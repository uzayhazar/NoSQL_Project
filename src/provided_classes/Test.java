package provided_classes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Test {
    public record Payload(String title, String comment, String timestamp){};
    public interface FlushableKVStore extends KVStore {
        void flushDB();
    }
    public static void main(String[] args) {


        /* -- Test -- */
        /* You can use the provided method to read the data like
        List<Map.Entry<String, Payload>> data = readData("path/to/test_data.csv");

        Correct output for range ["KEY002", "KEY004"] and timestamp 20 is
        KEY002=Payload[title=Some Title for KEY002, comment=Change 3 for key KEY002, timestamp=19]
        KEY003=Payload[title=Some Title for KEY003, comment=Change 4 for key KEY003, timestamp=20]
        KEY004=Payload[title=Some Title for KEY004, comment=Change 3 for key KEY004, timestamp=13]
         */

        /* -- Benchmark -- */
        /* ... */


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
