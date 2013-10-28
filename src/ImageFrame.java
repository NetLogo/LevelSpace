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

        try {
			SwingUtilities.invokeAndWait(new Runnable() {
			    public void run() {
			        if (image != null) {
			            setSize(image.getWidth(), image.getHeight());
			        } else {
			            setSize(375, 375);
			        }
			        setTitle(title);
			        setVisible(true);
			    }
			});
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    
    
    }
    
    
    public void updateImage(BufferedImage image)
    {
    	this.image = image;
    	repaint();
    }

    public void paint(Graphics g) {
    	g.drawImage(image, 0, 0, null);
    }
}