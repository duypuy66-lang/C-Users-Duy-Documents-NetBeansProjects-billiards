package billiards;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Parallel Billiards Simulation");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            GamePanel panel = new GamePanel(1000, 600);
            f.getContentPane().add(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            panel.start(); // start simulation
        });
    }
}
