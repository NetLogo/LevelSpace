
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.MenuElement;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.nlogo.agent.Agent;
import org.nlogo.agent.AgentSet;
import org.nlogo.api.*;
import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.app.App;
import org.nlogo.app.ToolsMenu;
import org.nlogo.nvm.*;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.ReporterTask;
import org.nlogo.nvm.Workspace.OutputDestination;
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
		// returns the path of the current model; useful for opening child models in same directory
		primitiveManager.addPrimitive("model-directory", new ModelDirectory());
		// These should probably go.
		primitiveManager.addPrimitive("display", new UpdateView());
		primitiveManager.addPrimitive("show", new Show());
		primitiveManager.addPrimitive("hide", new Hide());
		primitiveManager.addPrimitive("run", new RunTask());
		primitiveManager.addPrimitive("runresult", new RunReportTask());

		modelCounter = 0;
		
		// Adding event listener to Halt for halting child models
		MenuElement[] elements = App.app().frame().getJMenuBar().getSubElements();
		for (MenuElement e : elements){
			if (e instanceof ToolsMenu){
				ToolsMenu tm = (ToolsMenu)e;
				JMenuItem item = tm.getItem(0);
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						haltChildModels(myModels);
					}
				});
			}
		}	
				
		// Attaching a ChangeEventLister to the main model's speed slider so we can 
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

	public static LevelsModelAbstract getModel(int id) throws ExtensionException {
		if (myModels.containsKey(id)) {
			return myModels.get(id);
		} else {
			throw new ExtensionException("There is no model with ID " + id);
		}
	}


	@Override
	public void unload(ExtensionManager arg0) throws ExtensionException {
		// iterate through models and kill them
//		for (LevelsModelAbstract model : myModels.values()) {
//			try {
//				model.kill();
//				App.app().workspace().breathe();
//			} catch (HaltException e) {
//				// TODO Auto-generated catch block
//				throw new ExtensionException("Killing the model failed for some reason");
//			}
//		}
//		myModels.clear();
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
						"with its code. Exception said" + e.getMessage());
			}
			updateChildModelSpeed(aModel);
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
			// Get the path for the model
			String modelURL = args[0].getString();
			LevelsModelComponent aModel = null;
			try {
				aModel = new LevelsModelComponent(modelURL, modelCounter);
				updateChildModelSpeed(aModel);
				// add it to models
				myModels.put(modelCounter, aModel);
				// add to models counter
				modelCounter ++;
			} catch (InterruptedException e) {
				new ExtensionException("Loading " + modelURL + " failed with this message: " + e.getMessage());
			} catch (InvocationTargetException e) {
				new ExtensionException("Loading " + modelURL + " failed with this message: " + e.getMessage());
			} 
			// stop up, take a breath. You will be okay.
			App.app().workspace().breathe();
		}
	}

	public static class Reset extends DefaultCommand {
		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// resets the counter
			modelCounter = 0;
			
			for (LevelsModelAbstract model : myModels.values()){
				model.kill();
			}
			myModels.clear();
		}
	}

	public static class RunCommand extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					new int[]{Syntax.NumberType(), Syntax.StringType()});
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// get model number from args
			int modelNumber = (int) args[0].getDoubleValue();
			// get the command to run
			final org.nlogo.nvm.Context nvmContext = ((ExtensionContext) context).nvmContext();
			final LevelsModelAbstract aModel = getModel(modelNumber);
			final String command = args[1].getString();

			if (aModel instanceof LevelsModelHeadless && !aModel.usesLevelsSpace()) {
				try {
					aModel.command(command);
				} catch (CompilerException e) {
					throw new ExtensionException(e);
				}
			} else {
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
			App.app().workspace().breathe();
		}
	}

	public static class RunTask extends DefaultCommand {
		public Syntax getSyntax(){
			return Syntax.commandSyntax(
					new int[]{Syntax.NumberType(),
							Syntax.CommandTaskType(),
							Syntax.RepeatableType() | Syntax.WildcardType()},
					2);
		}

		@Override
		public void perform(Argument[] args, Context context) throws LogoException, ExtensionException {
			LevelsModelAbstract model = getModel(args[0].getIntValue());
			CommandTask task = (CommandTask) args[1].getCommandTask();
			if (task.procedure().code.length > 0 && task.procedure().code[0].workspace != model.workspace()) {
				throw new ExtensionException("You may only run tasks in the model they were created in.");
			}
			int n = args.length - 2;
			Object[] actuals = new Object[n];
			for (int i = 0; i < n; i++) {
				actuals[i] = args[i+2].get();
			}
			org.nlogo.nvm.Context nvmContext = ((ExtensionContext) context).nvmContext();
			Agent oldAgent = nvmContext.agent;
			nvmContext.agent = model.workspace().world().observer();
			nvmContext.agentBit = nvmContext.agent.getAgentBit();
			task.perform(nvmContext, actuals);
			nvmContext.agent = oldAgent;
			nvmContext.agentBit = nvmContext.agent.getAgentBit();
		}
	}

	public static class RunReportTask extends DefaultReporter {
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(
					new int[]{Syntax.NumberType(),
							Syntax.ReporterTaskType(),
							Syntax.RepeatableType() | Syntax.WildcardType()},
					Syntax.WildcardType(),
					2);
		}

		@Override
		public Object report(Argument[] args, Context context) throws LogoException, ExtensionException {
			LevelsModelAbstract model = getModel(args[0].getIntValue());
			ReporterTask task = (ReporterTask) args[1].getReporterTask();
			if (task.body().workspace != model.workspace()) {
				throw new ExtensionException("You may only run tasks in the model they were created in.");
			}
			int n = args.length - 2;
			Object[] actuals = new Object[n];
			for (int i = 0; i < n; i++) {
				actuals[i] = args[i+2].get();
			}
			org.nlogo.nvm.Context nvmContext = ((ExtensionContext) context).nvmContext();
			Agent oldAgent = nvmContext.agent;
			nvmContext.agent = model.workspace().world().observer();
			nvmContext.agentBit = nvmContext.agent.getAgentBit();
			Object result = task.report(nvmContext, actuals);
			nvmContext.agent = oldAgent;
			nvmContext.agentBit = nvmContext.agent.getAgentBit();
			return result;
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
				// Trying to do this with the parent to sync against - if it is headless, we
				// just ignore the parameter. it doesn't matter then.
				aModel.kill();

			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}			
			// and remove it from the hashtable
			myModels.remove(modelNumber);
			App.app().workspace().breathe();
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

	public static class Show extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					new int[] { Syntax.NumberType() });	        
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// get model number from args
			int modelNumber = (int) args[0].getDoubleValue();
			// find the model. if it exists, run the command 
			if(myModels.containsKey(modelNumber)) {
				myModels.get(modelNumber).show();
				App.app().workspace().breathe();
			}
			else{
				throw new ExtensionException("There is no model with ID " + modelNumber);
			}
		}
	}

	public static class Hide extends DefaultCommand {
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
				myModels.get(modelNumber).hide();
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

	public static class ModelDirectory extends DefaultReporter{
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(new int[0], Syntax.StringType());
		}
		public Object report(Argument[] args, Context context) throws ExtensionException {
			ExtensionContext extContext = (ExtensionContext) context;
			String dirPath = extContext.workspace().getModelDir();
			if (dirPath == null) {
				throw new ExtensionException("You must save this model before trying to get its directory.");
			}
			return dirPath + File.separator;
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
				if (aModel instanceof LevelsModelHeadless) {
					try {
						return aModel.report(varName);
					} catch (CompilerException e) {
						throw new ExtensionException(e);
					}
				} else {
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
						throw new ExtensionException("The reporter \'" + varName + "\' in the model with ID " + modelNumber + " returned the following exception message: " + e.getMessage());
					}
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
		try {
			aModel.kill();
		} catch (HaltException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void updateChildModelsSpeed(){
		double theSpeed = App.app().workspace().speedSliderPosition();
		for (LevelsModelAbstract model : myModels.values()){
			// find out if they have a LevelsSpace extension loaded
			if (model instanceof LevelsModelComponent){
				LevelsModelComponent lmodel = (LevelsModelComponent)model;
				lmodel.myWS.getComponents();
			}else
			{
				
			}
			model.setSpeed(theSpeed);

		}
	}
	
	static void killModelWithID(int modelId){
		LevelsModelAbstract aModel = myModels.get(modelId);
		try {
			aModel.kill();
		} catch (HaltException e) {
			// TODO Auto-generated catch block

		}
		myModels.remove(aModel);
		App.app().workspace().breathe();		
	}
	
	static void updateChildModelSpeed(LevelsModelAbstract model){
		double theSpeed = App.app().workspace().speedSliderPosition();
		model.setSpeed(theSpeed);
	}	
	
	static void haltChildModels( HashMap<Integer, LevelsModelAbstract> models){
		// Iterate through child models
		// First stop the child model, then get its (potential) child models and 
		// send them here too
		for (LevelsModelAbstract aModel : models.values()){
			String mssg1 = aModel.getName();
			try {
				App.app().workspace().outputObject(mssg1, null, true, true, OutputDestination.NORMAL);
			} catch (LogoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

			aModel.halt();
		}

	}	

	HashMap<Integer, LevelsModelAbstract> getModels(){
		return myModels;
	}
	
}
