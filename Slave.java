import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Slave {
    private static final int PORT = 3101; // Port for the slave server
    private static final int MAX_THREAD_COUNT = 1; // Number of threads

    public static void main(String[] args) {
        System.out.println("Number of threads used: " + MAX_THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREAD_COUNT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Slave is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClientRequest(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private static void handleClientRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Read task from master
            String task = in.readLine();
            String[] points = task.split(",");
            int start = Integer.parseInt(points[0]);
            int end = Integer.parseInt(points[1]);

            // Log the task received
            System.out.println("Slave: Received task to find primes between " + start + " and " + end);

            String result = findPrimes(start, end);

            // Log the completion
            System.out.println("Slave: Task completed. Sending result to master.");

            // Send the result back to master
            out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String findPrimes(int start, int end) {
        StringBuilder primes = new StringBuilder();
        for (int i = start; i <= end; i++) {
            if (checkPrime(i)) {
                primes.append(i).append(" ");
            }
            if (i % 1000000 == 0) { // Log progress every 1,000,000 numbers
                System.out.println("Slave: Checked up to " + i);
            }
        }
        return primes.toString();
    }

    private static boolean checkPrime(int n) {
        if (n <= 1) {
            return false;
        }
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }
}
