import javax.swing.*;
import java.awt.*;

public class AdminPanel extends JFrame {

    public AdminPanel() {

        setTitle("Admin Panel");

        JTextArea area =
                new JTextArea();

        area.setEditable(false);

        area.setText(
                "ADMIN FEATURES\n\n" +
                "1. View Users\n" +
                "2. Delete Users\n"
        );

        add(
                new JScrollPane(area),
                BorderLayout.CENTER
        );

        setSize(300, 250);

        setVisible(true);
    }
}