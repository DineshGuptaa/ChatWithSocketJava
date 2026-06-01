package com.chatroom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.InsetsUIResource;

import com.chatroom.client.ClientModel;
import com.chatroom.configuration.Config;
import com.chatroom.models.Request;
import com.chatroom.models.Response;
import com.chatroom.others.Hash;
import com.chatroom.others.LogFileWriter;

public class SignUpActivity {
	private JLabel jLabel;
	private JLabel jLabelsignin;
	private JLabel jLabelsignUptitle;
	private JFrame jFrame;
	private JButton jBtnSignUp;
	private JTextField jTvUsername;
	private JTextField jTvFullName;
	private JPasswordField jTvpassword;
	private JComboBox<String> jComboGender;
	private CompoundBorder compoundBorder;
	private CompoundBorder compoundBorderAfterClick;
	private BufferedImage iconLogo;
	private ClientModel clientModel;
	private int selectedAvatar = -1;
	private JLabel jLabelAvatarPreview;

	public SignUpActivity(ClientModel cm) throws IOException {
		clientModel = cm;
		jFrame = new JFrame("CHATROOM Sign Up");

		iconLogo = ImageIO.read(this.getClass().getResource("/logo.png"));

		jFrame.setContentPane(new JPanel() {
			BufferedImage myImage = ImageIO.read(this.getClass().getResource("/background.png"));
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(myImage, 0, 0, this);
			}
		});

		Border lineBorder = BorderFactory.createLineBorder(Config.colorLight, 1);
		Border emptyBorder = new EmptyBorder(0,10,0,0);
		compoundBorder = new CompoundBorder(lineBorder,emptyBorder);

		Border lineBorder1 = BorderFactory.createLineBorder(Config.colorPrimary, 3);
		Border emptyBorder1 = new EmptyBorder(0,10,0,0);
		compoundBorderAfterClick = new CompoundBorder(lineBorder1,emptyBorder1);

		jBtnSignUp = new JButton("Sign Up");
		jTvUsername = new JTextField("Enter Username");
		jTvFullName = new JTextField("Enter Full Name");

		jTvpassword = new JPasswordField();
		jTvpassword.setText("Enter Password");
		jTvpassword.setEchoChar((char)0);

		jComboGender = new JComboBox<>(new String[]{"Male", "Female", "Other"});

		jLabelsignUptitle = new JLabel("CREATE ACCOUNT");
		jLabelsignin = new JLabel("Already a user? Sign In here");
		jLabelsignin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		jLabelAvatarPreview = new JLabel("Choose an avatar below", SwingConstants.CENTER);
		jLabelAvatarPreview.setPreferredSize(new Dimension(80, 80));
		jLabelAvatarPreview.setOpaque(true);
		jLabelAvatarPreview.setBackground(Color.LIGHT_GRAY);
		jLabelAvatarPreview.setForeground(Color.WHITE);
		jLabelAvatarPreview.setFont(new Font("Arial", Font.BOLD, 32));
		jLabelAvatarPreview.setHorizontalAlignment(SwingConstants.CENTER);

