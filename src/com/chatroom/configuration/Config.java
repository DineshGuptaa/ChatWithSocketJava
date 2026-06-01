package com.chatroom.configuration;

import java.awt.Color;
import java.io.StringWriter;

public class Config {
	public static String USER_NAME  = "";
	public static String USER_PWD = "";
	public static String DATABASE_HOST = "";
	public static String DATABASE_URL = "jdbc:mysql://";
	public static String DATABASE_PORT = ":3306";
	public static final String DATABASE_NAME = "chatroom";
	public static final String TABLE_NAME = "users";
	public static final String CLIENT_ID = "client_id";
	public static final String CLIENT_NAME = "client_name";
	public static final String CLIENT_PWD = "client_pwd";
	public static final String DISPLAY_NAME = "display_name";
	public static final String GENDER = "gender";
	public static final String PROFILE_AVATAR = "profile_avatar";
	public static final String GROUP_TABLE_NAME = "chat_groups";
	public static final String GROUP_ID = "group_id";
	public static final String GROUP_NAME = "group_name";
	public static final String CREATED_BY = "created_by";

	public static final String GROUP_MEMBERS_TABLE = "group_members";
	public static final String GM_GROUP_ID = "group_id";
	public static final String GM_MEMBER_ID = "member_id";
	public static final String GM_STATUS = "status";
	public static final String GM_INVITED_BY = "invited_by";

	public static final String INVITE_PENDING = "pending";
	public static final String INVITE_ACCEPTED = "accepted";
	public static final String INVITE_REJECTED = "rejected";

	public static final String PM_TABLE_NAME = "private_messages";
	public static final String PM_ID = "pm_id";
	public static final String PM_SENDER_ID = "sender_id";
	public static final String PM_RECEIVER_ID = "receiver_id";
	public static final String PM_MESSAGE = "message";
	public static final String PM_TIMESTAMP = "sent_at";
	public static final String PM_STATUS = "status";
	public static final String PM_DELETED = "deleted";

	public static final String FILE_TABLE_NAME = "file_messages";
	public static final String FILE_ID = "file_id";
	public static final String FILE_SENDER_ID = "sender_id";
	public static final String FILE_RECEIVER_ID = "receiver_id";
	public static final String FILE_NAME = "file_name";
	public static final String FILE_SIZE = "file_size";
	public static final String FILE_MIME = "mime_type";
	public static final String FILE_STORED_PATH = "stored_path";
	public static final String FILE_TIMESTAMP = "sent_at";
	public static final String FILE_UPLOAD_DIR = "uploads";

	public static final Color colorPrimary = new Color(7, 94, 84); // WhatsApp dark green
	public static final Color colorAccent = new Color(37, 211, 102); // WhatsApp bright green
	public static final Color colorLight = new Color(18, 140, 126); // WhatsApp medium green
	public static StringWriter errors = new StringWriter();
}
