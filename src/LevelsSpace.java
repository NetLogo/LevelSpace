
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import org.nlogo.api.PrimitiveManager;
import org.nlogo.api.Syntax;
import org.nlogo.app.App;
import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.Workspace.OutputDestination;
import org.nlogo.window.SpeedSliderPanel;
import org.nlogo.window.ViewUpdatePanel;


public class LevelsSpace implements org.nlogo.api.ClassManager {
	final static AgentSetAgent myModels = new AgentSetAgent();
	
	// BAD HACK - need a better solution
	static Agent lastModel;

	// counter for keeping track of new models
	static int modelCounter = 0;
	
	

	@Override
	public void load(PrimitiveManager primitiveManager) throws ExtensionException {
		// this allows you to run a command in another model
		primitiveManager.addPrimitive("ask", new RunTask());
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
		primitiveManager.addPrimitive("with", new With());
		primitiveManager.addPrimitive("turtle-set", new TurtleSet());
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
//		primitiveManager.addPrimitive("_list-breeds", new ListBreeds());		
//		primitiveManager.addPrimitive("_globals", new Globals());		
		// this returns a list of all breeds and their own vars. currently implemented  
		// as a nl primitive, but probably won't need it as such. Although it is
		// sort of handy for LS programming
//		primitiveManager.addPrimitive("_breeds-own", new BreedsOwns());		
//		primitiveManager.addPrimitive("test", new Test());	
		// this is for exporting model information (like turtle vars, globals, etc)
		// to an external, graphical programming environment for describing
		// inter-model relationships and behaviors.
//		primitiveManager.addPrimitive("_export-models", new ExportModels());
//		primitiveManager.addPrimitive("_ask-hi", new HierarchicalAsk());
//		primitiveManager.addPrimitive("_report-hi", new HierarchicalReport());
//		primitiveManager.addPrimitive("_model-hierarchy", new ModelHierarchy());
		
		primitiveManager.addPrimitive("of", new Of());
		
		primitiveManager.addPrimitive("last-model", new LastModel());
		

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

//	public static LevelsModelAbstract getModel(int id) throws ExtensionException {
//		if (myModels.containsKey(id)) {
//			return myModels.get(id);
//		} else {
//			throw new ExtensionException("There is no model with ID " + id);
//		}
//	}


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
			ModelAgent aModelAgent = new ModelAgent(aModel);
			myModels.add(aModelAgent);
			
			lastModel = aModelAgent;
			// add to models counter
			//modelCounter ++;
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
				ModelAgent aModelAgent = new ModelAgent(aModel);
				myModels.add(aModelAgent);
				// set last model to this
				lastModel = aModelAgent;
			} catch (InterruptedException e) {
				throw new HaltException(false);
			} catch (InvocationTargetException e) {
				throw new ExtensionException("Loading " + modelURL + " failed with this message: " + e.getMessage());
			} 
			// stop up, take a breath. You will be okay.
			App.app().workspace().breathe();
		}
	}

	public static class Reset extends DefaultCommand {
		public void perform(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			for (Agent model : myModels){
				ModelAgent theModelAgent = (ModelAgent)model;
				theModelAgent.theModel.kill();
			}
			myModels.clear();
		}
	}

	public static class RunTask extends DefaultCommand {
		public Syntax getSyntax(){
			return Syntax.commandSyntax(
					new int[]{Syntax.WildcardType(),
							Syntax.CommandTaskType() | Syntax.StringType(),
							Syntax.RepeatableType() | Syntax.WildcardType()},
					2);
		}

		@Override
		public void perform(Argument[] args, Context context) throws LogoException, ExtensionException {
			Agent agent = (Agent) args[0].get();
			agent.ask(args, context);

		}
	}
	
//
//	public static class HierarchicalAsk extends DefaultCommand {
//		public Syntax getSyntax() {
//			return Syntax.commandSyntax(
//					new int[] { Syntax.ListType(), Syntax.StringType() });	        
//		}
//
//		public void perform(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//			// get model number from args
//			LogoList list = args[0].getList();
//			// get the command
//			String cmd = args[1].getString();
//			// get the model
//			double modelNumber = (Double) list.first();
//			int modelno = (int)modelNumber;
//			LevelsModelAbstract aModel;
//			if (myModels.containsKey(modelno)){
//				aModel = myModels.get(modelno);				
//			}
//			else {
//				throw new ExtensionException("The model with id " + modelno + " did not exist.");
//			}
//			// then remove the model from the list
//			list = list.butFirst();
//			// Command string
//			String modelCommand = "";
//
//			// if the list is longer than one, we need to go deeper in the hierarchy
//			if (list.size() > 1){
//				// need to reinsert escape chars
//				cmd = cmd.replace("\"", "\\\"");
//				// this currently doesn't work because it does this for the first and last 
//				// quotation marks too - which it should not.
//				modelCommand = "ls:_ask-hi " + org.nlogo.api.Dump.logoObject(list) + " \"" + cmd + "\"";
//			}
//
//			// if it is exactly 1 that means we are at the parent of the model that we want
//			// to ask to do something, so we just get the parent to ask its child
//			if (list.size() == 1){
//				// get the child model
//				double childModelNumber = (Double) list.first();
//				int childModelno = (int)childModelNumber;
//				cmd = cmd.replace("\"", "\\\"");				
//				modelCommand = "ls:ask " +  childModelno + " \""+ cmd + "\"";
//			}
//			// then call command
//			aModel.command(modelCommand);
//
//		}
//
//	}
	

