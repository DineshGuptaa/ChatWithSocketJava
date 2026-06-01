package com.chatroom.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import com.chatroom.configuration.Config;
import com.chatroom.models.Request;
import com.chatroom.others.LogFileWriter;
import com.chatroom.others.Message;

public class ClientThread extends Thread{

	Socket socket;
	ObjectInputStream objectInputStream;
	ObjectOutputStream objectOutputStream;
	int lastRoomId = -1;
	int lastClientId = -1;
	
	public ClientThread(Socket s) {
		socket = s;
	}
	
	public void run()
	{
		try {
			objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			objectInputStream = new ObjectInputStream(socket.getInputStream());
			
			while(true)
			{
				Request request = (Request) objectInputStream.readObject();
				lastRoomId = request.getRoomId();
				lastClientId = request.getClientId();
				Server.requestqueue.add(new ClientRequest(this, request));
			}
			
		} catch (Throwable e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
			if( e instanceof java.io.EOFException )
			{
				Request eofRequest;
				if(lastRoomId != -1) {
					eofRequest = new Request(Request.Type.MSG.ordinal(), lastClientId, lastRoomId, "sv_exit");
				} else if(lastClientId != -1) {
					eofRequest = new Request(Request.Type.LOGOUT.ordinal(), lastClientId, -1, "");
				} else {
					eofRequest = new Request(Request.Type.LOGOUT.ordinal(), -1, -1, "");
				}
				Server.requestqueue.add(new ClientRequest(this, eofRequest));
			}
		}
	}
}
