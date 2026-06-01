package com.chatroom.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.chatroom.client.ClientModel;
import com.chatroom.configuration.Config;
import com.chatroom.models.Request;
import com.chatroom.models.Response;
import com.chatroom.others.LogFileWriter;
import com.chatroom.others.TextBubbleBorder;

public class PrivateChatActivity {
    private JFrame jFrame;
    private JButton jBtnSend;
    private JButton jBtnAttach;
    private JTextField jTfMessage;
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
    private ClientModel clientModel;
    private int targetId;
    private String targetName;
    private MessageListener messageListener;
    private JButton jBtnLogout;
    private java.util.HashMap<Integer, JLabel> sentMessages;
    private java.util.HashMap<Integer, Integer> dbIdToTempId;
    private java.util.HashMap<Integer, JPanel> filePanels;
    private java.util.HashMap<String, JPanel> sentFilePanels;
    private int nextMsgId = 1;
    private String lastDateLabel = "";

    // Selection / deletion state
    private boolean selectionMode = false;
    private java.util.HashSet<Integer> selectedPmIds = new java.util.HashSet<>();
    private JPanel selectionToolbar;
    private JLabel selectionCountLabel;
    private JPanel undoPanel;
    private JLabel undoCountdownLabel;
    private javax.swing.Timer undoCountdownTimer;
    private int undoSecondsLeft = 5;
    private java.util.ArrayList<Component> removedMessageComponents = new java.util.ArrayList<>();
    private java.util.ArrayList<Integer> removedMessageGridYs = new java.util.ArrayList<>();
    private java.util.ArrayList<GridBagConstraints> removedMessageConstraints = new java.util.ArrayList<>();

