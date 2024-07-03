import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Slave {
    private static final int PORT = 3101; // Updated port number for Slave
    private static final int MAX_THREAD_COUNT = 4; // Number of threads

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

            // Read task from Master
            String task = in.readLine();
            String[] points = task.split(",");
            int start = Integer.parseInt(points[0]);
            int end = Integer.parseInt(points[1]);

            // Showing bounds
            System.out.println("Start: " + start);
            System.out.println("End: " + end);

            String result = findPrimes(start, end);

            // Send the result back to Master
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
