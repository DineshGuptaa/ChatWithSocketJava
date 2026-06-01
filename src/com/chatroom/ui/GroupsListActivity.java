package com.chatroom.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.InsetsUIResource;

import com.chatroom.client.ClientModel;
import com.chatroom.configuration.Config;
import com.chatroom.models.Request;
import com.chatroom.models.Response;
import com.chatroom.others.LogFileWriter;

public class GroupsListActivity {
	private JFrame jFrame;
	private JButton jBtnCreateGroup;
	private JButton jBtnRefresh;
	private JButton jBtnBack;
	private JButton jBtnLogout;
	private JList<String> jListGroups;
	private DefaultListModel<String> listModel;
	private BufferedImage iconLogo;
	private ClientModel clientModel;
	private Request request;
	private Response response;

	public GroupsListActivity(ClientModel cm) throws IOException {
		this.clientModel = cm;
		jFrame = new JFrame("My Groups");

		iconLogo = ImageIO.read(this.getClass().getResource("/background.png"));

		listModel = new DefaultListModel<>();
		jListGroups = new JList<>(listModel);
		jListGroups.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jListGroups.setFont(new Font("Arial", Font.PLAIN, 14));
		jListGroups.setBackground(Color.WHITE);

		jBtnCreateGroup = new JButton("CREATE GROUP");
		jBtnRefresh = new JButton("REFRESH");
		jBtnBack = new JButton("BACK");
		BufferedImage logoutImg = null;
		try {
			logoutImg = ImageIO.read(this.getClass().getResource("/logout.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		jBtnLogout = new JButton(new ImageIcon(logoutImg.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		jBtnLogout.setPreferredSize(new Dimension(40, 35));
		jBtnLogout.setBackground(new Color(229, 115, 115));
		jBtnLogout.setOpaque(true);
		jBtnLogout.setBorder(new LineBorder(new Color(229, 115, 115), 1));
		jBtnLogout.setFocusPainted(false);
		jBtnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jBtnLogout.setToolTipText("Logout");

		initializeAllWithProperties();
		loadGroups();
	}

	private void loadGroups() {
		listModel.clear();
		try {
			request = new Request(Request.Type.VIEW_GROUPS.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), "");
			ClientModel.objectOutputStream.writeObject(request);
			ClientModel.objectOutputStream.flush();
			response = (Response) ClientModel.objectInputStream.readObject();
			if(response.getSuccess()) {
				String[] lines = response.getContents().split("\n");
				for(String line : lines) {
					if(line.trim().length() > 0)
						listModel.addElement(line.trim());
				}
			} else {
				listModel.addElement("No groups found");
			}
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void createGroup() {
		JPanel panel = new JPanel();
		JTextField field = new JTextField(20);
		panel.add(new JLabel("Group name:"));
		panel.add(field);

		int result = JOptionPane.showConfirmDialog(null, panel, "Create Group", JOptionPane.OK_CANCEL_OPTION);
		if(result == JOptionPane.OK_OPTION) {
			String gName = field.getText().trim();
			if(gName.length() > 0) {
				try {
					request = new Request(Request.Type.CREATE_GROUP.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), gName);
					ClientModel.objectOutputStream.writeObject(request);
					ClientModel.objectOutputStream.flush();
					response = (Response) ClientModel.objectInputStream.readObject();
					JOptionPane.showMessageDialog(null, response.getContents());
					loadGroups();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void inviteToGroup(String groupIdStr) {
		if(groupIdStr == null) return;
		int gid;
		try {
			gid = Integer.parseInt(groupIdStr.substring(groupIdStr.indexOf("ID:") + 3, groupIdStr.indexOf(" |")));
		} catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Could not parse group ID");
			return;
		}

		String username = JOptionPane.showInputDialog("Enter username to invite:");
		if(username != null && username.trim().length() > 0) {
			try {
				String content = gid + "#" + username.trim();
				request = new Request(Request.Type.INVITE_TO_GROUP.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), content);
				ClientModel.objectOutputStream.writeObject(request);
				ClientModel.objectOutputStream.flush();
				response = (Response) ClientModel.objectInputStream.readObject();
				JOptionPane.showMessageDialog(null, response.getContents());
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void openGroupChat(String groupIdStr) {
		if(groupIdStr == null) return;
		final int gid;
		final String gName;
		try {
			int nameStart = groupIdStr.indexOf("|") + 2;
			int adminStart = groupIdStr.indexOf("(Admin");
			if(adminStart > 0) {
				gName = groupIdStr.substring(nameStart, adminStart).trim();
			} else {
				gName = groupIdStr.substring(nameStart).trim();
			}
			gid = Integer.parseInt(groupIdStr.substring(groupIdStr.indexOf("ID:") + 3, groupIdStr.indexOf(" |")).trim());
		} catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Could not parse group info");
			return;
		}

		SwingUtilities.invokeLater(() -> {
			try {
				new GroupChatActivity(clientModel, gid, gName);
			} catch (IOException e) {
				e.printStackTrace();
			}
			jFrame.dispose();
		});
	}

	private void ListeningEvents() {
		jBtnCreateGroup.addActionListener(e -> createGroup());

		jBtnRefresh.addActionListener(e -> loadGroups());

		jBtnBack.addActionListener(e -> {
			try {
				new MainMenuOptions(clientModel);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			jFrame.dispose();
		});

		jBtnLogout.addActionListener(e -> logOut());

		jListGroups.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2) {
					String selected = jListGroups.getSelectedValue();
					if(selected != null) {
						String[] options = {"Open Chat", "Invite User", "View Members", "Cancel"};
						int choice = JOptionPane.showOptionDialog(null,
							"Select action for:\n" + selected,
							"Group Options",
							JOptionPane.DEFAULT_OPTION,
							JOptionPane.INFORMATION_MESSAGE,
							null, options, options[0]);

						if(choice == 0) {
							openGroupChat(selected);
						} else if(choice == 1) {
							inviteToGroup(selected);
						} else if(choice == 2) {
							viewGroupMembers(selected);
						}
					}
				}
			}
		});
	}

	private void logOut() {
		try {
			Request req = new Request(Request.Type.LOGOUT.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), "");
			ClientModel.objectOutputStream.writeObject(req);
			ClientModel.objectOutputStream.flush();
			Response res = (Response) ClientModel.objectInputStream.readObject();
			if (res.getSuccess()) {
				clientModel.setRoomId(-1);
				clientModel.setClientID(-1);
				new SignInActivity(clientModel);
				jFrame.dispose();
			}
		} catch (Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void viewGroupMembers(String groupIdStr) {
		if(groupIdStr == null) return;
		int gid;
		String gName = "";
		try {
			gid = Integer.parseInt(groupIdStr.substring(groupIdStr.indexOf("ID:") + 3, groupIdStr.indexOf(" |")).trim());
			int nameStart = groupIdStr.indexOf("|") + 2;
			int adminStart = groupIdStr.indexOf("(Admin");
			gName = adminStart > 0 ? groupIdStr.substring(nameStart, adminStart).trim() : groupIdStr.substring(nameStart).trim();
		} catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Could not parse group info");
			return;
		}

		try {
			request = new Request(Request.Type.VIEW_GROUP_MEMBERS.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), String.valueOf(gid));
			ClientModel.objectOutputStream.writeObject(request);
			ClientModel.objectOutputStream.flush();
			response = (Response) ClientModel.objectInputStream.readObject();
			if(response.getSuccess()) {
				String[] options = {"Remove Member", "Close"};
				int choice = JOptionPane.showOptionDialog(null,
					response.getContents(),
					"Members of " + gName,
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.INFORMATION_MESSAGE,
					null, options, options[1]);

				if(choice == 0) {
					String midStr = JOptionPane.showInputDialog("Enter member ID to remove:");
					if(midStr != null) {
						try {
							int mid = Integer.parseInt(midStr.trim());
							String content = gid + "#" + mid;
							request = new Request(Request.Type.REMOVE_GROUP_MEMBER.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), content);
							ClientModel.objectOutputStream.writeObject(request);
							ClientModel.objectOutputStream.flush();
							response = (Response) ClientModel.objectInputStream.readObject();
							JOptionPane.showMessageDialog(null, response.getContents());
						} catch(Exception ex) {
							JOptionPane.showMessageDialog(null, "Invalid ID");
						}
					}
				}
			} else {
				JOptionPane.showMessageDialog(null, response.getContents());
			}
		} catch(Exception e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	@SuppressWarnings("serial")
	private void initializeAllWithProperties() {
		jBtnCreateGroup.setPreferredSize(new Dimension(150,35));
		jBtnCreateGroup.setBackground(Config.colorAccent);
		jBtnCreateGroup.setForeground(Color.WHITE);
		jBtnCreateGroup.setBorder(new LineBorder(Config.colorAccent, 1));
		jBtnCreateGroup.setFocusPainted(false);
		jBtnCreateGroup.setFont(new Font("Arial", Font.BOLD, 12));

		jBtnRefresh.setPreferredSize(new Dimension(100,35));
		jBtnRefresh.setBackground(Config.colorPrimary);
		jBtnRefresh.setForeground(Color.WHITE);
		jBtnRefresh.setBorder(new LineBorder(Config.colorPrimary, 1));
		jBtnRefresh.setFocusPainted(false);
		jBtnRefresh.setFont(new Font("Arial", Font.BOLD, 12));

		jBtnBack.setPreferredSize(new Dimension(100,35));
		jBtnBack.setBackground(new Color(229, 115, 115));
		jBtnBack.setForeground(Color.WHITE);
		jBtnBack.setBorder(new LineBorder(new Color(229, 115, 115), 1));
		jBtnBack.setFocusPainted(false);
		jBtnBack.setFont(new Font("Arial", Font.BOLD, 12));

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(jBtnCreateGroup);
		buttonPanel.add(jBtnRefresh);
		buttonPanel.add(jBtnBack);
		buttonPanel.add(jBtnLogout);

		jFrame.setLayout(new BorderLayout());
		jFrame.add(buttonPanel, BorderLayout.NORTH);
		jFrame.add(new JScrollPane(jListGroups), BorderLayout.CENTER);

		JLabel hint = new JLabel("Double-click a group for options", SwingConstants.CENTER);
		hint.setForeground(Color.GRAY);
		jFrame.add(hint, BorderLayout.SOUTH);

		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.setSize(600, 500);
		jFrame.setLocationRelativeTo(null);
		jFrame.setVisible(true);

		ListeningEvents();
	}
}
