package connector;

import processor.ServletProcessor;
import processor.StaticProcessor;

import javax.xml.stream.FactoryConfigurationError;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.*;
import java.util.Set;

public class Connector implements Runnable {
    private static final int DEFAULT_PORT = 8888;
    private ServerSocketChannel server;
    private Selector selector;
    private int port;

    public Connector() {
        this(DEFAULT_PORT);
    }

    public Connector(int port) {
        this.port = port;
    }

    public void start() {
        Thread  thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
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
        //Accept事件
        if (key.isAcceptable()) {
            ServerSocketChannel channel = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
        }else {
            SocketChannel client = (SocketChannel) key.channel();
            key.cancel();
            client.configureBlocking(true);
            Socket clientSocket = client.socket();
            InputStream input = clientSocket.getInputStream();
            OutputStream output = clientSocket.getOutputStream();
            Request request = new Request(input);
            request.parse();

            Response response = new Response(output);
            response.setRequest(request);
            if (request.getRequestUri().startsWith("/servlet")){
                ServletProcessor processor = new ServletProcessor();
                processor.process(request,response);
            }else {
                StaticProcessor processor = new StaticProcessor();
                processor.processor(request,response);
            }
            close(client);
        }

    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
