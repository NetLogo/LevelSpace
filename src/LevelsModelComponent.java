import java.awt.Component;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionException;
import org.nlogo.lite.InterfaceComponent;
import org.nlogo.window.InvalidVersionException;
import org.nlogo.window.SpeedSliderPanel;


public class LevelsModelComponent extends LevelsModelAbstract {

	final javax.swing.JFrame frame = new javax.swing.JFrame();
	final InterfaceComponent myWS = new InterfaceComponent(frame);	
	String name;
	String path;
	int levelsSpaceNumber;

	public LevelsModelComponent(final String path, final int levelsSpaceNumber)
	{
		this.levelsSpaceNumber = levelsSpaceNumber;
		// find the name of the model - it is the bit past the last dash
		int lastDashPosition = path.lastIndexOf("/") + 1;
		int lastDotPosition = path.lastIndexOf(".");
		name = path.substring(lastDashPosition, lastDotPosition);
		this.path = path;

		try {
			SwingUtilities.invokeAndWait(
					new Runnable() {
						public void run() {
							frame.add(myWS);
							frame.setVisible(true);
							try {
								myWS.open
								(path);
							} catch (IOException e) {
								e.printStackTrace();
								// TODO Auto-generated catch block
							} catch (InvalidVersionException e) {
								e.printStackTrace();
							}
							// get all components, find the speed slider, and hide it.
							Component[] c = myWS.workspace().viewWidget.controlStrip.getComponents();
							for (Component co : c){
								if (co instanceof SpeedSliderPanel){
									co.setVisible(false);
									((SpeedSliderPanel) co).setValue(0);
								}
							}
							frame.setTitle(name + " (LevelsSpace model-id: " + String.valueOf(levelsSpaceNumber) + ")");
							frame.pack();
							// Make sure that the model doesn't close if people accidentally click the close button
							frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
							// Adding window listener so that the model calls the method that removes it from
							// the extension if closed.
							frame.addWindowListener(new java.awt.event.WindowAdapter() {
							    @Override
							    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
							        if (JOptionPane.showConfirmDialog(frame, 
							            "Closing the window will not close the model but simply keep it running" +
							            " in the background. Are you sure you want to close the window?", "", 
							            JOptionPane.YES_NO_OPTION,
							            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
							        	frame.setVisible(false);
							        }
							    }
							});
						}});
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}


	/**
	 * Runs the given command in this model.
	 * WARNING: Not safe. Must be run via LevelsSpace.runSafely().
	 * See AppletPanel.command() for more information.
	 * @param command
	 * @throws CompilerException 
	 */
	public void command (String command) throws CompilerException
	{
		myWS.command(command);	
	}


	public void kill()
	{
		frame.dispose();
	}

	/**
	 * Runs the reporter in this model and returns the result
	 * WARNING: Not safe. Must be run via LevelsSpace.runSafely().
	 * See AppletPanel.report() for more information.g
	 * @param varName
	 * @return
	 * @throws ExtensionException 
	 */
	public Object report (String varName) throws ExtensionException, CompilerException
	{
		Object reportedValue = null;
		reportedValue = myWS.report(varName);
		return reportedValue;
	}

	public String getName()
	{
		return name;
	}

	public String getPath(){
		return path;
	}

	@Override
	void breathe() {
		myWS.workspace().breathe();
	}
	
	void setSpeed(double d){
		Component[] c = myWS.workspace().viewWidget.controlStrip.getComponents();
		for (Component co : c){
			if (co instanceof SpeedSliderPanel){
				((SpeedSliderPanel) co).setValue((int)d);
			}
		}

	}
	
	void showGUI(){
		frame.setVisible(true);
	}
	void hideGUI(){
		frame.setVisible(true);
	}
}
