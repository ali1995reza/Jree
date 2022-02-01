package jree.client_server.client.io;

import jutils.assertion.Assertion;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

public class JreeClient {

    public static void main(String[] args) {
        JreeClient client = new JreeClient();
    }

    private WebSocketClient client;
    private Session session;
    private Consumer<String> messageListener = s -> {};

    public JreeClient() {
    }

    public void setMessageListener(Consumer<String> messageListener) {
        Assertion.ifNull("listener is null", messageListener);
        this.messageListener = messageListener;
    }

    public void connect(URI uri) {
        if (client != null) {
            throw new IllegalStateException("already connected");
        }
        client = new WebSocketClient();
        try {
            client.start();
            client.connect(new SocketHandler(), uri);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public void executeCommand(String command) {
        doSendMsg(command);
    }

    private void doSendMsg(String message) {
        try {
            session.getRemote().sendString(message);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private final class SocketHandler implements WebSocketListener {

        @Override
        public void onWebSocketBinary(byte[] bytes, int i, int i1) {
        }

        @Override
        public void onWebSocketText(String s) {
            messageListener.accept(s);
        }

        @Override
        public void onWebSocketClose(int i, String s) {
            System.out.println("ITS CLOSEEDDDD !");
        }

        @Override
        public void onWebSocketConnect(Session s) {
            session = s;
        }

        @Override
        public void onWebSocketError(Throwable throwable) {
        }

    }

}
