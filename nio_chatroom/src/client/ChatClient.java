package client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Set;

public class ChatClient {
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private static final int DEFAULT_SERVER_PORT = 8888;
    private static final String QUIT = "quit";
    private static  final int BUFFER = 1024;
    private String host;
    private int port;
    private SocketChannel client;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER);

    private Selector selector;
    private Charset charset = Charset.forName("UTF-8");

    public ChatClient(){
        this(DEFAULT_SERVER_HOST,DEFAULT_SERVER_PORT);
    }

    public ChatClient(String host,int port){
        this.port = port;
        this.host = host;
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start(){
        try {
            client  = SocketChannel.open();
            client.configureBlocking(false);

            selector = Selector.open();
            client.register(selector, SelectionKey.OP_CONNECT);

            client.connect(new InetSocketAddress(host,port));

            while (true){
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    handles(key);
                }
                selectionKeys.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }catch (ClosedSelectorException e){
            //用户退出
        }finally {
            close(selector);
        }
    }

    private void handles(SelectionKey key) throws IOException {
        //连接就绪时间
        if (key.isConnectable()){
            SocketChannel client = (SocketChannel) key.channel();
            if (client.isConnectionPending()){//连接就绪，返回false代表还在建立连接
                client.finishConnect();
                // 处理用户的输入
                new Thread(new UserInputHandler(this)).start();//阻塞式调用等待用户输入
            }
            client.register(selector,SelectionKey.OP_READ);
        }else if (key.isReadable()){//read事件
            SocketChannel client = (SocketChannel) key.channel();
            String msg = receive(client);
            if (msg.isEmpty()){
                close(selector);
            }else {
                System.out.println(msg);
            }
        }


    }

    public void send(String msg) throws IOException {
        if (msg.isEmpty()){
            return;
        }
        wBuffer.clear();
        wBuffer.put(charset.encode(msg));
        wBuffer.flip();
        while (wBuffer.hasRemaining()){
            client.write(wBuffer);
        }
        //检查用户是否想要退出
        if(readyToQuit(msg)){
            close(selector);
        }
    }

    private String receive(SocketChannel client) throws IOException {
        rBuffer.clear();
        while (client.read(rBuffer)>0);
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    public static void main(String[] args) {
        ChatClient client = new  ChatClient("127.0.0.1",7777);
        client.start();
    }
}
