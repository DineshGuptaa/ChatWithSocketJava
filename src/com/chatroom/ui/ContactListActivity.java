package com.chatroom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;

import com.chatroom.client.ClientModel;
import com.chatroom.configuration.Config;
import com.chatroom.models.Request;
import com.chatroom.models.Response;
import com.chatroom.others.LogFileWriter;

public class ContactListActivity {
    private static ContactListActivity instance;
    private JFrame jFrame;
    private DefaultListModel<String> listModel;
    private JList<String> jList;
    private JButton jBtnBack;
    private JButton jBtnRefresh;
    private JButton jBtnLogout;
    private ClientModel clientModel;
    private java.util.HashMap<String, Integer> contactMap;

    public static void showExisting() {
        if(instance != null) {
            instance.jFrame.setVisible(true);
            instance.loadContacts();
        }
    }

    public ContactListActivity(ClientModel cm) throws IOException {
        instance = this;
        this.clientModel = cm;
        this.contactMap = new java.util.HashMap<>();
        jFrame = new JFrame("Online Contacts");

        listModel = new DefaultListModel<>();
        jList = new JList<>(listModel);
        jList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jList.setFont(new Font("Arial", Font.PLAIN, 16));
        jList.setBackground(Color.WHITE);
        jList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String text = value != null ? value.toString() : "";
                if(text.contains("(Online)")) {
                    c.setForeground(new Color(0, 128, 0));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(Color.BLACK);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        jBtnBack = new JButton("BACK");
        jBtnRefresh = new JButton("REFRESH");
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

    private void loadContacts() {
        listModel.clear();
        contactMap.clear();
        listModel.addElement("Loading...");
        new Thread(() -> {
            try {
                Request req = new Request(Request.Type.VIEW_ONLINE_USERS.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), "");
                ClientModel.objectOutputStream.writeObject(req);
                ClientModel.objectOutputStream.flush();
                Response res = (Response) ClientModel.objectInputStream.readObject();
                SwingUtilities.invokeLater(() -> {
                    listModel.clear();
                    if (res.getSuccess()) {
                        String[] lines = res.getContents().split("\n");
                        for (String line : lines) {
                            if (line.trim().length() > 0) {
                                String[] parts = line.split(":", 2);
                                if (parts.length == 2) {
                                    int uid = Integer.parseInt(parts[0].trim());
                                    String displayName = parts[1].trim();
                                    String cleanName = displayName.replace(" (Online)", "");
                                    contactMap.put(cleanName, uid);
                                    listModel.addElement(displayName);
                                }
                            }
                        }
                        if (listModel.isEmpty()) {
                            listModel.addElement("No other users online");
                        }
                    } else {
                        listModel.addElement("No other users online");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    listModel.clear();
                    listModel.addElement("Error loading contacts");
                });
                e.printStackTrace(new PrintWriter(Config.errors));
                LogFileWriter.Log(Config.errors.toString());
            }
        }).start();
    }

    private void ListeningEvents() {
        jBtnBack.addActionListener(e -> {
            try {
                new MainMenuOptions(clientModel);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            jFrame.dispose();
        });

        jBtnRefresh.addActionListener(e -> loadContacts());

        jBtnLogout.addActionListener(e -> logOut());

        jList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = jList.getSelectedValue();
                    if (selected != null) {
                        String cleanName = selected.replace(" (Online)", "");
                        if (contactMap.containsKey(cleanName)) {
                            int targetId = contactMap.get(cleanName);
                            String chatName = cleanName;
                            jFrame.setVisible(false);
                            try {
                                new PrivateChatActivity(clientModel, targetId, chatName);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                jFrame.setVisible(true);
                            }
                        }
                    }
                }
            }
        });
    }

    private void initializeAllWithProperties() {
        jBtnBack.setPreferredSize(new Dimension(100, 35));
        jBtnBack.setBackground(new Color(229, 115, 115));
        jBtnBack.setForeground(Color.WHITE);
        jBtnBack.setBorder(new LineBorder(new Color(229, 115, 115), 1));
        jBtnBack.setFocusPainted(false);
        jBtnBack.setFont(new Font("Arial", Font.BOLD, 12));
        jBtnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        jBtnRefresh.setPreferredSize(new Dimension(100, 35));
        jBtnRefresh.setBackground(Config.colorPrimary);
        jBtnRefresh.setForeground(Color.WHITE);
        jBtnRefresh.setBorder(new LineBorder(Config.colorPrimary, 1));
        jBtnRefresh.setFocusPainted(false);
        jBtnRefresh.setFont(new Font("Arial", Font.BOLD, 12));
        jBtnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(jBtnRefresh);
        buttonPanel.add(jBtnBack);
        buttonPanel.add(jBtnLogout);

        JLabel title = new JLabel("Online Contacts", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(Config.colorPrimary);

        jFrame.setLayout(new BorderLayout());
        jFrame.add(title, BorderLayout.NORTH);
        jFrame.add(new JScrollPane(jList), BorderLayout.CENTER);
        jFrame.add(buttonPanel, BorderLayout.SOUTH);

        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setSize(400, 500);
        jFrame.setLocationRelativeTo(null);

        ListeningEvents();
        loadContacts();
        jFrame.setVisible(true);
    }
}
