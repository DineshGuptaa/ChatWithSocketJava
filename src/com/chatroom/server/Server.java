package com.chatroom.server;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.chatroom.configuration.Config;
import com.chatroom.models.InviteInfo;
import com.chatroom.models.MessageTrackObject;
import com.chatroom.models.Request;
import com.chatroom.models.Response;
import com.chatroom.others.LogFileWriter;
import com.chatroom.others.Message;

class RequestAnalyser extends Thread{
	String name = null;
	ResultSet resultSet = null;
	java.sql.PreparedStatement preparedStatement = null;
	String query = null;
	Set<Integer> clientIds;
	MessageTrackObject messageTrackObject;
	
	public void run()
	{
		Response response = null;
		Request request = null;
		ClientThread clientThread = null;
		while(true)
		{
			try
			{
				ClientRequest cr = Server.requestqueue.take();
				clientThread = cr.clientThread;
				request = cr.request;
				if(request == null) continue;
				switch(request.getId())
				{
					case 1:
						int clientID = -1;
						
						String requestContent = request.getContents().trim();
						String[] signupParts = requestContent.split("#");
						String username = signupParts[0];
						String pwd = signupParts.length > 1 ? signupParts[1] : "";
						String fullName = signupParts.length > 2 ? signupParts[2] : username;
						String gender = signupParts.length > 3 ? signupParts[3] : "Other";
						int avatarIndex = signupParts.length > 4 ? Integer.parseInt(signupParts[4]) : 0;
						
						//first check if the user already exists or not
						query = "SELECT " +Config.CLIENT_ID+ " from "+ Config.TABLE_NAME + " WHERE " + Config.CLIENT_NAME+"=?";
						preparedStatement = Server.connection.prepareStatement(query);
						preparedStatement.setString(1,username);
						resultSet = preparedStatement.executeQuery();
						
						if(!resultSet.isBeforeFirst()) {							
							//if user is not already present then insert data into database
							String query =  "INSERT INTO " + Config.TABLE_NAME + "("+Config.CLIENT_NAME+","+ Config.CLIENT_PWD +","+ Config.DISPLAY_NAME +","+ Config.GENDER +","+ Config.PROFILE_AVATAR +") VALUES(?,?,?,?,?)";
							preparedStatement = Server.connection.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);
							preparedStatement.setString(1, username);
							preparedStatement.setString(2, pwd);
							preparedStatement.setString(3, fullName);
							preparedStatement.setString(4, gender);
							preparedStatement.setInt(5, avatarIndex);
							
							
							int response_code = preparedStatement.executeUpdate();
							if(response_code > 0) {							
								//success
								resultSet = preparedStatement.getGeneratedKeys();
								if(resultSet.next())
									clientID = resultSet.getInt(1);
								response = new Response( 1 , true, clientID + "#" + fullName + "#" + gender + "#" + avatarIndex);
								//store the client in our client holder hash map
								Server.clientHolder.put(clientID, clientThread);
								//update the user message count
								Server.messagesTrackHashmap.put(clientID,0);
							}
							else {
								//something went wrong or if data is not inserted in database
								response = new Response( 1 , false, "Something went wrong while signing up ...");
							}
						}
						else {
							response = new Response( 1 , false, "User already present ...");
						}
						
						Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));

						break;
					case 2: {
						requestContent = request.getContents().trim();
						String loginName = requestContent.substring(0, requestContent.indexOf("#"));
						String loginPwd = requestContent.substring(requestContent.indexOf("#")+1);
						query = "SELECT " +Config.CLIENT_ID+ "," + Config.DISPLAY_NAME + "," + Config.GENDER + "," + Config.PROFILE_AVATAR + " from "+ Config.TABLE_NAME + " WHERE " + Config.CLIENT_NAME+"=? AND " + Config.CLIENT_PWD + "=?";
						preparedStatement = Server.connection.prepareStatement(query);
						preparedStatement.setString(1,loginName);
						preparedStatement.setString(2,loginPwd);
						
						ResultSet resultSet = preparedStatement.executeQuery();
						if(!resultSet.isBeforeFirst()) {
							//no data
							response = new Response( 2 , false, "Check your username and password ...");
						}
						else {
							resultSet.next();
							int client_id = resultSet.getInt(1);
							String loginFullName = resultSet.getString(2);
							String loginGender = resultSet.getString(3);
							int loginAvatarIndex = resultSet.getInt(4);
							response = new Response( 2 , true, client_id + "#" + loginFullName + "#" + loginGender + "#" + loginAvatarIndex);
							//store the client in our client holder hash map
							Server.clientHolder.put(client_id, clientThread);
							
							//stores the client id and message count into the hash map
							Server.messagesTrackHashmap.put(client_id, 0);
						}
						
						Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));

