package com.chatroom.models;

import java.io.Serializable;

public class InviteInfo implements Serializable {
	private int groupId;
	private String groupName;
	private int invitedBy;
	private String inviterName;

	public InviteInfo(int groupId, String groupName, int invitedBy, String inviterName) {
		this.groupId = groupId;
		this.groupName = groupName;
		this.invitedBy = invitedBy;
		this.inviterName = inviterName;
	}

	public int getGroupId() { return groupId; }
	public String getGroupName() { return groupName; }
	public int getInvitedBy() { return invitedBy; }
	public String getInviterName() { return inviterName; }
}
