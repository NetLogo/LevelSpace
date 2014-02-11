import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.nlogo.api.*;
import org.nlogo.app.App;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.Workspace;
import org.nlogo.nvm.Workspace.OutputDestination;

import javax.swing.*;

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
	
	public void command (String command) throws CompilerException, LogoException
	{
		myWS.command(command);
	}
	
	
	public void kill()
	{
		// before we do anything, we need to check if this model has child-models.
		// If it does, we need to kill those too.
		if(myWS.getExtensionManager().anyExtensionsLoaded()){
			// iterate through loaded extensions
			for (ClassManager cm : myWS.getExtensionManager().loadedExtensions()){
				// they are loaded in another classloader, so we have to do string check
				if("class LevelsSpace".equals(cm.getClass().toString())){
					// If it has a levelsspace extension loaded, get a list of all loaded models
					Object theList = null;
					try {
						try {// This may have to be run in runsafely
							theList = report("ls:all-models");
						} catch (LogoException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} catch (ExtensionException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (CompilerException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
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
								try {
									// We run these safely for all models, even though strictly speaking, headless models
									// don't need it.
									LevelsSpace.runSafely(App.app().workspace().world(), new Callable<Object>() {
										@Override
										public Object call() throws CompilerException, LogoException, ExtensionException {
											command(theCommand);
											return null;
										}
									});
								} catch (HaltException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
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

	public Object report (String varName) throws LogoException, ExtensionException, CompilerException
	{
		Object reportedValue = null;
		reportedValue = myWS.report(varName);
		return reportedValue;
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
