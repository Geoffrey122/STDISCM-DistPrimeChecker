import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class PrimeChecker {
    private static final int PORT = 3100;
    private static final int MAX_THREAD_COUNT = 1024; // Number of threads

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
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String task = in.readLine();
                String[] points = task.split(",");
                int start = Integer.parseInt(points[0]);
                int end = Integer.parseInt(points[1]);

                String result = findPrimes(start, end);

                out.println(result);
            } catch (IOException e) {
                e.printStackTrace();
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
    }
}
