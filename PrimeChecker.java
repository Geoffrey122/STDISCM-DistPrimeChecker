import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class PrimeChecker {
    private static final int PORT = 3100;
    private static final String SLAVE_ADDRESS = "localhost";
    private static final int SLAVE_PORT = 3101; 
    private static final int MAX_THREAD_COUNT = 4; // Number of threads

    public static void main(String[] args) {
        System.out.println("Number of threads used: " + MAX_THREAD_COUNT);
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("PrimeChecker is running on port " + PORT);

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
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            ExecutorService taskPool = Executors.newFixedThreadPool(2);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                // Client task
                String task = in.readLine();
                String[] points = task.split(",");
                int start = Integer.parseInt(points[0]);
                int end = Integer.parseInt(points[1]);
                int mid = start + (end - start) / 2; // Calculate mid-point

                // Showing bounds
                System.out.println("Start: " + start);
                System.out.println("End: " + end);

                // Perform the task of finding primes using separate threads
                Future<String> result1 = taskPool.submit(() -> findPrimes(start, mid));
                Future<String> result2 = taskPool.submit(() -> sendTask(mid + 1, end));

                // Combine results
                String finalResult = result1.get() + result2.get();

                // Send the result back to Client
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

        private String sendTask(int start, int end) {
            try (Socket slaveSocket = new Socket(SLAVE_ADDRESS, SLAVE_PORT);
                 PrintWriter slaveOut = new PrintWriter(slaveSocket.getOutputStream(), true);
                 BufferedReader slaveIn = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()))) {

                // Prepare task for Slave
                String task = start + "," + end;

                // Send task to Slave
                slaveOut.println(task);

                // Receive result from Slave
                String result = slaveIn.readLine();
                System.out.println("Result from Slave: " + result);

                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }
    }
}
