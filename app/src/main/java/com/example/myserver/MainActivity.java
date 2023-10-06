package com.example.myserver;

import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity  {

    TextView infoIp;

    ProgressBar progressBar;
    TextView completedTextView;
    TextView fileNumberTextView;

    Button receiveFileButton;
    Button sendFileButton;
    Button instructionsButton;
    Button refreshIPsButton;
    RecyclerView availableIPsRecyclerView;
    RecyclerViewAdapter adapter;
    public static boolean recyclerViewClickEnabled = false;
    public static ArrayList<String> ips = new ArrayList<>();

    ServerSocket httpServerSocket;
    int portNumber = 8085;
    int bufferSize = 100*1024;
    long startTime;

    private DataOutputStream dataOutputStream = null;
    private DataInputStream dataInputStream = null;

    public static String ipToSendFileTo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        infoIp = findViewById(R.id.infoip);

        progressBar = findViewById(R.id.progressBar);
        completedTextView = findViewById(R.id.completedTextView);
        fileNumberTextView = findViewById(R.id.fileNumberTextView);

        receiveFileButton = findViewById(R.id.receiveFileButton);
        sendFileButton = findViewById(R.id.sendFileButton);
        instructionsButton = findViewById(R.id.instructionsButton);
        refreshIPsButton = findViewById(R.id.refreshIPsButton);
        availableIPsRecyclerView = findViewById(R.id.availableIPsRecyclerView);
        availableIPsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerViewAdapter();
        availableIPsRecyclerView.setAdapter(adapter);
        disableRecyclerView();
        String thisIPAddress = Methods.getIpAddress();
        findDevices(thisIPAddress);

        infoIp.setText("Device IP Address: "+thisIPAddress + "\n" + "Port: " + portNumber + "\n");

        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        refreshIPsButton.setOnClickListener(refreshIPsOnClickListener);
        receiveFileButton.setOnClickListener(receiveFileOnClickListener);
        instructionsButton.setOnClickListener(instructionsButtonOnClickListener);
        sendFileButton.setOnClickListener(sendFileOnClickListener);
    }

    public void findDevices(String myip){
        new Thread(new Runnable() {
            AtomicInteger integer = new AtomicInteger(0);
            @Override
            public void run() {
                try{
                    String subnet = myip.substring(0, myip.lastIndexOf(".")+1);
                    int timeout=1000;
                    for (int i=1;i<255;i++){
                        String host=subnet + i;
                        InetAddress inetAddress = InetAddress.getByName(host);
                        integer.getAndIncrement();
                        new Thread(new Runnable() {
                            @Override
                            public void run(){
                                try {
                                    if (inetAddress.isReachable(timeout)){
                                        ips.add(host);
                                    }
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }

                                integer.getAndDecrement();
                            }
                        }).start();
                    }
                }catch(UnknownHostException e){
                    e.printStackTrace();
                }

                while (integer.get()>0){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    public View.OnClickListener instructionsButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(getApplicationContext(), InstructionsActivity.class);
            startActivity(intent);
        }
    };

    public View.OnClickListener refreshIPsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ips.clear();
            findDevices(Methods.getIpAddress());
        }
    };

    public View.OnClickListener receiveFileOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            disableViews();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Handler handler = new Handler(Looper.getMainLooper());
                    try(ServerSocket serverSocket = new ServerSocket(portNumber)){
                        Socket clientSocket = serverSocket.accept();
                        dataInputStream = new DataInputStream(clientSocket.getInputStream());
                        dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());

                        startTime=System.currentTimeMillis();
                        int fileNumber=0;
                        int totalFiles=1;
                        while(fileNumber<totalFiles) {
                            fileNumber = dataInputStream.readInt();
                            totalFiles = dataInputStream.readInt();
                            int finalTotalFiles = totalFiles;
                            int finalFileNumber = fileNumber;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    fileNumberTextView.setText("Receiving file " + finalFileNumber + " of " + finalTotalFiles);
                                }
                            });
                            receiveFile();
                        }
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                completedTextView.setText("Completed in " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
                            }
                        });

                        dataInputStream.close();
                        dataOutputStream.close();
                        clientSocket.close();
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            enableViews();
                        }
                    });
                }
            }).start();
        }
    };

    public View.OnClickListener sendFileOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            disableViews();
            new Thread(new Runnable() {
                @Override
                public void run() {

                    enableRecyclerView();
                    while("".equals(ipToSendFileTo)){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    disableRecyclerView();
                    String ip = ipToSendFileTo;
                    ipToSendFileTo = "";

                    Handler handler = new Handler(Looper.getMainLooper());
                    try(Socket socket = new Socket(ip,portNumber)) {
                        dataInputStream = new DataInputStream(socket.getInputStream());
                        dataOutputStream = new DataOutputStream(socket.getOutputStream());

                        selectFile();
                        while (filesToSend==null) {
                            Thread.sleep(1000);
                        }
                        String[] filesToSendTemp = new String[filesToSend.length];
                        System.arraycopy(filesToSend, 0, filesToSendTemp, 0, filesToSend.length);
                        filesToSend=null;
                        startTime=System.currentTimeMillis();

                        for (int i=0; i<filesToSendTemp.length;++i) {

                            int finalI = i;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    fileNumberTextView.setText("Sending file " + (finalI +1) + " of " + filesToSendTemp.length);
                                }
                            });
                            sendFile(i+1, filesToSendTemp.length, filesToSendTemp[i]);
                        }


                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                completedTextView.setText("Completed in " + (System.currentTimeMillis()-startTime)/1000 + " seconds");
                            }
                        });

                        dataInputStream.close();
                        dataInputStream.close();
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            enableViews();
                        }
                    });
                }
            }).start();
        }
    };

    String[] filesToSend = null;
    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");

        startActivityForResult(intent, 2);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Fix no activity available
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null)
            return;
        if(requestCode == 2) {
            if (resultCode == RESULT_OK) {

                ClipData clipData = data.getClipData();
                if (clipData==null){
                    filesToSend = new String[1];
                    filesToSend[0] = data.getData().getPath();
                }else {
                    filesToSend = new String[clipData.getItemCount()];
                    for(int i=0;i< clipData.getItemCount();++i) {
                        filesToSend[i] = clipData.getItemAt(i).getUri().getPath();
                    }
                }
                System.out.println(filesToSend[0]);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (httpServerSocket != null) {
            try {
                httpServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void disableRecyclerView(){
        recyclerViewClickEnabled = false;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void enableRecyclerView(){
        recyclerViewClickEnabled = true;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    public void disableViews(){
        sendFileButton.setClickable(false);
        sendFileButton.setBackgroundColor(Color.parseColor("#888888"));
        receiveFileButton.setClickable(false);
        receiveFileButton.setBackgroundColor(Color.parseColor("#888888"));
        refreshIPsButton.setClickable(false);
        refreshIPsButton.setBackgroundColor(Color.parseColor("#888888"));
        instructionsButton.setClickable(false);
        instructionsButton.setBackgroundColor(Color.parseColor("#888888"));
    }

    public void enableViews(){
        sendFileButton.setClickable(true);
        sendFileButton.setBackgroundColor(Color.parseColor("#FF6200EE"));
        receiveFileButton.setClickable(true);
        receiveFileButton.setBackgroundColor(Color.parseColor("#FF6200EE"));
        refreshIPsButton.setClickable(true);
        refreshIPsButton.setBackgroundColor(Color.parseColor("#FF6200EE"));
        instructionsButton.setClickable(true);
        instructionsButton.setBackgroundColor(Color.parseColor("#FF6200EE"));
    }

    private void receiveFile() throws Exception{
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                completedTextView.setText("0%");
            }
        });

        int bytes = 0;

        long size = dataInputStream.readLong();     // read file size
        long totalSize = size;
        StringBuilder stringBuilder = new StringBuilder();
        char c;
        while((c=dataInputStream.readChar()) != '\n'){
            stringBuilder.append(c);
        }
        String fileName = stringBuilder.toString();
        System.out.println(stringBuilder.toString());

        File f = new File(Environment.getExternalStorageDirectory(),stringBuilder.toString());
        //File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),stringBuilder.toString());
        String filePath = f.getAbsolutePath();
        for(int i=1;;i++){
            if(f.exists()){
                int index = fileName.lastIndexOf(".");
                String s = fileName.substring(0, index);
                f = new File(filePath.substring(0,filePath.lastIndexOf("/")+1) + s + "(" + i + ")" + fileName.substring(index));
            }else{
                break;
            }
        }

        FileOutputStream fileOutputStream = new FileOutputStream(f.getAbsolutePath());

        byte[] buffer = new byte[bufferSize];
        long totalBytesRead=0;
        while (size > 0 && (bytes = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, size))) != -1) {
            fileOutputStream.write(buffer,0,bytes);
            size -= bytes;      // read upto file size

            totalBytesRead+=bytes;
            progressBar.setProgress((int) (((float) totalBytesRead)*100/totalSize));

            long finalTotalBytesRead = totalBytesRead;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    completedTextView.setText((int) (((float) finalTotalBytesRead)*100/totalSize) + "%");
                }
            });
        }
        progressBar.setProgress(100);
        fileOutputStream.close();
    }

    private void sendFile(int fileNumber, int totalFiles, String path) throws Exception{
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                completedTextView.setText("0%");
            }
        });

        int bytes = 0;

        path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + path.substring(path.indexOf(":")+1);
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + path);
        File file = new File(path);
        FileInputStream fileInputStream = new FileInputStream(file);

        dataOutputStream.writeInt(fileNumber);
        dataOutputStream.writeInt(totalFiles);

        long fileSize = file.length();
        long bytesSent=0;
        // send file size
        dataOutputStream.writeLong(fileSize);
        dataOutputStream.writeChars(/*path.substring(path.lastIndexOf("/")+1)*/ file.getName() + "\n");
        // break file into chunks
        byte[] buffer = new byte[bufferSize];
        while ((bytes=fileInputStream.read(buffer))!=-1){
            dataOutputStream.write(buffer,0,bytes);
            dataOutputStream.flush();

            bytesSent+=bytes;
            progressBar.setProgress((int) (((float) bytesSent)*100/fileSize));

            long finalTotalBytesSent = bytesSent;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    completedTextView.setText((int) (((float) finalTotalBytesSent)*100/fileSize) + "%");
                }
            });
        }
        progressBar.setProgress(100);
        fileInputStream.close();
    }

}