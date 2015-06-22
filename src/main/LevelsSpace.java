
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.nlogo.api.*;
import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.app.*;
import org.nlogo.api.ExtensionObject;
import org.nlogo.api.ImportErrorHandler;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.PrimitiveManager;
import org.nlogo.api.Syntax;
import org.nlogo.awt.EventQueue$;
import org.nlogo.nvm.*;
import org.nlogo.nvm.ReporterTask;
import org.nlogo.window.SpeedSliderPanel;
import org.nlogo.window.ViewUpdatePanel;

import gui.ModelManager;

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

    private static LSModelManager modelManager = useGUI() ? new BackingModelManager() : new HeadlessBackingModelManager();

    @Override
    public void load(PrimitiveManager primitiveManager) throws ExtensionException {
        primitiveManager.addPrimitive("ask", new Ask());
        primitiveManager.addPrimitive("of", new Of());
        primitiveManager.addPrimitive("report", new Report());
        primitiveManager.addPrimitive("load-headless-model", new LoadModel<HeadlessChildModel>(HeadlessChildModel.class));
        primitiveManager.addPrimitive("load-gui-model", new LoadModel<GUIChildModel>(GUIChildModel.class));
        primitiveManager.addPrimitive("name-of", new ModelName());
        primitiveManager.addPrimitive("set-name", new SetName());
        primitiveManager.addPrimitive("close", new CloseModel());
        primitiveManager.addPrimitive("models", new AllModels());
        primitiveManager.addPrimitive("model-exists?", new ModelExists());
        primitiveManager.addPrimitive("reset", new Reset());
        primitiveManager.addPrimitive("path-of", new ModelPath());
        primitiveManager.addPrimitive("display", new UpdateView());
        primitiveManager.addPrimitive("show", new Show());
        primitiveManager.addPrimitive("hide", new Hide());
        primitiveManager.addPrimitive("_list-breeds", new ListBreeds());
        primitiveManager.addPrimitive("_globals", new Globals());
        primitiveManager.addPrimitive("ask-descendant", new HierarchicalAsk());
        primitiveManager.addPrimitive("of-descendant", new HierarchicalOf());
        primitiveManager.addPrimitive("uses-level-space?", new UsesLevelSpace());
        primitiveManager.addPrimitive("_model-procedures", new ModelProcedures());
        primitiveManager.addPrimitive("to-OTPL", new ToOTPL());


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

    public static boolean isMainModel() {
        for (ClassManager cm : App.app().workspace().getExtensionManager().loadedExtensions()) {
            if (cm.getClass() == LevelsSpace.class) {
                return true;
            }
        }
        // The contains check determines whether or not LS has finished loading. If it hasn't, we're
        // almost certainly the main model.
        return !App.app().workspace().getExtensionManager().getExtensionNames().contains("ls");
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
        if (useGUI() && isMainModel()) {
            App.app().frame().getJMenuBar().remove(modelManager.guiComponent());
        }
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

    public static class LoadModel<T extends ChildModel> extends DefaultCommand {
        private Class<T> modelType;

        private LoadModel(Class<T> modelType) {
            this.modelType = modelType;
        }

        @Override
        public Syntax getSyntax() {
            return Syntax.commandSyntax(
                    new int[] { Syntax.StringType(), Syntax.CommandTaskType() | Syntax.RepeatableType()}, 1);
        }

        @Override
        public void perform(Argument args[], Context ctx) throws ExtensionException, org.nlogo.api.LogoException {
            String modelPath = getModelPath((ExtensionContext) ctx, args[0].getString());
            try {
                T model;
                if (modelType == HeadlessChildModel.class) {
                    model = modelType.cast(new HeadlessChildModel(ctx.getAgent().world(), modelPath, modelCounter));
                } else {
                    model = modelType.cast(new GUIChildModel(ctx.getAgent().world(), modelPath, modelCounter));
                    updateChildModelSpeed(model);
                }
                models.put(modelCounter, model);
                if (args.length > 1) {
                    args[1].getCommandTask().perform(ctx, new Object[]{(double) modelCounter});
                }
                modelCounter++;
                updateModelMenu();
            } catch (CompilerException e) {
                throw new ExtensionException(modelPath + " did not compile properly. There is probably something wrong " +
                        "with its code. Exception said" + e.getMessage());
            } catch (IOException e) {
                throw new ExtensionException("There was no .nlogo file at the path: \"" + modelPath + "\"");
            } catch (InterruptedException e) {
                throw new HaltException(false);
            }
        }
    }

    public static void updateModelMenu() {
        Runnable reportModelOpened = new Runnable() {
            @Override
            public void run() {
                modelManager.updateChildModels(models);
            }
        };
        EventQueue$.MODULE$.invokeLater(reportModelOpened);
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

    public static class Report extends DefaultReporter {
        @Override
        public Syntax getSyntax() {
            return Syntax.reporterSyntax(
                    new int[]{
                            Syntax.NumberType() | Syntax.ListType(),
                            Syntax.ReporterTaskType() | Syntax.StringType(),
                            Syntax.WildcardType() | Syntax.RepeatableType()
                    },
                    Syntax.WildcardType(),
                    2
            );
        }
        public Object report(Argument args[], Context context) throws LogoException, ExtensionException {
            LogoListBuilder results = new LogoListBuilder();
            String reporter = args[1].getString();
            Object[] actuals = getActuals(args, 2);
            for (ChildModel model : toModelList(args[0])){
                results.add(model.of(reporter, actuals));
            }
            LogoList returnValue = results.toLogoList();
            return args[0].get() instanceof Double ? returnValue.first() : returnValue;
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

    public static class ToOTPL extends DefaultReporter {
            public Syntax getSyntax(){
                return Syntax.reporterSyntax(new int[] {Syntax.CommandTaskType() | Syntax.ReporterTaskType() },
                        Syntax.StringType());
            }
        @Override
        public Object report(Argument[] args, Context arg1)
                throws ExtensionException, LogoException {
            Object task = args[0].get();
            if (task instanceof ReporterTask) {
                ReporterTask rTask = (ReporterTask) task;
                return rTask.body().agentClassString;
            } else {
                org.nlogo.nvm.CommandTask cTask = (org.nlogo.nvm.CommandTask) task;
                return cTask.procedure().syntax().agentClassString();
            }
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
        updateModelMenu();
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
    public static class ModelProcedures extends DefaultReporter{
        public Syntax getSyntax(){
            return Syntax.reporterSyntax(new int[] {Syntax.NumberType()},
                    Syntax.ListType());
        }
        public Object report(Argument[] args, Context context) throws ExtensionException, LogoException {
            int modelNumber = args[0].getIntValue();
            return getModel(modelNumber).getProcedures();
        }
    }

    public static class SetName extends DefaultCommand {

        @Override
        public Syntax getSyntax() {
            return Syntax.commandSyntax(new int[] {Syntax.NumberType(), Syntax.StringType()});
        }
        @Override
        public void perform(Argument[] args, Context context) throws LogoException, ExtensionException {
            getModel(args[0].getIntValue()).setName(args[1].getString());
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
        modelManager.updateChildModels(models);

        if (useGUI() && isMainModel()) {
            final JMenuBar menuBar = App.app().frame().getJMenuBar();
            if (menuBar.getComponentIndex(modelManager.guiComponent()) == -1) {
                menuBar.add(modelManager.guiComponent());
            }
        }
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
