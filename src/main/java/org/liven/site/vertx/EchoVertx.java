package org.liven.site.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

// echo server using vertx
public class EchoVertx {
    private static int numberOfConnections = 0;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        vertx.createNetServer().connectHandler(EchoVertx::handlerNewClient).listen(3000);

        vertx.setPeriodic(5000, id -> System.out.println(howMany()));

        vertx.createHttpServer().requestHandler(r -> r.response().end(howMany())).listen(8080);
    }


    private static void handlerNewClient(NetSocket socket) {
        numberOfConnections++;
        socket.handler(buffer -> {
            socket.write(buffer);
            if (buffer.toString().endsWith("/quit\n")) {
                socket.close();
            }
        });
        socket.closeHandler(v -> numberOfConnections--);
    }

    private static String howMany() {
        return "We now have " + numberOfConnections + " connections";
    }
}
