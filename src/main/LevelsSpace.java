
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.nlogo.api.*;
import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.LogoException;
import org.nlogo.app.App;
import org.nlogo.nvm.*;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.ReporterTask;
import org.nlogo.nvm.Workspace.OutputDestination;
import org.nlogo.window.SpeedSliderPanel;
import org.nlogo.window.ViewUpdatePanel;


public class LevelsSpace implements org.nlogo.api.ClassManager {
	final static GenericAgentSet<ModelAgent> myModels = new GenericAgentSet<ModelAgent>();
	
	// BAD HACK - need a better solution
	static Agent lastModel;

	// counter for keeping track of new models
	static int modelCounter = 0;
	
	

	@Override
	public void load(PrimitiveManager primitiveManager) throws ExtensionException {
		// this allows you to run a command in another model
		primitiveManager.addPrimitive("ask", new RunTask());
		primitiveManager.addPrimitive("of", new Of());
//		primitiveManager.addPrimitive("report", new RunReporterTask());
		// this loads a model
		primitiveManager.addPrimitive("load-headless-model", new LoadHeadlessModel());
		primitiveManager.addPrimitive("load-gui-model", new LoadGUIModel());
		// this returns the name (and path) of a model 
//		primitiveManager.addPrimitive("model-name", new ModelName());
		// this closes a model
		primitiveManager.addPrimitive("close", new CloseModel());
		// this returns a list of model IDs
		primitiveManager.addPrimitive("models", new Models());
		// this returns a boolean - does the model exist
//		primitiveManager.addPrimitive("model-exists?", new ModelExists());
		// this resets the the levelsspace extension
		primitiveManager.addPrimitive("reset", new Reset());
//		primitiveManager.addPrimitive("with", new With());
		// this returns just the path of a model
//		primitiveManager.addPrimitive("model-path", new ModelPath());
		// returns the path of the current model; useful for opening child models in same directory
//		primitiveManager.addPrimitive("model-directory", new ModelDirectory());
		// These should probably go.
//		primitiveManager.addPrimitive("display", new UpdateView());
//		primitiveManager.addPrimitive("show", new Show());
//		primitiveManager.addPrimitive("hide", new Hide());
		// testing primitives
		// this returns a list of all breeds. currently implemented as a nl 
		// primitive, but probably won't need it as such. Although it is
		// sort of handy for LS programming
		primitiveManager.addPrimitive("list-breeds", new ListBreeds());		
//		primitiveManager.addPrimitive("globals", new Globals());		
		// this returns a list of all breeds and their own vars. currently implemented  
		// as a nl primitive, but probably won't need it as such. Although it is
		// sort of handy for LS programming
		primitiveManager.addPrimitive("breeds-owns", new BreedsOwns());		
		primitiveManager.addPrimitive("globals", new Globals());
//		primitiveManager.addPrimitive("test", new Test());	
		// this is for exporting model information (like turtle vars, globals, etc)
		// to an external, graphical programming environment for describing
		// inter-model relationships and behaviors.
//		primitiveManager.addPrimitive("_export-models", new ExportModels());
//		primitiveManager.addPrimitive("_ask-hi", new HierarchicalAsk());
//		primitiveManager.addPrimitive("_report-hi", new HierarchicalReport());
//		primitiveManager.addPrimitive("_model-hierarchy", new ModelHierarchy());
		primitiveManager.addPrimitive("all-models-info", new AllModelsInfo());

		primitiveManager.addPrimitive("last-model", new LastModel());
		primitiveManager.addPrimitive("model", new ModelByID());


		modelCounter = 0;

		// Adding event listener to Halt for halting child models
//		MenuElement[] elements = App.app().frame().getJMenuBar().getSubElements();
//		for (MenuElement e : elements){
//			if (e instanceof ToolsMenu){
//				ToolsMenu tm = (ToolsMenu)e;
//				JMenuItem item = tm.getItem(0);
//				item.addActionListener(new ActionListener() {
//					@Override
//					public void actionPerformed(ActionEvent arg0) {
//						haltChildModels(myModels);
//					}
//				});
//			}
//		}	

		// Attaching a ChangeEventLister to the main model's speed slider so we can 
		// update child models' speed sliders at the same time.
		if (useGUI()) {
			Component[] c = App.app().tabs().interfaceTab().getComponents();
			for (Component co : c) {
				Component[] c2 = ((Container) co).getComponents();
				for (Component co2 : c2) {
					if (co2 instanceof ViewUpdatePanel) {
						Component[] c3 = ((Container) co2).getComponents();
						for (Component co3 : c3) {
							if (co3 instanceof SpeedSliderPanel) {
								SpeedSliderPanel speedSliderPanel = (SpeedSliderPanel) co3;
								JSlider slider = (JSlider) speedSliderPanel.getComponents()[0];
								slider.addChangeListener(new ChangeListener() {
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
	}

//	public static LevelsModelAbstract getModel(int id) throws ExtensionException {
//		if (myModels.containsKey(id)) {
//			return myModels.get(id);
//		} else {
//			throw new ExtensionException("There is no model with ID " + id);
//		}
//	}

	static public boolean useGUI() {
		return !"true".equals(System.getProperty("java.awt.headless"));
	}

	public static Workspace workspace(Context context) {
		return ((ExtensionContext) context).workspace();
	}


	@Override
	public void unload(ExtensionManager arg0) throws ExtensionException {

	}


	public static class LoadHeadlessModel extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					// we take in int[] {number, string} 
					new int[] { Syntax.StringType()});
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			// make a new LevelsModel
			String modelURL = args[0].getString();
			LevelsModelHeadless aModel = null;
			try {
				aModel = new LevelsModelHeadless(modelURL);
			} catch (IOException e) {
				throw new ExtensionException ("There was no .nlogo file at the path: \"" + modelURL + "\"");
			} catch (CompilerException e) {
				throw new ExtensionException (modelURL + " did not compile properly. There is probably something wrong " +
						"with its code. Exception said" + e.getMessage());
			}
			if (useGUI()) {
				updateChildModelSpeed(aModel);
			}
			ModelAgent aModelAgent = new ModelAgent(aModel, modelCounter);
			myModels.add(aModelAgent);
			
			lastModel = aModelAgent;
			// add to models counter
			modelCounter ++;
			// stop up, take a breath. You will be okay.
			LevelsSpace.workspace(context).breathe();
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
				aModel = new LevelsModelComponent(modelURL);
				updateChildModelSpeed(aModel);
				// add it to models
				ModelAgent aModelAgent = new ModelAgent(aModel, modelCounter);
				myModels.add(aModelAgent);
				// set last model to this
				lastModel = aModelAgent;
				// add to counter
				modelCounter++;

			} catch (InterruptedException e) {
				throw new HaltException(false);
			} catch (InvocationTargetException e) {
				throw new ExtensionException("Loading " + modelURL + " failed with this message: " + e.getMessage());
			} 
			// stop up, take a breath. You will be okay.
			LevelsSpace.workspace(context).breathe();
		}
	}

	public static class Reset extends DefaultCommand {
		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			for (Agent model : myModels){
				ModelAgent theModelAgent = (ModelAgent)model;
				theModelAgent.model.kill();
			}
			myModels.clear();
		}
	}

	private static Object[] getActuals(Argument[] args, int startIndex) throws LogoException, ExtensionException {
		Object[] actuals = new Object[args.length - startIndex];
		for(int i=startIndex; i < args.length; i++) {
			actuals[i - startIndex] = args[i].get();
		}
		return actuals;
	}

	public static class RunTask extends DefaultCommand {
		public Syntax getSyntax(){
			return Syntax.commandSyntax(
					new int[]{
							Syntax.WildcardType(), // Model
							Syntax.CommandTaskType() | Syntax.StringType(), // Code
							Syntax.RepeatableType() | Syntax.WildcardType() // Optional arguments
					},
					2
			);
		}

		@Override
		public void perform(Argument[] args, Context context) throws LogoException, ExtensionException {
			if (args[0].get() instanceof Agent || args[0].get() instanceof AgentSet) {
				Agent agent = (Agent) args[0].get();
				org.nlogo.nvm.Context nvmContext = ((ExtensionContext) context).nvmContext();
				Object command = args[1].get();
				if (command instanceof String) {
					agent.ask(nvmContext, (String) command, getActuals(args, 2));
				} else if (command instanceof CommandTask) {
					agent.ask(nvmContext, (CommandTask) command, getActuals(args, 2));
				} else {
					throw new ExtensionException("You must give ls:ask a command task or string to run");
				}
			} else {
				throw new ExtensionException("ls:ask only takes a LevelSpace agent or LevelSpacel agentset, such as a model, or an agent returned from ls:of.");
			}
		}
	}

	public static class Of extends DefaultReporter {
		@Override
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					Syntax.ReporterTaskType() | Syntax.StringType(), // Code
					new int[]{
							Syntax.WildcardType(), // Model
					},
					Syntax.WildcardType(),
					org.nlogo.api.Syntax.NormalPrecedence() + 1,
					true
			);

		}
		public Object report(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			if (args[1].get() instanceof Agent) {
				Agent agent = (Agent) args[1].get();
				org.nlogo.nvm.Context nvmContext = ((ExtensionContext) context).nvmContext();
				Object reporter = args[0].get();
				if (reporter instanceof String) {
					return agent.of(nvmContext, (String) reporter, getActuals(args, 2));
				} else if (reporter instanceof ReporterTask) {
					return agent.of(nvmContext, (ReporterTask) reporter, getActuals(args, 2));
				} else {
					throw new ExtensionException("You must give ls:of a string or reporter task to run");
				}
			} else {
				throw new ExtensionException("ls:of only takes a LevelSpace agent or LevelSpacel agentset, such as a model, or an agent returned from ls:of.");
			}
		}
	}

	public static class CloseModel extends DefaultCommand {
		public Syntax getSyntax() {
			return Syntax.commandSyntax(
					new int[] { Syntax.WildcardType() });	        
		}

		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			Object theAgent = args[0].get();
			if (theAgent instanceof ModelAgent){
				ModelAgent theModel = (ModelAgent)theAgent;
				((ModelAgent) theAgent).model.kill();
				myModels.remove(theModel);			
				LevelsSpace.workspace(context).breathe();
			} else if (theAgent instanceof GenericAgentSet) {
				GenericAgentSet<ModelAgent> removedModels = new GenericAgentSet<ModelAgent>();
				for (ModelAgent theModel : myModels) {
					theModel.model.kill();
					removedModels.add(theModel);
					LevelsSpace.workspace(context).breathe();
				}
				for (Agent model : removedModels){
					myModels.remove(model);
				}
			}
			else{
				throw new ExtensionException("You can only call ls:close-model on models.");
			}

		}
	}
	
	
	public static class ModelByID extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					// accept a model
					new int[] { Syntax.NumberType() },
					// and returns a model
					Syntax.WildcardType());	        
		}

		public ModelAgent report(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			double theWho = args[0].getDoubleValue();
			for(ModelAgent model : myModels){
				if (model.who() == theWho){
					return model;
				}
			}
			throw new ExtensionException("There was no model with ID: " + String.valueOf(theWho));

		}
	}


	
	
	//
	//	public static class UpdateView extends DefaultCommand {
