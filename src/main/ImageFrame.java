package org.nlogo.ls;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class ImageFrame extends JFrame {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    BufferedImage image;
    JPanel panel;


    public ImageFrame(final BufferedImage image, final String title) {
        setTitle(title);
        panel = new JPanel() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            @Override
            public void paintComponent(Graphics g) {
                g.drawImage(getImage(), 0, 0, null);
            }
        };
        setContentPane(panel);
        panel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        this.pack();
        updateImage(image);
        setVisible(true);
    }

    public BufferedImage getImage() {
        return image;
    }

    public void updateImage(BufferedImage image) {
        this.image = image;
        panel.repaint();
        repaint();
    }
}
