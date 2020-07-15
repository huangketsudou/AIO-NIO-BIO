import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        final String QUIT = "quit";
        final int DEFAULT_PORT = 8888;
        ServerSocket serverSocket = null;

        //绑定监听端口

        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("启动服务器，监听端口"+DEFAULT_PORT);

            while (true){
                //等待客户端的连接
                Socket socket = serverSocket.accept();//阻塞式的，一直等到又客户端来连接为之
                System.out.println("客户端["+socket.getPort()+"]已连接");
                //读取发信端数据
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //向客户端发数据
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // 读取客户端发送的消息
                String msg =null;
                while ((msg = reader.readLine())!=null) {
                    System.out.println("客户端["+socket.getPort()+"]:"+msg);
                    //回复消息
                    writer.write("服务器："+msg+"\n");
                    writer.flush();//保证的缓冲区的数据发出去
                    //退出服务
                    if (QUIT.equals(msg)){
                        System.out.println();
                        break;
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (serverSocket!=null) {
                try {
                    serverSocket.close();
                    System.out.println("关闭server");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
