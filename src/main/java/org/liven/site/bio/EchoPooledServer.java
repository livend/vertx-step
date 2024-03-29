package org.liven.site.bio;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// bio using threadpool
public class EchoPooledServer {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        try (ServerSocket ss = new ServerSocket()) {
            ss.bind(new InetSocketAddress(3000));
            while (true) {
                Socket socket = ss.accept();
                CompletableFuture.runAsync(clientHandler(socket), executorService);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Runnable clientHandler(Socket socket) {
        return () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                String line = "";
                while (!"/quit".equals(line)) {
                    line = reader.readLine(); // 读取操作护会导致该线程阻塞，例如读取数据不足
                    System.out.println("~ " + line);
                    writer.write(line + "\n"); // 写操作会导致线程阻塞，知道缓冲区数据通过网络发送出去
                    writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }
}
