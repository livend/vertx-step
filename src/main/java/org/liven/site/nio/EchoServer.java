package org.liven.site.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;

// java nio echo server
public class EchoServer {

    public static void main(String[] args) throws IOException {

        Selector selector = Selector.open();
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress(3000));
        ssc.configureBlocking(false); // async
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select(); // 阻塞
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                if (key.isAcceptable()) {
                    Context.newConnection(selector, key);
                } else if (key.isReadable()) { // 收到数据
                    Context.echo(key);
                } else if (key.isWritable()) { // socket变成可写状态
                    Context.continueEcho(selector, key);
                }
                iter.remove(); // 必须手动删除当前SelectKey,否则下轮循环中它任然可用
            }
        }
    }
}
