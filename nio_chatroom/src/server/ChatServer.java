package server;


import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

public class ChatServer {

    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;

    private ServerSocketChannel server;
    private Selector selector;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);//读取数据的缓存区
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);//写入转发消息

    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer(int port) {
        this.port = port;
    }

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    private void start() {
        try {
            server = ServerSocketChannel.open();//打开一个server
            server.configureBlocking(false);//设置为非阻塞调用模式
            server.socket().bind(new InetSocketAddress(port));//绑定监听的端口

            selector = Selector.open();//开启一个selector对象
            //这个selector对象最后只注册了两个待响应的检测状态，accept和read，read和write不是同一个流，所以不用担心进入read状态的channel没法写
            server.register(selector, SelectionKey.OP_ACCEPT);//设置监听事件，观察者模式

            System.out.println("启动服务器，监听端口：" + port + "...");
            while (true) {
                selector.select();//该方式是阻塞的，一直到至少有一个事件发生,然把事件放到下面的集合中
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    //被触发的事件
                    handles(key);
                }
                selectionKeys.clear();//清空事件集合
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(selector);//关闭selector会自动关闭其上注册的server
        }
    }

    private void handles(SelectionKey key) throws IOException {
        //ACCEPT事件-和客户端建立连接
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);//转换为非阻塞式调用
            client.register(selector, SelectionKey.OP_READ);
            System.out.println(getClientName(client) + "已连接");
        } else if (key.isReadable()) {//READ事件——客户端发送了消息
            SocketChannel client = (SocketChannel) key.channel();
            String fwdMsg = receive(client);
            if (fwdMsg.isEmpty()) {
                //客户端异常
                key.cancel();//取消监听事件
                selector.wakeup();//因为selector可能被阻塞，所有更新时需要通知selector
            } else {
                System.out.println(getClientName(client)+":"+ fwdMsg);
                forwardMessage(client, fwdMsg);
                // 检查用户是否退出
                if (readyToQuit(fwdMsg)) {
                    key.cancel();
                    selector.wakeup();
                    System.out.println(getClientName(client) + "已断开");
                }
            }
        }


    }

    private void forwardMessage(SocketChannel client, String fwdMsg) throws IOException {
        for (SelectionKey key: selector.keys()) {
            Channel connectedClient = key.channel();
            if (connectedClient instanceof ServerSocketChannel) {
                continue;
            }

            if (key.isValid() && !client.equals(connectedClient)) {
                wBuffer.clear();
                wBuffer.put(charset.encode(getClientName(client) + ":" + fwdMsg));
                wBuffer.flip();
                while (wBuffer.hasRemaining()) {
                    ((SocketChannel)connectedClient).write(wBuffer);
                }
            }
        }
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while (client.read(rBuffer) > 0) ;
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    private String getClientName(SocketChannel client) {
        return "客户端[" + client.socket().getPort() + "]";
    }

    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer(7777);
        chatServer.start();
    }
}
