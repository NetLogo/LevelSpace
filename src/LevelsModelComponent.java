import java.awt.Component;

import javax.swing.SwingUtilities;

import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionException;
import org.nlogo.lite.InterfaceComponent;
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
							}
							catch(Exception ex) {
								ex.printStackTrace();
							}
							// get all components, find the speed slider, and hide it.
							Component[] c = myWS.workspace().viewWidget.controlStrip.getComponents();
							for (Component co : c){
								if (co instanceof SpeedSliderPanel){
									co.setVisible(false);
								}
							}
							frame.setTitle(name + " (LevelsSpace model-id: " + String.valueOf(levelsSpaceNumber) + ")");
							frame.pack();
							// Set speed slider to 110, so that child models never throttle their parents
							myWS.workspace().speedSliderPosition(110);
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
	public Object report (String varName) throws ExtensionException
	{
		Object reportedValue = null;
		try {
			reportedValue = myWS.report(varName);
		} catch (CompilerException e) {
			// TODO Auto-generated catch block
			throw new ExtensionException("That reporter does not exist in this child model.");
		}
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
}
