package com.chatroom.models;
import java.io.Serializable;
public class Request implements Serializable{
	public enum Type {
		ACK,SIGN_UP, LOGIN, LOGOUT, CREATE_ROOM, JOIN_ROOM, VIEW_ROOMS, MSG, STATUS_MSG,
		CREATE_GROUP, INVITE_TO_GROUP, ACCEPT_INVITE, REJECT_INVITE,
		REMOVE_GROUP_MEMBER, VIEW_GROUPS, VIEW_GROUP_MEMBERS, GROUP_MSG,
		CHECK_PENDING_INVITES, VIEW_ONLINE_USERS, FORGOT_PASSWORD, LOAD_MESSAGES,
		SEND_READ_RECEIPT, FILE_MSG, GET_FILE, DELETE_MESSAGES, UNDO_DELETE;
	}
	
	int id;
	int clientId;
	int roomId;
	String contents = "";
	boolean isConsoleRequest = false;
	
	public Request(int id, int clientId, int roomId, String contents) {
		this.id = id;
		this.clientId = clientId;
		this.roomId = roomId;
		this.contents = contents;
	}
	
	//getter methods
	public int getId() {
		return id;
	}
	
	public int getClientId() {
		return clientId;
	}
	
	public int getRoomId() {
		return roomId;
	}
	
	public String getContents() {
		return contents;
	}
	
	public boolean getIsConsole() {
		return isConsoleRequest;
	}
	
	//setter methods
	public void setIsConsole(boolean consoleRequest) {
		this.isConsoleRequest = consoleRequest;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setClientId(int clientId) {
		this.clientId = clientId;
	}
	
	public void setRoomId(int roomId) {
		this.roomId = roomId;
	}
	
	public void setContents(String content) {
		this.contents = content;
	}
}
