import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.nlogo.api.*;
import org.nlogo.nvm.*;
import org.nlogo.app.App;
import org.nlogo.lite.InterfaceComponent;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.ReporterTask;
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
	 * Runs the given command in this model safely.
	 * @param command
	 * @throws CompilerException 
	 */
	@Override
	public void command(final String command) throws CompilerException, ExecutionException, HaltException {
		runSafely(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				myWS.command(command);
				return null;
			}
		});
	}

	@Override
	public void command(final Context context, final CommandTask command, final Object[] args) throws ExtensionException {
		try {
			runSafely(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					LevelsModelComponent.super.command(context, command, args);
					return null;
				}
			});
		} catch (HaltException e) {
			// ignore
		} catch (ExecutionException e) {
			throw new ExtensionException(e);
		}
	}

	/**
	 * Runs the reporter in this model and returns the result safely.
	 * @param reporter
	 * @return
	 * @throws ExtensionException
	 */
	@Override
	public Object report (final String reporter) throws ExtensionException, CompilerException, ExecutionException, HaltException {
		return runSafely(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return myWS.report(reporter);
			}
		});
	}

	@Override
	public Object report(final Context context, final ReporterTask reporter, final Object[] args) throws ExtensionException {
		try {
			return runSafely(new Callable<Object>() {
				@Override
				public Object call() throws Exception {
					LevelsModelComponent.super.report(context, reporter, args);
					return null;
				}
			});
		} catch (HaltException e) {
			return null;
		} catch (ExecutionException e) {
			throw new ExtensionException(e);
		}
	}

	final public void kill() throws HaltException {
		// before we do anything, we need to check if this model has child-models.
		// If it does, we need to kill those too.
		if(usesLevelsSpace()) {
			Object theList = null;
			try {
				theList = report("ls:all-models");
			} catch (Exception e) {
				// Normally we'd want to bubble these up as ExtensionExceptions, but
				// can't because this is inherited
				throw new RuntimeException(e);
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
					command(theCommand);
				} catch (Exception e) {
					// Normally we'd want to bubble these up as ExtensionExceptions, but
					// can't because this is inherited
					throw new RuntimeException(e);
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
