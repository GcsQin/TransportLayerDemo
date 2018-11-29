package com.duke.armory.transportlayerdemo;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by qinfuchao on 2018/11/29.
 */

public class ServerSocketService extends IntentService {
    private static final String TAG = ServerSocketService.class.getSimpleName();
    private static boolean switchFlag = true;
    public static final int SSS_PORT = 8488, SSS_THREAD_COUNT = 64;

    private ExecutorService executorService = Executors.newFixedThreadPool(SSS_THREAD_COUNT);

    public ServerSocketService() {
        super(TAG);
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ServerSocketService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        new Thread(new TCPServer(), TAG).start();
        while (switchFlag) {

        }
    }

    public static boolean setSwitchFlag(boolean flag) {
        return switchFlag = flag;
    }

    class TCPServer implements Runnable {

        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(SSS_PORT);
                Log.d(TAG, "TCP服务已创建 端口号为 = "+ serverSocket.getLocalPort());
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "TCP服务创建失败：" + e.getMessage());
            }
            while (switchFlag) {
                try {
                    Socket socket = serverSocket.accept();
                    if (socket != null) {
                        executorService.execute(handleClientSocket(socket));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Runnable handleClientSocket(final Socket socket) {

        return new Runnable() {
            @Override
            public void run() {
                try {
                    //pw用于服务端向客户端回复消息
                    PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    pw.println(socket.getInetAddress().getHostAddress() +
                            " : " +
                            socket.getPort() +
                            " 的Socket与服务端建立连接。");
                    //bf用于服务端接收来自客户端的消息。
                    BufferedReader bf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    StringBuffer sb = new StringBuffer();
                    while (switchFlag) {
                        String data = bf.readLine();
                        if (TextUtils.isEmpty(data)) {
                            break;
                        }
                        sb.append(data).append("\n");
                        pw.println("好的，我接收到你的消息了。");
                    }
                    Log.d(TAG, "客户端数据所有数据 ：" + sb.toString());
                    //关闭io流以及socket连接
                    pw.close();
                    bf.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }
}
