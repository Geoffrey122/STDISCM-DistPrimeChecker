import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class MasterClient {
    private static final int MASTER_PORT = 3100; // Port for the master server
    private static final int SLAVE_PORT = 3101;  // Port for the slave server
    private static final String SLAVE_ADDRESS = "localhost"; // Address for the slave server
    private static final int MAX_THREAD_COUNT = 1; // Number of threads

    public static void main(String[] args) {
        int start = 1;
        int end = 100000000;

        // Run master server in a separate thread
        ExecutorService masterService = Executors.newSingleThreadExecutor();
        masterService.submit(() -> runMasterServer(MASTER_PORT));

        // Client part
        try (Socket socket = new Socket("localhost", MASTER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Measure the time from sending the request to receiving the response
            long startTime = System.currentTimeMillis();
            System.out.println("Client: Sending request...");
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

        masterService.shutdown();
    }

    private static void runMasterServer(int port) {
        System.out.println("Master server is running on port " + port);
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        pool.shutdown();
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            ExecutorService taskPool = Executors.newFixedThreadPool(2); // Divide task between master and one slave

            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                // Read the task from client
                String task = in.readLine();
                String[] points = task.split(",");
                int start = Integer.parseInt(points[0]);
                int end = Integer.parseInt(points[1]);
                int mid = start + (end - start) / 2;

                // Log the task received
                System.out.println("Master: Received task to find primes between " + start + " and " + end);

                // Perform the task of finding primes using master and slave
                Future<String> masterResult = taskPool.submit(() -> findPrimes(start, mid));
                Future<String> slaveResult = taskPool.submit(() -> sendTaskToSlave(mid + 1, end));

                // Combine results
                String finalResult = masterResult.get() + slaveResult.get();

                // Log the completion
                System.out.println("Master: Task completed. Sending result to client.");

                // Send the result back to client
                out.println(finalResult);
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } finally {
                taskPool.shutdown();
            }
        }

        private String findPrimes(int start, int end) {
            StringBuilder primes = new StringBuilder();
            for (int i = start; i <= end; i++) {
                if (checkPrime(i)) {
                    primes.append(i).append(" ");
                }
                if (i % 1000000 == 0) { // Log progress every 1,000,000 numbers
                    System.out.println("Master: Checked up to " + i);
                }
            }
            return primes.toString();
        }

        private boolean checkPrime(int n) {
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

        private String sendTaskToSlave(int start, int end) {
            try (Socket socket = new Socket(SLAVE_ADDRESS, SLAVE_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Send the task to the slave
                System.out.println("Master: Sending task to slave to find primes between " + start + " and " + end);
                out.println(start + "," + end);

                // Receive and return the result
                String result = in.readLine();
                System.out.println("Master: Received result from slave.");
                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }
    }
}
