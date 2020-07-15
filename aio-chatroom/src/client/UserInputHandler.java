package client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserInputHandler implements Runnable{
    private ChatClient client;
    public UserInputHandler(ChatClient client){
        this.client = client;
    }

    @Override
    public void run() {
        try {
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            while (true){
                String msg = consoleReader.readLine();
                if (msg!=null) {
                    client.send(msg);
                }
                if (client.readyToQuit(msg)){
                    break;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}
