package client;


import java.io.*;
import java.net.Socket;

/**
 * 主线程使用socket，与服务端建立连接，并将收到的消息显示到控制台
 */
public class ChatClient {
    private final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final int DEFAULT_SERVER_PORT = 8888;
    private final String QUIT = "quit";

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;


    //发送消息
    public void send(String msg) throws IOException {
        if (!socket.isOutputShutdown()){
            writer.write(msg+"\n");
            writer.flush();
        }
    }

    //接收消息的函数
    public String receive() throws IOException {
        String msg = null;
        if (!socket.isInputShutdown()){
            msg = reader.readLine();//阻塞式等待消息
        }
        return msg;
    }


    // 客户是否准备退出
    public boolean readyToQuit(String msg){
        return QUIT.equals(msg);
    }

    public void start(){
        try {
            //创建socket
            socket = new Socket(DEFAULT_SERVER_HOST,DEFAULT_SERVER_PORT);
            //创建IO流
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            //处理用户输入
            new Thread(new UserInputHandler(this)).start();


            //读取服务器转发的消息
            String msg = null;
            while ((msg = receive())!=null){
                System.out.println(msg);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close();
        }
    }

    public void close(){
        if (writer!=null){
            try {
                writer.close();
                System.out.println("关闭socket...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}