//		public Syntax getSyntax() {
//			return Syntax.commandSyntax(
//					new int[] { Syntax.NumberType() });	        
//		}
//
//		public void perform(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//			// get model number from args
//			int modelNumber = (int) args[0].getDoubleValue();
//			// find the model. if it exists, update graphics
//			if (getModel(modelNumber) instanceof LevelsModelHeadless){
//				LevelsModelHeadless aModel = (LevelsModelHeadless) getModel(modelNumber);
//				aModel.updateView();
//			}
//		}
//	}	
//
//	public static class Show extends DefaultCommand {
//		public Syntax getSyntax() {
//			return Syntax.commandSyntax(
//					new int[] { Syntax.NumberType() });	        
//		}
//
//		public void perform(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//			// get model number from args
//			int modelNumber = (int) args[0].getDoubleValue();
//			// find the model. if it exists, run the command
//			getModel(modelNumber).show();
//			LevelsSpace.workspace(context).breathe();
//		}
//	}
//
//	public static class Hide extends DefaultCommand {
//		public Syntax getSyntax() {
//			return Syntax.commandSyntax(
//					new int[] { Syntax.NumberType() });	        
//		}
//
//		public void perform(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//			// get model number from args
//			int modelNumber = (int) args[0].getDoubleValue();
//			// find the model. if it exists, run the command
//			getModel(modelNumber).hide();
//			LevelsSpace.workspace(context).breathe();
//		}
//	}
//	
//	public static class ExportModels extends DefaultCommand {
//		public Syntax getSyntax() {
//			return Syntax.commandSyntax(
//					// we take in int[] {number, string} 
//					//					new int[] { Syntax.StringType()}
//					);
//		}
//
//		public void perform(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//			for (Integer d : myModels.keySet()){
//				LevelsModelAbstract aModel = myModels.get(d);
//				aModel.usesLevelsSpace();
//			}
//
//
//		}	
//	}

