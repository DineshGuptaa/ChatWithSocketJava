package com.chatroom.Database;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.chatroom.configuration.Config;
import com.chatroom.others.LogFileWriter;
import com.chatroom.others.Message;


public class createdb {
	public createdb() {
		Connection connection = null;
		java.sql.Statement statement= null;
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
			//creating database	
			connection = DriverManager.getConnection(Config.DATABASE_URL+Config.DATABASE_HOST+Config.DATABASE_PORT,Config.USER_NAME,Config.USER_PWD);
			String Query = "CREATE DATABASE IF NOT EXISTS "+ Config.DATABASE_NAME;
			statement = connection.createStatement();
			statement.executeUpdate(Query);
			//for execute multiple queries separate queries by semicolon
			connection = DriverManager.getConnection(Config.DATABASE_URL+Config.DATABASE_HOST+Config.DATABASE_PORT+'/'+Config.DATABASE_NAME+"?allowMultiQueries=true",Config.USER_NAME,Config.USER_PWD);
			String Queries = "CREATE TABLE IF NOT EXISTS " + Config.TABLE_NAME + "(" + Config.CLIENT_ID + " int auto_increment," + Config.CLIENT_NAME + " VARCHAR(50) not null, "+ Config.CLIENT_PWD + " VARCHAR(150), " +"primary key(" +Config.CLIENT_ID+ "))";
			
			statement = connection.createStatement();
			statement.executeUpdate(Queries);

			try {
				statement.executeUpdate("ALTER TABLE " + Config.TABLE_NAME + " ADD COLUMN " + Config.DISPLAY_NAME + " VARCHAR(100) DEFAULT ''");
			} catch(Exception e) {}
			try {
				statement.executeUpdate("ALTER TABLE " + Config.TABLE_NAME + " ADD COLUMN " + Config.GENDER + " VARCHAR(10) DEFAULT 'Other'");
			} catch(Exception e) {}
			try {
				statement.executeUpdate("ALTER TABLE " + Config.TABLE_NAME + " ADD COLUMN " + Config.PROFILE_AVATAR + " INT DEFAULT 0");
			} catch(Exception e) {}

			String groupTable = "CREATE TABLE IF NOT EXISTS " + Config.GROUP_TABLE_NAME + "("
				+ Config.GROUP_ID + " int auto_increment,"
				+ Config.GROUP_NAME + " VARCHAR(100) not null,"
				+ Config.CREATED_BY + " int not null,"
				+ "primary key(" + Config.GROUP_ID + "))";
			statement.executeUpdate(groupTable);

			String groupMembersTable = "CREATE TABLE IF NOT EXISTS " + Config.GROUP_MEMBERS_TABLE + "("
				+ Config.GM_GROUP_ID + " int,"
				+ Config.GM_MEMBER_ID + " int,"
				+ Config.GM_STATUS + " VARCHAR(20) default '" + Config.INVITE_PENDING + "',"
				+ Config.GM_INVITED_BY + " int,"
				+ "primary key(" + Config.GM_GROUP_ID + "," + Config.GM_MEMBER_ID + "))";
			statement.executeUpdate(groupMembersTable);
			
			String pmTable = "CREATE TABLE IF NOT EXISTS " + Config.PM_TABLE_NAME + "("
				+ Config.PM_ID + " int auto_increment,"
				+ Config.PM_SENDER_ID + " int not null,"
				+ Config.PM_RECEIVER_ID + " int not null,"
				+ Config.PM_MESSAGE + " TEXT,"
				+ Config.PM_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
				+ Config.PM_STATUS + " TINYINT DEFAULT 0,"
				+ Config.PM_DELETED + " TINYINT DEFAULT 0,"
				+ "primary key(" + Config.PM_ID + "))";
			statement.executeUpdate(pmTable);
			try {
				statement.executeUpdate("ALTER TABLE " + Config.PM_TABLE_NAME + " ADD COLUMN " + Config.PM_STATUS + " TINYINT DEFAULT 0");
			} catch(Exception e) {}
			try {
				statement.executeUpdate("ALTER TABLE " + Config.PM_TABLE_NAME + " ADD COLUMN " + Config.PM_DELETED + " TINYINT DEFAULT 0");
			} catch(Exception e) {}

			String fileTable = "CREATE TABLE IF NOT EXISTS " + Config.FILE_TABLE_NAME + "("
				+ Config.FILE_ID + " int auto_increment,"
				+ Config.FILE_SENDER_ID + " int not null,"
				+ Config.FILE_RECEIVER_ID + " int not null,"
				+ Config.FILE_NAME + " VARCHAR(255) not null,"
				+ Config.FILE_SIZE + " BIGINT not null,"
				+ Config.FILE_MIME + " VARCHAR(100) not null,"
				+ Config.FILE_STORED_PATH + " VARCHAR(500) not null,"
				+ Config.FILE_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
				+ "primary key(" + Config.FILE_ID + "))";
			statement.executeUpdate(fileTable);

		} catch (ClassNotFoundException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		} catch (SQLException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		} catch (InstantiationException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		} catch (IllegalAccessException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
		finally {
			try {
				connection.close(); //close the database connection
			} catch (SQLException e) {
				e.printStackTrace(new PrintWriter(Config.errors));
				LogFileWriter.Log(Config.errors.toString());
			}
		}
	}
}
