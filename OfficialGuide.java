import javax.swing.JFrame;

public class OfficialGuide {
    private static String title = "FriendRun - Official full docs and guides";
    public static void main(String[] args) {
        JFrame frame = new JFrame(title);
        try {
            frame.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage("friendrun.png"));
        } catch (Exception ignored) {
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 700);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }
}
