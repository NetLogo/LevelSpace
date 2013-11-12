import java.awt.Component;

import javax.swing.SwingUtilities;

import org.nlogo.api.CompilerException;
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
							Component[] c = myWS.workspace().viewWidget.controlStrip.getComponents();
							for (Component co : c){
								if (co instanceof SpeedSliderPanel){
									co.setVisible(false);
								}
							}
							frame.setTitle("LevelsSpace model no. " + String.valueOf(levelsSpaceNumber));
							frame.pack();
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
	 */
	public void command (String command)
	{
		try {
			myWS.command(command);
		} catch (CompilerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
	 */
	public Object report (String varName)
	{
		Object reportedValue = null;
		try {
			reportedValue = myWS.report(varName);
		} catch (CompilerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		// TODO Auto-generated method stub

	}




}
