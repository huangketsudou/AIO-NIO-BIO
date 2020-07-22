package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;
    private static final int THREADPOOL_SIZE = 8;

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverChannel;
    //private List<ClientHandler> connectedClients;//这里尝试改一下用CopyOnWriteArrayList实现下
    private CopyOnWriteArrayList<ClientHandler> connectedClients;
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
        this.connectedClients = new CopyOnWriteArrayList<>();
    }


    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    private String receive(ByteBuffer buffer) {
        return String.valueOf(charset.decode(buffer));
    }

    private void addClient(ClientHandler handler) {
        connectedClients.add(handler);
        System.out.println(getClientName(handler.clientChannel) + "已连接到服务器！");
    }

    private void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println(getClientName(handler.clientChannel) + "已经断开连接");
        close(handler.clientChannel);//关闭与客户端连接的channel
    }

    private String getClientName(AsynchronousSocketChannel clientChannel) {
        int clientPort = -1;
        try {
            InetSocketAddress address = (InetSocketAddress) clientChannel.getRemoteAddress();
            clientPort = address.getPort();
        } catch (IOException e) {
//            e.printStackTrace();//这里吗
        }
        return "客户端[" + clientPort + "]";
    }

    private void forwardMessage(AsynchronousSocketChannel clientChannel, String fwdMsg) {//因为有部分线程会修改该列表，导致列表会变化，要加同步
        for (ClientHandler handler : connectedClients) {
            if (!clientChannel.equals(handler.clientChannel)) {
                try {
                    ByteBuffer buffer = charset.encode(getClientName(handler.clientChannel) + ":" + fwdMsg);
                    handler.clientChannel.write(buffer,null,handler);
                    //这里估计有发信异常，当一个线程有用户退出时，而在另一线程，其他服务器对着该用户发信息就会找不到对象,
                    //添加一个sleep来测试以下
                    //Thread.sleep(1000);
                    //sleep测试不到，因为所有的线程都是等到发完quit信息之后才退出的，应该像个办法在外部阻塞它，被阻塞的应该是handler线程
                    //可以用一个try catch抓住异常，将此时的channel关闭
                    //如果是在控制台强行关闭的话，保存用户的数组里面有一个不正常关闭的连接,在转发消息的时候会在write这里报错，并将转而调用相应handler的fail函数
                } catch (Exception e) {
//                    e.printStackTrace();
                    removeClient(handler);
                }
            }
        }
        System.out.println(connectedClients.size());
    }

    public void start() {
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(THREADPOOL_SIZE);
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);

            serverChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverChannel.bind(new InetSocketAddress(LOCALHOST, port));
            System.out.println("启动服务器，监听端口：" + port);

            while (true) {
                serverChannel.accept(null, new AcceptHandler());
                System.in.read();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);
        }
    }


    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            if (serverChannel.isOpen()) {
                serverChannel.accept(null, this);
            }
            if (clientChannel != null && clientChannel.isOpen()) {
                ClientHandler handler = new ClientHandler(clientChannel);//将clientChannel与具体的handler绑定
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER);
                // 将新用户添加到在线用户列表
                addClient(handler);
                clientChannel.read(buffer, buffer, handler);//第一个buffer把读的数据写到buffer中，第二个buffer把写完的数据传递给handler
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("连接失败：" + exc);
        }
    }


    private class ClientHandler implements CompletionHandler<Integer, Object> {

        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {//result是read读到了多少个信息
            ByteBuffer buffer = (ByteBuffer) attachment;
            if (buffer != null) {
                if (result <= 0) {
                    //客户端异常
                    // TODO 将客户从在线客户列表移除
                    removeClient(this);
                } else {
                    buffer.flip();
                    String fwdMsg = receive(buffer);
                    System.out.println(getClientName(clientChannel) + ": " + fwdMsg);
                    forwardMessage(clientChannel, fwdMsg);//这里拟定的用户列表中采用的是列表中存入handler以实现异步调用，否则又变成同步的了
                    buffer.clear();

                    //检查用户是否决定退出
                    if (readyToQuit(fwdMsg)) {
                        removeClient(this);
                    } else {
                        clientChannel.read(buffer, buffer, this);
                    }
                }
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("读写失败:" + exc);
            removeClient(this);//将断开连接的handler移出列表
        }
    }


    public static void main(String[] args) {
        ChatServer server = new ChatServer(7777);
        server.start();
    }

}
