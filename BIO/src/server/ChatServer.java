package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 监听是否有客户端发生请求,这个模块只监听连接请求
 */
public class ChatServer {

    private int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";

    private ServerSocket serverSocket;
    private Map<Integer, Writer> connectedClients;

    private ExecutorService executorService;

    public ChatServer() {
        connectedClients = new HashMap<>();
        executorService = Executors.newFixedThreadPool(10);//添加线程池的支持
    }

    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connectedClients.put(port, writer);
            System.out.println("客户端[" + port + "]已连接到服务器");
        }
    }

    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket!=null){
            int port = socket.getPort();
            if (connectedClients.containsKey(port)){
                connectedClients.get(port).close();
            }
            connectedClients.remove(port);
            System.out.println("客户端[" + port + "]已断开连接");
        }
    }

    public synchronized void forwardMessage(Socket socket,String fwdMsg) throws IOException {
        for (Integer id:connectedClients.keySet()){
            if (!id.equals(socket.getPort())){
                Writer writer = connectedClients.get(id);
                writer.write(fwdMsg);//这里也是阻塞式的
                writer.flush();
            }
        }
    }

    public void start(){
        // 绑定监听端口
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("服务器启动，监听："+DEFAULT_PORT+"...");

            while (true){
                Socket socket = serverSocket.accept();//等待客户端连接
                // 创建一个chatHandler线程
                //new Thread(new ChatHandler(this,socket)).start();//创建线程开销大
                executorService.execute(new ChatHandler(this,socket));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close();
        }
    }

    public boolean readyToQuit(String msg){
        return QUIT.equals(msg);
    }


    public synchronized void close(){
        if (serverSocket!=null){
            try {
                serverSocket.close();
                System.out.println("关闭了服务端");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
}
