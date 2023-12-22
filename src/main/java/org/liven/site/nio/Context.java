package org.liven.site.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.regex.Pattern;

public class Context {

    private final ByteBuffer nioBuffer = ByteBuffer.allocate(512); // 初始可写 position=0,limit=0

    private String currentLine = "";

    private boolean terminating = false;

    private static final Pattern QUIT = Pattern.compile("(\\r)?(\\n)?/quit$");

    private static final HashMap<SocketChannel, Context> contexts = new HashMap<>();

    public static void newConnection(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        SocketChannel sc = ssc.accept();
        sc.configureBlocking(false).register(selector, SelectionKey.OP_READ);
        contexts.put(sc, new Context());
    }

    public static void echo(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        Context context = contexts.get(sc);

        sc.read(context.nioBuffer); // 写入
        context.nioBuffer.flip(); // 可读模式
        context.currentLine = context.currentLine + Charset.defaultCharset().decode(context.nioBuffer);

        if (QUIT.matcher(context.currentLine).find()) {
            context.terminating = true;
        } else if (context.currentLine.length() > 16) {
            context.currentLine = context.currentLine.substring(8);
        }

        context.nioBuffer.flip(); // 可写

        int count = sc.write(context.nioBuffer);
        if (count < context.nioBuffer.limit()) {
            key.cancel();
            sc.register(key.selector(), SelectionKey.OP_WRITE);
        } else {
            context.nioBuffer.clear();
            if (context.terminating) {
                clearup(sc);
            }
        }
    }

    public static void continueEcho(Selector selector, SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        Context context = contexts.get(sc);

        int remainingBytes = context.nioBuffer.limit() - context.nioBuffer.position();
        int count = sc.write(context.nioBuffer);
        if (count == remainingBytes) {
            context.nioBuffer.clear();
            key.cancel();
            if (context.terminating) {
                clearup(sc);
            } else {
                sc.register(selector, SelectionKey.OP_READ);
            }
        }
    }

    private static void clearup(SocketChannel sc) throws IOException {
        sc.close();
        contexts.remove(sc);
    }

}