		initializeAllWithProperties();
	}

	private void ListeningEvents() {
		jTvUsername.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				if(jTvUsername.getText().length() == 0) {
					jTvUsername.setForeground(Color.gray);
					jTvUsername.setText("Enter Username");
					jTvUsername.setBorder(compoundBorder);
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				if(jTvUsername.getText().equals("Enter Username"))
					jTvUsername.setText("");
				jTvUsername.setForeground(Color.black);
				jTvUsername.setBorder(compoundBorderAfterClick);
			}
		});

		jTvFullName.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				if(jTvFullName.getText().length() == 0) {
					jTvFullName.setForeground(Color.gray);
					jTvFullName.setText("Enter Full Name");
					jTvFullName.setBorder(compoundBorder);
				}
			}
			@Override
			public void focusGained(FocusEvent e) {
				if(jTvFullName.getText().equals("Enter Full Name"))
					jTvFullName.setText("");
				jTvFullName.setForeground(Color.black);
				jTvFullName.setBorder(compoundBorderAfterClick);
			}
		});

		jTvpassword.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				if(jTvpassword.getText().length() == 0) {
					jTvpassword.setForeground(Color.gray);
					jTvpassword.setEchoChar((char)0);
					jTvpassword.setText("Enter Password");
					jTvpassword.setBorder(compoundBorder);
				}
			}
			@Override
			public void focusGained(FocusEvent e) {
				if(jTvpassword.getText().equals("Enter Password"))
					jTvpassword.setText("");
				jTvpassword.setEchoChar('\u2022');
				jTvpassword.setForeground(Color.gray);
				jTvpassword.setBorder(compoundBorderAfterClick);
			}
		});

		jBtnSignUp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String username = jTvUsername.getText();
				String password = jTvpassword.getText();
				String fullName = jTvFullName.getText();
				if(fullName.equals("Enter Full Name")) fullName = username;
				String gender = (String) jComboGender.getSelectedItem();
				int avatarIdx = selectedAvatar >= 0 ? selectedAvatar : (username.hashCode() & 0x7FFFFFFF) % 12;
				signUp(username, password, fullName, gender, avatarIdx);
			}
		});

		jLabelsignin.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					jFrame.dispose();
					new SignInActivity(clientModel);
				} catch (IOException e1) {
					e1.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
				}
			}
		});
	}

	private void signUp(String username, String password, String fullName, String gender, int avatarIdx) {
		if(username.equals("Enter Username") || username.length() <= 0) {
			JOptionPane.showMessageDialog(null, "Enter username properly", null, JOptionPane.ERROR_MESSAGE);
		}
		else if(password.equals("Enter Password") || password.length() <= 0) {
			JOptionPane.showMessageDialog(null, "Enter password properly", null, JOptionPane.ERROR_MESSAGE);
		}
		else {
			String cont = username + "#" + Hash.getHash(password) + "#" + fullName + "#" + gender + "#" + avatarIdx;
			Request request = new Request(Request.Type.SIGN_UP.ordinal(),clientModel.getClientID(),clientModel.getRoomId(),cont);
			try {
				ClientModel.objectOutputStream.writeObject(request);
				ClientModel.objectOutputStream.flush();
				Response response = (Response) ClientModel.objectInputStream.readObject();

				if(response.getId() == Response.Type.SIGN_UP.ordinal()) {
					if(response.getSuccess()) {
						String[] parts = response.getContents().split("#");
						clientModel.setClientID(Integer.parseInt(parts[0]));
						clientModel.setFullName(parts.length > 1 ? parts[1] : fullName);
						clientModel.setGender(parts.length > 2 ? parts[2] : gender);
						clientModel.setAvatarIndex(parts.length > 3 ? Integer.parseInt(parts[3]) : avatarIdx);
						new MainMenuOptions(clientModel);
						jFrame.dispose();
					} else {
						JOptionPane.showMessageDialog(null, response.getContents(), null, JOptionPane.ERROR_MESSAGE);
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace(new PrintWriter(Config.errors));
				LogFileWriter.Log(Config.errors.toString());
			}
		}
	}

	private void initializeAllWithProperties() {
		jTvUsername.setPreferredSize(new Dimension(250,35));
		jTvUsername.setBackground(Color.WHITE);
		jTvUsername.setBorder(compoundBorder);
		jTvUsername.setForeground(Color.gray);

		jTvFullName.setPreferredSize(new Dimension(250,35));
		jTvFullName.setBackground(Color.WHITE);
		jTvFullName.setBorder(compoundBorder);
		jTvFullName.setForeground(Color.gray);

		jTvpassword.setPreferredSize(new Dimension(250,35));
		jTvpassword.setBackground(Color.WHITE);
		jTvpassword.setBorder(compoundBorder);
		jTvpassword.setForeground(Color.gray);

		jComboGender.setPreferredSize(new Dimension(250,35));
		jComboGender.setBackground(Color.WHITE);

		jBtnSignUp.setPreferredSize(new Dimension(150,35));
		jBtnSignUp.setBackground(Config.colorAccent);
		jBtnSignUp.setForeground(Color.WHITE);
		jBtnSignUp.setBorder(new LineBorder(Config.colorAccent, 1));
		jBtnSignUp.setFocusPainted(false);
		jBtnSignUp.setFont(new Font("Arial", Font.BOLD, 14));
		jBtnSignUp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jBtnSignUp.requestFocus();

		JPanel avatarPanel = new JPanel(new GridLayout(3, 4, 5, 5));
		avatarPanel.setOpaque(false);
		for(int i = 0; i < 12; i++) {
			final int idx = i;
			BufferedImage avImg = null;
			try {
				avImg = ImageIO.read(this.getClass().getResource("/avatar_" + i + ".png"));
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			JLabel avLabel = new JLabel(new ImageIcon(avImg));
			avLabel.setPreferredSize(new Dimension(60, 60));
			avLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
			avLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					selectedAvatar = idx;
					try {
						BufferedImage preview = ImageIO.read(getClass().getResource("/avatar_" + idx + ".png"));
						Image scaled = preview.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
						jLabelAvatarPreview.setIcon(new ImageIcon(scaled));
					} catch (IOException ex) {
						ex.printStackTrace();
					}
					jLabelAvatarPreview.setText("");
				}
			});
			avatarPanel.add(avLabel);
		}

		JPanel centerPanel = new JPanel(new GridBagLayout());
		centerPanel.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 0; c.gridy = 0;
		c.insets = new Insets(0, 0, 5, 0);
		centerPanel.add(jLabelAvatarPreview, c);
		c.gridy = 1;
		centerPanel.add(avatarPanel, c);

		jFrame.setLayout(new BorderLayout());

		JPanel topPanel = new JPanel(new GridBagLayout());
		topPanel.setOpaque(false);
		GridBagConstraints tc = new GridBagConstraints();
		tc.anchor = GridBagConstraints.NORTHWEST;
		tc.gridx = 0; tc.gridy = 0;
		tc.insets = new Insets(10, 10, 5, 10);
		topPanel.add(jLabelsignUptitle, tc);

		tc.gridy = 1;
		tc.insets = new Insets(2, 10, 2, 10);
		tc.fill = GridBagConstraints.HORIZONTAL;
		topPanel.add(jTvUsername, tc);

		tc.gridy = 2;
		topPanel.add(jTvFullName, tc);

		tc.gridy = 3;
		topPanel.add(jTvpassword, tc);

		tc.gridy = 4;
		topPanel.add(jComboGender, tc);

		tc.gridy = 5;
		tc.insets = new Insets(10, 10, 5, 10);
		tc.fill = GridBagConstraints.NONE;
		topPanel.add(jBtnSignUp, tc);

		tc.gridy = 6;
		tc.insets = new Insets(2, 10, 10, 10);
		topPanel.add(jLabelsignin, tc);

		jFrame.add(topPanel, BorderLayout.WEST);
		jFrame.add(centerPanel, BorderLayout.CENTER);

		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.setSize(864, 614);
		jFrame.setResizable(false);
		jFrame.setLocationRelativeTo(null);
		jFrame.setVisible(true);

		jFrame.getRootPane().setDefaultButton(jBtnSignUp);
		jBtnSignUp.requestFocus();

		SwingUtilities.invokeLater(() -> jTvUsername.requestFocus());

		ListeningEvents();
	}
}
