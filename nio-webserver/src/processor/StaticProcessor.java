package processor;

import connector.Request;
import connector.Response;

import java.io.IOException;

public class StaticProcessor {
    public void processor(Request request, Response response){
        try {
            response.sendStaticResource();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