//	
//	
//	
//	// this returns the path of the model
//	public static class ModelName extends DefaultReporter{
//		public Syntax getSyntax(){
//			return Syntax.reporterSyntax(new int[] {Syntax.NumberType()},
//					Syntax.StringType());
//
//		}
//		public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
//			int modelNumber = args[0].getIntValue();
//			return getModel(modelNumber).getName();
//		}
//
//	}
//
//	public static class ModelDirectory extends DefaultReporter{
//		public Syntax getSyntax(){
//			return Syntax.reporterSyntax(new int[0], Syntax.StringType());
//		}
//		public Object report(Argument[] args, Context context) throws ExtensionException {
//			ExtensionContext extContext = (ExtensionContext) context;
//			String dirPath = extContext.workspace().getModelDir();
//			if (dirPath == null) {
//				throw new ExtensionException("You must save this model before trying to get its directory.");
//			}
//			return dirPath + File.separator;
//		}
//
//	}
//	// this returns the path of the model
//	public static class ModelPath extends DefaultReporter{
//		public Syntax getSyntax(){
//			return Syntax.reporterSyntax(new int[] {Syntax.NumberType()},
//					Syntax.StringType());
//
//		}
//		public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
//			return getModel(args[0].getIntValue()).getPath();
//
//		}
//
//	}
//
//	/*
//	 * This primitive returns the last created model number
//	 */
	public static class LastModel extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					// no parameters 
					new int[] {},
					// and return a number
					Syntax.NumberType());
		}

		public Agent report(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {

			return lastModel;

		}
	}

	public static class ListBreeds extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					// accept a model
					new int[] { Syntax.WildcardType() },
					// and returns a list of breeds
					Syntax.ListType());	        
		}

		public LogoList report(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			Object arg = args[0].get();
			if (arg instanceof ModelAgent){
				ModelAgent theModel = (ModelAgent)arg;
				return theModel.model.listBreeds();
			}
			else{
				throw new ExtensionException("You can only list breeds from models. Your provided an object of class " + arg.getClass().toString());
			}

		}
	}
	

	public static class AllModelsInfo extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					// takes no params
					new int[] { },
					// and returns a list of all models with all information
					Syntax.ListType());	        
		}

		public Object report(Argument args[], Context context){
			LogoListBuilder myLLBuilder = new LogoListBuilder();
			for (ModelAgent model : myModels){
				myLLBuilder.add(model.allInfo());
			}
			
			return myLLBuilder.toLogoList();

		}	
	}
	
	public static class BreedsOwns extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					// accept a model
					new int[] { Syntax.WildcardType() },
					// and returns a list of breeds and vars
					Syntax.ListType());	        
		}

		public LogoList report(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			Object arg = args[0].get();
			if (arg instanceof ModelAgent){
				ModelAgent theModel = (ModelAgent)arg;
				return theModel.model.listBreedsOwns();
			}
			else{
				throw new ExtensionException("You can only list breeds and variable lists from models. Your provided an object of class " + arg.getClass().toString());
			}

		}
	}
	public static class Globals extends DefaultReporter {
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(
					// accept a model
					new int[] { Syntax.WildcardType() },
					// and returns a list of breeds and vars
					Syntax.ListType());	        
		}

		public LogoList report(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			Object arg = args[0].get();
			if (arg instanceof ModelAgent){
				ModelAgent theModel = (ModelAgent)arg;
				return theModel.model.listGlobals();
			}
			else{
				throw new ExtensionException("You can only list breeds and variable lists from models. Your provided an object of class " + arg.getClass().toString());
			}

		}
	}

	/*
	 * This primitive returns the last created model number
	 */
