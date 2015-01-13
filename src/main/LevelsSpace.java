
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.MenuElement;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.nlogo.api.*;
import org.nlogo.app.App;
import org.nlogo.api.ExtensionObject;
import org.nlogo.api.ImportErrorHandler;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.PrimitiveManager;
import org.nlogo.api.Syntax;
import org.nlogo.app.ToolsMenu;
import org.nlogo.nvm.ExtensionContext;
import org.nlogo.nvm.HaltException;
import org.nlogo.window.SpeedSliderPanel;
import org.nlogo.window.ViewUpdatePanel;


public class LevelsSpace implements org.nlogo.api.ClassManager {

    private final static HashMap<Integer, ChildModel> models = new HashMap<Integer, ChildModel>();

    // counter for keeping track of new models
    private static int modelCounter = 0;

    // These need to be cleaned up on unload
    private static JMenuItem haltButton;
    private static JSlider speedSlider;
    private static ChangeListener speedSliderListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent arg0) {
            updateChildModelsSpeed();
        }
    };
    private static ActionListener haltListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            haltChildModels(models);
        }
    };

    @Override
    public void load(PrimitiveManager primitiveManager) throws ExtensionException {
        primitiveManager.addPrimitive("ask", new Ask());
        primitiveManager.addPrimitive("of", new Of());
        primitiveManager.addPrimitive("load-headless-model", new LoadHeadlessModel());
        primitiveManager.addPrimitive("load-gui-model", new LoadGUIModel());
        primitiveManager.addPrimitive("name-of", new ModelName());
        primitiveManager.addPrimitive("close", new CloseModel());
        primitiveManager.addPrimitive("models", new AllModels());
        primitiveManager.addPrimitive("model-exists?", new ModelExists());
        primitiveManager.addPrimitive("reset", new Reset());
        primitiveManager.addPrimitive("last-model-id", new LastModel());
        primitiveManager.addPrimitive("path-of", new ModelPath());
        primitiveManager.addPrimitive("display", new UpdateView());
        primitiveManager.addPrimitive("show", new Show());
        primitiveManager.addPrimitive("hide", new Hide());
        primitiveManager.addPrimitive("_list-breeds", new ListBreeds());
        primitiveManager.addPrimitive("_globals", new Globals());
        primitiveManager.addPrimitive("_breeds-own", new BreedsOwns());
        primitiveManager.addPrimitive("ask-descendant", new HierarchicalAsk());
        primitiveManager.addPrimitive("of-descendant", new HierarchicalOf());
        primitiveManager.addPrimitive("uses-level-space?", new UsesLevelSpace());

        if (useGUI()) {
            // Adding event listener to Halt for halting child models
            MenuElement[] elements = App.app().frame().getJMenuBar().getSubElements();
            for (MenuElement e : elements) {
                if (e instanceof ToolsMenu) {
                    ToolsMenu tm = (ToolsMenu) e;
                    haltButton = tm.getItem(0);
                    haltButton.addActionListener(haltListener);
                }
            }

            // Attaching a ChangeEventLister to the main model's speed slider so we can
            // update child models' speed sliders at the same time.
            Component[] c = App.app().tabs().interfaceTab().getComponents();
            for (Component co : c) {
                Component[] c2 = ((Container) co).getComponents();
                for (Component co2 : c2) {
                    if (co2 instanceof ViewUpdatePanel) {
                        Component[] c3 = ((Container) co2).getComponents();
                        for (Component co3 : c3) {
                            if (co3 instanceof SpeedSliderPanel) {
                                SpeedSliderPanel speedSliderPanel = (SpeedSliderPanel) co3;
                                speedSlider = (JSlider) speedSliderPanel.getComponents()[0];
                                speedSlider.addChangeListener(speedSliderListener);
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean useGUI() {
        return !"true".equals(System.getProperty("java.awt.headless"));
    }

    public static ChildModel getModel(int id) throws ExtensionException {
        if (models.containsKey(id)) {
            return models.get(id);
        } else {
            throw new ExtensionException("There is no model with ID " + id);
        }
    }

    public static int castToId(Object id) throws ExtensionException {
        if (id instanceof Number) {
            return ((Number) id).intValue();
        } else {
            throw new ExtensionException("Expected a model ID but got: " + id);
        }
    }


    @Override
    public void unload(ExtensionManager arg0) throws ExtensionException {
        if (haltButton != null) {
            haltButton.removeActionListener(haltListener);
        }
        if (speedSlider != null) {
            speedSlider.removeChangeListener(speedSliderListener);
        }
        try {
            reset();
        } catch (HaltException e) {
            // we can ignore this
        }
    }


    private static String getModelPath(ExtensionContext ctx, String basePath) throws ExtensionException {
        try {
            return ctx.attachCurrentDirectory(basePath);
        } catch (MalformedURLException e) {
            throw new ExtensionException(e);
        }
    }

    public static class LoadHeadlessModel extends DefaultCommand {
        public Syntax getSyntax() {
            return Syntax.commandSyntax(
                    // we take in int[] {number, string}
                    new int[] { Syntax.StringType()});
        }

        public void perform(Argument args[], Context context)
                throws ExtensionException, org.nlogo.api.LogoException {
            // saving current modelCounter as that will be the hash table key to the
            // model we are making
            // make a new LevelsModel
            String modelURL = getModelPath((ExtensionContext) context, args[0].getString());
            HeadlessChildModel aModel;
            try {
                aModel = new HeadlessChildModel(context.getAgent().world(), modelURL, modelCounter);
            } catch (IOException e) {
                throw new ExtensionException ("There was no .nlogo file at the path: \"" + modelURL + "\"");
            } catch (CompilerException e) {
                throw new ExtensionException (modelURL + " did not compile properly. There is probably something wrong " +
                        "with its code. Exception said" + e.getMessage());
            }
            updateChildModelSpeed(aModel);
            models.put(modelCounter, aModel);
            // add to models counter
            modelCounter++;
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
            String modelURL = getModelPath((ExtensionContext) context, args[0].getString());
            GUIChildModel aModel;
            try {
                aModel = new GUIChildModel(context.getAgent().world(), modelURL, modelCounter);
                updateChildModelSpeed(aModel);
                // add it to models
                models.put(modelCounter, aModel);
                // add to models counter
                modelCounter ++;
            } catch (InterruptedException e) {
                throw new HaltException(false);
            } catch (InvocationTargetException e) {
                throw new ExtensionException("Loading " + modelURL + " failed with this message: " + e.getCause().getMessage(), (Exception)e.getCause());
            }
        }
    }

    public static void reset() throws ExtensionException, HaltException {
        modelCounter = 0;

        for (ChildModel model : models.values()){
            model.kill();
        }
        models.clear();
    }

    public static class Reset extends DefaultCommand {
        public void perform(Argument args[], Context context)
                throws org.nlogo.api.LogoException, ExtensionException {
            reset();
        }
    }

    private static ChildModel[] toModelList(Argument arg) throws LogoException, ExtensionException {
        Object obj = arg.get();
        if (obj instanceof Double) {
            return new ChildModel[] { getModel(arg.getIntValue()) };
        } else if (obj instanceof LogoList) {
            LogoList idList = arg.getList();
            ChildModel[] models = new ChildModel[idList.size()];
            int i = 0;
            for (Object modelIdObj : arg.getList()) {
                models[i] = getModel(castToId(modelIdObj));
                i++;
            }
            return models;
        } else {
            throw new ExtensionException("Expected a number or list");
        }
    }

    public static class Ask extends DefaultCommand {
        public Syntax getSyntax() {
            return Syntax.commandSyntax(
                    new int[]{Syntax.NumberType() | Syntax.ListType(),
                            Syntax.CommandTaskType() | Syntax.StringType(),
                            Syntax.RepeatableType() | Syntax.WildcardType()},
                    2);
        }
        public void perform(Argument[] args, Context context) throws LogoException, ExtensionException {
            String command = args[1].getString();
            Object[] actuals = getActuals(args, 2);
            for (ChildModel model : toModelList(args[0])) {
                model.ask(command, actuals);
            }
        }
    }

    public static class Of extends DefaultReporter {
        @Override
        public Syntax getSyntax() {
            return Syntax.reporterSyntax(
                    Syntax.ReporterTaskType() | Syntax.StringType(), // Code
                    new int[]{
                            Syntax.WildcardType() | Syntax.RepeatableType() // This covers both models (as a number or list) and args
                    },
                    Syntax.WildcardType(),
                    org.nlogo.api.Syntax.NormalPrecedence() + 1,
                    true
            );
        }
        public Object report(Argument args[], Context context) throws LogoException, ExtensionException {
            LogoListBuilder results = new LogoListBuilder();
            String reporter = args[0].getString();
            Object[] actuals = getActuals(args, 2);
            for (ChildModel model : toModelList(args[1])){
                results.add(model.of(reporter, actuals));
            }
            LogoList returnValue = results.toLogoList();
            return args[1].get() instanceof Double ? returnValue.first() : returnValue;
        }
    }

    public static void askDescendant(LogoList modelTreePath, String command, Object[] args) throws ExtensionException, HaltException {
        ChildModel child = getModel(castToId(modelTreePath.first()));
        if (modelTreePath.size() == 1) {
            child.ask(command, args);
        } else {
            try {
                child.getLevelSpace()
                        .getMethod("askDescendant", LogoList.class, String.class, Object[].class)
                        .invoke(null, modelTreePath.butFirst(), command, args);
            } catch (NoSuchMethodException e) {
                throw ErrorUtils.bugDetected(e);
            } catch (InvocationTargetException e) {
                throw ErrorUtils.bugDetected(e);
            } catch (IllegalAccessException e) {
                throw ErrorUtils.bugDetected(e);
            }
        }
    }

    public static class HierarchicalAsk extends DefaultCommand {
        public Syntax getSyntax() {
            return Syntax.commandSyntax(
                    new int[]{Syntax.ListType(), Syntax.StringType(), Syntax.RepeatableType() | Syntax.WildcardType()},
                    2);
        }

        public void perform(Argument args[], Context context)
                throws ExtensionException, LogoException {
            LogoList list = args[0].getList();
            String cmd = args[1].getString();
            Object[] actuals = getActuals(args, 2);
            askDescendant(list, cmd, actuals);
        }

    }

    public static Object ofDescendant(LogoList modelTreePath, String command, Object[] args) throws ExtensionException, HaltException {
        ChildModel child = getModel(castToId(modelTreePath.first()));
        if (modelTreePath.size() == 1) {
            return child.of(command, args);
        } else {
            try {
                return child.getLevelSpace()
                        .getMethod("ofDescendant", LogoList.class, String.class, Object[].class)
                        .invoke(null, modelTreePath.butFirst(), command, args);
            } catch (NoSuchMethodException e) {
                throw ErrorUtils.bugDetected(e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof ExtensionException) {
                    throw (ExtensionException) e.getCause();
                } else if (e.getCause() instanceof HaltException) {
                    throw (HaltException) e.getCause();
                } else {
                    throw ErrorUtils.bugDetected(e);
                }
            } catch (IllegalAccessException e) {
                throw ErrorUtils.bugDetected(e);
            }
        }
    }


    public static class HierarchicalOf extends DefaultReporter {
        public Syntax getSyntax(){
            return Syntax.reporterSyntax(
                    Syntax.StringType(),
                    new int[]{ Syntax.ListType() },
                    Syntax.WildcardType(),
                    org.nlogo.api.Syntax.NormalPrecedence() + 1,
                    true
            );

        }

        @Override
        public Object report(Argument[] args, Context arg1)
                throws ExtensionException, LogoException {
            String reporter = args[0].getString();
            LogoList modelTreePath = args[1].getList();
            return ofDescendant(modelTreePath, reporter, new Object[0]);
        }

    }


    public static class CloseModel extends DefaultCommand {
        public Syntax getSyntax() {
            return Syntax.commandSyntax(
                    new int[] { Syntax.NumberType() });
        }

        public void perform(Argument args[], Context context)
                throws ExtensionException, org.nlogo.api.LogoException {
            closeModel((int) args[0].getDoubleValue());
        }
    }

    public static void closeModel(int modelNumber) throws ExtensionException, HaltException {
        getModel(modelNumber).kill();
        models.remove(modelNumber);
    }

    public static class UpdateView extends DefaultCommand {
        public Syntax getSyntax() {
            return Syntax.commandSyntax(
                    new int[]{Syntax.NumberType()});
        }

        public void perform(Argument args[], Context context)
                throws ExtensionException, org.nlogo.api.LogoException {
            // get model number from args
            int modelNumber = (int) args[0].getDoubleValue();
            // find the model. if it exists, update graphics
            if (getModel(modelNumber) instanceof HeadlessChildModel){
                HeadlessChildModel aModel = (HeadlessChildModel) getModel(modelNumber);
                aModel.updateView();
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
            getModel(modelNumber).show();
        }
    }

    public static class Hide extends DefaultCommand {
        public Syntax getSyntax() {
            return Syntax.commandSyntax(
                    new int[]{Syntax.NumberType()});
        }

        public void perform(Argument args[], Context context)
                throws ExtensionException, org.nlogo.api.LogoException {
            // get model number from args
            int modelNumber = (int) args[0].getDoubleValue();
            // find the model. if it exists, run the command
            getModel(modelNumber).hide();
        }
    }


    // this returns the path of the model
    public static class ModelName extends DefaultReporter{
        public Syntax getSyntax(){
            return Syntax.reporterSyntax(new int[] {Syntax.NumberType()},
                    Syntax.StringType());

        }
        public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
            int modelNumber = args[0].getIntValue();
            return getModel(modelNumber).getName();
        }

    }

    // this returns the path of the model
    public static class ModelPath extends DefaultReporter{
        public Syntax getSyntax(){
            return Syntax.reporterSyntax(new int[] {Syntax.NumberType()},
                    Syntax.StringType());

        }
        public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
            return getModel(args[0].getIntValue()).getPath();

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
            return models.containsKey(modelNumber);

        }
    }


    public static class BreedsOwns extends DefaultReporter {
        public Syntax getSyntax() {
            return Syntax.reporterSyntax(
                    // we take in int[] {modelNumber, varName}
                    new int[] { Syntax.NumberType() },
                    // and return a number
                    Syntax.ListType());
        }

        public Object report(Argument args[], Context context)
                throws ExtensionException, org.nlogo.api.LogoException {
            // get model number from args
            int modelNumber = (int) args[0].getDoubleValue();

            // find the model. if it exists, get all breeds + owns
            if(models.containsKey(modelNumber))
            {
                ChildModel theModel = models.get(modelNumber);
                return theModel.listBreedsOwns();

            }
            else{
                return false;
            }

        }
    }
    public static class ListBreeds extends DefaultReporter {
        public Syntax getSyntax() {
            return Syntax.reporterSyntax(
                    // we take in int[] {modelNumber, varName}
                    new int[] { Syntax.NumberType() },
                    // and return a number
                    Syntax.ListType());
        }

        public Object report(Argument args[], Context context)
                throws ExtensionException, org.nlogo.api.LogoException {
            // get model number from args
            int modelNumber = (int) args[0].getDoubleValue();
            // find the model. if it exists, update graphics
            if(models.containsKey(modelNumber))
            {
                ChildModel theModel = models.get(modelNumber);
                return theModel.listBreedsOwns();
            }
            else{
                return false;
            }

        }
    }

    public static class Globals extends DefaultReporter {
        public Syntax getSyntax() {
            return Syntax.reporterSyntax(
                    // we take in int[] {modelNumber, varName}
                    new int[] { Syntax.NumberType() },
                    // and return a number
                    Syntax.ListType());
        }

        public Object report(Argument args[], Context context)
                throws ExtensionException, org.nlogo.api.LogoException {
            // get model number from args
            int modelNumber = (int) args[0].getDoubleValue();
            // find the model. if it exists, update graphics
            if(models.containsKey(modelNumber))
            {
                ChildModel theModel = models.get(modelNumber);
                return theModel.listGlobals();
            }
            else{
                throw new ExtensionException("There is no model with ID " + modelNumber);

            }

        }
    }

    public static class AllModels extends DefaultReporter {
        public Syntax getSyntax() {
            return Syntax.reporterSyntax(
                    new int[] {},
                    Syntax.ListType());
        }

        public Object report(Argument args[], Context context)
                throws ExtensionException, org.nlogo.api.LogoException {
            LogoListBuilder myLLB = new LogoListBuilder();

            for (Integer id :  models.keySet()) {
                myLLB.add((double) id);
            }
            return myLLB.toLogoList();
        }
    }

    public static class UsesLevelSpace extends DefaultReporter {
        @Override
        public Syntax getSyntax() {
            return Syntax.reporterSyntax(new int[] {Syntax.NumberType()}, Syntax.BooleanType());
        }

        @Override
        public Object report(Argument[] args, Context context) throws LogoException, ExtensionException {
            return getModel(args[0].getIntValue()).usesLevelsSpace();
        }
    }

    @Override
    public List<String> additionalJars() {
        return null;
    }

    @Override
    public void clearAll() {
        // We want to keep models between clear-alls, yes?
    }

    @Override
    public StringBuilder exportWorld() {
        // Not supported
        return new StringBuilder();
    }

    @Override
    public void importWorld(List<String[]> arg0, ExtensionManager arg1,
                            ImportErrorHandler arg2) throws ExtensionException {
        // Not supported
    }

    @Override
    public ExtensionObject readExtensionObject(ExtensionManager arg0,
                                               String arg1, String arg2) throws ExtensionException,
            CompilerException {
        // Not supported
        return null;
    }

    @Override
    public void runOnce(ExtensionManager arg0) throws ExtensionException {

    }

    private static void updateChildModelsSpeed(){
        for (ChildModel model : models.values()){
            updateChildModelSpeed(model);
        }
    }


    private static void updateChildModelSpeed(ChildModel model){
        // If we're running tests, this should noop. So, we check if we've got a GUI.
        if (useGUI()) {
            double theSpeed = App.app().workspace().speedSliderPosition();
            model.setSpeed(theSpeed);
        }
    }

    private static void haltChildModels( HashMap<Integer, ChildModel> models){
        // Iterate through child models
        // First stop the child model, then get its (potential) child models and
        // send them here too
        for (ChildModel aModel : models.values()){
            aModel.halt();
        }

    }

    public static Object[] getActuals(Argument[] args, int startIndex) throws LogoException, ExtensionException {
        Object[] actuals = new Object[args.length - startIndex];
        for(int i=startIndex; i < args.length; i++) {
            actuals[i - startIndex] = args[i].get();
        }
        return actuals;
    }
}
