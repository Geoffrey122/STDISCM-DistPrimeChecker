import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Master {
    private static final int PORT = 3000;
    private static final int MAX_THREAD_COUNT = 1024; // Number of threads
    private static final String SLAVE_IP = "192.168.1.10"; // IP of the Slave server
    private static final int SLAVE_PORT = 3101;

    public static void main(String[] args) {
        System.out.println("Number of threads used: " + MAX_THREAD_COUNT);
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Master is running on port " + PORT);

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

                // Read the task from Client
                String task = in.readLine();
                String[] points = task.split(",");
                int start = Integer.parseInt(points[0]);
                int end = Integer.parseInt(points[1]);
                int mid = start + (end - start) / 2;

                // Divide task between PrimeChecker and Slave
                Future<String> result1 = sendTaskToSlave("localhost", 3100, start, mid); // PrimeChecker
                Future<String> result2 = sendTaskToSlave(SLAVE_IP, SLAVE_PORT, mid + 1, end); // Slave

                // Combine results
                String finalResult = result1.get() + result2.get();

                // Send the result back to Client
                out.println(finalResult);
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        private Future<String> sendTaskToSlave(String ip, int port, int start, int end) {
            return Executors.newSingleThreadExecutor().submit(() -> {
                try (Socket slaveSocket = new Socket(ip, port);
                     PrintWriter out = new PrintWriter(slaveSocket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(slaveSocket.getInputStream()))) {

                    out.println(start + "," + end);
                    return in.readLine();
                }
            });
        }
    }
}
