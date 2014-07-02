import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.app.App;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.ReporterTask;
import org.nlogo.nvm.Workspace;
import org.nlogo.nvm.Workspace.OutputDestination;

public class LevelsModelHeadless extends LevelsModelAbstract {
	
	HeadlessWorkspace myWS;
	ImageFrame frame;
	String name;
	String path;
	int levelsSpaceNumber;
	
	public LevelsModelHeadless(String path, final int levelsSpaceNumber) throws IOException, CompilerException, LogoException {
		this.levelsSpaceNumber = levelsSpaceNumber;		
        // find the name of the model - it is the bit past the last dash
        int lastDashPosition = path.lastIndexOf("/") + 1;
        name = path.substring(lastDashPosition);
        this.path = path;
				
		// make a new headless workspace
		myWS = HeadlessWorkspace.newInstance();
		myWS.open(path);
	}

	private void ensureImageFrame() {
		if (frame == null) {
			try {
				SwingUtilities.invokeAndWait(new Runnable() {
					@Override
					public void run() {
						// get an image from the model so we know how big it is
						final BufferedImage bi = myWS.exportView();
						// create a new image frame to show what's going on in the model
						// send it the image to set the size correctly
						final String aTitle = name.concat(" (LevelsSpace Model No. ").concat(Integer.toString(levelsSpaceNumber)).concat(")");
						frame = new ImageFrame(bi, aTitle);
					}
				});
			} catch (Exception e) {
				// Yes this is bad practice. I'm sorry. Deal with it.
				throw new RuntimeException(e);
			}
		}
	}


	public void updateView()
	{
		if (frame != null && frame.isVisible()){
			// get the image from the workspace
			BufferedImage bi = myWS.exportView();
			// update the frame

			frame.updateImage(bi);
		}

	}
	
	public void removeImageFrame(){
		frame.dispose();
		frame = null;
	}
	
	public void command (final String command) throws ExtensionException {
		if (usesLevelsSpace()) {
			try {
				runSafely(new Callable<Object>() {
					@Override
					public Object call() throws ExtensionException {
						try {
							myWS.command(command);
						} catch (LogoException e) {
							throw ErrorUtils.handle(LevelsModelHeadless.this, command, e);
						} catch (CompilerException e) {
							throw ErrorUtils.handle(LevelsModelHeadless.this, command, e);
						}
						return null;
					}
				});
			} catch (HaltException e) {
				// okay
			}
		} else {
			try {
				myWS.command(command);
			} catch (LogoException e) {
				throw ErrorUtils.handle(LevelsModelHeadless.this, command, e);
			} catch (CompilerException e) {
				throw ErrorUtils.handle(LevelsModelHeadless.this, command, e);
			}
		}
	}

	@Override
	public void command(final Context context, final CommandTask command, final Object[] args) throws ExtensionException {
		if (usesLevelsSpace()) {
			try {
				runSafely(new Callable<Object>() {
					@Override
					public Object call() throws ExtensionException {
						LevelsModelHeadless.super.command(context, command, args);
						return null;
					}
				});
			} catch (HaltException e) {
				// okay, halted
			}
		} else {
			super.command(context, command, args);
		}
	}

	public Object report (final String reporter) throws ExtensionException {
		if (usesLevelsSpace()) {
			try {
				return runSafely(new Callable<Object>() {
					@Override
					public Object call() throws ExtensionException {
						try {
							return myWS.report(reporter);
						} catch (LogoException e) {
							throw ErrorUtils.handle(LevelsModelHeadless.this, reporter, e);
						} catch (CompilerException e) {
							throw ErrorUtils.handle(LevelsModelHeadless.this, reporter, e);
						}
					}
				});
			} catch (HaltException e) {
				// okay
				return null;
			}
		} else {
			try {
				return myWS.report(reporter);
			} catch (LogoException e) {
				throw ErrorUtils.handle(LevelsModelHeadless.this, reporter, e);
			} catch (CompilerException e) {
				throw ErrorUtils.handle(LevelsModelHeadless.this, reporter, e);
			}
		}
	}

	@Override
	public Object report(final Context context, final ReporterTask reporter, final Object[] args) throws ExtensionException {
		if (usesLevelsSpace()) {
			try {
				return runSafely(new Callable<Object>() {
					@Override
					public Object call() throws ExtensionException {
						return LevelsModelHeadless.super.report(context, reporter, args);
					}
				});
			} catch (HaltException e) {
				// Okay, halted
				return null;
			}
		} else {
			return super.report(context, reporter, args);
		}
	}

	public void kill() {
		if(usesLevelsSpace()){
			// If it has a levelsspace extension loaded, get a list of all loaded models
			Object theList;
			try {
				theList = report("ls:all-models");
			} catch (Exception e) {
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
					// Would be better to use ExtensionException, but can't here
					throw new RuntimeException(e);
				}
			}
		}

		// then close down this model
		if (frame != null){
			frame.dispose();
		}
		try {
			myWS.dispose();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void halt(){
		myWS.halt();
	}

	@Override
	public Workspace workspace() {
		return myWS;
	}

	public String getName()
	{
		return name;
	}

	public String getPath() {
		// TODO Auto-generated method stub
		return path;
	}

	@Override
	public void breathe() {
		myWS.breathe();
		
	}

	@Override
	JFrame frame() {
		return frame;
	}

	@Override
	public void show() {
		ensureImageFrame();
		super.show();
		updateView();
	}

	@Override
	public void hide() {
		if (frame != null) {
			super.hide();
		}
	}

	public void setSpeed(double d){
		
	}	
}
