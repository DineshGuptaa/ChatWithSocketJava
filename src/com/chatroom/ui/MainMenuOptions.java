package com.chatroom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.InsetsUIResource;

import com.chatroom.client.ClientModel;
import com.chatroom.configuration.Config;
import com.chatroom.models.Request;
import com.chatroom.models.Response;
import com.chatroom.others.LogFileWriter;
import com.chatroom.others.Message;

public class MainMenuOptions {
	private JLabel jLabel;
	private JLabel jLabelTitle;
	private JFrame jFrame;
	private JButton jBtnCreateRoom;
	private JButton jBtnJoinRoom;
	private JButton jBtnViewRooms;
	private JButton jBtnGroups;
	private JButton jBtnLogout;
	private JButton jBtnContacts;
	private JButton jBtnLogoutIcon;
	private BufferedImage iconLogo;
	private ClientModel clientModel;
	private Request request;
	private Response response;



	@SuppressWarnings("serial")
	public MainMenuOptions(ClientModel cm) throws IOException {
		clientModel = cm;
		jFrame = new JFrame("CHATROOM");
		
		iconLogo = ImageIO.read(this.getClass().getResource("/logo.png"));
		
		jFrame.setContentPane(new JPanel() {
			BufferedImage myImage = ImageIO.read(this.getClass().getResource("/background.png"));
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(myImage, 0, 0, this);
			}
		});
		
		jBtnCreateRoom = new JButton("CREATE ROOM");
		jBtnJoinRoom = new JButton("JOIN ROOM");
		jBtnViewRooms = new JButton("VIEW ROOMS");
		jBtnGroups = new JButton("MY GROUPS");
		jBtnContacts = new JButton("CONTACTS");
		jBtnLogout = new JButton("LOGOUT");
		
		jLabelTitle = new JLabel("WELCOME TO CHATROOM - A LIVE CHAT SOFTWARE");
		
