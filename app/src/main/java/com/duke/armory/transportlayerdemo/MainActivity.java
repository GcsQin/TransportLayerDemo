package com.duke.armory.transportlayerdemo;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    ExecutorService executorService = Executors.newFixedThreadPool(ServerSocketService.SSS_THREAD_COUNT);
    Handler mainHandler = new Handler(Looper.getMainLooper());
    private final static ThreadLocal<SimpleDateFormat> dateFormater = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };
    //
    Button btnTransportTCP, btnTransportUDP, btnConnectTCP, btnConnectUDP;
    EditText etSendData;
    TextView tvReceiveData;
    StringBuffer sb = new StringBuffer();
    Socket clientSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnConnectTCP = findViewById(R.id.btn_tcp_connect);
        btnTransportTCP = findViewById(R.id.btn_transport_tcpData);
        etSendData = findViewById(R.id.et_transport_data);
        tvReceiveData = findViewById(R.id.tv_receive_data);
        btnTransportTCP.setOnClickListener(this);
        btnConnectTCP.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_tcp_connect:
                startService(new Intent(MainActivity.this, ServerSocketService.class));
                break;
            case R.id.btn_transport_tcpData:
                transportTCPData();
                break;
        }
    }

    //pw用于向服务端传输数据。
    PrintWriter pw;
    BufferedReader bf = null;

    private void transportTCPData() {
        final String data = etSendData.getText().toString();
        sb.append(dateFormater.get().format(new Date())).append("\n");
        sb.append("客户端：").append(data).append("\n");
        //网络操作需要运行于子线程。
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (clientSocket == null) {//如果ServerSocket没有创建，那么一直重试，直到连接成功为止。
                    try {
                        clientSocket = new Socket("localhost", ServerSocketService.SSS_PORT);
                        pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));
                        bf = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //将EditText中的数据传输给服务端
                pw.println(data);
                //死循环不断的监听
                while (bf != null) {
                    try {
                        final String result = bf.readLine();
                        if (!TextUtils.isEmpty(result)) {
                            sb.append(dateFormater.get().format(new Date())).append("\n");
                            sb.append("服务端：").append(result).append("\n");
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    tvReceiveData.setText(sb.toString());
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
}
