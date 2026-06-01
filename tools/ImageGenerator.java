import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

public class ImageGenerator {
    private static Color[] avatarColors = {
        new Color(52, 152, 219), new Color(46, 204, 113), new Color(155, 89, 182),
        new Color(231, 76, 60), new Color(241, 196, 15), new Color(230, 126, 34),
        new Color(26, 188, 156), new Color(52, 73, 94), new Color(149, 165, 166),
        new Color(243, 156, 18), new Color(142, 68, 173), new Color(22, 160, 133)
    };
    private static String[] avatarLetters = {
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"
    };

    public static void main(String[] args) throws Exception {
        String resPath = args.length > 0 ? args[0] : "res";

        for (int i = 0; i < 12; i++) {
            BufferedImage img = createAvatar(avatarColors[i], avatarLetters[i], 120);
            ImageIO.write(img, "png", new File(resPath, "avatar_" + i + ".png"));
            System.out.println("Created " + resPath + "/avatar_" + i + ".png");
        }

        BufferedImage sendIcon = createSendIcon(40);
        ImageIO.write(sendIcon, "png", new File(resPath, "send_icon.png"));
        System.out.println("Created " + resPath + "/send_icon.png");
    }

    private static BufferedImage createAvatar(Color bg, String letter, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillOval(0, 0, size, size);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, size / 2));
        FontMetrics fm = g2.getFontMetrics();
        int lw = fm.stringWidth(letter);
        int lh = fm.getAscent();
        g2.drawString(letter, (size - lw) / 2, (size + lh) / 2 - 4);
        g2.dispose();
        return img;
    }

    private static BufferedImage createSendIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        int[] xPoints = {0, size - 2, size - 2, 0, 3};
        int[] yPoints = {0, size/2 - 4, size/2 + 4, size - 1, size/2};
        g2.fillPolygon(xPoints, yPoints, 5);
        g2.dispose();
        return img;
    }
}
