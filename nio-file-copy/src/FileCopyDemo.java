import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

interface FileCopyRunner {
    void copyFile(File source, File target);
}

public class FileCopyDemo {

    private static final int ROUNDS = 5;

    private static void benchmark(FileCopyRunner test, File source, File target) {
        long elapsed = 0L;
        for (int i = 0; i < ROUNDS; i++) {
            long startTime = System.currentTimeMillis();
            test.copyFile(source, target);
            elapsed += System.currentTimeMillis() - startTime;
            target.delete();
        }
        System.out.println(test + ": " + elapsed / ROUNDS);
    }

    private static void close(Closeable closeable) {//所有的流都是closeable的实现类
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    FileCopyRunner noBufferStreamCopy = new FileCopyRunner() {
        @Override
        public void copyFile(File source, File target) {
            InputStream fin = null;
            OutputStream fout = null;
            try {
                fin = new FileInputStream(source);
                fout = new FileOutputStream(target);

                int result;
                while ((result = fin.read()) != -1) {//每次读一个字节
                    fout.write(result);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(fin);
                close(fout);
            }
        }

        @Override
        public String toString() {
            return "noBufferStreamCopy";
        }
    };
    FileCopyRunner bufferedStreamCopy = new FileCopyRunner() {
        @Override
        public void copyFile(File source, File target) {
            InputStream fin = null;
            OutputStream fout = null;
            try {
                fin = new BufferedInputStream(new FileInputStream(source));
                fout = new BufferedOutputStream(new FileOutputStream(target));

                byte[] buffer = new byte[1024];//读到的字节会被写入buffer中

                int result;//result表示读出来的字节的长度
                while ((result = fin.read(buffer)) != -1) {
                    fout.write(buffer, 0, result);//不能写1024，因为读文件到最后不一定还有1024个字节
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(fin);
                close(fout);
            }
        }

        @Override
        public String toString() {
            return "bufferedStreamCopy";
        }
    };


    FileCopyRunner noiBufferCopy = new FileCopyRunner() {
        @Override
        public void copyFile(File source, File target) {
            FileChannel fin = null;
            FileChannel fout = null;
            try {
                fin = new FileInputStream(source).getChannel();//从文件输入输出流得到文件的通道
                fout = new FileOutputStream(target).getChannel();

                ByteBuffer buffer = ByteBuffer.allocate(1024);

                while (fin.read(buffer) != -1) {
                    buffer.flip();//buffer要变成读模式
                    while (buffer.hasRemaining()) {
                        fout.write(buffer);//不一定会全部读入
                    }
                    buffer.clear();//buffer从读模式切换为写模式
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(fin);
                close(fout);
            }
        }

        @Override
        public String toString() {
            return "noiBufferCopy";
        }
    };

    FileCopyRunner nioTransferCopy = new FileCopyRunner() {
        @Override
        public void copyFile(File source, File target) {
            FileChannel fin = null;
            FileChannel fout = null;
            try {
                fin = new FileInputStream(source).getChannel();
                fout = new FileOutputStream(target).getChannel();
                long size = fin.size();
                long transferred = 0l;
                while (transferred != size) {
                    transferred += fin.transferTo(0, fin.size(), fout);//不能保证100%转移,返回值为每次拷贝的字节
                }


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                close(fin);
                close(fout);
            }
        }

        @Override
        public String toString() {
            return "nioTransferCopy";
        }
    };


}