						break;
					}
					case 3:
							//for logout
							logout(clientThread,request,1);
							break;
					case 4:
							//create room
							int roomId = Server.getRoomId();
							String roomName = request.getContents();
							
							if(Server.roomsMapping.containsValue(roomName)) {
								//if the room name is already present
								response = new Response( 4 , false, "Room name already exists ...");
							}
							else {
								//insert the new mapping if it is not already present in the hash map
								Server.roomsMapping.put(roomId,roomName);
								Server.listOfRooms.add(roomId);
								clientIds = new HashSet<>();
								clientIds.add(request.getClientId()); //insert the client into the set
								Server.roomsHolder.put(roomId, clientIds);
								response = new Response( 4 , true, "Room #" + roomId  + " created and joined successfully");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 5:
							//join room
							String roomName1 = request.getContents();
							int clientId = request.getClientId();
							int roomId1 = -1;
							roomId1 = Server.getKey(roomName1);
							
							if(Server.listOfRooms.contains(roomId1)) {
								//insert the client id in the set of the specific room id in hash map
								if(Server.roomsHolder.get(roomId1).add(clientId)) {
									response = new Response( Response.Type.JOIN_ROOM.ordinal() , true, "Room #" + roomId1 + " joined successfully ...");
								}
								else {
									response = new Response( Response.Type.JOIN_ROOM.ordinal() , false, "Error while joining room ...");
								}
							}
							else {
								response = new Response( 5 , false, "Room not found ...");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 6:
							//list of rooms
							String roomNames = "";
							if(Server.listOfRooms.size() > 0) {
								Iterator<Integer> iterator = Server.listOfRooms.iterator();
								while(iterator.hasNext()) {
									//get the room names from hash map
									roomNames += Server.roomsMapping.get(iterator.next()) + "\n";
								}
								response = new Response( 6 , true, roomNames);
							}
							else {
								response = new Response( 6 , false, "Currently there are no active rooms ...");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
							
					case 7:
							//message
							if(request.getContents().equals(" ") || request.getContents().equals(""))
								break;
							Server.messagequeue.add(request);

							break;
					case 8:
							if(request.getContents().equals(" ") || request.getContents().equals(""))
								break;
							Server.messagequeue.add(request);

							break;
					case 9:
							// CREATE_GROUP
							String groupName = request.getContents();
							int groupId = Server.getGroupId();

							if(Server.groupMapping.containsValue(groupName)) {
								response = new Response(Response.Type.CREATE_GROUP.ordinal(), false, "Group name already exists");
							} else {
								int adminId = request.getClientId();
								Server.groupMapping.put(groupId, groupName);
								Server.groupAdmins.put(groupId, adminId);
								Set<Integer> members = new HashSet<>();
								members.add(adminId);
								Server.groupMembers.put(groupId, members);
								Server.listOfGroups.add(groupId);
								response = new Response(Response.Type.CREATE_GROUP.ordinal(), true,
									"Group #" + groupId + " '" + groupName + "' created successfully");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 10:
							// INVITE_TO_GROUP
							String[] parts = request.getContents().split("#");
							if(parts.length == 2) {
								int targetGroupId = Integer.parseInt(parts[0]);
								String invitedUserName = parts[1];
								int inviterId = request.getClientId();

								if(Server.groupAdmins.get(targetGroupId) != null &&
								   Server.groupAdmins.get(targetGroupId) == inviterId) {
									// Get invited user's ID
									query = "SELECT " + Config.CLIENT_ID + " FROM " + Config.TABLE_NAME +
										" WHERE " + Config.CLIENT_NAME + "=?";
									preparedStatement = Server.connection.prepareStatement(query);
									preparedStatement.setString(1, invitedUserName);
									resultSet = preparedStatement.executeQuery();
									if(resultSet.next()) {
										int invitedId = resultSet.getInt(1);
										Set<Integer> members = Server.groupMembers.get(targetGroupId);
										if(members != null && members.contains(invitedId)) {
											response = new Response(Response.Type.INVITE_TO_GROUP.ordinal(), false,
												"User already a member of this group");
										} else {
											String inviterName = Server.getClientNameFromId(inviterId);
											String grpName = Server.groupMapping.get(targetGroupId);
											InviteInfo invite = new InviteInfo(targetGroupId, grpName, inviterId, inviterName);

											Server.userPendingInvites.computeIfAbsent(invitedId, k -> new ArrayList<>()).add(invite);

											// Notify the invited user if online
											ClientThread invitedClient = Server.clientHolder.get(invitedId);
											if(invitedClient != null) {
												Response notifyRes = new Response(Response.Type.GROUP_INVITE_NOTIFICATION.ordinal(),
													true, "You have been invited to group '" + grpName + "' by " + inviterName);
												Server.responseMakerQueue.add(new ResponseHolder(notifyRes, invitedClient.objectOutputStream));
											}
											response = new Response(Response.Type.INVITE_TO_GROUP.ordinal(), true,
												"Invitation sent to " + invitedUserName);
										}
									} else {
										response = new Response(Response.Type.INVITE_TO_GROUP.ordinal(), false,
											"User '" + invitedUserName + "' not found");
									}
								} else {
									response = new Response(Response.Type.INVITE_TO_GROUP.ordinal(), false,
										"Only group admin can invite members");
								}
							} else {
								response = new Response(Response.Type.INVITE_TO_GROUP.ordinal(), false, "Invalid format");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 11:
							// ACCEPT_INVITE
							int acceptGroupId = Integer.parseInt(request.getContents());
							int userId = request.getClientId();
							ArrayList<InviteInfo> invites = Server.userPendingInvites.get(userId);
							InviteInfo foundInvite = null;
							if(invites != null) {
								for(InviteInfo inv : invites) {
									if(inv.getGroupId() == acceptGroupId) {
										foundInvite = inv;
										break;
									}
								}
							}
							if(foundInvite != null) {
								Server.groupMembers.get(acceptGroupId).add(userId);
								invites.remove(foundInvite);
								response = new Response(Response.Type.ACCEPT_INVITE.ordinal(), true,
									"You joined group '" + foundInvite.getGroupName() + "'");

								// Notify admin
								int adminId = Server.groupAdmins.get(acceptGroupId);
								ClientThread adminClient = Server.clientHolder.get(adminId);
								if(adminClient != null) {
									String userName = Server.getClientNameFromId(userId);
									Response adminNotif = new Response(Response.Type.GROUP_INVITE_NOTIFICATION.ordinal(), true,
										userName + " accepted invitation to group '" + foundInvite.getGroupName() + "'");
									Server.responseMakerQueue.add(new ResponseHolder(adminNotif, adminClient.objectOutputStream));
								}
							} else {
								response = new Response(Response.Type.ACCEPT_INVITE.ordinal(), false, "No pending invitation found");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 12:
							// REJECT_INVITE
							int rejectGroupId = Integer.parseInt(request.getContents());
							int rejUserId = request.getClientId();
							ArrayList<InviteInfo> rejInvites = Server.userPendingInvites.get(rejUserId);
							InviteInfo rejFound = null;
							if(rejInvites != null) {
								for(InviteInfo inv : rejInvites) {
									if(inv.getGroupId() == rejectGroupId) {
										rejFound = inv;
										break;
									}
								}
							}
							if(rejFound != null) {
								rejInvites.remove(rejFound);
								response = new Response(Response.Type.REJECT_INVITE.ordinal(), true,
									"Rejected invitation to group '" + rejFound.getGroupName() + "'");

								int adminId = Server.groupAdmins.get(rejectGroupId);
								ClientThread adminClient = Server.clientHolder.get(adminId);
								if(adminClient != null) {
									String userName = Server.getClientNameFromId(rejUserId);
									Response adminNotif = new Response(Response.Type.GROUP_INVITE_NOTIFICATION.ordinal(), true,
										userName + " rejected invitation to group '" + rejFound.getGroupName() + "'");
									Server.responseMakerQueue.add(new ResponseHolder(adminNotif, adminClient.objectOutputStream));
								}
							} else {
								response = new Response(Response.Type.REJECT_INVITE.ordinal(), false, "No pending invitation found");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 13:
							// REMOVE_GROUP_MEMBER
							String[] removeParts = request.getContents().split("#");
							if(removeParts.length == 2) {
								int remGroupId = Integer.parseInt(removeParts[0]);
								int memberToRemove = Integer.parseInt(removeParts[1]);
								int requesterId = request.getClientId();

								if(Server.groupAdmins.get(remGroupId) != null &&
								   Server.groupAdmins.get(remGroupId) == requesterId) {
									Set<Integer> members = Server.groupMembers.get(remGroupId);
									if(members != null && members.remove(memberToRemove)) {
										response = new Response(Response.Type.REMOVE_GROUP_MEMBER.ordinal(), true,
											"Member removed successfully");

										ClientThread removedClient = Server.clientHolder.get(memberToRemove);
										if(removedClient != null) {
											Response kickNotif = new Response(Response.Type.GROUP_INVITE_NOTIFICATION.ordinal(), true,
												"You were removed from group '" + Server.groupMapping.get(remGroupId) + "'");
											Server.responseMakerQueue.add(new ResponseHolder(kickNotif, removedClient.objectOutputStream));
										}
									} else {
										response = new Response(Response.Type.REMOVE_GROUP_MEMBER.ordinal(), false,
											"Member not found in group");
									}
								} else {
									response = new Response(Response.Type.REMOVE_GROUP_MEMBER.ordinal(), false,
										"Only group admin can remove members");
								}
							} else {
								response = new Response(Response.Type.REMOVE_GROUP_MEMBER.ordinal(), false, "Invalid format");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 14:
							// VIEW_GROUPS - list groups where user is a member
							int viewUserId = request.getClientId();
							String groupsList = "";
							for(java.util.Map.Entry<Integer, Set<Integer>> entry : Server.groupMembers.entrySet()) {
								if(entry.getValue().contains(viewUserId)) {
									int gId = entry.getKey();
									String gName = Server.groupMapping.get(gId);
									int adminId = Server.groupAdmins.get(gId);
									String adminName = Server.getClientNameFromId(adminId);
									groupsList += "ID:" + gId + " | " + gName + " (Admin: " + adminName + ")\n";
								}
							}
							if(groupsList.isEmpty()) {
								response = new Response(Response.Type.VIEW_GROUPS.ordinal(), false,
									"You are not a member of any group");
							} else {
								response = new Response(Response.Type.VIEW_GROUPS.ordinal(), true, groupsList);
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 15:
							// VIEW_GROUP_MEMBERS
							int vgmGroupId = Integer.parseInt(request.getContents());
							Set<Integer> vgmMembers = Server.groupMembers.get(vgmGroupId);
							if(vgmMembers != null && vgmMembers.contains(request.getClientId())) {
								String memberList = "Members of '" + Server.groupMapping.get(vgmGroupId) + "':\n";
								for(int mid : vgmMembers) {
									String mName = Server.getClientNameFromId(mid);
									boolean isAdmin = Server.groupAdmins.get(vgmGroupId) == mid;
									memberList += "  " + mName + (isAdmin ? " (Admin)" : "") + "\n";
								}
								response = new Response(Response.Type.VIEW_GROUP_MEMBERS.ordinal(), true, memberList);
							} else {
								response = new Response(Response.Type.VIEW_GROUP_MEMBERS.ordinal(), false,
									"You are not a member of this group");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 16:
							// GROUP_MSG
							if(request.getContents().equals(" ") || request.getContents().equals(""))
								break;
							Server.messagequeue.add(request);
							break;
					case 17:
							// CHECK_PENDING_INVITES
							int chkUserId = request.getClientId();
							ArrayList<InviteInfo> pendingInvites = Server.userPendingInvites.get(chkUserId);
							if(pendingInvites != null && !pendingInvites.isEmpty()) {
								String pendingStr = "";
								for(InviteInfo inv : pendingInvites) {
									pendingStr += inv.getGroupId() + "|" + inv.getGroupName() + "|" + inv.getInviterName() + "\n";
								}
								response = new Response(Response.Type.PENDING_INVITES.ordinal(), true, pendingStr);
							} else {
								response = new Response(Response.Type.PENDING_INVITES.ordinal(), false, "No pending invitations");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 18:
							// VIEW_ONLINE_USERS - show all registered users except self
							String onlineUsers = "";
							String sql = "SELECT " + Config.CLIENT_ID + "," + Config.CLIENT_NAME + " FROM " + Config.TABLE_NAME;
							Set<String> onlineUids = new HashSet<>();
							for(Integer uid : Server.clientHolder.keySet()) {
								onlineUids.add(String.valueOf(uid));
							}
							try {
								java.sql.Statement stmt = Server.connection.createStatement();
								ResultSet rs = stmt.executeQuery(sql);
								while(rs.next()) {
									int uid = rs.getInt(1);
									if(uid != request.getClientId()) {
										String uname = rs.getString(2);
										String status = onlineUids.contains(String.valueOf(uid)) ? " (Online)" : "";
										onlineUsers += uid + ":" + uname + status + "\n";
									}
								}
								rs.close();
								stmt.close();
							} catch (SQLException e) {
								e.printStackTrace(new PrintWriter(Config.errors));
								LogFileWriter.Log(Config.errors.toString());
							}
							if(onlineUsers.isEmpty()) {
								response = new Response(Response.Type.ONLINE_USERS.ordinal(), false, "No other users online");
							} else {
								response = new Response(Response.Type.ONLINE_USERS.ordinal(), true, onlineUsers);
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 19:
							// FORGOT_PASSWORD - update password without old password
							String[] fpParts = request.getContents().split("#", 2);
							if(fpParts.length == 2) {
								String fpUsername = fpParts[0];
								String fpNewPwd = fpParts[1];
								query = "SELECT " + Config.CLIENT_ID + " FROM " + Config.TABLE_NAME + " WHERE " + Config.CLIENT_NAME + "=?";
								try {
									preparedStatement = Server.connection.prepareStatement(query);
									preparedStatement.setString(1, fpUsername);
									resultSet = preparedStatement.executeQuery();
									if(resultSet.isBeforeFirst()) {
										query = "UPDATE " + Config.TABLE_NAME + " SET " + Config.CLIENT_PWD + "=? WHERE " + Config.CLIENT_NAME + "=?";
										preparedStatement = Server.connection.prepareStatement(query);
										preparedStatement.setString(1, fpNewPwd);
										preparedStatement.setString(2, fpUsername);
										int updated = preparedStatement.executeUpdate();
										if(updated > 0) {
											response = new Response(Response.Type.FORGOT_PASSWORD.ordinal(), true, "Password updated successfully");
										} else {
											response = new Response(Response.Type.FORGOT_PASSWORD.ordinal(), false, "Failed to update password");
										}
									} else {
										response = new Response(Response.Type.FORGOT_PASSWORD.ordinal(), false, "Username not found");
									}
								} catch(SQLException e) {
									e.printStackTrace(new PrintWriter(Config.errors));
									LogFileWriter.Log(Config.errors.toString());
									response = new Response(Response.Type.FORGOT_PASSWORD.ordinal(), false, "Server error");
								}
							} else {
								response = new Response(Response.Type.FORGOT_PASSWORD.ordinal(), false, "Invalid request format");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 20:
							// LOAD_MESSAGES - get 90 day old PMs between two users
							int otherId = Integer.parseInt(request.getContents().trim());
							int myId = request.getClientId();
							String history = "";
							try {
								query = "SELECT " + Config.PM_SENDER_ID + "," + Config.PM_MESSAGE + ","
									+ "DATE_FORMAT(" + Config.PM_TIMESTAMP + ",'%Y-%m-%d %H:%i')"
									+ "," + Config.PM_ID + "," + Config.PM_STATUS
									+ " FROM " + Config.PM_TABLE_NAME
									+ " WHERE ((" + Config.PM_SENDER_ID + "=? AND " + Config.PM_RECEIVER_ID + "=?)"
									+ " OR (" + Config.PM_SENDER_ID + "=? AND " + Config.PM_RECEIVER_ID + "=?))"
									+ " AND " + Config.PM_TIMESTAMP + " >= DATE_SUB(NOW(), INTERVAL 90 DAY)"
									+ " AND (" + Config.PM_DELETED + " IS NULL OR " + Config.PM_DELETED + "=0)"
									+ " ORDER BY " + Config.PM_TIMESTAMP + " ASC";
								preparedStatement = Server.connection.prepareStatement(query);
								preparedStatement.setInt(1, myId);
								preparedStatement.setInt(2, otherId);
								preparedStatement.setInt(3, otherId);
								preparedStatement.setInt(4, myId);
								resultSet = preparedStatement.executeQuery();
								while(resultSet.next()) {
									int sid = resultSet.getInt(1);
									String sName = Server.getClientNameFromId(sid);
									String pmText = resultSet.getString(2);
									String ts = resultSet.getString(3);
									int pmId = resultSet.getInt(4);
									int pmStatus = resultSet.getInt(5);
									history += sid + "|" + sName + "|" + pmText + "|" + ts + "|" + pmId + "|" + pmStatus + "\n";
								}
							} catch(SQLException e) {
								e.printStackTrace(new PrintWriter(Config.errors));
								LogFileWriter.Log(Config.errors.toString());
							}
							if(history.isEmpty()) {
								response = new Response(Response.Type.MESSAGE_HISTORY.ordinal(), false, "No messages found");
							} else {
								response = new Response(Response.Type.MESSAGE_HISTORY.ordinal(), true, history);
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 21:
							// SEND_READ_RECEIPT - update DB status to read and forward to message sender
							String[] rrParts = request.getContents().split("#", 2);
							if(rrParts.length == 2) {
								try {
									int origSenderId = Integer.parseInt(rrParts[0].trim());
									int pmId = Integer.parseInt(rrParts[1].trim());
									// update DB status to read (2)
									try {
										query = "UPDATE " + Config.PM_TABLE_NAME + " SET " + Config.PM_STATUS + "=2 WHERE " + Config.PM_ID + "=" + pmId;
										preparedStatement = Server.connection.prepareStatement(query);
										preparedStatement.executeUpdate();
									} catch(SQLException e) {
										e.printStackTrace(new PrintWriter(Config.errors));
										LogFileWriter.Log(Config.errors.toString());
									}
									ClientThread origSenderCT = Server.clientHolder.get(origSenderId);
									if(origSenderCT != null) {
										Response rrRes = new Response(Response.Type.READ_RECEIPT.ordinal(), true, String.valueOf(pmId));
										Server.responseMakerQueue.add(new ResponseHolder(rrRes, origSenderCT.objectOutputStream));
									}
								} catch(NumberFormatException e) {
									e.printStackTrace(new PrintWriter(Config.errors));
									LogFileWriter.Log(Config.errors.toString());
								}
							}
							break;
					case 22:
							// FILE_MSG - send a file in a private message
							// contents: targetName||fileName||fileSize||mimeType||base64Data
							Message.println("FILE_MSG received from " + request.getClientId() + ": content length=" + request.getContents().length());
							try {
								String[] fileParts = request.getContents().split("\\|\\|", 5);
								Message.println("  fileParts.length=" + fileParts.length);
								if(fileParts.length >= 1) Message.println("  target=" + fileParts[0]);
								if(fileParts.length == 5) {
									String fileTarget = fileParts[0].trim();
									String fileName = fileParts[1].trim();
									long fileSize = Long.parseLong(fileParts[2].trim());
									String mimeType = fileParts[3].trim();
									String base64Data = fileParts[4].trim();
									int fileRecvId = -1;
									try {
										String q = "SELECT " + Config.CLIENT_ID + " FROM " + Config.TABLE_NAME + " WHERE " + Config.CLIENT_NAME + "=?";
										java.sql.PreparedStatement ps = Server.connection.prepareStatement(q);
										ps.setString(1, fileTarget);
										java.sql.ResultSet rs = ps.executeQuery();
										if(rs.next()) fileRecvId = rs.getInt(1);
										rs.close(); ps.close();
									} catch(SQLException e) {
										e.printStackTrace(new PrintWriter(Config.errors));
										LogFileWriter.Log(Config.errors.toString());
									}
									if(fileRecvId != -1 && fileRecvId != request.getClientId()) {
										byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);
										int fileId = 0;
										try {
											String q = "INSERT INTO " + Config.FILE_TABLE_NAME
												+ "(" + Config.FILE_SENDER_ID + "," + Config.FILE_RECEIVER_ID + ","
												+ Config.FILE_NAME + "," + Config.FILE_SIZE + "," + Config.FILE_MIME
												+ "," + Config.FILE_STORED_PATH + ")"
												+ " VALUES(?,?,?,?,?,?)";
											java.sql.PreparedStatement ps = Server.connection.prepareStatement(q, java.sql.Statement.RETURN_GENERATED_KEYS);
											String storedPath = Config.FILE_UPLOAD_DIR + "/" + System.currentTimeMillis() + "_" + fileName;
											ps.setInt(1, request.getClientId());
											ps.setInt(2, fileRecvId);
											ps.setString(3, fileName);
											ps.setLong(4, fileSize);
											ps.setString(5, mimeType);
											ps.setString(6, storedPath);
											ps.executeUpdate();
											java.sql.ResultSet gk = ps.getGeneratedKeys();
											if(gk.next()) fileId = gk.getInt(1);
											gk.close(); ps.close();
											// write file to disk
											java.io.FileOutputStream fos = new java.io.FileOutputStream(storedPath);
											fos.write(fileBytes);
											fos.close();
										} catch(Exception e) {
											e.printStackTrace(new PrintWriter(Config.errors));
											LogFileWriter.Log(Config.errors.toString());
										}
										// store in private_messages for history
										String pmText = "__file__:" + fileId + ":" + fileName;
										try {
											String q = "INSERT INTO " + Config.PM_TABLE_NAME
												+ "(" + Config.PM_SENDER_ID + "," + Config.PM_RECEIVER_ID + ","
												+ Config.PM_MESSAGE + "," + Config.PM_STATUS + ")"
												+ " VALUES(?,?,?,0)";
											java.sql.PreparedStatement ps = Server.connection.prepareStatement(q);
											ps.setInt(1, request.getClientId());
											ps.setInt(2, fileRecvId);
											ps.setString(3, pmText);
											ps.executeUpdate();
											ps.close();
										} catch(SQLException e) {
											e.printStackTrace(new PrintWriter(Config.errors));
											LogFileWriter.Log(Config.errors.toString());
										}
										// forward to recipient
										String senderName = Server.getClientNameFromId(request.getClientId());
										Message.println("  senderName=" + senderName + " recvId=" + fileRecvId);
										ClientThread recvCT = Server.clientHolder.get(fileRecvId);
										if(recvCT != null) {
											String fwdContent = senderName + "||" + fileId + "||" + fileName + "||" + fileSize + "||" + mimeType;
											Message.println("  forwarding file notification to " + fileRecvId + " fileId=" + fileId);
											Response fwdRes = new Response(Response.Type.FILE_MSG.ordinal(), true, fwdContent);
											Server.responseMakerQueue.add(new ResponseHolder(fwdRes, recvCT.objectOutputStream));
										} else {
											Message.println("  WARNING: recipient " + fileRecvId + " is offline (recvCT is null)");
										}
										// echo back to sender
										ClientThread sendCT = Server.clientHolder.get(request.getClientId());
										if(sendCT != null) {
											String echoContent = "__file_echo__ " + fileTarget + " " + fileId + " " + fileName;
											Message.println("  sending echo to " + request.getClientId() + ": fileId=" + fileId);
											Response echoRes = new Response(Response.Type.FILE_MSG.ordinal(), true, echoContent);
											Server.responseMakerQueue.add(new ResponseHolder(echoRes, sendCT.objectOutputStream));
										} else {
											Message.println("  WARNING: sendCT is null for client " + request.getClientId());
										}
									}
								}
							} catch(Exception e) {
								e.printStackTrace(new PrintWriter(Config.errors));
								LogFileWriter.Log(Config.errors.toString());
							}
							break;
					case 23:
							// GET_FILE - retrieve file data by fileId
							int getFileId = Integer.parseInt(request.getContents().trim());
							try {
								String q = "SELECT " + Config.FILE_NAME + "," + Config.FILE_SIZE + ","
									+ Config.FILE_MIME + "," + Config.FILE_STORED_PATH + " FROM "
									+ Config.FILE_TABLE_NAME + " WHERE " + Config.FILE_ID + "=?";
								java.sql.PreparedStatement ps = Server.connection.prepareStatement(q);
								ps.setInt(1, getFileId);
								java.sql.ResultSet rs = ps.executeQuery();
								if(rs.next()) {
									String fName = rs.getString(1);
									long fSize = rs.getLong(2);
									String fMime = rs.getString(3);
									String fPath = rs.getString(4);
									rs.close(); ps.close();
									java.io.File f = new java.io.File(fPath);
									if(f.exists()) {
										byte[] fBytes = new byte[(int)f.length()];
										java.io.FileInputStream fis = new java.io.FileInputStream(f);
										fis.read(fBytes);
										fis.close();
										String b64 = java.util.Base64.getEncoder().encodeToString(fBytes);
										String respContent = getFileId + "||" + fName + "||" + fSize + "||" + fMime + "||" + b64;
										response = new Response(Response.Type.FILE_DATA.ordinal(), true, respContent);
									} else {
										response = new Response(Response.Type.FILE_DATA.ordinal(), false, "File not found");
									}
								} else {
									response = new Response(Response.Type.FILE_DATA.ordinal(), false, "File not found");
								}
							} catch(Exception e) {
								e.printStackTrace(new PrintWriter(Config.errors));
								LogFileWriter.Log(Config.errors.toString());
								response = new Response(Response.Type.FILE_DATA.ordinal(), false, "File not found");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 24:
							// DELETE_MESSAGES - soft-delete with 5-second undo window
							// contents: "me:1,2,3" or "everyone:1,2,3"
							String deleteContents = request.getContents();
							String deleteMode = "me";
							String idsPart = deleteContents;
							if(deleteContents.startsWith("me:") || deleteContents.startsWith("everyone:")) {
								int colonIdx = deleteContents.indexOf(":");
								deleteMode = deleteContents.substring(0, colonIdx);
								idsPart = deleteContents.substring(colonIdx + 1);
							}
							String[] pmIdStrs = idsPart.split(",");
							java.util.ArrayList<Integer> pmIds = new java.util.ArrayList<>();
							for(String s : pmIdStrs) {
								try { pmIds.add(Integer.parseInt(s.trim())); } catch(NumberFormatException e) {}
							}
							if(!pmIds.isEmpty()) {
								StringBuilder setDeleted = new StringBuilder("UPDATE " + Config.PM_TABLE_NAME
									+ " SET " + Config.PM_DELETED + "=1 WHERE (");
								for(int i = 0; i < pmIds.size(); i++) {
									if(i > 0) setDeleted.append(" OR ");
									setDeleted.append(Config.PM_ID + "=" + pmIds.get(i));
								}
								setDeleted.append(")");
								if(deleteMode.equals("me")) {
									setDeleted.append(" AND " + Config.PM_SENDER_ID + "=" + request.getClientId());
								}
								try {
									java.sql.Statement st = Server.connection.createStatement();
									st.executeUpdate(setDeleted.toString());
									st.close();
								} catch(SQLException e) {
									e.printStackTrace(new PrintWriter(Config.errors));
									LogFileWriter.Log(Config.errors.toString());
								}
								// notify other participant when deleting for everyone
								if(deleteMode.equals("everyone")) {
									try {
										StringBuilder peerQ = new StringBuilder("SELECT DISTINCT sender_id, receiver_id FROM "
											+ Config.PM_TABLE_NAME + " WHERE (");
										for(int i = 0; i < pmIds.size(); i++) {
											if(i > 0) peerQ.append(" OR ");
											peerQ.append(Config.PM_ID + "=" + pmIds.get(i));
										}
										peerQ.append(")");
										java.sql.Statement peerSt = Server.connection.createStatement();
										java.sql.ResultSet peerRs = peerSt.executeQuery(peerQ.toString());
										java.util.HashSet<Integer> notified = new java.util.HashSet<>();
										while(peerRs.next()) {
											int sId = peerRs.getInt(1);
											int rId = peerRs.getInt(2);
											int peerId = (sId == request.getClientId()) ? rId : sId;
											if(peerId == request.getClientId() || notified.contains(peerId)) continue;
											notified.add(peerId);
											ClientThread peerCT = Server.clientHolder.get(peerId);
											if(peerCT != null) {
												Response peerNotif = new Response(Response.Type.DELETE_CONFIRM.ordinal(),
													true, "peer_deleted:" + idsPart);
												Server.responseMakerQueue.add(new ResponseHolder(peerNotif, peerCT.objectOutputStream));
											}
										}
										peerRs.close();
										peerSt.close();
									} catch(SQLException e) {
										e.printStackTrace(new PrintWriter(Config.errors));
										LogFileWriter.Log(Config.errors.toString());
									}
								}
								// schedule permanent finalization in 5 seconds (for both modes)
								final java.util.ArrayList<Integer> fPmIds = new java.util.ArrayList<>(pmIds);
								ScheduledFuture<?> future = Server.deletionScheduler.schedule(() -> {
									StringBuilder del = new StringBuilder("DELETE FROM " + Config.PM_TABLE_NAME + " WHERE (");
									for(int i = 0; i < fPmIds.size(); i++) {
										if(i > 0) del.append(" OR ");
										del.append(Config.PM_ID + "=" + fPmIds.get(i));
									}
									del.append(")");
									try {
										java.sql.Statement st = Server.connection.createStatement();
										st.executeUpdate(del.toString());
										st.close();
									} catch(SQLException e) {
										e.printStackTrace(new PrintWriter(Config.errors));
										LogFileWriter.Log(Config.errors.toString());
									}
									for(Integer pid : fPmIds) Server.pendingDeletions.remove(pid);
								}, 5, TimeUnit.SECONDS);
								for(Integer pid : pmIds) {
									Server.pendingDeletions.put(pid, future);
								}
								response = new Response(Response.Type.DELETE_CONFIRM.ordinal(), true, "ok");
							} else {
								response = new Response(Response.Type.DELETE_CONFIRM.ordinal(), false, "no valid pmIds");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					case 25:
							// UNDO_DELETE - cancel pending deletion within 5s grace period
							// contents: comma-separated pmIds
							String[] undoStrs = request.getContents().split(",");
							java.util.ArrayList<Integer> undoIds = new java.util.ArrayList<>();
							for(String s : undoStrs) {
								try { undoIds.add(Integer.parseInt(s.trim())); } catch(NumberFormatException e) {}
							}
							boolean anyCancelled = false;
							for(int pid : undoIds) {
								ScheduledFuture<?> f = Server.pendingDeletions.remove(pid);
								if(f != null) {
									f.cancel(false);
									anyCancelled = true;
								}
							}
							if(anyCancelled) {
								// restore messages
								StringBuilder restore = new StringBuilder("UPDATE " + Config.PM_TABLE_NAME
									+ " SET " + Config.PM_DELETED + "=0 WHERE (");
								for(int i = 0; i < undoIds.size(); i++) {
									if(i > 0) restore.append(" OR ");
									restore.append(Config.PM_ID + "=" + undoIds.get(i));
								}
								restore.append(")");
								try {
									java.sql.Statement st = Server.connection.createStatement();
									st.executeUpdate(restore.toString());
									st.close();
								} catch(SQLException e) {
									e.printStackTrace(new PrintWriter(Config.errors));
									LogFileWriter.Log(Config.errors.toString());
								}
								response = new Response(Response.Type.DELETE_CONFIRM.ordinal(), true, "undo_ok");
							} else {
								response = new Response(Response.Type.DELETE_CONFIRM.ordinal(), true, "already_finalized");
							}
							Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
							break;
					default:
						Message.println("Invalid");
					}
				}
				catch( Exception e)
				{
					e.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
				}
			}
		}

	public static void logout(ClientThread clientThread, Request request, int temp) {
		Response response = new Response( Response.Type.LOGOUT.ordinal() , true, "Logout Succesfully");
		Server.responseMakerQueue.add(new ResponseHolder(response, clientThread.objectOutputStream));
		if(temp == 1) {
			if(request.getRoomId() != -1)
				Server.roomsHolder.get(request.getRoomId()).remove(request.getClientId());
			Server.clientHolder.remove(request.getClientId());
		}
		
		if(request.getClientId() != -1) {
			String name = Server.getClientNameFromId(request.getClientId());
			if(name != null) {
				Message.println( name + " logged out sucessfully");
			} else {
				Message.println( "User " + request.getClientId() + " logged out sucessfully");
			}
		} else {
			Message.println( request.getClientId() + " logged out sucessfully");
		}
	}
}


class ResponseMaker  extends Thread{
	
	public void run()
	{
		ResponseHolder responseHolder = null;
		java.util.HashSet<ObjectOutputStream> brokenStreams = new java.util.HashSet<>();
		
		while(true)
		{
			try
			{
				responseHolder = Server.responseMakerQueue.take();
				if(brokenStreams.contains(responseHolder.objectOutputStream)) {
					continue;
				}
				responseHolder.objectOutputStream.writeObject(responseHolder.response);
				responseHolder.objectOutputStream.flush();
			}
			catch (java.net.SocketException e) {
				if(responseHolder != null) {
					brokenStreams.add(responseHolder.objectOutputStream);
				}
				Config.errors.getBuffer().setLength(0);
				LogFileWriter.Log("Client disconnected (broken pipe)");
			}
			catch (Throwable e) {
				e.printStackTrace(new PrintWriter(Config.errors));
				LogFileWriter.Log(Config.errors.toString());
			}
		}
	}
}


class MessageHandler  extends Thread{
	
	public void run()
	{	
		Request request;
		ResultSet resultSet = null;
		java.sql.PreparedStatement preparedStatement = null;
		String query = null;
		String msg = "";
		String sender = "";
		String reciever = "";
		String data = "";
		int recieverId = -1;
		while(true)
		{
			data = "";
			try
			{
				request = Server.messagequeue.take();
				
				query = "SELECT " +Config.CLIENT_NAME+ " from "+ Config.TABLE_NAME + " WHERE " + Config.CLIENT_ID+"=?";
				preparedStatement = Server.connection.prepareStatement(query);
				preparedStatement.setInt(1,request.getClientId());
				resultSet = preparedStatement.executeQuery();
				
				if(resultSet.next())
					sender = resultSet.getString(1);

				boolean isGroupMsg = request.getId() == Request.Type.GROUP_MSG.ordinal();
				Set<Integer> set = isGroupMsg
					? Server.groupMembers.get(request.getRoomId())
					: Server.roomsHolder.get(request.getRoomId());
				
				//update the user message count
				
				Server.messagesTrackHashmap.put(request.getClientId(), Server.messagesTrackHashmap.get(request.getClientId())+1);
				
				boolean personalMessage = !isGroupMsg && request.getContents().indexOf("@") != -1 ? true : false;
				if(personalMessage)
				{
					reciever = request.getContents().substring(request.getContents().indexOf("@")+1,request.getContents().indexOf(" "));
					query = "SELECT " +Config.CLIENT_ID+ " from "+ Config.TABLE_NAME + " WHERE " + Config.CLIENT_NAME+"=?";
					preparedStatement = Server.connection.prepareStatement(query);
					preparedStatement.setString(1,reciever);
					resultSet = preparedStatement.executeQuery();
					if(!resultSet.isBeforeFirst()) {
						recieverId = -1;	
					}
					else {
						resultSet.next();
						recieverId = resultSet.getInt(1);
					}
					if(recieverId != -1)
					{
						if(recieverId == request.getClientId())
							continue;

						// store message in database first, get generated ID
						String rawContent = request.getContents().substring(request.getContents().indexOf(" ")).trim();
						String tempId = "";
						String actualMsg = rawContent;
						if(rawContent.contains("##")) {
							tempId = rawContent.substring(0, rawContent.indexOf("##"));
							actualMsg = rawContent.substring(rawContent.indexOf("##") + 2);
						}
						int dbId = 0;
						try {
							query = "INSERT INTO " + Config.PM_TABLE_NAME
								+ "(" + Config.PM_SENDER_ID + "," + Config.PM_RECEIVER_ID + "," + Config.PM_MESSAGE + ","
								+ Config.PM_STATUS + ")"
								+ " VALUES(?,?,?,0)";
							preparedStatement = Server.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
							preparedStatement.setInt(1, request.getClientId());
							preparedStatement.setInt(2, recieverId);
							preparedStatement.setString(3, actualMsg);
							preparedStatement.executeUpdate();
							ResultSet genKeys = preparedStatement.getGeneratedKeys();
							if(genKeys.next()) {
								dbId = genKeys.getInt(1);
							}
						} catch(SQLException e) {
							e.printStackTrace(new PrintWriter(Config.errors));
							LogFileWriter.Log(Config.errors.toString());
						}

						// forward to recipient
						String tsSuffix = "||" + System.currentTimeMillis();
						ClientThread ct =  Server.clientHolder.get(recieverId);
						if(ct != null) {
							ObjectOutputStream oos = ct.objectOutputStream;
							msg = sender + " " + dbId + "##" + actualMsg + tsSuffix;
							Response res = new Response(Response.Type.P_MSG.ordinal(), true, msg);
							Server.responseMakerQueue.add(new ResponseHolder(res, oos));
						}

						// echo back to sender with dbId
						ClientThread senderCT = Server.clientHolder.get(request.getClientId());
						if(senderCT != null) {
							ObjectOutputStream senderOOS = senderCT.objectOutputStream;
							String echoMsg = "__pm_echo__ " + reciever + " " + dbId + " " + rawContent + tsSuffix;
							Response echoRes = new Response(Response.Type.P_MSG.ordinal(), true, echoMsg);
							Server.responseMakerQueue.add(new ResponseHolder(echoRes, senderOOS));
						}

						// update status to delivered (1) if recipient was online
						if(ct != null && dbId > 0) {
							try {
								query = "UPDATE " + Config.PM_TABLE_NAME + " SET " + Config.PM_STATUS + "=1 WHERE " + Config.PM_ID + "=" + dbId;
								preparedStatement = Server.connection.prepareStatement(query);
								preparedStatement.executeUpdate();
							} catch(SQLException e) {
								e.printStackTrace(new PrintWriter(Config.errors));
								LogFileWriter.Log(Config.errors.toString());
							}
						}

						continue;
					}
					else
					{
						// user name wrong
						ClientThread ct =  Server.clientHolder.get(request.getClientId()); //gives the client thread object
						ObjectOutputStream oos = ct.objectOutputStream;
						Response res = null;
						msg = "Wrong username " + reciever;
						res = new Response(Response.Type.STATUS_MSG.ordinal(),true,msg);
						Server.responseMakerQueue.add(new ResponseHolder(res, oos));
						continue;
						
					}
				}
				
				Iterator<Integer> iterator = set.iterator();
				while(iterator.hasNext()) {
					int id = iterator.next();
					//take id and check if its not sender and then create response
					//if(id != request.getClientId()) {
						try {
							if(request.getContents().equals("sv_exit") || request.getContents().equals("sv_logout"))
							{
								msg = sender + " has left the chat\n";
								if(id != request.getClientId())
								{
									ClientThread ct =  Server.clientHolder.get(id); //gives the client thread object
									try
									{
										ObjectOutputStream oos = ct.objectOutputStream;
										Response res = new Response(Response.Type.STATUS_MSG.ordinal(),true,msg);
										Server.responseMakerQueue.add(new ResponseHolder(res, oos));
									}
									catch( Exception e )
									{
										e.printStackTrace(new PrintWriter(Config.errors));
										LogFileWriter.Log(Config.errors.toString());
									}
								}
								else
								{
									
									ClientThread ct =  Server.clientHolder.get(id); //gives the client thread object
									try
									{
										if(request.getContents().equals("sv_exit")) {
											ObjectOutputStream oos = ct.objectOutputStream;
											Response res = new Response(Response.Type.STATUS_MSG.ordinal(),true,"sv_exit_successful");
											Server.responseMakerQueue.add(new ResponseHolder(res, oos));
										}
									}
									catch( Exception e )
									{
										e.printStackTrace(new PrintWriter(Config.errors));
										LogFileWriter.Log(Config.errors.toString());
									}
									finally
									{
										if( request.getContents().equals("sv_logout") )
										{
											RequestAnalyser.logout(ct,request,0);
										}
									}
								}
							}
							else if(request.getContents().equals("sv_showusers")) {
								//show the current online users in that room
								if(id != request.getClientId())
									data += Server.getClientNameFromId(id) + ",";
							}
							else
							{
								if(((request.getId() == Request.Type.STATUS_MSG.ordinal()) || id != request.getClientId()) || request.getIsConsole()) {
									if(request.getIsConsole()) {
										msg = sender + " " + request.getContents();
									}
									else {
										if(id == request.getClientId()) 
											msg = sender + " " + request.getContents() + " Active Users: "+ set.size();
										else	
											msg = sender + " " + request.getContents();
									}
									ClientThread ct =  Server.clientHolder.get(id); //gives the client thread object
									ObjectOutputStream oos = ct.objectOutputStream;
									Response res = null;
									if(request.getId() == Request.Type.STATUS_MSG.ordinal())
										res = new Response(Response.Type.STATUS_MSG.ordinal(),true,msg);
									else if(request.getId() == Request.Type.GROUP_MSG.ordinal())
										res = new Response(Response.Type.GROUP_MSG.ordinal(),true,msg);
									else 
										res = new Response(Response.Type.MSG.ordinal(),true,msg);
									Server.responseMakerQueue.add(new ResponseHolder(res, oos));
								}
								
							}
							
						}
						catch(Exception e) {
							e.printStackTrace(new PrintWriter(Config.errors));
							LogFileWriter.Log(Config.errors.toString());
						}
				}
				if(request.getContents().equals("sv_exit")) {
					Set<Integer> set1 = Server.roomsHolder.get(request.getRoomId());
					set1.remove(request.getClientId());
				}
				else if(request.getContents().equals("sv_logout")) {
					Server.roomsHolder.get(request.getRoomId()).remove(request.getClientId());
					Server.clientHolder.remove(request.getClientId());
				}
				else if(request.getContents().equals("sv_showusers")) {
					ClientThread ct = Server.clientHolder.get(request.getClientId());
					ObjectOutputStream oos = ct.objectOutputStream;
					Response response = new Response(Response.Type.GEN.ordinal(),true,data);
					Server.responseMakerQueue.add(new ResponseHolder(response, oos));
				}
			}
			catch (Exception e) {
				e.printStackTrace(new PrintWriter(Config.errors));
				LogFileWriter.Log(Config.errors.toString());
			}
		}
	}
}

class MessagesTrackComparator implements Comparator<MessageTrackObject> {

	@Override
	public int compare(MessageTrackObject o1, MessageTrackObject o2) {
		// TODO Auto-generated method stub
		if(o1.getCount() > o2.getCount()) {
			return -1;
		}
		else if(o1.getCount() < o2.getCount())
			return 1;
		else
			return 0;
	}
}

public class Server {
	Scanner scanner = new Scanner(System.in);
	int choice;
	ServerSocket serverSocket;
	Socket socket;
	int port = -1;
	Request request;
	Response response;
	static BlockingQueue<ClientRequest> requestqueue;
	static BlockingQueue<ResponseHolder> responseMakerQueue;
	static BlockingQueue<Request> messagequeue;
	static RequestAnalyser requestAnalyser;
	static ResponseMaker responseMaker;
	static MessageHandler messageHandler;
	static Connection connection = null;
	static ArrayList<Integer> listOfRooms;
	static HashMap<Integer,Set<Integer>> roomsHolder;
	static HashMap<Integer,String> roomsMapping;
	static HashMap<Integer,ClientThread> clientHolder; //hold the clients with their unique id
	static PriorityQueue<MessageTrackObject> messagesTrackQueue; //holds the log of which user sends how many messages
	static HashMap<Integer, Integer> messagesTrackHashmap;
	static ServerOperations serverOperations;
	static int roomIdGenerator = 0;

	static HashMap<Integer, String> groupMapping;
	static HashMap<Integer, Set<Integer>> groupMembers;
	static HashMap<Integer, Integer> groupAdmins;
	static HashMap<Integer, ArrayList<InviteInfo>> userPendingInvites;
	static ArrayList<Integer> listOfGroups;
	static int groupIdGenerator = 0;
	static ScheduledExecutorService deletionScheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
	static ConcurrentHashMap<Integer, ScheduledFuture<?>> pendingDeletions = new ConcurrentHashMap<>();
	public Server(int port)
	{
		this.port = port;
		requestqueue = new LinkedBlockingQueue<>();
		messagequeue = new LinkedBlockingQueue<>();
		responseMakerQueue = new LinkedBlockingQueue<>();
		requestAnalyser = new RequestAnalyser();
		requestAnalyser.start();
		responseMaker = new ResponseMaker();
		responseMaker.start();
		messageHandler = new MessageHandler();
		messageHandler.start();
		roomsHolder = new HashMap<>(); // for mapping the room id's and client id's in particular room
		roomsMapping = new HashMap<>();
		listOfRooms = new ArrayList<>();
		clientHolder = new HashMap<>();
		messagesTrackHashmap = new HashMap<>();
		messagesTrackQueue = new PriorityQueue<MessageTrackObject>(100,new MessagesTrackComparator());
		serverOperations = new ServerOperations();
		serverOperations.start();

		groupMapping = new HashMap<>();
		groupMembers = new HashMap<>();
		groupAdmins = new HashMap<>();
		userPendingInvites = new HashMap<>();
		listOfGroups = new ArrayList<>();
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			connection = DriverManager.getConnection(Config.DATABASE_URL+"/"+Config.DATABASE_NAME,Config.USER_NAME,Config.USER_PWD);
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		} catch (InstantiationException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		} catch (IllegalAccessException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
		java.io.File uploadDir = new java.io.File(Config.FILE_UPLOAD_DIR);
		if(!uploadDir.exists()) uploadDir.mkdirs();
	}
	
	@Override
	protected void finalize() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}
	
	public static int getRoomId() {
		return roomIdGenerator++;
	}
	
	public static int getGroupId() {
		return groupIdGenerator++;
	}
	
	public static void shutdown() { 
		Message.println("Server stopped");
		System.exit(1);
	}
	
	public static String getClientNameFromId(int id) {
		try {
			Connection connection = DriverManager.getConnection(Config.DATABASE_URL+"/"+Config.DATABASE_NAME,Config.USER_NAME,Config.USER_PWD);
			String sql = "select client_name from users where client_id = ?";
			java.sql.PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setInt(1, id);
			ResultSet resultSet = preparedStatement.executeQuery();
			if(resultSet.next()) {
				return resultSet.getString(1);
			}
			return null;
		} catch (SQLException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
			return null;
		}
	}
	
	public static Integer getKey(String value) {
        for (Entry<Integer, String> entry : roomsMapping.entrySet()) {
            if (entry.getValue().equals(value)) {
            	return entry.getKey();
            }
        }
        return -1;
	}
	
	public void connect() {
		try {
			serverSocket = new ServerSocket(port);
			while(true)
			{
				socket = serverSocket.accept();
				new ClientThread(socket).start();

			}
		} catch (IOException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

}