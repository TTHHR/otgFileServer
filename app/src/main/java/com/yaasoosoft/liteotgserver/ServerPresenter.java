package com.yaasoosoft.liteotgserver;

import android.util.Log;

import com.github.mjdev.libaums.fs.UsbFile;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ServerPresenter {
    private String TAG="sp";
    MainInterface mainInterface;
    private UsbFile root;
    private int port;
    private String path;
    public boolean isRunning=false;
    AsyncHttpServer server = new AsyncHttpServer();

    List<WebSocket> _sockets = new ArrayList<WebSocket>();

    ServerPresenter(MainInterface mainInterface)
    {
        this.mainInterface=mainInterface;
    }
    boolean busy=false;
    void start()
    {
        if(busy)
            return;
        else
        {
            busy=true;
            new Thread(()->{
                boolean b=mainInterface.readDevice();
                if(b)
                {
                    try {
                       start(port);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mainInterface.setRunState(true);
                }
                else
                {
                    mainInterface.setRunState(false);
                }
                busy=false;
            }).start();
        }
    }
    void stop()
    {
        server.stop();
    }


    public void setRootFile(UsbFile root) {
        this.root=root;
    }

    public void setPort(int port) {
        this.port=port;
    }

    public void setRootPath(String path) {
        this.path=path;
    }
    public void start(int port) throws Exception
    {
        if(isRunning)
            throw new Exception("already running");
        server.stop();

        server.get("/file", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                StringBuilder stringBuffer=new StringBuilder();
                if(request.getMethod().equals("GET")) {
                    Multimap p = request.getQuery();
                    String filePath=p.getString("name");
                    if(filePath==null)
                    {
                        filePath="/";
                    }
                    ArrayList<UsbFile> sl = readFile(root, filePath);
                    if(sl==null)
                    {
                        response.send("文件不存在");
                    }
                    else {
                        if (sl.size()>1) {
                            for (int i=1;i<sl.size();i++) {
                                UsbFile name=sl.get(i);
                                stringBuffer.append("<a href='file?name=").append(name.getAbsolutePath()).append("'>").append(name.getName()).append("</a></br>");
                            }
                            response.send(stringBuffer.toString());
                        } else {
                                response.send("暂不支持文件下载");
                            //response.sendStream(is,);
                        }
                    }
                }
            }

        });
        server.setErrorCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                Log.e(TAG,ex.toString());
            }
        });

        server.listen(port);
        isRunning=true;
    }
    private UsbFile tree(UsbFile file,String name) throws IOException {
        for (UsbFile f: file.listFiles()){
            if(f.getName().equals(name))
                return f;
        }
        return null;
    }
    private ArrayList<UsbFile> readFile(UsbFile root, String filePath){
        ArrayList<UsbFile> mUsbFiles = new ArrayList<>();
        String[] trees=filePath.split("/");
        try {
            if(trees.length>1)
                for (int i = 1; i < trees.length; i++) {
                    root=tree(root,trees[i]);
                    if(root==null)
                    {
                        mainInterface.log("文件不存在"+filePath);
                        return null;
                    }
                }
            if(root.isDirectory()) {
                mUsbFiles.add(root);
                mainInterface.log("当前读取文件夹："+root.getName());
                for (UsbFile file : root.listFiles()) {
                    mainInterface.log(file.getName());
                    mUsbFiles.add(file);
                }
            }
            else
            {
                mUsbFiles.add(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mUsbFiles;
    }
}