//
//	public static class ModelExists extends DefaultReporter {
//		public Syntax getSyntax() {
//			return Syntax.reporterSyntax(
//					// we take in int[] {modelNumber, varName} 
//					new int[] { Syntax.NumberType() },
//					// and return a number
//					Syntax.BooleanType());	        
//		}
//
//		public Object report(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//			// get model number from args
//			int modelNumber = (int) args[0].getDoubleValue();
//
//			// find the model. if it exists, update graphics 
//			if(myModels.containsKey(modelNumber))
//			{
//				return true;
//			}
//			else{
//				return false;
//			}
//
//		}
//	}

//	
//	public static class Test extends DefaultReporter {
//		public Syntax getSyntax() {
//			return Syntax.reporterSyntax(
//					// we take in int[] {modelNumber, varName} 
//					new int[] { Syntax.NumberType() },
//					// and return a number
//					Syntax.BooleanType());	        
//		}
//
//		public Object report(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//			// get model number from args
//			int modelNumber = (int) args[0].getDoubleValue();
//
//			// find the model. if it exists, get all breeds + owns
//			if(myModels.containsKey(modelNumber))
//			{
//				LevelsModelAbstract model = myModels.get(modelNumber);
//				return model.usesLevelsSpace();
//
//			}
//			else{
//				return "no";
//			}
//
//		}
//	}
//
//	public static class BreedsOwns extends DefaultReporter {
//		public Syntax getSyntax() {
//			return Syntax.reporterSyntax(
//					// we take in int[] {modelNumber, varName} 
//					new int[] { Syntax.NumberType() },
//					// and return a number
//					Syntax.ListType());	        
//		}
//
//		public Object report(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//			// get model number from args
//			int modelNumber = (int) args[0].getDoubleValue();
//
//			// find the model. if it exists, get all breeds + owns
//			if(myModels.containsKey(modelNumber))
//			{
//				LevelsModelAbstract model = myModels.get(modelNumber);
//				return model.listBreedsOwns();
//
//			}
//			else{
//				return false;
//			}
//
//		}
//	}
//	public static class ListBreeds extends DefaultReporter {
//		public Syntax getSyntax() {
//			return Syntax.reporterSyntax(
//					// we take in int[] {modelNumber, varName} 
//					new int[] { Syntax.NumberType() },
//					// and return a number
//					Syntax.ListType());	        
//		}
//
//		public Object report(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//			// get model number from args
//			int modelNumber = (int) args[0].getDoubleValue();
//			// find the model. if it exists, update graphics 
//			if(myModels.containsKey(modelNumber))
//			{
//				LevelsModelAbstract model = myModels.get(modelNumber);
//				return model.listBreedsOwns();
//			}
//			else{
//				return false;
//			}
//
//		}
//	}
//
//	public static class Globals extends DefaultReporter {
//		public Syntax getSyntax() {
//			return Syntax.reporterSyntax(
//					// we take in int[] {modelNumber, varName} 
//					new int[] { Syntax.NumberType() },
//					// and return a number
//					Syntax.ListType());	        
//		}
//
//		public Object report(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//			// get model number from args
//			int modelNumber = (int) args[0].getDoubleValue();
//			// find the model. if it exists, update graphics 
//			if(myModels.containsKey(modelNumber))
//			{
//				LevelsModelAbstract model = myModels.get(modelNumber);
//				return model.listGlobals();
//			}
//			else{
//				throw new ExtensionException("There is no model with ID " + modelNumber);
//
//			}
//
//		}
//	}	

		public static class Models extends DefaultReporter {

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
			return myModels;
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
//
	void updateChildModelsSpeed(){
		double theSpeed = App.app().workspace().speedSliderPosition();
		for (Agent model : myModels){
			Model theModel = (Model)model;
			// find out if they have a LevelsSpace extension loaded
			if (theModel instanceof LevelsModelComponent){
				LevelsModelComponent lmodel = (LevelsModelComponent)theModel;
				lmodel.myWS.getComponents();
			}
			theModel.setSpeed(theSpeed);

		}
	}
	

	static void updateChildModelSpeed(Model model){
		double theSpeed = App.app().workspace().speedSliderPosition();
		model.setSpeed(theSpeed);
	}	
	
	static void haltChildModels( HashMap<Integer, Model> models){
		// Iterate through child models
		// First stop the child model, then get its (potential) child models and 
		// send them here too
		for (Model aModel : models.values()){
			aModel.halt();
		}

	}	
	
	static void killModel(ModelAgent model){
		try {
			App.app().workspace().outputObject("kill model called", null, true, true, OutputDestination.NORMAL);
		} catch (LogoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			model.model.kill();
		} catch (HaltException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		myModels.remove(model);
	}

	GenericAgentSet getModels(){
		return myModels;
	}
	
}
