package server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * 实现与客户端的一对一通信
 */
public class ChatHandler implements Runnable {

    private ChatServer server;
    private Socket socket;

    public ChatHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            //存放了新上线用户
            server.addClient(socket);

            //读取用户发送的消息
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            String msg = null;
            while ((msg = reader.readLine()) != null) {//这里等待用户发送的信息，当用户长时间没有返回信息时，该线程一直被阻塞
                String fwdMsg = "客户端[" + socket.getPort() + "]:" + msg+"\n";
                System.out.print(fwdMsg);

                //转发消息给其他用户
                server.forwardMessage(socket, fwdMsg);

                // 检查准备退出
                if(server.readyToQuit(msg)){
                    break;
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                server.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
