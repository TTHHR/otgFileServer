package com.yaasoosoft.liteotgserver.utils;

import android.util.Log;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ServerUtil {
    private static ServerUtil serverUtil =new ServerUtil();
    private final String TAG="su";
    public boolean isRunning=false;
    private ServerUtil(){}
    public static ServerUtil getIns()
    {
        return serverUtil;
    }
    AsyncHttpServer server = new AsyncHttpServer();

    List<WebSocket> _sockets = new ArrayList<WebSocket>();

    public void start(int port,String rootPath) throws Exception
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
                       filePath="";
                   }
                    Log.e(TAG, filePath);

                    File file=new File(rootPath+filePath);
                    if(!file.exists())
                    {
                        response.code(201);
                        response.send("文件不存在");
                    }
                   else
                    {
                        if(file.isDirectory())
                        {
                            for (File name:file.listFiles()) {
                                stringBuffer.append("<a href='file?name=").append(file.getPath().replace(rootPath, "")).append("/").append(name.getName()).append("'>").append(name.getName()).append("</a></br>");
                            }
                            response.send(stringBuffer.toString());
                        }
                        else
                        {
                            response.sendFile(file);
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

    public void stop(
    )
    {
        if(isRunning) {
            server.stop();
            isRunning=false;
        }
    }

}
