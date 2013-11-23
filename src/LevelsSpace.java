
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.nlogo.agent.Agent;
import org.nlogo.agent.AgentSet;
import org.nlogo.api.Argument;
import org.nlogo.api.CompilerException;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.ExtensionManager;
import org.nlogo.api.ExtensionObject;
import org.nlogo.api.ImportErrorHandler;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.PrimitiveManager;
import org.nlogo.api.Syntax;
import org.nlogo.api.World;
import org.nlogo.app.App;
import org.nlogo.nvm.HaltException;
import org.nlogo.window.SpeedSliderPanel;
import org.nlogo.window.ViewUpdatePanel;



public class LevelsSpace implements org.nlogo.api.ClassManager {

	final static HashMap<Integer, LevelsModelAbstract> myModels = new HashMap<Integer, LevelsModelAbstract>();

	// counter for keeping track of new models
	static int modelCounter = 0;


	@Override
	public void load(PrimitiveManager primitiveManager) throws ExtensionException {
		// this allows you to run a command in another model
		primitiveManager.addPrimitive("ask", new RunCommand());
		// this loads a model
		primitiveManager.addPrimitive("load-headless-model", new LoadHeadlessModel());
		primitiveManager.addPrimitive("load-gui-model", new LoadGUIModel());
		// this returns the name (and path) of a model 
		primitiveManager.addPrimitive("model-name", new ModelName());
		// this closes a model
		primitiveManager.addPrimitive("close-model", new CloseModel());
		// this returns a list of model IDs
		primitiveManager.addPrimitive("all-models", new AllModels());
		// this returns a boolean - does the model exist
		primitiveManager.addPrimitive("model-exists?", new ModelExists());
		// this resets the the levelsspace extension
		primitiveManager.addPrimitive("reset", new Reset());
		// this returns the last model id number
		primitiveManager.addPrimitive("last-model-id", new LastModel());
		// this returns whatever it is asked to report from a model
		primitiveManager.addPrimitive("report", new Report());	
		// this returns just the path of a model
		primitiveManager.addPrimitive("model-path", new ModelPath());
		// These should probably go.
		primitiveManager.addPrimitive("open-image-frame", new OpenImageFrame());
		primitiveManager.addPrimitive("display", new UpdateView());
		primitiveManager.addPrimitive("show-gui", new ShowGUI());

		modelCounter = 0;
		
		// Attaching a ChangeEventListener to the main model's speed slider so we can 
		// update child models' speed sliders at the same time.
		Component[] c = App.app().tabs().interfaceTab().getComponents();
		for (Component co : c){
			Component[] c2 = ((Container) co).getComponents();
			for (Component co2 : c2){
				if (co2 instanceof ViewUpdatePanel){
					Component[] c3 = ((Container) co2).getComponents();
					for(Component co3 : c3){
						if (co3 instanceof SpeedSliderPanel){
							SpeedSliderPanel speedSliderPanel = (SpeedSliderPanel)co3;
							JSlider slider = (JSlider)speedSliderPanel.getComponents()[0];
							slider.addChangeListener(new ChangeListener(){
								@Override
								public void stateChanged(ChangeEvent arg0) {
									updateChildModelsSpeed();

								}
							});
						}
					}
				}
			}
		}
	}


	@Override
	public void unload(ExtensionManager arg0) throws ExtensionException {
		// iterate through models and kill them
		for (LevelsModelAbstract model : myModels.values()) {
			model.kill();
		}
		myModels.clear();
	}


