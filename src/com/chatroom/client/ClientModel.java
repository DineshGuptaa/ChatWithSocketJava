package com.chatroom.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientModel {
	private int clientID=-1;
	private int roomId = -1;
	private String host = "";
	private int port = -1;
	public static ObjectOutputStream objectOutputStream;
	public static ObjectInputStream objectInputStream;
	private Socket socket;

	private String fullName = "";
	private String gender = "Other";
	private int avatarIndex = 0;

	public ClientModel(String host, int port) {
		super();
		this.host = host;
		this.port = port;

		try {
			socket = new Socket(host, port);
			socket.setSoTimeout(30000);
			objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			objectInputStream = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getClientID() {
		return clientID;
	}

	public void setClientID(int clientID) {
		this.clientID = clientID;
	}

	public int getRoomId() {
		return roomId;
	}

	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}

	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }
	public String getGender() { return gender; }
	public void setGender(String gender) { this.gender = gender; }
	public int getAvatarIndex() { return avatarIndex; }
	public void setAvatarIndex(int avatarIndex) { this.avatarIndex = avatarIndex; }
}
