import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;

public class Server {
    final String LOCALHOST = "localhost";
    final int DEFAULT_PORT = 8888;
    AsynchronousServerSocketChannel serverChannel;


    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                System.out.println("关闭" + closeable);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        //绑定监听端口
        try {
            serverChannel = AsynchronousServerSocketChannel.open();
            //实际上在低层具有一些异步的线程，AsynchronousChannelGroup，内部有一个线程池，执行回调函数
            //这样实际上是将之前不管是BIO还是NIO，线程都是由应用程序创建的，这里是直接将工作交给了系统去完成
            serverChannel.bind(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            System.out.println("启动服务器，监听端口：" + DEFAULT_PORT);

            while (true) {
                serverChannel.accept(null, new AcceptHandler());//这里是异步的
                System.in.read();//在这里被阻塞，输入值也没什么用，保证主线程不要退出的操作
            }


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);
        }
    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
        //acceptHandler负责接受连接请求，当请求完成的时候，会调用completed并把与客户端连接的socket给到方法的第一个参数中
        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {//会调函数正常完成，Object是一个额外的信息，就是accept函数里的null
            if (serverChannel.isOpen()) {
                serverChannel.accept(null, this);//非阻塞的，会正常退出，不用担心递归问题
            }
            AsynchronousSocketChannel clientChannel = result;
            if (clientChannel != null && clientChannel.isOpen()) {
                ClientHandler handler = new ClientHandler(clientChannel);
                //等待收数据
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                Map<String, Object> info = new HashMap<String, Object>();
                info.put("type", "read");
                info.put("buffer", buffer);//准备好要读取数据
                clientChannel.read(buffer, info, handler);//异步
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            //需要处理错误

        }
    }

    private class ClientHandler implements CompletionHandler<Integer,Map<String,Object>>{
        //这个负责收发消息，
        private AsynchronousSocketChannel clientChannel;
        public ClientHandler(AsynchronousSocketChannel client){
            this.clientChannel = client;//以后要修改估计是这个表，还要加一个map记录所有的client
        }

        @Override
        public void completed(Integer result, Map<String,Object> attachment) {
            //读到数据之后会调用这个方法
            //这里实现的逻辑是把数据发回给用户而已
            Map<String,Object> info = (Map<String,Object>) attachment;
            String type = (String) info.get("type");
            if("read".equals(type)){
                ByteBuffer buffer = (ByteBuffer) info.get("buffer");
                buffer.flip();
                info.put("type","write");
                clientChannel.write(buffer,info,this);//交给低层的线程执行了
                //对于多用户的网络模型应该是在这里，
                buffer.clear();
            }else if("write".equals(type)){
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                info.put("type", "read");
                info.put("buffer", buffer);
                clientChannel.read(buffer, info, this);//异步

            }
        }

        @Override
        public void failed(Throwable exc, Map<String,Object> attachment) {
            //处理错误
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}