		initializeAllWithProperties();
		checkForPendingInvites();

	}
	
	private void logOut() {
		try {
			request = new Request(Request.Type.LOGOUT.ordinal(),clientModel.getClientID(),clientModel.getRoomId(),"");
			ClientModel.objectOutputStream.writeObject(request);
			ClientModel.objectOutputStream.flush();
			response = (Response) ClientModel.objectInputStream.readObject();
			if( response.getSuccess())
			{
				clientModel.setRoomId(-1);
				clientModel.setClientID(-1);
				new SignInActivity(clientModel);
				jFrame.dispose();
			}
			else
			{
				JOptionPane.showMessageDialog(null, response.getContents(), null, JOptionPane.ERROR_MESSAGE);
			}
		}
		catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}
	
	private void ListeningEvents() {
		jBtnCreateRoom.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				displayAlertDialog(1);
			}
		});
		
		jBtnJoinRoom.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				displayAlertDialog(2);
			}
		});
		
		jBtnViewRooms.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					new ViewRoomsActivity(clientModel);
					jFrame.dispose();
				} catch (IOException e1) {
					e1.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
				}
				
			}
		});
		jBtnGroups.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					new GroupsListActivity(clientModel);
					jFrame.dispose();
				} catch (IOException e1) {
					e1.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
				}
			}
		});
		jBtnContacts.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					new ContactListActivity(clientModel);
					jFrame.dispose();
				} catch (IOException e1) {
					e1.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
				}
			}
		});
		jBtnLogoutIcon.addActionListener(e -> logOut());
		jBtnLogout.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					logOut();
				} catch (Exception e1) {
					e1.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
				}
			}
		});
	}
	
	private void initializeAllWithProperties() {
		//create room button
		jBtnCreateRoom.setPreferredSize(new Dimension(150,35));
		jBtnCreateRoom.setBackground(Config.colorAccent);
		jBtnCreateRoom.setForeground(Color.WHITE);
		jBtnCreateRoom.setBorder(new LineBorder(Config.colorAccent, 1));
		jBtnCreateRoom.setFocusPainted(false);
		jBtnCreateRoom.setFont(new Font("Arial", Font.BOLD, 12));
		jBtnCreateRoom.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		
		//join room button
		jBtnJoinRoom.setPreferredSize(new Dimension(150,35));
		jBtnJoinRoom.setBackground(Config.colorAccent);
		jBtnJoinRoom.setForeground(Color.WHITE);
		jBtnJoinRoom.setBorder(new LineBorder(Config.colorAccent, 1));
		jBtnJoinRoom.setFocusPainted(false);
		jBtnJoinRoom.setFont(new Font("Arial", Font.BOLD, 12));
		jBtnJoinRoom.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		
		//view all rooms button
		jBtnViewRooms.setPreferredSize(new Dimension(150,35));
		jBtnViewRooms.setBackground(Config.colorAccent);
		jBtnViewRooms.setForeground(Color.WHITE);
		jBtnViewRooms.setBorder(new LineBorder(Config.colorAccent, 1));
		jBtnViewRooms.setFocusPainted(false);
		jBtnViewRooms.setFont(new Font("Arial", Font.BOLD, 12));
		jBtnViewRooms.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		
		//groups button
		jBtnGroups.setPreferredSize(new Dimension(150,35));
		jBtnGroups.setBackground(new Color(52, 152, 219));
		jBtnGroups.setForeground(Color.WHITE);
		jBtnGroups.setBorder(new LineBorder(new Color(52, 152, 219), 1));
		jBtnGroups.setFocusPainted(false);
		jBtnGroups.setFont(new Font("Arial", Font.BOLD, 12));
		jBtnGroups.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		//contacts button
		jBtnContacts.setPreferredSize(new Dimension(150,35));
		jBtnContacts.setBackground(new Color(46, 204, 113));
		jBtnContacts.setForeground(Color.WHITE);
		jBtnContacts.setBorder(new LineBorder(new Color(46, 204, 113), 1));
		jBtnContacts.setFocusPainted(false);
		jBtnContacts.setFont(new Font("Arial", Font.BOLD, 12));
		jBtnContacts.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		//logout button
		jBtnLogout.setPreferredSize(new Dimension(150,35));
		jBtnLogout.setBackground(new Color(229, 115, 115));
		jBtnLogout.setForeground(Color.WHITE);
		jBtnLogout.setBorder(new LineBorder(new Color(229, 115, 115), 1));
		jBtnLogout.setFocusPainted(false);
		jBtnLogout.setFont(new Font("Arial", Font.BOLD, 12));
		jBtnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		// Top bar with user avatar and name
		int avatarIdx = clientModel.getAvatarIndex();
		if(avatarIdx < 0 || avatarIdx > 11) avatarIdx = 0;
		BufferedImage avatarImg = null;
		try {
			avatarImg = ImageIO.read(this.getClass().getResource("/avatar_" + avatarIdx + ".png"));
		} catch (IOException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}

		JLabel avatarLabel = new JLabel(new ImageIcon(avatarImg));
		avatarLabel.setPreferredSize(new Dimension(40, 40));

		JLabel nameLabel = new JLabel(clientModel.getFullName().equals("Enter Full Name") || clientModel.getFullName().isEmpty()
			? "User" : clientModel.getFullName());
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(new Font("Arial", Font.BOLD, 16));

		BufferedImage logoutImg = null;
		try {
			logoutImg = ImageIO.read(this.getClass().getResource("/logout.png"));
		} catch (IOException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
		jBtnLogoutIcon = new JButton(new ImageIcon(logoutImg.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		jBtnLogoutIcon.setPreferredSize(new Dimension(40, 35));
		jBtnLogoutIcon.setBackground(new Color(229, 115, 115));
		jBtnLogoutIcon.setOpaque(true);
		jBtnLogoutIcon.setBorder(new LineBorder(new Color(229, 115, 115), 1));
		jBtnLogoutIcon.setFocusPainted(false);
		jBtnLogoutIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jBtnLogoutIcon.setToolTipText("Logout");

		JPanel topBar = new JPanel(new BorderLayout());
		topBar.setBackground(Config.colorPrimary);
		topBar.setPreferredSize(new Dimension(864, 50));
		JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
		userInfo.setOpaque(false);
		userInfo.add(avatarLabel);
		userInfo.add(nameLabel);
		topBar.add(userInfo, BorderLayout.WEST);
		topBar.add(jBtnLogoutIcon, BorderLayout.EAST);

		// Main center panel with buttons
		JPanel centerPanel = new JPanel(new GridBagLayout());
		centerPanel.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		Insets textTitle = new Insets(4, 100, 20, 4);
		Insets buttonInsets = new Insets(4, 200, 20, 4);

		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 0; c.gridy = 0;
		c.insets = new Insets(20, 0, 10, 0);
		centerPanel.add(jLabelTitle, c);

		c.gridy = 1;
		c.insets = buttonInsets;
		centerPanel.add(jBtnCreateRoom, c);

		c.gridy = 2;
		centerPanel.add(jBtnJoinRoom, c);

		c.gridy = 3;
		centerPanel.add(jBtnViewRooms, c);

		c.gridy = 4;
		centerPanel.add(jBtnContacts, c);

		c.gridy = 5;
		centerPanel.add(jBtnGroups, c);

		c.gridy = 6;
		c.insets = new Insets(4, 200, 4, 4);
		centerPanel.add(jBtnLogout, c);

		jFrame.setLayout(new BorderLayout());
		jFrame.add(topBar, BorderLayout.NORTH);
		jFrame.add(centerPanel, BorderLayout.CENTER);

		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.setSize(864,614);
		jFrame.setResizable(false);
		jFrame.setLocationRelativeTo(null);
		jFrame.setVisible(true);
		
		ListeningEvents();
	}
	
	private void createAndJoinRoom(String rName, boolean create)
	{
		try {
			if(create)
				request = new Request(Request.Type.CREATE_ROOM.ordinal(),clientModel.getClientID(),clientModel.getRoomId(),rName);
			else
				request = new Request(Request.Type.JOIN_ROOM.ordinal(),clientModel.getClientID(),clientModel.getRoomId(),rName);
			
			ClientModel.objectOutputStream.writeObject(request);
			ClientModel.objectOutputStream.flush();
			Object obj =  ClientModel.objectInputStream.readObject();
			if( obj.getClass() == Response.class )
				response = (Response) obj;
			else
			{
				throw new Exception("Object returned is not of type Response. but of " + obj.getClass().toString() );
			}
			if( response.getSuccess())
			{
				int hashIndex = response.getContents().indexOf('#');
				clientModel.setRoomId(Integer.parseInt(response.getContents().substring(hashIndex+1, response.getContents().indexOf(" ", hashIndex))));
				new ChatActivity(clientModel);
				jFrame.dispose();
			}
			else
			{
				UIManager.put("OptionPane.okButtonText", "OK");
				JOptionPane.showMessageDialog(null, response.getContents(), null, JOptionPane.ERROR_MESSAGE);
			}
		}
		catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}
	
	private void displayAlertDialog(int which) {
		JPanel jPanel = new JPanel();
		jPanel.setSize(new Dimension(200, 64));
		jPanel.setLayout(null);
		
		JTextField jTextField = new JTextField();
		jTextField.setBackground(Color.white);
		jTextField.setBounds(0, 0, 250, 35);
		
		jTextField.setHorizontalAlignment(SwingConstants.CENTER);
		jPanel.add(jTextField);
		
		SwingUtilities.invokeLater(() -> jTextField.requestFocus());
		
		if(which == 1)
			UIManager.put("OptionPane.okButtonText", "Create Room");
		else
			UIManager.put("OptionPane.okButtonText", "Join Room");
		
		int choice = JOptionPane.showOptionDialog(null, jPanel, "Enter Room Name", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
		if(choice == JOptionPane.OK_OPTION) {
			try {
				if(which == 1)
					createAndJoinRoom(jTextField.getText().toString(), true);
				else
					createAndJoinRoom(jTextField.getText().toString(), false);
			}
			catch(Exception e) {
				e.printStackTrace(new PrintWriter(Config.errors));
				LogFileWriter.Log(Config.errors.toString());
			}
		}
	}
	
	private void checkForPendingInvites() {
		try {
			request = new Request(Request.Type.CHECK_PENDING_INVITES.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), "");
			ClientModel.objectOutputStream.writeObject(request);
			ClientModel.objectOutputStream.flush();
			response = (Response) ClientModel.objectInputStream.readObject();
			if(response.getSuccess()) {
				String[] invites = response.getContents().split("\n");
				for(String inv : invites) {
					if(inv.trim().length() > 0) {
						String[] parts = inv.split("\\|");
						if(parts.length == 3) {
							int gid = Integer.parseInt(parts[0].trim());
							String gname = parts[1].trim();
							String inviter = parts[2].trim();
							int choice = JOptionPane.showConfirmDialog(null,
								"You've been invited to group '" + gname + "' by " + inviter + "\n\nAccept invitation?",
								"Group Invitation",
								JOptionPane.YES_NO_OPTION);
							if(choice == JOptionPane.YES_OPTION) {
								Request acceptReq = new Request(Request.Type.ACCEPT_INVITE.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), String.valueOf(gid));
								ClientModel.objectOutputStream.writeObject(acceptReq);
								ClientModel.objectOutputStream.flush();
								Response resp = (Response) ClientModel.objectInputStream.readObject();
								JOptionPane.showMessageDialog(null, resp.getContents());
							} else {
								Request rejectReq = new Request(Request.Type.REJECT_INVITE.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), String.valueOf(gid));
								ClientModel.objectOutputStream.writeObject(rejectReq);
								ClientModel.objectOutputStream.flush();
								Response resp = (Response) ClientModel.objectInputStream.readObject();
							}
						}
					}
				}
			}
		} catch(Exception e) {
			// silently handle - invites are not critical
		}
	}
}

