package com.chatroom.server;

import com.chatroom.models.Request;

public class ClientRequest {
    public ClientThread clientThread;
    public Request request;

    public ClientRequest(ClientThread clientThread, Request request) {
        this.clientThread = clientThread;
        this.request = request;
    }
}
