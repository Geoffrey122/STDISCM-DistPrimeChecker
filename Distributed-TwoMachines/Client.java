import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        int start = 1;
        int end = 1000000;

        try (Socket socket = new Socket("localhost", 3000); // Connect to Master on port 3000
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Measure the time from sending the request to receiving the response
            long startTime = System.currentTimeMillis();
            out.println(start + "," + end);
            String response = in.readLine(); 
            long endTime = System.currentTimeMillis();

            // Calculate and print the runtime
            long runtime = endTime - startTime;
            System.out.println("Runtime: " + runtime + " ms");
            System.out.println("Primes: " + response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
