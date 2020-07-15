package client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 处理用户在控制台上的输入
 */
public class UserInputHandler implements Runnable{
    private ChatClient chatClient;

    public UserInputHandler(ChatClient chatclient){
        this.chatClient = chatclient;
    }

    @Override
    public void run() {
        try {
            //等待用户输入信息
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true){
                String input = consoleReader.readLine();
                //向服务器发送消息
                chatClient.send(input);
                //检查用户是否准备退出
                if(chatClient.readyToQuit(input)){
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