//	public static class HierarchicalReport extends DefaultReporter {
//		public Syntax getSyntax(){
//			return Syntax.reporterSyntax(
//					new int[] {Syntax.ListType(), Syntax.StringType()},
//					Syntax.StringType());
//
//		}
//
//		public void perform(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//
//		}
//
//		@Override
//		public Object report(Argument[] args, Context arg1)
//				throws ExtensionException, LogoException {
//			// TODO Auto-generated method stub
//			// get model number from args
//			LogoList list = args[0].getList();
//			// get the command
//			String reporter = args[1].getString();
//			// get the model
//			double modelNumber = (Double) list.first();
//			int modelno = (int)modelNumber;
//			LevelsModelAbstract aModel;
//			if (myModels.containsKey(modelno)){
//				aModel = myModels.get(modelno);				
//			}
//			else {
//				throw new ExtensionException("The model with id " + modelno + " did not exist.");
//			}
//			// then remove the model from the list
//			list = list.butFirst();
//			// Command string
//			String modelCommand = "";
//
//			// if the list is longer than one, we need to go deeper in the hierarchy
//			if (list.size() > 1){
//				// need to reinsert escape chars
//				reporter = reporter.replace("\"", "\\\"");
//				// this currently doesn't work because it does this for the first and last 
//				// quotation marks too - which it should not.
//				modelCommand = "ls:_report-hi " + org.nlogo.api.Dump.logoObject(list) + " \"" + reporter + "\"";
//			}
//
//			// if it is exactly 1 that means we are at the parent of the model that we want
//			// to ask to do something, so we just get the parent to ask its child
//			if (list.size() == 1){
//				// get the child model
//				double childModelNumber = (Double) list.first();
//				int childModelno = (int)childModelNumber;
//				reporter = reporter.replace("\"", "\\\"");				
//				modelCommand = "ls:report " +  childModelno + " \""+ reporter + "\"";
//			}
//			
//			// then call command
//			return aModel.report(modelCommand);
//
//		}
//
//	}
//	public static class ModelHierarchy extends DefaultReporter {
//		public Syntax getSyntax(){
//			return Syntax.reporterSyntax(
//					new int[] {},
//					Syntax.StringType());
//
//		}
//
//		public void perform(Argument args[], Context context)
//				throws ExtensionException, org.nlogo.api.LogoException {
//
//		}
//
//		@Override
//		public Object report(Argument[] args, Context arg1)
//				throws ExtensionException, LogoException {
//			// TODO Auto-generated method stub
//			// get model number from args
//			String returnValue = " [";
//			
//			for (Integer key : myModels.keySet()){
//				LevelsModelAbstract model = myModels.get(key);
//				if (model.usesLevelsSpace()){
//					returnValue = returnValue + "[ " + key.toString() + " \"" + model.getPath() + "\" " + model.report("ls:_model-hierarchy") + "]";
//				}
//				else{
//					returnValue = returnValue + "[ " + key.toString() + " \"" + model.getPath() + "\" " + " [ ]]";
//				}
//			}
//
//			returnValue = returnValue + " ]";
//			// then return it
//			return returnValue;
//
//		}
//
//	}
//	
//
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
				((ModelAgent) theAgent).theModel.kill();
				myModels.remove(theModel);			
				App.app().workspace().breathe();
			} else if (theAgent instanceof AgentSetAgent) {
				AgentSetAgent removedModels = new AgentSetAgent();
				for (Agent theModel : myModels) {
					if (theModel instanceof ModelAgent){
						((ModelAgent) theModel).theModel.kill();
						removedModels.add(theModel);			
						App.app().workspace().breathe();
					}
					else{
						throw new ExtensionException("You provided a set containing non-model agents. Only" +
								" models can be provided to ls:close");
					}
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
//			App.app().workspace().breathe();
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
//			App.app().workspace().breathe();
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

	public static class Of extends DefaultReporter {

		@Override
		public Syntax getSyntax() {
			return Syntax.reporterSyntax(Syntax.WildcardType(), new int[]{
				Syntax.WildcardType()},
				Syntax.WildcardType(),
				org.nlogo.api.Syntax.NormalPrecedence() + 1,
				true);

		}
		public Object report(Argument args[], Context context)
				throws ExtensionException, org.nlogo.api.LogoException {
			Agent anAgent = (Agent)args[1].get();
			return anAgent.of(args[0].getString()); 

		}
	}


	public static class With extends DefaultReporter{
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(Syntax.WildcardType(), new int[]{
				Syntax.WildcardType()},
				Syntax.WildcardType(),
				org.nlogo.api.Syntax.NormalPrecedence() + 2,
				false // left associative
					);
		}
		public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
			AgentSetAgent anAgentSet = (AgentSetAgent)args[0].get();
			return anAgentSet.with(args[1].getString());

		}

	}

	public static class TurtleSet extends DefaultReporter{
		public Syntax getSyntax(){
			return Syntax.reporterSyntax(
					new int[] {Syntax.ListType()},
					// and return a number
					Syntax.WildcardType());
		}
		public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
			LogoList aList = args[0].getList();
			return new AgentSetAgent(aList);

		}

	}

	
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
//				LevelsModelAbstract theModel = myModels.get(modelNumber);
//				return theModel.usesLevelsSpace();
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
//				LevelsModelAbstract theModel = myModels.get(modelNumber);
//				return theModel.listBreedsOwns();
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
//				LevelsModelAbstract theModel = myModels.get(modelNumber);
//				return theModel.listBreedsOwns();
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
//				LevelsModelAbstract theModel = myModels.get(modelNumber);
//				return theModel.listGlobals();
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
			model.theModel.kill();
		} catch (HaltException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		myModels.remove(model);
	}

	AgentSetAgent getModels(){
		return myModels;
	}
	
}
