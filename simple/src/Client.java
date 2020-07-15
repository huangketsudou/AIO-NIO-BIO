import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {

        final String QUIT = "quit";
        final String DEFAULT_SERVER_HOST = "127.0.0.1";
        final int DEFAULT_PORT = 8888;
        Socket socket = null;
        BufferedWriter writer = null;

        try {
            socket = new Socket(DEFAULT_SERVER_HOST,DEFAULT_PORT);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            //等待用户输入信息
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String input = consoleReader.readLine();

                //等待用户输入的方法一共有三种：代码中的这种inputstreamrreader包装System.in,Scanner包装System.in,System.Console()直接获取输入
                //发送给服务器

                writer.write(input+"\n");
                writer.flush();

                //读取服务器返回的消息

                String msg = reader.readLine();
                System.out.println(msg);
                // 查看用户是否退出
                if(QUIT.equals(input)){
                    System.out.println("客户端["+socket.getLocalPort()+"]退出");
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (writer != null){
                try {
                    writer.close();
                    System.out.println("关闭客户端");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
