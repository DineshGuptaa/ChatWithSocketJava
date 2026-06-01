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
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import com.chatroom.client.ClientModel;
import com.chatroom.configuration.Config;
import com.chatroom.models.Request;
import com.chatroom.models.Response;
import com.chatroom.others.LogFileWriter;
import com.chatroom.others.TextBubbleBorder;

public class GroupChatActivity {
	private JFrame jFrame;
	private JButton jBtnSend;
	private JTextField jTfMessageHere;
	private CompoundBorder compoundBorder;
	private JPanel jPanel;
	private JPanel jPanelChatWindow;
	private int i = 0;
	private JScrollPane jScrollPane;
	private JLabel jLabelMessage;
	private AbstractBorder leftBubble;
	private AbstractBorder rightBubble;
	private GridBagConstraints leftBubbleConstraints;
	private GridBagConstraints rightBubbleConstraints;
	private GridBagConstraints centerConstraints;
	private ClientModel clientModel;
	private int groupId;
	private String groupName;
	private Request request = null;
	private Response response = null;
	private MessageListener messageListener;
	private JPanel optionsButtonsHolder;
	private JButton jLabel_leave;
	private JButton jLabel_logout;
	private JLabel jLabel_live;
	private int tracker;
	private boolean isSenderMsg;
	private boolean isReadMode;
	private int scrollDistance = 300;

	public GroupChatActivity(ClientModel clientModel, int groupId, String groupName) throws IOException {
		this.clientModel = clientModel;
		this.groupId = groupId;
		this.groupName = groupName;
		jFrame = new JFrame("Group: " + groupName);
		jPanel = new JPanel();
		jPanelChatWindow = new JPanel();
		optionsButtonsHolder = new JPanel();

		messageListener = new MessageListener();
		messageListener.start();

		Border lineBorder = BorderFactory.createLineBorder(Config.colorLight, 1);
		Border emptyBorder = new EmptyBorder(0,10,0,0);
		compoundBorder = new CompoundBorder(lineBorder,emptyBorder);

		leftBubble = new TextBubbleBorder(Config.colorPrimary,2, 10, 16);
		rightBubble = new TextBubbleBorder(Config.colorPrimary,2, 10, 16,false);

		BufferedImage liveUsers = ImageIO.read(this.getClass().getResource("/live.png"));

		jLabel_leave = new JButton("Leave");
		jLabel_leave.setPreferredSize(new Dimension(70,30));
		jLabel_leave.setBackground(new Color(229, 115, 115));
		jLabel_leave.setForeground(Color.WHITE);
		jLabel_leave.setBorder(new LineBorder(new Color(229, 115, 115), 1));
		jLabel_leave.setFocusPainted(false);
		jLabel_leave.setFont(new Font("Arial", Font.BOLD, 11));

		jLabel_live = new JLabel(new ImageIcon(liveUsers));
		jLabel_live.setPreferredSize(new Dimension(50,50));
		jLabel_live.setToolTipText("Group Info");

		BufferedImage logoutImg = ImageIO.read(this.getClass().getResource("/logout.png"));
		jLabel_logout = new JButton(new ImageIcon(logoutImg.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		jLabel_logout.setPreferredSize(new Dimension(40, 35));
		jLabel_logout.setBackground(new Color(229, 115, 115));
		jLabel_logout.setOpaque(true);
		jLabel_logout.setBorder(new LineBorder(new Color(229, 115, 115), 1));
		jLabel_logout.setFocusPainted(false);
		jLabel_logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jLabel_logout.setToolTipText("Logout");

		// Top bar with user avatar and name
		int avatarIdx = clientModel.getAvatarIndex();
		if(avatarIdx < 0 || avatarIdx > 11) avatarIdx = 0;
		BufferedImage avatarImg = ImageIO.read(this.getClass().getResource("/avatar_" + avatarIdx + ".png"));
		JLabel avatarLabel = new JLabel(new ImageIcon(avatarImg));
		avatarLabel.setPreferredSize(new Dimension(35, 35));
		JLabel nameLabel = new JLabel(groupName);
		nameLabel.setForeground(Color.WHITE);
		nameLabel.setFont(new Font("Arial", Font.BOLD, 14));

		JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
		userInfo.setOpaque(false);
		userInfo.add(avatarLabel);
		userInfo.add(nameLabel);

		optionsButtonsHolder.setBackground(Config.colorPrimary);
		optionsButtonsHolder.setLayout(new BorderLayout());
		optionsButtonsHolder.add(userInfo, BorderLayout.WEST);

		JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		rightActions.setOpaque(false);
		rightActions.add(jLabel_live);
		rightActions.add(jLabel_leave);
		rightActions.add(jLabel_logout);
		optionsButtonsHolder.add(rightActions, BorderLayout.EAST);
		optionsButtonsHolder.setBorder(new MatteBorder(0, 0, 2, 0, Config.colorLight));

		rightBubbleConstraints = new GridBagConstraints(0, i, 1, 1, 1.0, 0,
				GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0);

		leftBubbleConstraints = new GridBagConstraints(0, i, 1, 1, 1.0, 0,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0);

		centerConstraints = new GridBagConstraints(0, i, 1, 1, 1.0, 0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0);

		jTfMessageHere = new JTextField("Type your message here");

		ImageIcon sendIcon = new ImageIcon(ImageIO.read(this.getClass().getResource("/send_icon.png")).getScaledInstance(20, 20, Image.SCALE_SMOOTH));
		jBtnSend = new JButton(sendIcon);
		jBtnSend.setPreferredSize(new Dimension(50, 35));
		jBtnSend.setBackground(Config.colorAccent);
		jBtnSend.setForeground(Color.BLACK);
		jBtnSend.setOpaque(true);
		jBtnSend.setBorder(new LineBorder(Config.colorAccent, 1));
		jBtnSend.setFocusPainted(false);
		jBtnSend.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		initializeAllWithProperties();
		displayStatusMessages("You joined group: " + groupName);
	}

	private void updateScrollbarPosition() {
		jScrollPane.getVerticalScrollBar().setValue(jScrollPane.getVerticalScrollBar().getMaximum());
	}

	private void ListeningEvents() {
		jTfMessageHere.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				if(jTfMessageHere.getText().length() == 0) {
					jTfMessageHere.setForeground(Color.gray);
					jTfMessageHere.setText("Type your message here");
				}
			}
			@Override
			public void focusGained(FocusEvent e) {
				if(jTfMessageHere.getText().equals("Type your message here"))
					jTfMessageHere.setText("");
				jTfMessageHere.setForeground(Color.black);
			}
		});

		jBtnSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tracker = jScrollPane.getVerticalScrollBar().getMaximum();
				setSenderMessage();
				isSenderMsg = true;
			}
		});

		jLabel_leave.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				messageListener.isContinue = false;
				clientModel.setRoomId(-1);
				SwingUtilities.invokeLater(() -> {
					try {
						new GroupsListActivity(clientModel);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					jFrame.dispose();
				});
			}
		});

		jLabel_live.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JOptionPane.showMessageDialog(null, "Group: " + groupName + "\nID: " + groupId, "Group Info", JOptionPane.INFORMATION_MESSAGE);
			}
		});

		jLabel_logout.addActionListener(e -> logOut());

		jScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent e) {
				JScrollBar jsb = (JScrollBar) e.getAdjustable();
				int e1 = jsb.getModel().getExtent();
				if((jsb.getValue() + e1) <= jsb.getMaximum()-scrollDistance) {
					isReadMode = true;
					scrollDistance = 10;
				} else {
					isReadMode = false;
					scrollDistance = 300;
				}
				if(isSenderMsg || (!isReadMode && tracker != jsb.getMaximum())) {
					updateScrollbarPosition();
					tracker = jsb.getMaximum();
				}
				if(isSenderMsg) isSenderMsg = false;
			}
		});

		int condition = JComponent.WHEN_FOCUSED;
		InputMap inputMap = jTfMessageHere.getInputMap(condition);
		ActionMap actionMap = jTfMessageHere.getActionMap();
		KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		inputMap.put(enterKey, enterKey.toString());
		actionMap.put(enterKey.toString(), new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tracker = jScrollPane.getVerticalScrollBar().getMaximum();
				setSenderMessage();
				isSenderMsg = true;
				jTfMessageHere.setForeground(Color.gray);
				jTfMessageHere.setText("Type your message here");
				jBtnSend.requestFocus();
			}
		});
	}

	private void setSenderMessage() {
		try {
			if(!jTfMessageHere.getText().equals("Type your message here") && jTfMessageHere.getText().trim().length() > 0 && jTfMessageHere.getText().trim().length() <= 300) {
				request = new Request(Request.Type.GROUP_MSG.ordinal(), clientModel.getClientID(), groupId, jTfMessageHere.getText());
				ClientModel.objectOutputStream.writeObject(request);
				ClientModel.objectOutputStream.flush();

				jLabelMessage = new JLabel();
				String temp;
				if(jTfMessageHere.getText().trim().length() <= 3)
					temp = "20px";
				else if(jTfMessageHere.getText().trim().length() <= 51)
					temp = "auto";
				else
					temp = "300px";
				String text = String.format("<html><div><p style=\"width:%s;word-break: break-all;\">%s</p></div></html>", temp, jTfMessageHere.getText());
				jLabelMessage.setText(text);
				jLabelMessage.setBorder(rightBubble);
				rightBubbleConstraints.gridy = i;
				jPanelChatWindow.add(jLabelMessage, rightBubbleConstraints);
				jPanelChatWindow.revalidate();
				jPanelChatWindow.repaint();
				i++;
				clearTextMessage();
			} else {
				UIManager.put("OptionPane.okButtonText", "OK");
				JOptionPane.showMessageDialog(null, "The message length between 1-300 characters.", null, JOptionPane.ERROR_MESSAGE);
			}
		} catch(IOException e) {
			e.printStackTrace(new PrintWriter(Config.errors));
			LogFileWriter.Log(Config.errors.toString());
		}
	}

	private void clearTextMessage() {
		jTfMessageHere.setForeground(Color.gray);
		jTfMessageHere.setText("Type your message here");
		jBtnSend.requestFocus();
	}

	private void logOut() {
		try {
			messageListener.isContinue = false;
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

	private void displayStatusMessages(String message) {
		jLabelMessage = new JLabel();
		String text = String.format("<html><div style=\"width:%dpx;text-align:center;\">%s</div></html>", 400, message);
		jLabelMessage.setText(text);
		centerConstraints.gridy = i;
		jPanelChatWindow.add(jLabelMessage, centerConstraints);
		jPanelChatWindow.revalidate();
		jPanelChatWindow.repaint();
		i++;
	}

	private void setReceiverMessage(String senderName, String message) {
		jLabelMessage = new JLabel();
		String temp;
		if(message.trim().length() <= 3)
			temp = "20px";
		else if(message.trim().length() <= 51)
			temp = "auto";
		else
			temp = "300px";
		String text = String.format("<html><div style=\"font-size:8px;text-align:right;\">%3$s</div><div style=\"width:%1$spx;\"><p style=\"width:%s;word-break: break-all;\">%2$s</p></div></html>", temp, message, senderName + "<br>");
		jLabelMessage.setText(text);
		jLabelMessage.setBorder(leftBubble);
		leftBubbleConstraints.gridy = i;
		jPanelChatWindow.add(jLabelMessage, leftBubbleConstraints);
		jPanelChatWindow.revalidate();
		jPanelChatWindow.repaint();
		i++;
		clearTextMessage();
	}

	private void initializeAllWithProperties() {
		jTfMessageHere.setPreferredSize(new Dimension(550,35));
		jTfMessageHere.setBackground(Color.WHITE);
		jTfMessageHere.setBorder(compoundBorder);
		jTfMessageHere.setForeground(Color.gray);

		jPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		jPanel.add(jTfMessageHere, gbc);
		gbc.fill = GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.gridx = 1;
		jPanel.add(jBtnSend, gbc);
		jPanel.setOpaque(false);

		jPanelChatWindow.setOpaque(false);
		jPanelChatWindow.setLayout(new GridBagLayout());

		jFrame.setLayout(new BorderLayout());
		jFrame.add(BorderLayout.PAGE_END, jPanel);

		Color chatBg = new Color(236, 229, 221);
		jScrollPane = new JScrollPane(jPanelChatWindow) {
			@Override
			protected void paintComponent(Graphics g) {
				g.setColor(chatBg);
				g.fillRect(0, 0, getWidth(), getHeight());
				super.paintComponent(g);
			}
		};
		jScrollPane.setBorder(BorderFactory.createEmptyBorder());
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		jScrollPane.setBounds(0, 0, 864, 490);
		JPanel p1 = new JPanel(null);
		p1.setPreferredSize(new Dimension(864,530));
		p1.setBackground(chatBg);
		p1.add(jScrollPane);
		jFrame.add(BorderLayout.CENTER, p1);

		jFrame.add(BorderLayout.NORTH, optionsButtonsHolder);
		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.setSize(864,614);
		jFrame.setLocationRelativeTo(null);
		jFrame.setVisible(true);
		jFrame.setResizable(false);

		jFrame.getRootPane().setDefaultButton(jBtnSend);
		jBtnSend.requestFocus();

		SwingUtilities.invokeLater(() -> jTfMessageHere.requestFocus());

		ListeningEvents();
	}

	class MessageListener extends Thread {
		boolean isContinue = true;
		public void run() {
			while(true && isContinue) {
				try {
					response = (Response) ClientModel.objectInputStream.readObject();
					SwingUtilities.invokeLater(() -> {
						if(response.getId() == Response.Type.STATUS_MSG.ordinal() || response.getId() == Response.Type.LOGOUT.ordinal())
							displayStatusMessages(response.getContents());
						else if(response.getId() == Response.Type.GEN.ordinal()) {
							JOptionPane.showMessageDialog(null, response.getContents());
						} else if(response.getId() == Response.Type.GROUP_MSG.ordinal()) {
							String msg = response.getContents();
							String name = msg.substring(0, msg.indexOf(" "));
							msg = msg.substring(msg.indexOf(" ")+1);
							setReceiverMessage(name, msg);
						} else if(response.getId() == Response.Type.GROUP_INVITE_NOTIFICATION.ordinal()) {
							displayStatusMessages("[NOTIFICATION] " + response.getContents());
						}
						if(response.getContents().equals("sv_exit_successful")) {
							isContinue = false;
							clientModel.setRoomId(-1);
							SwingUtilities.invokeLater(() -> {
								try {
									new GroupsListActivity(clientModel);
								} catch (IOException e) {
									e.printStackTrace();
								}
								jFrame.dispose();
							});
						}
					});
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
					break;
				}
			}
		}
	}
}