    public PrivateChatActivity(ClientModel cm, int targetId, String targetName) throws IOException {
        this.clientModel = cm;
        this.targetId = targetId;
        this.targetName = targetName;
        jFrame = new JFrame("Chat with " + targetName);
        jPanel = new JPanel();
        jPanelChatWindow = new JPanel();

        messageListener = new MessageListener();
        sentMessages = new java.util.HashMap<>();
        dbIdToTempId = new java.util.HashMap<>();
        filePanels = new java.util.HashMap<>();
        sentFilePanels = new java.util.HashMap<>();

        Border lineBorder = BorderFactory.createLineBorder(Config.colorLight, 1);
        Border emptyBorder = new EmptyBorder(0, 10, 0, 0);
        compoundBorder = new CompoundBorder(lineBorder, emptyBorder);

        leftBubble = new TextBubbleBorder(Config.colorPrimary, 2, 10, 16);
        rightBubble = new TextBubbleBorder(Config.colorPrimary, 2, 10, 16, false);

        int avatarIdx = clientModel.getAvatarIndex();
        if (avatarIdx < 0 || avatarIdx > 11) avatarIdx = 0;
        BufferedImage avatarImg = ImageIO.read(this.getClass().getResource("/avatar_" + avatarIdx + ".png"));
        JLabel avatarLabel = new JLabel(new ImageIcon(avatarImg));
        avatarLabel.setPreferredSize(new Dimension(35, 35));

        JLabel nameLabel = new JLabel(targetName);
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));

        JButton jBtnBack = new JButton("BACK");
        jBtnBack.setPreferredSize(new Dimension(70, 30));
        jBtnBack.setBackground(new Color(229, 115, 115));
        jBtnBack.setForeground(Color.WHITE);
        jBtnBack.setBorder(new LineBorder(new Color(229, 115, 115), 1));
        jBtnBack.setFocusPainted(false);
        jBtnBack.setFont(new Font("Arial", Font.BOLD, 11));
        jBtnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jBtnBack.addActionListener(e -> {
            jFrame.dispose();
            messageListener.isContinue = false;
            messageListener.interrupt();
            new Thread(() -> {
                try {
                    messageListener.join(3000);
                } catch (InterruptedException ie) { }
                SwingUtilities.invokeLater(() -> ContactListActivity.showExisting());
            }).start();
        });

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Config.colorPrimary);
        topBar.setPreferredSize(new Dimension(864, 50));

        JPanel leftInfo = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 7));
        leftInfo.setOpaque(false);
        leftInfo.add(avatarLabel);
        leftInfo.add(nameLabel);
        topBar.add(leftInfo, BorderLayout.WEST);

        BufferedImage logoutImg = ImageIO.read(this.getClass().getResource("/logout.png"));
        jBtnLogout = new JButton(new ImageIcon(logoutImg.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
        jBtnLogout.setPreferredSize(new Dimension(40, 35));
        jBtnLogout.setBackground(new Color(229, 115, 115));
        jBtnLogout.setOpaque(true);
        jBtnLogout.setBorder(new LineBorder(new Color(229, 115, 115), 1));
        jBtnLogout.setFocusPainted(false);
        jBtnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jBtnLogout.setToolTipText("Logout");
        jBtnLogout.addActionListener(e -> logOut());

        JPanel rightActions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 5));
        rightActions.setOpaque(false);
        rightActions.add(jBtnLogout);
        rightActions.add(jBtnBack);
        topBar.add(rightActions, BorderLayout.EAST);

        rightBubbleConstraints = new GridBagConstraints(0, i, 1, 1, 1.0, 0,
                GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0);
        leftBubbleConstraints = new GridBagConstraints(0, i, 1, 1, 1.0, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0);

        jTfMessage = new JTextField("Type a message");

        ImageIcon sendIcon = new ImageIcon(ImageIO.read(this.getClass().getResource("/send_icon.png")).getScaledInstance(20, 20, Image.SCALE_SMOOTH));
        jBtnSend = new JButton(sendIcon);
        jBtnSend.setPreferredSize(new Dimension(50, 35));
        jBtnSend.setBackground(Config.colorAccent);
        jBtnSend.setForeground(Color.BLACK);
        jBtnSend.setOpaque(true);
        jBtnSend.setBorder(new LineBorder(Config.colorAccent, 1));
        jBtnSend.setFocusPainted(false);
        jBtnSend.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ImageIcon attachIcon = new ImageIcon(ImageIO.read(this.getClass().getResource("/send_icon.png")).getScaledInstance(18, 18, Image.SCALE_SMOOTH));
        jBtnAttach = new JButton("+");
        jBtnAttach.setPreferredSize(new Dimension(45, 35));
        jBtnAttach.setBackground(Config.colorAccent);
        jBtnAttach.setForeground(Color.BLACK);
        jBtnAttach.setOpaque(true);
        jBtnAttach.setBorder(new LineBorder(Config.colorAccent, 1));
        jBtnAttach.setFocusPainted(false);
        jBtnAttach.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jBtnAttach.setToolTipText("Attach File");

        buildSelectionToolbar();
        buildUndoPanel();
        initializeAllWithProperties();
        jFrame.add(topBar, BorderLayout.NORTH);
        loadMessageHistory();
        messageListener.start();
    }

    private void loadMessageHistory() {
        java.util.ArrayList<Integer> pendingFileIds = new java.util.ArrayList<>();
        try {
            Request req = new Request(Request.Type.LOAD_MESSAGES.ordinal(), clientModel.getClientID(), -1, String.valueOf(targetId));
            ClientModel.objectOutputStream.writeObject(req);
            ClientModel.objectOutputStream.flush();
            Response res = (Response) ClientModel.objectInputStream.readObject();
            if(res.getId() == Response.Type.MESSAGE_HISTORY.ordinal() && res.getSuccess()) {
                String[] lines = res.getContents().split("\n");
                for(String line : lines) {
                    if(line.trim().length() > 0) {
                        String[] parts = line.split("\\|", 6);
                        if(parts.length >= 4) {
                            int sid = Integer.parseInt(parts[0]);
                            String sName = parts[1];
                            String msgText = parts[2];
                            String tsStr = parts[3];
                            long tsMillis = System.currentTimeMillis();
                            try {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                                tsMillis = sdf.parse(tsStr).getTime();
                            } catch(Exception e) { }
                            if (msgText.startsWith("__file__:")) {
                                // __file__:fileId:fileName
                                String fileMeta = msgText.substring("__file__:".length());
                                int colonIdx = fileMeta.indexOf(":");
                                if (colonIdx > 0) {
                                    int fileId = Integer.parseInt(fileMeta.substring(0, colonIdx));
                                    String fileName = fileMeta.substring(colonIdx + 1);
                                    int histPmId = parts.length >= 5 ? Integer.parseInt(parts[4]) : -1;
                                    insertDateSeparatorIfNeeded(tsMillis);
                                    JPanel panel = createFileMessagePanel(fileName, 0, null, null, tsMillis, sid == clientModel.getClientID(), true, histPmId);
                                    addPanelToChat(panel, sid == clientModel.getClientID());
                                    filePanels.put(fileId, panel);
                                    pendingFileIds.add(fileId);
                                }
                            } else {
                                if(msgText.contains("##")) {
                                    msgText = msgText.substring(msgText.indexOf("##") + 2);
                                }
                                int histPmId = parts.length >= 5 ? Integer.parseInt(parts[4]) : -1;
                                if(sid == clientModel.getClientID()) {
                                    JLabel label = displaySentMessage(sName, msgText, tsMillis, histPmId);
                                    if(parts.length >= 6) {
                                        int pmStatus = Integer.parseInt(parts[5]);
                                        if(pmStatus >= 1) updateMessageStatus(label, "delivered");
                                        if(pmStatus >= 2) updateMessageStatus(label, "read");
                                    }
                                } else {
                                    displayReceivedMessage(sName, msgText, tsMillis, histPmId);
                                }
                            }
                        }
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace(new PrintWriter(Config.errors));
            LogFileWriter.Log(Config.errors.toString());
        }
        // Request file data for all file messages found in history
        for (int fid : pendingFileIds) {
            try {
                Request getReq = new Request(Request.Type.GET_FILE.ordinal(), clientModel.getClientID(), -1, String.valueOf(fid));
                ClientModel.objectOutputStream.writeObject(getReq);
                ClientModel.objectOutputStream.flush();
            } catch (Exception e) {
                e.printStackTrace(new PrintWriter(Config.errors));
                LogFileWriter.Log(Config.errors.toString());
            }
        }
    }

    private void buildSelectionToolbar() {
        selectionToolbar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 16, 6));
        selectionToolbar.setBackground(new Color(240, 240, 240));
        selectionToolbar.setVisible(false);
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 12));
        cancelBtn.addActionListener(e -> exitSelectionMode());
        selectionCountLabel = new JLabel("0 selected");
        selectionCountLabel.setFont(new Font("Arial", Font.BOLD, 13));
        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setFont(new Font("Arial", Font.BOLD, 12));
        deleteBtn.setForeground(Color.RED);
        deleteBtn.addActionListener(e -> deleteSelectedMessages());
        selectionToolbar.add(cancelBtn);
        selectionToolbar.add(selectionCountLabel);
        selectionToolbar.add(deleteBtn);
    }

    private void buildUndoPanel() {
        undoPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 16, 6));
        undoPanel.setBackground(new Color(255, 235, 235));
        undoPanel.setVisible(false);
        JLabel msg = new JLabel("Message deleted");
        msg.setFont(new Font("Arial", Font.BOLD, 13));
        undoCountdownLabel = new JLabel("5s");
        undoCountdownLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        JButton undoBtn = new JButton("Undo");
        undoBtn.setFont(new Font("Arial", Font.BOLD, 12));
        undoBtn.setForeground(new Color(0, 100, 200));
        undoBtn.addActionListener(e -> performUndo());
        undoPanel.add(msg);
        undoPanel.add(undoCountdownLabel);
        undoPanel.add(undoBtn);
    }

    private void initializeAllWithProperties() {
        jTfMessage.setPreferredSize(new Dimension(550, 35));
        jTfMessage.setBackground(Color.WHITE);
        jTfMessage.setBorder(compoundBorder);
        jTfMessage.setForeground(Color.gray);

        jPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        jPanel.add(jTfMessage, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.gridx = 1;
        jPanel.add(jBtnAttach, gbc);
        gbc.gridx = 2;
        jPanel.add(jBtnSend, gbc);
        jPanel.setOpaque(false);

        jPanelChatWindow.setOpaque(false);
        jPanelChatWindow.setLayout(new GridBagLayout());

        JPanel bottomWrapper = new JPanel();
        bottomWrapper.setLayout(new BoxLayout(bottomWrapper, BoxLayout.Y_AXIS));
        bottomWrapper.add(jPanel);
        bottomWrapper.add(selectionToolbar);
        bottomWrapper.add(undoPanel);
        jFrame.setLayout(new BorderLayout());
        jFrame.add(BorderLayout.PAGE_END, bottomWrapper);

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
        p1.setPreferredSize(new Dimension(864, 530));
        p1.setBackground(chatBg);
        p1.add(jScrollPane);
        jFrame.add(BorderLayout.CENTER, p1);

        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setSize(864, 614);
        jFrame.setLocationRelativeTo(null);
        jFrame.setVisible(true);
        jFrame.setResizable(false);

		jFrame.getRootPane().setDefaultButton(jBtnSend);
		jBtnSend.requestFocus();

		SwingUtilities.invokeLater(() -> jTfMessage.requestFocus());

		ListeningEvents();
    }

    private void ListeningEvents() {
        jTfMessage.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                if (jTfMessage.getText().length() == 0) {
                    jTfMessage.setForeground(Color.gray);
                    jTfMessage.setText("Type a message");
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
                if (jTfMessage.getText().equals("Type a message"))
                    jTfMessage.setText("");
                jTfMessage.setForeground(Color.black);
            }
        });

        jBtnAttach.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });

        jBtnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        int condition = JComponent.WHEN_FOCUSED;
        InputMap inputMap = jTfMessage.getInputMap(condition);
        ActionMap actionMap = jTfMessage.getActionMap();
        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        inputMap.put(enterKey, enterKey.toString());
        actionMap.put(enterKey.toString(), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String msg = jTfMessage.getText().trim();
        if (msg.length() == 0 || msg.equals("Type a message"))
            return;
        try {
            int msgId = nextMsgId++;
            String pmContent = "@" + targetName + " " + msgId + "##" + msg;
            Request req = new Request(Request.Type.MSG.ordinal(), clientModel.getClientID(), clientModel.getRoomId(), pmContent);
            ClientModel.objectOutputStream.writeObject(req);
            ClientModel.objectOutputStream.flush();

            String senderName = clientModel.getFullName().isEmpty() ? "You" : clientModel.getFullName();
            long now = System.currentTimeMillis();
            JLabel sentLabel = displaySentMessage(senderName, msg, now);
            sentMessages.put(msgId, sentLabel);
            jTfMessage.setForeground(Color.gray);
            jTfMessage.setText("Type a message");
        } catch (IOException e) {
            e.printStackTrace(new PrintWriter(Config.errors));
            LogFileWriter.Log(Config.errors.toString());
        }
    }

    private void sendFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select a file to send");
        int result = fc.showOpenDialog(jFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
                String base64Data = Base64.getEncoder().encodeToString(fileBytes);
                String fileName = file.getName();
                long fileSize = file.length();
                String mimeType = getMimeType(fileName);
                String content = targetName + "||" + fileName + "||" + fileSize + "||" + mimeType + "||" + base64Data;
                Request req = new Request(Request.Type.FILE_MSG.ordinal(), clientModel.getClientID(), -1, content);
                ClientModel.objectOutputStream.writeObject(req);
                ClientModel.objectOutputStream.flush();

                long now = System.currentTimeMillis();
                insertDateSeparatorIfNeeded(now);
                JPanel panel = createFileMessagePanel(fileName, fileSize, mimeType, fileBytes, now, true, false);
                addPanelToChat(panel, true);
                sentFilePanels.put(fileName, panel);
            } catch (Exception ex) {
                ex.printStackTrace(new PrintWriter(Config.errors));
                LogFileWriter.Log(Config.errors.toString());
            }
        }
    }

    private String getMimeType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "bmp": return "image/bmp";
            case "webp": return "image/webp";
            case "pdf": return "application/pdf";
            case "doc": case "docx": return "application/msword";
            case "xls": case "xlsx": return "application/vnd.ms-excel";
            case "zip": case "rar": case "7z": return "application/zip";
            case "mp4": return "video/mp4";
            case "mp3": return "audio/mpeg";
            default: return "application/octet-stream";
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    private Icon createThumbnail(byte[] data, int size) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(data));
            if (original == null) return createFileTypeIcon("application/octet-stream", "file");
            Image scaled = original.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return createFileTypeIcon("application/octet-stream", "file");
        }
    }

    private Icon createFileTypeIcon(String mimeType, String fileName) {
        int size = 48;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(200, 200, 200));
        g2d.fillRoundRect(2, 2, size - 4, size - 4, 8, 8);
        String ext = "?";
        if (fileName != null && fileName.contains(".")) {
            ext = fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
        }
        g2d.setColor(new Color(80, 80, 80));
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (size - fm.stringWidth(ext)) / 2;
        int y = (size + fm.getHeight() / 3);
        g2d.drawString(ext, x, y);
        g2d.dispose();
        return new ImageIcon(img);
    }

    private Icon createPlaceholderIcon() {
        int size = 48;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(255, 180, 180));
        g2d.fillRoundRect(2, 2, size - 4, size - 4, 8, 8);
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        FontMetrics fm = g2d.getFontMetrics();
        int x = (size - fm.stringWidth("?")) / 2;
        int y = (size + fm.getHeight() / 3);
        g2d.drawString("?", x, y);
        g2d.dispose();
        return new ImageIcon(img);
    }

    private JPanel createFileMessagePanel(String fileName, long fileSize, String mimeType, byte[] data, long tsMillis, boolean isSent, boolean isPlaceholder) {
        return createFileMessagePanel(fileName, fileSize, mimeType, data, tsMillis, isSent, isPlaceholder, -1);
    }

    private JPanel createFileMessagePanel(String fileName, long fileSize, String mimeType, byte[] data, long tsMillis, boolean isSent, boolean isPlaceholder, int pmId) {
        JPanel panel = new JPanel(new BorderLayout(8, 2));
        panel.setOpaque(false);

        if(pmId > 0) panel.putClientProperty("pmId", pmId);
        panel.putClientProperty("fileName", fileName);
        panel.putClientProperty("fileMimeType", mimeType);
        panel.putClientProperty("fileData", data);
        panel.putClientProperty("fileSize", fileSize);

        Icon icon;
        if (isPlaceholder || data == null) {
            icon = createPlaceholderIcon();
        } else if (mimeType != null && mimeType.startsWith("image/")) {
            icon = createThumbnail(data, 48);
        } else {
            icon = createFileTypeIcon(mimeType, fileName);
        }

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setPreferredSize(new Dimension(48, 48));

        String timeStr = formatTime(tsMillis);
        String sizeStr = isPlaceholder ? "loading..." : formatFileSize(fileSize);
        String tick = isSent ? " &#10003;" : "";
        String html = String.format("<html><b>%s</b><br><span style='font-size:10px;color:gray;'>%s</span><br><span style='font-size:10px;color:gray;'>%s%s</span></html>",
            fileName, sizeStr, timeStr, tick);
        JLabel textLabel = new JLabel(html);

        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(textLabel, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        Border bubble = isSent ? rightBubble : leftBubble;
        panel.setBorder(BorderFactory.createCompoundBorder(bubble, BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        panel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openFileViewer(panel);
                }
            }
        });
        addSelectionListeners(panel);

        return panel;
    }

    private void updateFilePanelIcon(JPanel panel, String mimeType, byte[] data, String fileName, long fileSize) {
        panel.putClientProperty("fileMimeType", mimeType);
        panel.putClientProperty("fileData", data);
        panel.putClientProperty("fileName", fileName);
        panel.putClientProperty("fileSize", fileSize);

        JLabel iconLabel = (JLabel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.WEST);
        Icon icon;
        if (mimeType != null && mimeType.startsWith("image/") && data != null) {
            icon = createThumbnail(data, 48);
        } else {
            icon = createFileTypeIcon(mimeType, fileName);
        }
        iconLabel.setIcon(icon);

        JLabel textLabel = (JLabel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        String html = textLabel.getText();
        html = html.replace("loading...", formatFileSize(fileSize));
        textLabel.setText(html);
        panel.revalidate();
        panel.repaint();
    }

    private void openFileViewer(JPanel panel) {
        String fileName = (String) panel.getClientProperty("fileName");
        String mimeType = (String) panel.getClientProperty("fileMimeType");
        byte[] data = (byte[]) panel.getClientProperty("fileData");

        if (data == null || data.length == 0) {
            JOptionPane.showMessageDialog(jFrame, "File data is not available yet. Please wait for it to load.", "File Not Available", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (mimeType != null && mimeType.startsWith("image/")) {
            openImageViewer(data, fileName);
        } else if (isTextFile(mimeType, fileName)) {
            openTextViewer(data, fileName);
        } else {
            openWithSystemApp(data, fileName, mimeType);
        }
    }

    private boolean isTextFile(String mimeType, String fileName) {
        if (mimeType != null && mimeType.startsWith("text/")) return true;
        if (fileName == null) return false;
        String n = fileName.toLowerCase();
        return n.endsWith(".txt") || n.endsWith(".java") || n.endsWith(".py")
            || n.endsWith(".html") || n.endsWith(".htm") || n.endsWith(".css")
            || n.endsWith(".js") || n.endsWith(".ts") || n.endsWith(".jsx")
            || n.endsWith(".tsx") || n.endsWith(".xml") || n.endsWith(".json")
            || n.endsWith(".sql") || n.endsWith(".md") || n.endsWith(".sh")
            || n.endsWith(".bat") || n.endsWith(".cmd") || n.endsWith(".properties")
            || n.endsWith(".cfg") || n.endsWith(".ini") || n.endsWith(".yaml")
            || n.endsWith(".yml") || n.endsWith(".csv") || n.endsWith(".log")
            || n.endsWith(".conf") || n.endsWith(".gradle") || n.endsWith(".kt")
            || n.endsWith(".scala") || n.endsWith(".php") || n.endsWith(".rb")
            || n.endsWith(".go") || n.endsWith(".rs") || n.endsWith(".toml");
    }

    private void openImageViewer(byte[] data, String fileName) {
        JDialog dialog = new JDialog(jFrame, fileName, true);
        ImageIcon icon = new ImageIcon(data);
        JLabel label = new JLabel(icon);
        JScrollPane scroll = new JScrollPane(label);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        dialog.add(scroll);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(jFrame);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    private void openTextViewer(byte[] data, String fileName) {
        JDialog dialog = new JDialog(jFrame, fileName + " — Text Viewer", true);
        String text = new String(data, StandardCharsets.UTF_8);
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textArea.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(textArea);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(scroll);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(jFrame);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setVisible(true);
    }

    private void openWithSystemApp(byte[] data, String fileName, String mimeType) {
        try {
            if (!Desktop.isDesktopSupported()) {
                JOptionPane.showMessageDialog(jFrame, "Opening files is not supported on this system.", "Unsupported", JOptionPane.WARNING_MESSAGE);
                return;
            }
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            File tmpFile = new File(tmpDir, fileName);
            int counter = 1;
            while (tmpFile.exists()) {
                String base = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
                String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";
                tmpFile = new File(tmpDir, base + "_" + (counter++) + ext);
            }
            FileOutputStream fos = new FileOutputStream(tmpFile);
            fos.write(data);
            fos.close();
            tmpFile.deleteOnExit();
            Desktop.getDesktop().open(tmpFile);
        } catch (Exception e) {
            e.printStackTrace(new PrintWriter(Config.errors));
            LogFileWriter.Log(Config.errors.toString());
            JOptionPane.showMessageDialog(jFrame, "Could not open file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---- multi-message selection & deletion ----

    private void addSelectionListeners(Component c) {
        c.addMouseListener(new MouseAdapter() {
            javax.swing.Timer longPressTimer;
            public void mousePressed(MouseEvent e) {
                if (selectionMode) {
                    toggleSelection(c);
                    return;
                }
                longPressTimer = new javax.swing.Timer(500, ev -> {
                    enterSelectionMode(c);
                });
                longPressTimer.setRepeats(false);
                longPressTimer.start();
            }
            public void mouseReleased(MouseEvent e) {
                if (longPressTimer != null) { longPressTimer.stop(); longPressTimer = null; }
            }
            public void mouseExited(MouseEvent e) {
                if (longPressTimer != null) { longPressTimer.stop(); longPressTimer = null; }
            }
        });
    }

    private void enterSelectionMode(Component source) {
        if (!(source instanceof JComponent)) return;
        Object pmIdObj = ((JComponent)source).getClientProperty("pmId");
        if (!(pmIdObj instanceof Integer) || (Integer)pmIdObj <= 0) return;
        selectionMode = true;
        selectedPmIds.clear();
        selectionToolbar.setVisible(true);
        jTfMessage.setEnabled(false);
        jBtnSend.setEnabled(false);
        jBtnAttach.setEnabled(false);
        toggleSelection(source);
    }

    private void exitSelectionMode() {
        selectionMode = false;
        selectedPmIds.clear();
        selectionToolbar.setVisible(false);
        jTfMessage.setEnabled(true);
        jBtnSend.setEnabled(true);
        jBtnAttach.setEnabled(true);
        for (int idx = 0; idx < jPanelChatWindow.getComponentCount(); idx++) {
            Component child = jPanelChatWindow.getComponent(idx);
            SelectionBorder.remove(child);
        }
        jPanelChatWindow.repaint();
    }

    private void toggleSelection(Component c) {
        if (!(c instanceof JComponent)) return;
        Object pmIdObj = ((JComponent)c).getClientProperty("pmId");
        if (!(pmIdObj instanceof Integer)) return;
        int pmId = (Integer) pmIdObj;
        if (pmId <= 0) return;
        if (selectedPmIds.contains(pmId)) {
            selectedPmIds.remove(pmId);
            SelectionBorder.attach(c, false);
        } else {
            selectedPmIds.add(pmId);
            SelectionBorder.attach(c, true);
        }
        selectionCountLabel.setText(selectedPmIds.size() + " selected");
        if (selectedPmIds.isEmpty()) {
            exitSelectionMode();
        }
    }

    private void deleteSelectedMessages() {
        if (selectedPmIds.isEmpty()) return;
        String[] options = {"Delete for me", "Delete for everyone", "Cancel"};
        int choice = JOptionPane.showOptionDialog(jFrame,
            "Delete selected messages?",
            "Delete Messages",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null, options, options[2]);
        if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) return;
        String mode = (choice == 0) ? "me" : "everyone";
        StringBuilder sb = new StringBuilder();
        for (int pid : selectedPmIds) {
            if (sb.length() > 0) sb.append(",");
            sb.append(pid);
        }
        String idsStr = sb.toString();
        // Collect removed components and their grid positions
        removedMessageComponents.clear();
        removedMessageGridYs.clear();
        for (int idx = jPanelChatWindow.getComponentCount() - 1; idx >= 0; idx--) {
            Component child = jPanelChatWindow.getComponent(idx);
            Object pmIdObj = (child instanceof JComponent) ? ((JComponent)child).getClientProperty("pmId") : null;
            if (pmIdObj instanceof Integer && selectedPmIds.contains((Integer)pmIdObj)) {
                GridBagLayout layout = (GridBagLayout) jPanelChatWindow.getLayout();
                GridBagConstraints gbc = layout.getConstraints(child);
                removedMessageGridYs.add(gbc.gridy);
                removedMessageConstraints.add((GridBagConstraints)gbc.clone());
                removedMessageComponents.add(child);
                jPanelChatWindow.remove(idx);
            }
        }
        jPanelChatWindow.revalidate();
        jPanelChatWindow.repaint();
        // Rebuild grid Y indices
        rebuildGridY();
        // Send delete request
        exitSelectionMode();
        try {
            String content = mode + ":" + idsStr;
            Request req = new Request(Request.Type.DELETE_MESSAGES.ordinal(), clientModel.getClientID(), -1, content);
            ClientModel.objectOutputStream.writeObject(req);
            ClientModel.objectOutputStream.flush();
        } catch (Exception ex) {
            ex.printStackTrace(new PrintWriter(Config.errors));
            LogFileWriter.Log(Config.errors.toString());
        }
        showUndoSnackbar();
    }

    private void performUndo() {
        if (undoCountdownTimer != null) undoCountdownTimer.stop();
        undoPanel.setVisible(false);
        // Restore removed components with original constraints
        for (int idx = 0; idx < removedMessageComponents.size(); idx++) {
            Component c = removedMessageComponents.get(idx);
            GridBagConstraints gbc = removedMessageConstraints.get(idx);
            gbc.gridy = removedMessageGridYs.get(idx);
            jPanelChatWindow.add(c, gbc);
        }
        removedMessageComponents.clear();
        removedMessageGridYs.clear();
        removedMessageConstraints.clear();
        rebuildGridY();
        jPanelChatWindow.revalidate();
        jPanelChatWindow.repaint();
        // Send undo request
        try {
            java.util.ArrayList<Integer> undoIds = new java.util.ArrayList<>();
            for (int pid : selectedPmIds) undoIds.add(pid);
            StringBuilder sb = new StringBuilder();
            for (int pid : undoIds) {
                if (sb.length() > 0) sb.append(",");
                sb.append(pid);
            }
            if (sb.length() > 0) {
                Request req = new Request(Request.Type.UNDO_DELETE.ordinal(), clientModel.getClientID(), -1, sb.toString());
                ClientModel.objectOutputStream.writeObject(req);
                ClientModel.objectOutputStream.flush();
            }
        } catch (Exception ex) {
            ex.printStackTrace(new PrintWriter(Config.errors));
            LogFileWriter.Log(Config.errors.toString());
        }
    }

    private void showUndoSnackbar() {
        undoSecondsLeft = 5;
        undoCountdownLabel.setText(undoSecondsLeft + "s");
        undoPanel.setVisible(true);
        if (undoCountdownTimer != null) undoCountdownTimer.stop();
        undoCountdownTimer = new javax.swing.Timer(1000, e -> {
            undoSecondsLeft--;
            if (undoSecondsLeft <= 0) {
                undoCountdownTimer.stop();
                undoPanel.setVisible(false);
                removedMessageComponents.clear();
                removedMessageGridYs.clear();
                removedMessageConstraints.clear();
            } else {
                undoCountdownLabel.setText(undoSecondsLeft + "s");
            }
        });
        undoCountdownTimer.start();
    }

    private void rebuildGridY() {
        int count = jPanelChatWindow.getComponentCount();
        for (int idx = 0; idx < count; idx++) {
            Component child = jPanelChatWindow.getComponent(idx);
            GridBagLayout layout = (GridBagLayout) jPanelChatWindow.getLayout();
            GridBagConstraints gbc = layout.getConstraints(child);
            gbc.gridy = idx;
            layout.setConstraints(child, gbc);
        }
        i = count;
    }

    // ---- inner class for selection border decoration ----
    static class SelectionBorder {
        static final Border SELECTED = BorderFactory.createLineBorder(new Color(0, 120, 215), 3);
        static final Border UNSELECTED = BorderFactory.createLineBorder(new Color(200, 200, 200), 1);
        static void attach(Component c, boolean selected) {
            if (c instanceof JComponent) {
                JComponent jc = (JComponent)c;
                if(jc.getClientProperty("origBorder") == null) {
                    jc.putClientProperty("origBorder", jc.getBorder());
                }
                jc.setBorder(selected ? SELECTED : UNSELECTED);
            }
        }
        static void remove(Component c) {
            if (c instanceof JComponent) {
                JComponent jc = (JComponent)c;
                Border orig = (Border)jc.getClientProperty("origBorder");
                if(orig != null) {
                    jc.setBorder(orig);
                }
            }
        }
    }

    private void addPanelToChat(JPanel panel, boolean isSent) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = i;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, isSent ? 80 : 10, 5, isSent ? 10 : 80);
        gbc.anchor = isSent ? GridBagConstraints.NORTHEAST : GridBagConstraints.NORTHWEST;
        jPanelChatWindow.add(panel, gbc);
        jPanelChatWindow.revalidate();
        jPanelChatWindow.repaint();
        i++;
        smartScrollToBottom();
    }

    private JLabel displaySentMessage(String sender, String message) {
        return displaySentMessage(sender, message, System.currentTimeMillis());
    }

    private JLabel displaySentMessage(String sender, String message, long tsMillis) {
        return displaySentMessage(sender, message, tsMillis, -1);
    }

    private JLabel displaySentMessage(String sender, String message, long tsMillis, int pmId) {
        insertDateSeparatorIfNeeded(tsMillis);
        JLabel label = new JLabel();
        if(pmId > 0) label.putClientProperty("pmId", pmId);
        String temp;
        if (message.length() <= 3)
            temp = "20px";
        else if (message.length() <= 51)
            temp = "auto";
        else
            temp = "300px";
        String timeStr = formatTime(tsMillis);
        String text = String.format("<html><div><p style=\"width:%s;word-break: break-all;margin:0;\">%s</p>"
                + "<span style=\"font-size:10px;float:right;color:gray;\">%s &#10003;</span></div></html>", temp, message, timeStr);
        label.setText(text);
        label.setBorder(rightBubble);
        addSelectionListeners(label);
        rightBubbleConstraints.gridy = i;
        jPanelChatWindow.add(label, rightBubbleConstraints);
        jPanelChatWindow.revalidate();
        jPanelChatWindow.repaint();
        i++;
        smartScrollToBottom();
        return label;
    }

    private void updateMessageStatus(JLabel label, String status) {
        String currentText = label.getText();
        if(status.equals("delivered")) {
            String updated = currentText.replace("&#10003;</span>", "&#10003;&#10003;</span>");
            label.setText(updated);
        } else if(status.equals("read")) {
            String updated = currentText.replace("font-size:10px;float:right;color:gray;", "font-size:10px;float:right;color:#53bdeb;");
            label.setText(updated);
        }
        label.revalidate();
        label.repaint();
    }

    private String formatTime(long millis) {
        SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a");
        return timeFmt.format(new Date(millis));
    }

    private String getDateLabel(long millis) {
        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        if(msgCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            return "Today";
        }
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if(msgCal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday";
        }
        Calendar weekAgo = Calendar.getInstance();
        weekAgo.add(Calendar.DAY_OF_YEAR, -7);
        if(msgCal.after(weekAgo)) {
            return new SimpleDateFormat("EEEE").format(new Date(millis));
        }
        return new SimpleDateFormat("d MMMM yyyy").format(new Date(millis));
    }

    private void insertDateSeparatorIfNeeded(long millis) {
        String label = getDateLabel(millis);
        if(!label.equals(lastDateLabel)) {
            lastDateLabel = label;
            JLabel dateLabel = new JLabel(label, SwingConstants.CENTER);
            dateLabel.setFont(new Font("Arial", Font.BOLD, 12));
            dateLabel.setForeground(new Color(7, 94, 84));
            dateLabel.setOpaque(true);
            dateLabel.setBackground(new Color(220, 248, 198));
            dateLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            GridBagConstraints gbc = new GridBagConstraints(0, i, 1, 1, 1.0, 0,
                    GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(8, 0, 8, 0), 0, 0);
            jPanelChatWindow.add(dateLabel, gbc);
            jPanelChatWindow.revalidate();
            jPanelChatWindow.repaint();
            i++;
        }
    }

    private void smartScrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar sb = jScrollPane.getVerticalScrollBar();
            sb.setValue(sb.getMaximum());
        });
    }

    private void sendReadReceipt(int originalSenderId, int dbId) {
        try {
            Request rrReq = new Request(Request.Type.SEND_READ_RECEIPT.ordinal(), clientModel.getClientID(), -1, originalSenderId + "#" + dbId);
            ClientModel.objectOutputStream.writeObject(rrReq);
            ClientModel.objectOutputStream.flush();
        } catch(IOException e) {
            e.printStackTrace(new PrintWriter(Config.errors));
            LogFileWriter.Log(Config.errors.toString());
        }
    }

    private void displayReceivedMessage(String sender, String message) {
        displayReceivedMessage(sender, message, System.currentTimeMillis(), -1);
    }

    private void displayReceivedMessage(String sender, String message, long tsMillis) {
        displayReceivedMessage(sender, message, tsMillis, -1);
    }

    private void displayReceivedMessage(String sender, String message, long tsMillis, int pmId) {
        insertDateSeparatorIfNeeded(tsMillis);
        JLabel label = new JLabel();
        if(pmId > 0) label.putClientProperty("pmId", pmId);
        String temp;
        if (message.length() <= 3)
            temp = "20px";
        else if (message.length() <= 51)
            temp = "auto";
        else
            temp = "300px";
        String timeStr = formatTime(tsMillis);
        String text = String.format("<html><div style=\"font-size:8px;text-align:left;\">%s</div>"
                + "<div style=\"width:%s;\"><p style=\"width:%s;word-break: break-all;\">%s</p>"
                + "<span style=\"font-size:9px;color:gray;float:right;\">%s</span></div></html>", sender, temp, temp, message, timeStr);
        label.setText(text);
        label.setBorder(leftBubble);
        addSelectionListeners(label);
        leftBubbleConstraints.gridy = i;
        jPanelChatWindow.add(label, leftBubbleConstraints);
        jPanelChatWindow.revalidate();
        jPanelChatWindow.repaint();
        i++;
        smartScrollToBottom();
    }

    private void logOut() {
        messageListener.isContinue = false;
        messageListener.interrupt();
        try {
            messageListener.join(3000);
        } catch (InterruptedException ie) { }
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

    private void updateScrollbarPosition() {
        jScrollPane.getVerticalScrollBar().setValue(jScrollPane.getVerticalScrollBar().getMaximum());
    }

    class MessageListener extends Thread {
        boolean isContinue = true;

        public void run() {
            while (isContinue) {
                try {
                    Response res = (Response) ClientModel.objectInputStream.readObject();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (res.getId() == Response.Type.P_MSG.ordinal()) {
                                String content = res.getContents();
                                if (content.startsWith("__pm_echo__")) {
                                    // echo from server - message was delivered
                                    String afterEcho = content.substring("__pm_echo__ ".length());
                                    // format: targetName dbId tempId##actualMessage||tsMillis
                                    String rest = afterEcho.substring(afterEcho.indexOf(" ") + 1).trim();
                                    String[] echoParts = rest.split(" ", 2);
                                    if(echoParts.length == 2) {
                                        try {
                                            int dbId = Integer.parseInt(echoParts[0].trim());
                                            String tempContent = echoParts[1].trim();
                                            if(tempContent.contains("##")) {
                                                int tempId = Integer.parseInt(tempContent.substring(0, tempContent.indexOf("##")).trim());
                                                JLabel label = sentMessages.get(tempId);
                                                if(label != null) {
                                                    label.putClientProperty("pmId", dbId);
                                                    dbIdToTempId.put(dbId, tempId);
                                                    updateMessageStatus(label, "delivered");
                                                }
                                            }
                                        } catch(NumberFormatException e) {
                                            // ignore
                                        }
                                    }
                                } else {
                                    String senderName = content.substring(0, content.indexOf(" "));
                                    String msgText = content.substring(content.indexOf(" ") + 1).trim();
                                    String dbIdStr = "";
                                    long tsMillis = System.currentTimeMillis();
                                    if(msgText.contains("||")) {
                                        try {
                                            tsMillis = Long.parseLong(msgText.substring(msgText.lastIndexOf("||") + 2));
                                        } catch(Exception e) { }
                                        msgText = msgText.substring(0, msgText.lastIndexOf("||"));
                                    }
                                    int recvPmId = -1;
                                    if(msgText.contains("##")) {
                                        String pmIdStr = msgText.substring(0, msgText.indexOf("##")).trim();
                                        msgText = msgText.substring(msgText.indexOf("##") + 2);
                                        try { recvPmId = Integer.parseInt(pmIdStr); } catch(NumberFormatException e) {}
                                    }
                                    displayReceivedMessage(senderName, msgText, tsMillis, recvPmId);
                                    if(recvPmId > 0) {
                                        sendReadReceipt(targetId, recvPmId);
                                    }
                                }
                            } else if (res.getId() == Response.Type.READ_RECEIPT.ordinal() && res.getSuccess()) {
                                // read receipt from the other user - content contains pmId
                                String rrContent = res.getContents();
                                try {
                                    int pmId = Integer.parseInt(rrContent.trim());
                                    Integer tempIdObj = dbIdToTempId.get(pmId);
                                    if(tempIdObj != null) {
                                        JLabel label = sentMessages.get(tempIdObj);
                                        if(label != null) {
                                            updateMessageStatus(label, "read");
                                        }
                                    }
                                } catch(NumberFormatException e) {
                                    // ignore
                                }
                            } else if (res.getId() == Response.Type.FILE_MSG.ordinal()) {
                                String content = res.getContents();
                                if (content.startsWith("__file_echo__")) {
                                    // echo from server for a sent file
                                    String afterEcho = content.substring("__file_echo__ ".length());
                                    // format: targetName fileId fileName
                                    String[] echoParts = afterEcho.split(" ", 3);
                                    if (echoParts.length >= 3) {
                                        try {
                                            int fileId = Integer.parseInt(echoParts[1].trim());
                                            String fileName = echoParts[2].trim();
                                            JPanel panel = sentFilePanels.remove(fileName);
                                            if (panel != null) {
                                                filePanels.put(fileId, panel);
                                                JLabel textLabel = (JLabel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                                                String html = textLabel.getText();
                                                if (html.contains("&#10003;")) {
                                                    html = html.replace("&#10003;", "&#10003;&#10003;");
                                                } else {
                                                    html = html.replace("</span></html>", " &#10003;&#10003;</span></html>");
                                                }
                                                textLabel.setText(html);
                                                textLabel.revalidate();
                                                textLabel.repaint();
                                            }
                                        } catch (NumberFormatException e) { }
                                    }
                                } else {
                                    // received a file notification from someone - format: senderName||fileId||fileName||fileSize||mimeType
                                    // (no base64 data to avoid large response timeouts; client fetches via GET_FILE)
                                    String[] fileParts = content.split("\\|\\|", 5);
                                    if (fileParts.length >= 5) {
                                        final String senderName = fileParts[0].trim();
                                        final int fileId = Integer.parseInt(fileParts[1].trim());
                                        final String fileName = fileParts[2].trim();
                                        final long fileSize = Long.parseLong(fileParts[3].trim());
                                        final String mimeType = fileParts[4].trim();
                                        long now = System.currentTimeMillis();
                                        insertDateSeparatorIfNeeded(now);
                                        JPanel panel = createFileMessagePanel(fileName, fileSize, mimeType, null, now, false, true);
                                        addPanelToChat(panel, false);
                                        filePanels.put(fileId, panel);
                                        // request file data asynchronously so we never block reading the next response
                                        new Thread(() -> {
                                            try {
                                                Thread.sleep(200);
                                                Request getReq = new Request(Request.Type.GET_FILE.ordinal(), clientModel.getClientID(), -1, String.valueOf(fileId));
                                                ClientModel.objectOutputStream.writeObject(getReq);
                                                ClientModel.objectOutputStream.flush();
                                            } catch (Exception ex) {
                                                ex.printStackTrace(new PrintWriter(Config.errors));
                                                LogFileWriter.Log(Config.errors.toString());
                                            }
                                        }).start();
                                    }
                                }
                            } else if (res.getId() == Response.Type.FILE_DATA.ordinal()) {
                                String content = res.getContents();
                                if (res.getSuccess()) {
                                    // fileId||fileName||fileSize||mimeType||base64Data
                                    String[] fileParts = content.split("\\|\\|", 5);
                                    if (fileParts.length >= 5) {
                                        int fileId = Integer.parseInt(fileParts[0].trim());
                                        String fileName = fileParts[1].trim();
                                        long fileSize = Long.parseLong(fileParts[2].trim());
                                        String mimeType = fileParts[3].trim();
                                        String base64Data = fileParts[4].trim();
                                        byte[] data = Base64.getDecoder().decode(base64Data);
                                        JPanel panel = filePanels.get(fileId);
                                        if (panel != null) {
                                            updateFilePanelIcon(panel, mimeType, data, fileName, fileSize);
                                        }
                                    }
                                } else {
                                    // File not found - update placeholder to show error
                                    try {
                                        int fileId = Integer.parseInt(content.replaceAll("[^0-9]", ""));
                                        JPanel panel = filePanels.get(fileId);
                                        if (panel != null) {
                                            JLabel textLabel = (JLabel) ((BorderLayout) panel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
                                            String html = textLabel.getText();
                                            html = html.replace("loading...", "<span style='color:red;'>not found</span>");
                                            textLabel.setText(html);
                                            textLabel.revalidate();
                                            textLabel.repaint();
                                        }
                                    } catch (NumberFormatException e) { }
                                }
                            } else if (res.getId() == Response.Type.STATUS_MSG.ordinal()) {
                                String content = res.getContents();
                                if (content.equals("sv_exit_successful") || content.equals("sv_logout_successful")) {
                                    isContinue = false;
                                }
                            } else if (res.getId() == Response.Type.DELETE_CONFIRM.ordinal()) {
                                String content = res.getContents();
                                if(content != null && content.startsWith("peer_deleted:")) {
                                    String[] peerIds = content.substring("peer_deleted:".length()).split(",");
                                    java.util.HashSet<Integer> peerDeleted = new java.util.HashSet<>();
                                    for(String pidStr : peerIds) {
                                        try { peerDeleted.add(Integer.parseInt(pidStr.trim())); } catch(NumberFormatException e) {}
                                    }
                                    if(!peerDeleted.isEmpty()) {
                                        java.util.ArrayList<Component> toRemove = new java.util.ArrayList<>();
                                        for(int idx = 0; idx < jPanelChatWindow.getComponentCount(); idx++) {
                                            Component child = jPanelChatWindow.getComponent(idx);
                                            Object pmIdObj = (child instanceof JComponent) ? ((JComponent)child).getClientProperty("pmId") : null;
                                            if(pmIdObj instanceof Integer && peerDeleted.contains((Integer)pmIdObj)) {
                                                toRemove.add(child);
                                            }
                                        }
                                        for(Component c : toRemove) {
                                            jPanelChatWindow.remove(c);
                                        }
                                        if(!toRemove.isEmpty()) {
                                            jPanelChatWindow.revalidate();
                                            jPanelChatWindow.repaint();
                                            rebuildGridY();
                                        }
                                    }
                                }
                            }
                        }
                    });
                } catch (java.net.SocketTimeoutException e) {
                    // timeout is normal - just loop and check isContinue
                    continue;
                } catch (Exception e) {
                    if(!isContinue || Thread.currentThread().isInterrupted()) break;
                    e.printStackTrace(new PrintWriter(Config.errors));
                    LogFileWriter.Log(Config.errors.toString());
                    break;
                }
            }
        }
    }
}
