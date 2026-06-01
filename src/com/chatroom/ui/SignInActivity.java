package com.chatroom.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import com.chatroom.others.Message;

public class SignInActivity {
	private JLabel jLabel;
	private JFrame jFrame;
	private JButton jBtnSignIn;
	private JTextField jTvUsername;
	private JPasswordField jTvpassword;
	private CompoundBorder compoundBorder;
	private CompoundBorder compoundBorderAfterClick;
	private JLabel jLabelSignup;
	private JLabel jLabelForgotPwd;
	private JLabel jLabelSignIntitle;
	private BufferedImage iconLogo;
	private ClientModel clientModel;
	
	public SignInActivity(ClientModel cm) throws IOException {
		clientModel = cm;
		jFrame = new JFrame("CHATROOM Sign In");
		
		//scaling logo
		iconLogo = ImageIO.read(this.getClass().getResource("/logo.png"));
		
		//setting main background
		jFrame.setContentPane(new JPanel() {
			BufferedImage myImage = ImageIO.read(this.getClass().getResource("/background.png"));
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				g.drawImage(myImage, 0, 0, this);
			}
		});
		
		//creating compound border for Text Field to specify left margin to the text
		Border lineBorder = BorderFactory.createLineBorder(Config.colorLight, 1);
		Border emptyBorder = new EmptyBorder(0,10,0,0); //left margin for text
		compoundBorder = new CompoundBorder(lineBorder,emptyBorder);
		
		Border lineBorder1 = BorderFactory.createLineBorder(Config.colorPrimary, 3);
		Border emptyBorder1 = new EmptyBorder(0,10,0,0); //left margin for text
		compoundBorderAfterClick = new CompoundBorder(lineBorder1,emptyBorder1);
		
		jBtnSignIn = new JButton("SIGN IN");
		jTvUsername = new JTextField("Enter Username");
		
		jTvpassword = new JPasswordField();
		jTvpassword.setText("Enter Password");
		//making text visible in password field
		jTvpassword.setEchoChar((char)0);
		
		jLabelSignIntitle = new JLabel("SIGN IN");
		jLabelSignup = new JLabel("Not a user? Sign Up here");
		jLabelSignup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		jLabelForgotPwd = new JLabel("Forgot password?");
		jLabelForgotPwd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jLabelForgotPwd.setForeground(Config.colorAccent);
		
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
		
