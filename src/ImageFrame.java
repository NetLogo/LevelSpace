import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;

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