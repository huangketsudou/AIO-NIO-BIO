package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class UserInputHandler implements Runnable{

    private ChatClient chatClient;
    public UserInputHandler(ChatClient chatClient){
        this.chatClient = chatClient;
    }

    @Override
    public void run() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true){
            try {
                String msg = reader.readLine();
                chatClient.send(msg);
                if (chatClient.readyToQuit(msg)){
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
