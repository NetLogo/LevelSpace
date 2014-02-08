import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class ImageFrame extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	BufferedImage image;


    public ImageFrame(final BufferedImage image, final String title) {
		setTitle(title);
		setSize(image.getWidth(), image.getHeight());
		setVisible(true);
		updateImage(image);
    }
    
    
    public void updateImage(BufferedImage image) {
    	this.image = image;
    	repaint();
    }

    public void paint(Graphics g) {
    	g.drawImage(image, 0, 0, null);
    }
}