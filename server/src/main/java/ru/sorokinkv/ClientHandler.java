package ru.sorokinkv;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;
    private int id;

    public String getNick() {
        return nick;
    }

    public int getId() {
        return id;
    }

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/auth ")) {
                            // /auth login1 pass1
                            String[] tokens = msg.split(" ");
                            String nick = SQLHandler.getNickByLoginPass(tokens[1], tokens[2]);
                            if (nick != null) {
                                if (server.isNickBusy(nick)) {
                                    out.writeUTF("Учетная запись уже используется");
                                    continue;
                                }
                                out.writeUTF("/authok " + nick);
                                this.nick = nick;
                                this.id = SQLHandler.getIdByNick(nick);
                                server.subscribe(this);
                                break;
                            } else {
                                out.writeUTF("Неверный логин/пароль");
                            }
                        }
                    }
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/")) {
                            if (msg.startsWith("/w ")) {
                                String[] tokens = msg.split(" ", 3);
                                server.sendPrivateMsg(this, tokens[1], tokens[2]);
                            }
                            if (msg.equals("/history")) {
                                sendMsg(SQLHandler.getHistory(id));
                            }
                        } else {
                            server.broadcastMsg(this, msg);
                        }
                        System.out.println(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