	public static class LoadHeadlessModel extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					// we take in int[] {number, string} 
					new int[] { Syntax.StringType()});
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// saving current modelCounter as that will be the hashtable key to the 
			// model we are making
			// make a new LevelsModel
			String modelURL = args[0].getString();
			LevelsModelHeadless aModel = null;
			try {
				aModel = new LevelsModelHeadless(modelURL, modelCounter);
			} catch (IOException e) {
				throw new ExtensionException ("There was no .nlogo file at the path: \"" + modelURL + "\"");
			} catch (CompilerException e) {
				throw new ExtensionException (modelURL + " did not compile properly. There is probably something wrong " +
						"with its code.");
			} catch (LogoException e) {
			}

			myModels.put(modelCounter, aModel);
			// add to models counter
			modelCounter ++;
			// stop up, take a breath. You will be okay.
			App.app().workspace().breathe();
		}
	}
	public static class LoadGUIModel extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					// we take in int[] {number, string} 
					new int[] { Syntax.StringType()});
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// saving current modelCounter as that will be the hashtable key to the 
			// model we are making
			// make a new LevelsModel
			String modelURL = args[0].getString();

			LevelsModelComponent aModel = new LevelsModelComponent(modelURL, modelCounter);
			// add it to models
			myModels.put(modelCounter, aModel);
			// add to models counter
			modelCounter ++;
			// stop up, take a breath. You will be okay.
			App.app().workspace().breathe();
		}
	}

	public static class Reset extends DefaultCommand {
		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// resets the counter
			modelCounter = 0;
			// stop all running models
			for (LevelsModelAbstract model : myModels.values()) {
				model.kill();
			}
			myModels.clear();
		}
	}

	public static class RunCommand extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					new int[] { Syntax.NumberType(), Syntax.StringType() });	        
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// get model number from args
			int modelNumber = (int) args[0].getDoubleValue();
			// get the command to run
			final String command = args[1].getString();
			// find the model. if it exists, run the command 
			if(myModels.containsKey(modelNumber))
			{
				final LevelsModelAbstract aModel = myModels.get(modelNumber);

				try {
					LevelsSpace.runSafely(context.getAgent().world(), new Callable<Object>() {
						@Override
						public Object call() throws CompilerException, LogoException, ExtensionException {
							aModel.command(command);
							return null;
						}
					});
				} catch (ExecutionException e) {
					throw new ExtensionException("\"" + command + "\" is not defined in the model with ID " + modelNumber);
				}
			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}

			App.app().workspace().breathe();
		}
	}

	public static class CloseModel extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					new int[] { Syntax.NumberType() });	        
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// get model number from args
			int modelNumber = (int) args[0].getDoubleValue();
			// find the model. if it exists, kill it 
			if(myModels.containsKey(modelNumber))
			{
				LevelsModelAbstract aModel = myModels.get(modelNumber);
				aModel.kill();
			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}			
			// and remove it from the hashtable
			myModels.remove(modelNumber);
		}
	}	

	public static class UpdateView extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					new int[] { Syntax.NumberType() });	        
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// get model number from args
			int modelNumber = (int) args[0].getDoubleValue();
			// find the model. if it exists, update graphics 
			if(myModels.containsKey(modelNumber))
			{
				if (myModels.get(modelNumber) instanceof LevelsModelHeadless){
					LevelsModelHeadless aModel = (LevelsModelHeadless) myModels.get(modelNumber);
					aModel.updateView();
				}

			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}


		}
	}	

	public static class OpenImageFrame extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					new int[] { Syntax.NumberType() });	        
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// get model number from args
			int modelNumber = (int) args[0].getDoubleValue();
			// find the model. if it exists, run the command 
			if(myModels.containsKey(modelNumber))
			{
				LevelsModelHeadless aModel = (LevelsModelHeadless)myModels.get(modelNumber);
				aModel.createImageFrame();
				aModel.myWS.breathe();
				App.app().workspace().breathe();
			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}


		}
	}
	public static class ShowGUI extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					new int[] { Syntax.NumberType() });	        
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// get model number from args
			int modelNumber = (int) args[0].getDoubleValue();
			// find the model. if it exists, run the command 
			if(myModels.containsKey(modelNumber))
			{
				if (myModels.get(modelNumber) instanceof LevelsModelComponent){
					LevelsModelComponent aModel = (LevelsModelComponent)myModels.get(modelNumber); 
					aModel.showGUI();
				}
				else {
					throw new ExtensionException("You can only show the GUI of GUI models. Model with ID " +
							modelNumber + " is not a GUI model");
				}
				App.app().workspace().breathe();
			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}


		}
	}
	// this returns the path of the model
	public static class ModelName extends DefaultReporter{
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.NumberType()},
					Syntax.StringType());

		}
		public Object report(Argument[] args, Context context) throws ExtensionException{
			String modelName = new String();
			// get model number
			int modelNumber = -1;
			try {
				modelNumber = args[0].getIntValue();
			} catch (ExtensionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LogoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(myModels.containsKey(modelNumber)){
				modelName = myModels.get(modelNumber).getName();
			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}
			return modelName;

		}

	}
	// this returns the path of the model
	public static class ModelPath extends DefaultReporter{
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[] {Syntax.NumberType()},
					Syntax.StringType());

		}
		public Object report(Argument[] args, Context context) throws ExtensionException{
			String modelName = new String();
			// get model number
			int modelNumber = -1;
			try {
				modelNumber = args[0].getIntValue();
			} catch (ExtensionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LogoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(myModels.containsKey(modelNumber)){
				modelName = myModels.get(modelNumber).getPath();
			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}

			return modelName;

		}

	}

	/*
	 * This primitive returns the last created model number
	 */
	public static class LastModel extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					// no parameters 
					new int[] {},
					// and return a number
					Syntax.NumberType());
		}

		public Double report(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {

			return (double) modelCounter - 1;

		}
	}

	public static class Report extends DefaultReporter{
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					new int[]{ Syntax.NumberType(), Syntax.StringType() },
					Syntax.WildcardType());
		}

		public Object report(Argument args[], Context context) throws ExtensionException,  LogoException{
			int modelNumber = (int) args[0].getDoubleValue();
			// get var name
			final String varName = args[1].getString();
			// find the model. if it exists, update graphics 
			if(myModels.containsKey(modelNumber))
			{
				final LevelsModelAbstract aModel = myModels.get(modelNumber);
				try {
					return LevelsSpace.runSafely(context.getAgent().world(), new Callable<Object>() {
						@Override
						public Object call() throws Exception {
							Object returnValue = aModel.report(varName);
							if (returnValue instanceof Agent)
							{
								throw new ExtensionException("You cannot report turtles, patches, or links. If you want to do something" +
										"with turtles, patches, or links, use the ls:ask instead.");
							} 
							else if (returnValue instanceof AgentSet){
								throw new ExtensionException("You cannot report turtle-, patch-, or linksets. If you want to do something" +
										"with turtlesets, patchsets, or linkset, use the ls:ask instead.");							
							}
							else if (returnValue instanceof LogoList){
								checkAgentsAndSets((LogoList)returnValue);								
							}

							return returnValue;
						}
					});
				} catch (ExecutionException e) {
					throw new RuntimeException("The reporter \'" + varName + "\' does not exist in the model with ID " + modelNumber);
				}
			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}

		}

		private void checkAgentsAndSets(LogoList ll) throws ExtensionException{
			for (Object o : ll){
				if (o instanceof Agent || o instanceof AgentSet){
					throw new ExtensionException("You cannot report agents or agentsets.");
				}
				if(o instanceof LogoList){
					checkAgentsAndSets((LogoList)o);
				}
			}

		}
	}

	public static class ModelExists extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					// we take in int[] {modelNumber, varName} 
					new int[] { Syntax.NumberType() },
					// and return a number
					Syntax.BooleanType());	        
		}

		public Object report(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// get model number from args
			int modelNumber = (int) args[0].getDoubleValue();

			// find the model. if it exists, update graphics 
			if(myModels.containsKey(modelNumber))
			{
				return true;
			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}

		}
	}


	public static class AllModels extends DefaultReporter {

		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					// no parameters 
					new int[] {},
					// and return a logolist
					Syntax.ListType());	        
		}		

		// returns a logo list with all model numbers
		public Object report(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			LogoListBuilder myLLB = new LogoListBuilder();

			for (Integer id :  myModels.keySet()) {
				myLLB.add(new Double(id));
			}
			return myLLB.toLogoList();
		}

	}

	@Override
	public List<String> additionalJars() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearAll() {
		// TODO Auto-generated method stub

	}

	@Override
	public StringBuilder exportWorld() {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder();
		return sb;
	}

	@Override
	public void importWorld(List<String[]> arg0, ExtensionManager arg1,
			ImportErrorHandler arg2) throws ExtensionException {
		// TODO Auto-generated method stub

	}
	@Override
	public ExtensionObject readExtensionObject(ExtensionManager arg0,
			String arg1, String arg2) throws ExtensionException,
			CompilerException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void runOnce(ExtensionManager arg0) throws ExtensionException {

	}

	// Probably only want a single job to run at a time.
	private static Executor safeExecutor = Executors.newSingleThreadExecutor();
	/**
	 * Runs the given callable such that it doesn't create a deadlock between
	 * the AWT event thread and the JobThread. It does this using a similar
	 * technique as ThreadUtils.waitForResponse().
	 * @param world The world to synchronize on. Should be the main model's world.
	 * @param callable What to run.
	 * @return
	 */
	public static <T> T runSafely(final World world, final Callable<T> callable) throws HaltException, ExecutionException {
		final FutureTask<T> reporterTask = new FutureTask<T>(new Callable<T>() {
			@Override
			public T call() throws Exception {
				T result = callable.call();
				synchronized (world) {
					world.notify();
				}
				return result;
			}
		});
		safeExecutor.execute(reporterTask);
		while (!reporterTask.isDone()) {
			synchronized (world) {
				try {
					world.wait(50);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new HaltException(false);
				}

			}
		}
		try {
			return reporterTask.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new HaltException(false);
		}
	}

	public static void killClosedModel(int levelsSpaceNumber){
		LevelsModelAbstract aModel = myModels.get(levelsSpaceNumber);
		myModels.remove(levelsSpaceNumber);
		aModel.kill();
	}

	void updateChildModelsSpeed(){
		double theSpeed = App.app().workspace().speedSliderPosition();
		for (LevelsModelAbstract model : myModels.values()){
			model.setSpeed(theSpeed);

		}
	}

}
