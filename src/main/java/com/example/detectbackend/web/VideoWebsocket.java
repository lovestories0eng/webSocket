package com.example.detectbackend.web;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.springframework.stereotype.Component;

@ServerEndpoint(value = "/v")
@Component
public class VideoWebsocket {

    //concurrent包的线程安全，用来存放每个客户端对应的WebSocket
    private static ConcurrentHashMap<String, VideoWebsocket> webSocket = new ConcurrentHashMap<String, VideoWebsocket>();
    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    // 表示用户
    private static final String user = "user";
    // 表示处理视频
    private static final String process = "process";
    // 给用户一个id
    private static int id = 0;
    private static String thisUser = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println(session);
        try {
            String type = session.getQueryString();
            if (type.equals("process")) {
                // 表示主播
                thisUser = process;
                webSocket.put(thisUser, this);
                System.out.println("发送视频流");
            } else {
                // 表示用户
                thisUser = String.valueOf(id);
                webSocket.put(thisUser, this);
                System.out.println("有人加入，是用户");
                id = id + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) throws Exception {
        System.out.println("关闭连接：" + thisUser);
        //需要清除当前和移除内存里的，不然还能接收信息
        session.close();
        webSocket.remove(thisUser);
    }

    /**
     * 收到客户端消息后调用的方法
     */
    //@OnMessage(maxMessageSize = 12)表示超出12个字节会自动关闭这个连接
    @OnMessage(maxMessageSize = 65536)
    public void onMessage(String message, Session session) throws IOException {
        if (message.equals("ping")) {
            session.getBasicRemote().sendText("pong");
        } else {
            System.out.println(session);
            // System.out.println("来自客户端的消息:" + message);9
            // 群发消息
            Iterator<Map.Entry<String, VideoWebsocket>> iter = webSocket.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, VideoWebsocket> entry = iter.next();
                // TODO: 不把消息重复广播给自己
                // if (!entry.getValue().session.equals(session)) {
                    webSocket.get(entry.getKey()).session.getBasicRemote().sendText(message);
                // }
            }
        }


    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) throws Exception {
        System.out.println("发生错误：" + thisUser);
        session.close();
        webSocket.remove(thisUser);
    }
}