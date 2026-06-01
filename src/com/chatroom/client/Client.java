package com.chatroom.client;

import java.io.Console;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.State;
import java.net.Socket;
import java.util.Scanner;

import com.chatroom.configuration.Config;
import com.chatroom.models.Request;
import com.chatroom.models.Response;
import com.chatroom.others.Hash;
import com.chatroom.others.LogFileWriter;
import com.chatroom.others.Message;

public class Client {
	private int clientID = -1;
	private int roomId = -1;
	private Scanner scanner = new Scanner(System.in);
	private int choice;
	private String cont;
	private String host = "";
	private int port = -1;
	private ObjectOutputStream objectOutputStream;
	private ObjectInputStream objectInputStream;
	private Socket socket;
	private Request request = null;
	private Response response = null;
	private MessageListener messageListener;
	
	public Client(String host, int port) {
		this.host = host;
		this.port = port;
		messageListener = new MessageListener();
	}
	
	public void connect() {
		try {
			socket = new Socket(host,port);
			objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			objectInputStream = new ObjectInputStream(socket.getInputStream());
			mainFunc();
		} catch (IOException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}
	
	private void mainOptions() {
		try {
			Message.println("1. Create Room\n2. Join Room\n3. View Available Roooms\n4. My Groups\n5. Pending Invites\n6. Logout");
			choice = scanner.nextInt();
			switch(choice)
			{
				case 1:
					Message.println("Enter name of chat room to create:");
					cont = scanner.next();
					createAndJoinRoom(cont,true);
					break;
				case 2:
					Message.println("Enter name of chat room to join:");
					cont = scanner.next();
					createAndJoinRoom(cont,false);
					break;
				case 3:
					viewRooms();
					break;
				case 4:
					groupMenu();
					break;
				case 5:
					checkPendingInvites();
					break;
				case 6:
					logOut();
					break;
				default: 
					Message.println("Invalid");
					System.exit(1);
			}
		}
		catch (Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void groupMenu() {
		try {
			Message.println("1. Create Group\n2. View My Groups\n3. Group Chat\n4. View Group Members\n5. Back");
			choice = scanner.nextInt();
			switch(choice) {
				case 1:
					Message.println("Enter group name:");
					cont = scanner.next();
					createGroup(cont);
					break;
				case 2:
					viewMyGroups();
					break;
				case 3:
					Message.println("Enter group ID to chat in:");
					int gid = scanner.nextInt();
					groupConversation(gid);
					break;
				case 4:
					Message.println("Enter group ID to view members:");
					gid = scanner.nextInt();
					viewGroupMembers(gid);
					break;
				case 5:
					mainOptions();
					break;
			}
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void createGroup(String gName) {
		try {
			request = new Request(Request.Type.CREATE_GROUP.ordinal(), clientID, roomId, gName);
			request.setIsConsole(true);
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			response = (Response) objectInputStream.readObject();
			Message.println(response.getContents());
			if(response.getSuccess()) {
				groupMenu();
			} else {
				mainOptions();
			}
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void viewMyGroups() {
		try {
			request = new Request(Request.Type.VIEW_GROUPS.ordinal(), clientID, roomId, "");
			request.setIsConsole(true);
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			response = (Response) objectInputStream.readObject();
			if(response.getSuccess()) {
				Message.println("****** Your Groups ******");
				Message.println(response.getContents());
				Message.println("Enter group ID to invite users, or 0 to go back:");
				int gid = scanner.nextInt();
				if(gid > 0) {
					Message.println("Enter username to invite:");
					String uname = scanner.next();
					inviteToGroup(gid, uname);
				} else {
					groupMenu();
				}
			} else {
				Message.println(response.getContents());
				groupMenu();
			}
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void inviteToGroup(int gid, String username) {
		try {
			String content = gid + "#" + username;
			request = new Request(Request.Type.INVITE_TO_GROUP.ordinal(), clientID, roomId, content);
			request.setIsConsole(true);
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			response = (Response) objectInputStream.readObject();
			Message.println(response.getContents());
			groupMenu();
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void checkPendingInvites() {
		try {
			request = new Request(Request.Type.CHECK_PENDING_INVITES.ordinal(), clientID, roomId, "");
			request.setIsConsole(true);
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			response = (Response) objectInputStream.readObject();
			if(response.getSuccess()) {
				Message.println("****** Pending Invitations ******");
				String[] invites = response.getContents().split("\n");
				for(String inv : invites) {
					String[] parts = inv.split("\\|");
					if(parts.length == 3) {
						Message.println("Group ID: " + parts[0] + " | " + parts[1] + " | Invited by: " + parts[2]);
					}
				}
				Message.println("Enter group ID to accept, -1 to reject, 0 to skip:");
				int gid = scanner.nextInt();
				if(gid > 0) {
					acceptInvite(gid);
				} else if(gid == -1) {
					Message.println("Enter group ID to reject:");
					gid = scanner.nextInt();
					rejectInvite(gid);
				} else {
					mainOptions();
				}
			} else {
				Message.println(response.getContents());
				mainOptions();
			}
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void acceptInvite(int gid) {
		try {
			request = new Request(Request.Type.ACCEPT_INVITE.ordinal(), clientID, roomId, String.valueOf(gid));
			request.setIsConsole(true);
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			response = (Response) objectInputStream.readObject();
			Message.println(response.getContents());
			mainOptions();
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void rejectInvite(int gid) {
		try {
			request = new Request(Request.Type.REJECT_INVITE.ordinal(), clientID, roomId, String.valueOf(gid));
			request.setIsConsole(true);
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			response = (Response) objectInputStream.readObject();
			Message.println(response.getContents());
			mainOptions();
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void viewGroupMembers(int gid) {
		try {
			request = new Request(Request.Type.VIEW_GROUP_MEMBERS.ordinal(), clientID, roomId, String.valueOf(gid));
			request.setIsConsole(true);
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			response = (Response) objectInputStream.readObject();
			if(response.getSuccess()) {
				Message.println(response.getContents());
				Message.println("Enter member ID to remove (admin only), or 0 for back:");
				int mid = scanner.nextInt();
				if(mid > 0) {
					removeGroupMember(gid, mid);
				} else {
					groupMenu();
				}
			} else {
				Message.println(response.getContents());
				groupMenu();
			}
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void removeGroupMember(int gid, int memberId) {
		try {
			String content = gid + "#" + memberId;
			request = new Request(Request.Type.REMOVE_GROUP_MEMBER.ordinal(), clientID, roomId, content);
			request.setIsConsole(true);
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			response = (Response) objectInputStream.readObject();
			Message.println(response.getContents());
			groupMenu();
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void groupConversation(int gid) {
		try {
			if( messageListener.getState() == State.WAITING )
			{
				synchronized (messageListener) {
					messageListener.notify();
				}
			}
			else {
					messageListener.start();
			}

			Message.println("NOTE: You've entered group chat. \n"
					+ "Type your message and press enter to send.\n"
					+ "For leaving the group chat type 'sv_exit' without quotes");

			scanner.nextLine(); // consume newline
			while(true) {
				cont = scanner.nextLine();
				request = new Request(Request.Type.GROUP_MSG.ordinal(), clientID, gid, cont);
				request.setIsConsole(true);
				objectOutputStream.writeObject(request);
				objectOutputStream.flush();
				if(cont.equals("sv_exit")) {
					break;
				}
			}
			groupMenu();
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}
	
	private void logOut() throws Exception{
		cont = "";
		request = new Request(Request.Type.LOGOUT.ordinal(),clientID,roomId,cont);
		request.setIsConsole(true);
		objectOutputStream.writeObject(request);
		objectOutputStream.flush();
		response = (Response) objectInputStream.readObject();
		if( response.getSuccess())
		{
			Message.println(response.getContents());
			connect();
		}
		else
		{
			Message.println(response.getContents());
			mainOptions();
		}
	}

	private void viewRooms() throws Exception{
		try {
			cont = "";
			request = new Request(Request.Type.VIEW_ROOMS.ordinal(),clientID,roomId,cont);
			request.setIsConsole(true);
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			response = (Response) objectInputStream.readObject();
			if( response.getSuccess())
			{
				Message.println("****** List of available rooms are ******");
				Message.println(response.getContents());
				Message.print("Enter name of chat room:");
				cont = scanner.next();
				createAndJoinRoom(cont, false);
			}
			else
			{
				Message.println(response.getContents());
				mainOptions();
			}
		}
		catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void createAndJoinRoom(String rName, boolean create) throws Exception
	{
		if(create)
			request = new Request(Request.Type.CREATE_ROOM.ordinal(),clientID,roomId,cont);
		else
			request = new Request(Request.Type.JOIN_ROOM.ordinal(),clientID,roomId,cont);
		
		request.setIsConsole(true);
		objectOutputStream.writeObject(request);
		objectOutputStream.flush();
		Object obj =  objectInputStream.readObject();		
		if( obj.getClass() == Response.class )
			response = (Response) obj;
		else
		{
			//ObjectInputStream iis = (ObjectInputStream) obj;
			throw new Exception("Object returned is not of type Response. but of " + obj.getClass().toString() );
		}
		if( response.getSuccess())
		{
				//connected();
				Message.println(response.getContents()); //room created and joined successfully
				int hashIndex = response.getContents().indexOf('#');
				roomId = Integer.parseInt(response.getContents().substring(hashIndex+1, response.getContents().indexOf(" ", hashIndex)));
				Message.println(roomId+"");
				//send message interface
				conversation();
		}
		else
		{
			Message.println(response.getContents());
			mainOptions();
		}
	}
	
	private void conversation() throws Exception{
		if( messageListener.getState() == State.WAITING )
		{
			synchronized (messageListener) {
				messageListener.notify();
			}
		}
		else {
				messageListener.start();
		}
		
		Message.println("NOTE: You've entered the server. \n"
				+ "Type your message and press enter to send.\n"
				+ "To send message to a particular user use: '@user_name your_message' without quotes\n"
				+ "For exiting the room type 'sv_exit' without quotes\n"
				+ "For logging out type 'sv_logout' without quotes");
		
		request = new Request(Request.Type.STATUS_MSG.ordinal(),clientID,roomId,"joined the chat");
		request.setIsConsole(true);
		
		objectOutputStream.writeObject(request);
		objectOutputStream.flush();
		
		while(true) {
			cont = scanner.nextLine();
			request = new Request(Request.Type.MSG.ordinal(),clientID,roomId,cont);
			request.setIsConsole(true);
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			if( cont.equals("sv_exit") || cont.equals("sv_logout"))
			{
				break;
			}
		}
		if( cont.equals("sv_exit"))
		{
			mainOptions();
		}
		if( cont.equals("sv_logout"))
		{
			//mainFunc();
			connect();
		}
		
		
	}
	
	class MessageListener extends Thread{
		public void run()
		{
			while(true) {
				try {
					response = (Response) objectInputStream.readObject();
					
					if(response.getId() == Response.Type.LOGOUT.ordinal()) {
						Message.println(response.getContents());
					}
					else if(response.getId() == Response.Type.STATUS_MSG.ordinal() && response.getContents().equals("sv_exit_successful")) {
						Message.println(response.getContents());
					}
					else if(response.getId() == Response.Type.STATUS_MSG.ordinal() && response.getContents().contains("Wrong username ")) {
						Message.println(response.getContents());
					}
					else if(response.getId() == Response.Type.MSG.ordinal() || response.getId() == Response.Type.STATUS_MSG.ordinal() || response.getId() == Response.Type.P_MSG.ordinal() || response.getId() == Response.Type.GROUP_MSG.ordinal()){
						String msg = response.getContents();
						String name = msg.substring(0, msg.indexOf(" "));
						msg = msg.substring(msg.indexOf(" ")+1);
						if(response.getId() == Response.Type.P_MSG.ordinal())
							Message.println("\n<" + name + "> (Personal Message): " + msg);
						else if(response.getId() == Response.Type.GROUP_MSG.ordinal())
							Message.println("\n<" + name + "> (Group): " + msg);
						else	
							Message.println("\n<" + name + ">: " + msg);
					}
					else if(response.getId() == Response.Type.GROUP_INVITE_NOTIFICATION.ordinal()) {
						Message.println("\n[NOTIFICATION] " + response.getContents());
					}
					else if(response.getId() == Response.Type.GEN.ordinal()) {
						String data = "List of Online Users \n";
						int i = 1;
						data += i + ". You \n";
						String temp = response.getContents();
						if(temp.length() != 0 && !temp.equals("")) {
							String arrayOFNames[] = temp.split(",");
							for (String string : arrayOFNames) {
								i++;
								data += i + ". " + string + "\n";
							}
						}
						Message.println(data);
					}
					if(response.getContents().equals("sv_exit_successful")) {
						synchronized(this){
							this.wait();
						}
					}
					else if(response.getId() == Response.Type.LOGOUT.ordinal()) {
						synchronized(this){
							this.wait();
						}
					}
					
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
					break;
				} catch (InterruptedException e) {
					e.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
				}
			}
		}
	}
	
	public void mainFunc() {
		Console console = System.console();
		try {
			Message.println("1. SIGN UP \n2. LOGIN");
			choice = scanner.nextInt();
			if(choice == 1) {
				Message.print("Enter username: ");
				String uname = scanner.next();
				char[] pwd = console.readPassword("Enter password: ");
				Message.print("Enter full name (or press enter for username): ");
				scanner.nextLine(); // consume newline
				String fullName = scanner.nextLine();
				if(fullName.trim().isEmpty()) fullName = uname;
				Message.print("Enter gender (Male/Female/Other): ");
				String gender = scanner.next();
				if(!gender.equals("Male") && !gender.equals("Female")) gender = "Other";
				Message.println("Choose avatar (0-11):");
				Message.println("0:Blue 1:Green 2:Purple 3:Red 4:Yellow 5:Orange");
				Message.println("6:Teal 7:DarkGray 8:Gray 9:Gold 10:DarkPurple 11:GreenSea");
				int avatarIdx = scanner.nextInt();
				if(avatarIdx < 0 || avatarIdx > 11) avatarIdx = 0;
				cont = uname + "#" + Hash.getHash(new String(pwd)) + "#" + fullName + "#" + gender + "#" + avatarIdx;
				request = new Request(Request.Type.SIGN_UP.ordinal(),clientID,roomId,cont);
				request.setIsConsole(true);
				Message.println("Signing Up ... ");
			}
			else if(choice == 2) {
				Message.print("Enter username: ");
				cont = scanner.next();
				cont += "#";
				char[] pwd = console.readPassword("Enter password: "); //take the password and separate it from user name by # delimiter
				cont += Hash.getHash(new String(pwd));
				request = new Request(Request.Type.LOGIN.ordinal(),clientID,roomId,cont);
				request.setIsConsole(true);
				Message.println("Logging In ... ");
			}
			else {
				System.out.print("Invalid input!");
				System.exit(1);
			}
			objectOutputStream.writeObject(request);
			objectOutputStream.flush();
			
			response = (Response) objectInputStream.readObject();
			if( response.getId() == Response.Type.SIGN_UP.ordinal())
			{
				if(response.getSuccess()) {
					Message.println("Signed Up Successfully.");
					clientID = Integer.parseInt(response.getContents());
					Message.println("Your ID is: "+ clientID);
					mainOptions();
	
				}
				else {
					Message.println(response.getContents());
					mainFunc();
				}
			}
			else if(response.getId() == Request.Type.LOGIN.ordinal()) {
				if(response.getSuccess()) {
					String[] parts = response.getContents().split("#");
					clientID = Integer.parseInt(parts[0]);
					Message.println("Login Successfull ... ");
					Message.println("Your ID is: "+ clientID);
					if(parts.length > 1) Message.println("Welcome, " + parts[1]);
					mainOptions();
				}
				else {
					Message.println(response.getContents());
					mainFunc();
				}
			}

		}
		catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}
}