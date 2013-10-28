import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

public class ImageFrame extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	BufferedImage image;


    public ImageFrame(final BufferedImage image, final String title) {
    	this.setTitle(title);
        this.setSize(image.getWidth(), image.getHeight());
        this.setVisible(true);
    }
    
    public void updateImage(BufferedImage image)
    {
    	this.image = image;
    	this.repaint();
    }

    public void paint(Graphics g) {
    	g.drawImage(image, 0, 0, null);
    }
}