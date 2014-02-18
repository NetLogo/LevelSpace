import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.nlogo.api.ClassManager;
import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.app.App;
import org.nlogo.lite.InterfaceComponent;
import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.Workspace;
import org.nlogo.nvm.Workspace.OutputDestination;
import org.nlogo.window.GUIWorkspace;
import org.nlogo.window.SpeedSliderPanel;
import org.nlogo.window.ThreadUtils;


public class LevelsModelComponent extends LevelsModelAbstract {

	final javax.swing.JFrame frame = new javax.swing.JFrame();
	final InterfaceComponent myWS = new InterfaceComponent(frame);	
	String name;
	String path;
	final int levelsSpaceNumber;
	LevelsSpace myLS;

	public LevelsModelComponent(final String path, final int levelsSpaceNumber) throws InterruptedException, InvocationTargetException, ExtensionException 
	{
		this.levelsSpaceNumber = levelsSpaceNumber;
		// find the name of the model - it is the bit past the last dash
		this.path = path;

		final Exception[] ex = new Exception[] { null };

		SwingUtilities.invokeAndWait(
				new Runnable() {
					public void run() {					
						frame.add(myWS);
						frame.setVisible(true);
						try {
							myWS.open
							(path);
						} catch (Exception e) {
							ex[0] = e;
						}
						// get all components, find the speed slider, and hide it.
						Component[] c = myWS.workspace().viewWidget.controlStrip.getComponents();
						for (Component co : c){
							if (co instanceof SpeedSliderPanel){
								co.setVisible(false);
								((SpeedSliderPanel) co).setValue(0);
							}
						}
						name = myWS.workspace().modelNameForDisplay();
						frame.setTitle(name + " (LevelsSpace model-id: " + String.valueOf(levelsSpaceNumber) + ")");
						frame.pack();
						// Make sure that the model doesn't close if people accidentally click the close button
						frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
						// Adding window listener so that the model calls the method that removes it from
						// the extension if closed.
						frame.addWindowListener(new java.awt.event.WindowAdapter() {
							@Override
							public void windowClosing(java.awt.event.WindowEvent windowEvent) {
								Object[] options = {"Close Model", "Run in Background", "Cancel"};
								int n = JOptionPane.showOptionDialog(frame,
										"Close the model, run it in the background, or do nothing?",
										null, JOptionPane.YES_NO_CANCEL_OPTION,
										JOptionPane.QUESTION_MESSAGE,
										null,
										options,
										options[2]);	
								switch (n){
								case 0 : try {
										kill();
										App.app().workspace().breathe();	
									} catch (HaltException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								break;
								case 1 : hide();

								}


							}
						});
					}});
		if (ex[0] != null){
			frame.dispose();
			Exception e = ex[0];
			throw new ExtensionException(e.getMessage());
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


	final public void kill() throws HaltException {
		// before we do anything, we need to check if this model has child-models.
		// If it does, we need to kill those too.
		if(myWS.workspace().getExtensionManager().anyExtensionsLoaded()){
			// iterate through loaded extensions
			for (ClassManager cm : myWS.workspace().getExtensionManager().loadedExtensions()){
				// they are loaded in another classloader, so we have to do string check
				if("class LevelsSpace".equals(cm.getClass().toString())){
					// OK, what if we do it in a way less clever way, and just get a LogoList of numbers
					// and then runsafely(command()) the model to kill them?
					Object theList = null;
					
					
					try {
						theList = 	LevelsSpace.runSafely(App.app().workspace().world(), new Callable<Object>() {
							@Override
							public Object call() throws CompilerException, LogoException, ExtensionException {
								return report("ls:all-models");
							}
						});
					} catch (ExecutionException e) {
						try {
							throw new ExtensionException("Something went wrong when closing down the model");
						} catch (ExtensionException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}	
					LogoList theLogoList = (LogoList)theList;
					for (Object theIndex : theLogoList.toArray()){
						final String theCommand = "ls:close-model " + String.valueOf(Math.round(Float.valueOf(theIndex.toString())));
						try {
							App.app().workspace().outputObject(theCommand, null, true, true, OutputDestination.NORMAL);
						} catch (LogoException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						try {
							try {
								LevelsSpace.runSafely(App.app().workspace().world(), new Callable<Object>() {
									@Override
									public Object call() throws CompilerException, LogoException, ExtensionException {
										command(theCommand);
										return null;
									}
								});
							} catch (ExecutionException e) {
								try {
									throw new ExtensionException("Something went wrong when closing down the model");
								} catch (ExtensionException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}		
							
						} catch (NumberFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}					
				}

			}
		}

		killJobThread();
		killLifeguard();

		SwingUtilities.invokeLater(new Runnable(){
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				frame.dispose();
			}
			
		});
	}

	private void killJobThread() {
		try {
			((GUIWorkspace) workspace()).jobManager.die();
		} catch (InterruptedException e) {
			// we can safely ignore this I think
		}
	}

	private void killLifeguard() {
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.getName().equals("Lifeguard")) {
				try {
					Field outerField = thread.getClass().getDeclaredField("this$0");
					outerField.setAccessible(true);
					Object outer = outerField.get(thread);
					if (outer == workspace()) {
						thread.interrupt();
						thread.join();
					}
				} catch (NoSuchFieldException e) {
					throw new RuntimeException("There is a bug in LevelSpace! Please report this.", e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("There is a bug in LevelSpace! Please report this.", e);
				} catch (InterruptedException e) {
					throw new RuntimeException("There is a bug in LevelSpace! Please report this.", e);
				}

			}
		}

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
	public void breathe() {
		myWS.workspace().breathe();
	}

	public void setSpeed(double d){
		Component[] c = myWS.workspace().viewWidget.controlStrip.getComponents();
		for (Component co : c){
			if (co instanceof SpeedSliderPanel){
				((SpeedSliderPanel) co).setValue((int)d);
			}
		}

	}
	public void halt(){
		myWS.workspace().halt();
	}

	@Override
	public Workspace workspace() {
		return myWS.workspace();
	}

	@Override
	JFrame frame() {
		return frame;
	}
}