		jLabelSignup.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				try {
					jFrame.dispose();
					new SignUpActivity(clientModel);
				} catch (IOException e1) {
					e1.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
				}
			}
		});

		jLabelForgotPwd.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				forgotPassword();
			}
		});
		
		jBtnSignIn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String username = jTvUsername.getText();
				String password = jTvpassword.getText();		
				signIn(username,password);
			}
		});
		
	}
	
	private void signIn(String username, String password) {
		jBtnSignIn.setEnabled(false);
		String cont = username;
		cont += "#";
		cont += Hash.getHash(new String(password));
		Request request = new Request(Request.Type.LOGIN.ordinal(),clientModel.getClientID(),clientModel.getRoomId(),cont);

		new SwingWorker<Response, Void>() {
			@Override
			protected Response doInBackground() throws Exception {
				ClientModel.objectOutputStream.writeObject(request);
				ClientModel.objectOutputStream.flush();
				return (Response) ClientModel.objectInputStream.readObject();
			}

			@Override
			protected void done() {
				jBtnSignIn.setEnabled(true);
				try {
					Response response = get();
					if(response == null) return;
					if(response.getId() == Response.Type.LOGIN.ordinal()) {
						if(response.getSuccess()) {
							String[] parts = response.getContents().split("#");
							clientModel.setClientID(Integer.parseInt(parts[0]));
							if(parts.length > 1) clientModel.setFullName(parts[1]);
							if(parts.length > 2) clientModel.setGender(parts[2]);
							if(parts.length > 3) clientModel.setAvatarIndex(Integer.parseInt(parts[3]));
							new MainMenuOptions(clientModel);
							jFrame.dispose();
						} else {
							JOptionPane.showMessageDialog(null, response.getContents(), null, JOptionPane.ERROR_MESSAGE);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
					JOptionPane.showMessageDialog(jFrame, "Connection error. Please try again.", null, JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}
	
	private void forgotPassword() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.insets = new Insets(4, 4, 4, 4);
		gc.gridx = 0; gc.gridy = 0;
		panel.add(new JLabel("Username:"), gc);
		JTextField fpUser = new JTextField(15);
		gc.gridx = 1;
		panel.add(fpUser, gc);
		gc.gridx = 0; gc.gridy = 1;
		panel.add(new JLabel("New Password:"), gc);
		JPasswordField fpPwd = new JPasswordField(15);
		gc.gridx = 1;
		panel.add(fpPwd, gc);
		gc.gridx = 0; gc.gridy = 2;
		panel.add(new JLabel("Confirm Password:"), gc);
		JPasswordField fpConfirm = new JPasswordField(15);
		gc.gridx = 1;
		panel.add(fpConfirm, gc);

		int choice = JOptionPane.showConfirmDialog(jFrame, panel, "Reset Password",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if(choice != JOptionPane.OK_OPTION) return;

		String username = fpUser.getText().trim();
		String newPwd = new String(fpPwd.getPassword());
		String confirmPwd = new String(fpConfirm.getPassword());

		if(username.isEmpty() || username.equals("Enter Username")) {
			JOptionPane.showMessageDialog(jFrame, "Please enter your username", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if(newPwd.isEmpty()) {
			JOptionPane.showMessageDialog(jFrame, "Please enter a new password", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if(!newPwd.equals(confirmPwd)) {
			JOptionPane.showMessageDialog(jFrame, "Passwords do not match", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		String cont = username + "#" + Hash.getHash(newPwd);
		Request req = new Request(Request.Type.FORGOT_PASSWORD.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), cont);

		new SwingWorker<Response, Void>() {
			@Override
			protected Response doInBackground() throws Exception {
				ClientModel.objectOutputStream.writeObject(req);
				ClientModel.objectOutputStream.flush();
				return (Response) ClientModel.objectInputStream.readObject();
			}

			@Override
			protected void done() {
				try {
					Response res = get();
					if(res.getId() == Response.Type.FORGOT_PASSWORD.ordinal()) {
						JOptionPane.showMessageDialog(jFrame, res.getContents(),
							res.getSuccess() ? "Success" : "Error",
							res.getSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
					}
				} catch(Exception e) {
					e.printStackTrace(new PrintWriter(Config.errors));
					LogFileWriter.Log(Config.errors.toString());
					JOptionPane.showMessageDialog(jFrame, "Connection error", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		}.execute();
	}

	private void initializeAllWithProperties() {
		//User name text field
		jTvUsername.setPreferredSize(new Dimension(250,35));
		jTvUsername.setBackground(Color.WHITE);
		jTvUsername.setBorder(compoundBorder);
		jTvUsername.setForeground(Color.gray);
		
		//password text field
		jTvpassword.setPreferredSize(new Dimension(250,35));
		jTvpassword.setBackground(Color.WHITE);
		jTvpassword.setBorder(compoundBorder);
		jTvpassword.setForeground(Color.gray);
		
		
		//sign Up button
		jBtnSignIn.setPreferredSize(new Dimension(150,35));
		jBtnSignIn.setBackground(Config.colorAccent);
		jBtnSignIn.setForeground(Color.WHITE);
		jBtnSignIn.setBorder(new LineBorder(Config.colorAccent, 1));
		jBtnSignIn.setFocusPainted(false);
		jBtnSignIn.setFont(new Font("Arial", Font.BOLD, 14));
		jBtnSignIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		jBtnSignIn.requestFocus();
		
		jFrame.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		Insets buttonInsets = new Insets(4, 200, 20, 4);
		Insets textFieldInsets = new Insets(4, 150, 10, 4);
		Insets temp = new Insets(4, 185, 4, 4);
		Insets textTitle = new Insets(4, 250, 20, 4);
		Insets logoInsets = new InsetsUIResource(0, 200, 50, 0);
		
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1.0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.insets = logoInsets;
		c.gridy = 1;
		
		//setting logo
		jLabel = new JLabel(new ImageIcon(iconLogo));
		jLabel.setPreferredSize(new Dimension(150,150));
		jFrame.add(jLabel,c);
		
		//setting sign in title
		c.gridx = 0;
		c.gridy = 2;
		c.insets = textTitle;		
		jFrame.add(jLabelSignIntitle,c);
		
		//setting user name text field
		c.gridx = 0;
		c.gridy = 3;
		c.insets = textFieldInsets;
		jFrame.add(jTvUsername,c);
		
		//setting password text field
		c.gridy = 4;
		jFrame.add(jTvpassword,c);
		
		//setting sign up button
		c.gridx = 0;
		c.gridy = 5;
		c.insets = buttonInsets;
		jFrame.add(jBtnSignIn,c);
		
		//setting sign up link
		c.gridy = 6;
		c.insets = temp;
		jFrame.add(jLabelSignup,c);

		//setting forgot password link
		c.gridy = 7;
		c.insets = new Insets(4, 210, 4, 4);
		jFrame.add(jLabelForgotPwd,c);

		jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jFrame.setSize(864,614);
		jFrame.setResizable(false);
		jFrame.setLocationRelativeTo(null);
		jFrame.setVisible(true);
		
		//removing focus from edit text and set it to the button
		jFrame.getRootPane().setDefaultButton(jBtnSignIn);
		jBtnSignIn.requestFocus();
		
		SwingUtilities.invokeLater(() -> jTvUsername.requestFocus());
		
		ListeningEvents();
	}
	
}
