import java.io.*;
import java.net.*;

public class Slave {
    private static final int PORT = 3101; // Port number for Slave
    private static final int MAX_THREAD_COUNT = 1024; // Number of threads

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Slave is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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

                String task;
                while ((task = in.readLine()) != null) {
                    String[] ranges = task.split(";");
                    StringBuilder resultBuilder = new StringBuilder();
                    for (String range : ranges) {
                        String[] points = range.split(",");
                        int start = Integer.parseInt(points[0]);
                        int end = Integer.parseInt(points[1]);

                        String result = findPrimes(start, end);
                        resultBuilder.append(result).append(";");
                    }
                    out.println(resultBuilder.toString());
                }
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
