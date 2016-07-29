package org.nlogo.ls;

import scala.collection.JavaConverters;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.collect.MapMaker;

import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.ExtensionContext;

import org.nlogo.api.*;
import org.nlogo.app.*;
import org.nlogo.core.ExtensionObject;
import org.nlogo.core.LogoList;
import org.nlogo.core.CompilerException;
import org.nlogo.core.Token;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;
import org.nlogo.awt.EventQueue$;
import org.nlogo.window.ViewUpdatePanel;
import org.nlogo.workspace.AbstractWorkspaceScala;

import org.nlogo.ls.gui.ModelManager;

public class LevelSpace implements org.nlogo.api.ClassManager {

    // This can be accessed by both the JobThread and EDT (when halting)
    private final static Map<Integer, ChildModel> models = new ConcurrentHashMap<Integer, ChildModel>();

    // counter for keeping track of new models
    private static int modelCounter = 0;

    // These need to be cleaned up on unload
    private static JMenuItem haltButton;
    private static ActionListener haltListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            haltChildModels(models);
        }
    };

    private static LSModelManager modelManager = GraphicsEnvironment.isHeadless() ? new HeadlessBackingModelManager() : new BackingModelManager();

    @Override
    public void load(PrimitiveManager primitiveManager) throws ExtensionException {
        primitiveManager.addPrimitive("let", LetPrim$.MODULE$);
        primitiveManager.addPrimitive("ask", Ask$.MODULE$);
        primitiveManager.addPrimitive("of", Of$.MODULE$);
        primitiveManager.addPrimitive("report", Report$.MODULE$);
        primitiveManager.addPrimitive("with", With$.MODULE$);
        primitiveManager.addPrimitive("load-headless-model", new LoadModel<HeadlessChildModel>(HeadlessChildModel.class));
        primitiveManager.addPrimitive("load-gui-model", new LoadModel<GUIChildModel>(GUIChildModel.class));
        primitiveManager.addPrimitive("name-of", Name$.MODULE$);
        primitiveManager.addPrimitive("set-name", new SetName());
        primitiveManager.addPrimitive("close", Close$.MODULE$);
        primitiveManager.addPrimitive("models", new AllModels());
        primitiveManager.addPrimitive("model-exists?", new ModelExists());
        primitiveManager.addPrimitive("reset", new Reset());
        primitiveManager.addPrimitive("path-of", Path$.MODULE$);
        primitiveManager.addPrimitive("display", UpdateView$.MODULE$);
        primitiveManager.addPrimitive("show", Show$.MODULE$);
        primitiveManager.addPrimitive("hide", Hide$.MODULE$);
        // TODO: Add these back in
        //primitiveManager.addPrimitive("ask-descendant", new HierarchicalAsk());
        //primitiveManager.addPrimitive("of-descendant", new HierarchicalOf());
        primitiveManager.addPrimitive("uses-level-space?", new UsesLevelSpace());


        if (!GraphicsEnvironment.isHeadless()) {
            // Adding event listener to Halt for halting child models
            MenuElement[] elements = App.app().frame().getJMenuBar().getSubElements();
            for (MenuElement e : elements) {
                if (e instanceof ToolsMenu) {
                    ToolsMenu tm = (ToolsMenu) e;
                    haltButton = tm.getItem(0);
                    haltButton.addActionListener(haltListener);
                }
            }
        }
    }

    public static boolean isMainModel(ExtensionManager myEM) {
        return myEM == App.app().workspace().getExtensionManager();
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
    public void unload(ExtensionManager em) throws ExtensionException {
        if (!GraphicsEnvironment.isHeadless() && isMainModel(em)) {
            App.app().frame().getJMenuBar().remove(modelManager.guiComponent());
        }
        if (haltButton != null) {
            haltButton.removeActionListener(haltListener);
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

    // TODO Move the hierarchical methods in Prims.scala and ditch this
    private static String getCodeString(Object codeObj) {
        String code;
        if (codeObj instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Token> tokens = (List<Token>) codeObj;
            StringBuilder builder = new StringBuilder();
            for (Token t : tokens) {
                builder.append(t.text()).append(" ");
            }
            code = builder.toString();
        } else {
            code = (String) codeObj;
        }
        return code;
    }

    public static class LoadModel<T extends ChildModel> implements Command {
        private Class<T> modelType;

        private LoadModel(Class<T> modelType) {
            this.modelType = modelType;
        }

        @Override
        public Syntax getSyntax() {
            return SyntaxJ.commandSyntax(
                    new int[] { Syntax.StringType(), Syntax.CommandTaskType() | Syntax.RepeatableType()}, 1);
        }

        @Override
        public void perform(Argument args[], Context ctx) throws ExtensionException, org.nlogo.api.LogoException {
            AbstractWorkspaceScala parentWS = (AbstractWorkspaceScala) ctx.workspace();

            String modelPath = getModelPath((ExtensionContext) ctx, args[0].getString());
            try {
                ChildModel model;
                if (modelType == HeadlessChildModel.class || GraphicsEnvironment.isHeadless() || parentWS.behaviorSpaceRunNumber() != 0) {
                    model = new HeadlessChildModel((AbstractWorkspaceScala) ctx.workspace(), modelPath, modelCounter);
                } else {
                    model = new GUIChildModel((AbstractWorkspaceScala) ctx.workspace(), modelPath, modelCounter);
                }
                model.workspace().behaviorSpaceRunNumber(parentWS.behaviorSpaceRunNumber());
                model.workspace().behaviorSpaceExperimentName(parentWS.behaviorSpaceExperimentName());
                models.put(modelCounter, model);
                if (args.length > 1) {
                    args[1].getCommandTask().perform(ctx, new Object[]{(double) modelCounter});
                }
                modelCounter++;
                updateModelMenu();
            } catch (CompilerException e) {
                throw new ExtensionException(modelPath + " has an error in its code: " + e.getMessage(), e);
            } catch (IOException e) {
                throw new ExtensionException("NetLogo couldn't read the file \"" + modelPath + "\". Are you sure it exists and that NetLogo has permission to read it?", e);
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

    public static class Reset implements Command {
        public Syntax getSyntax() {
            return SyntaxJ.commandSyntax();
        }
        public void perform(Argument args[], Context context)
                throws org.nlogo.api.LogoException, ExtensionException {
            reset();
        }
    }

    public static ChildModel[] toModelList(Argument arg) throws LogoException, ExtensionException {
        Object obj = arg.get();
        if (obj instanceof Double) {
            return new ChildModel[] { getModel(arg.getIntValue()) };
        } else if (obj instanceof LogoList) {
            LogoList idList = arg.getList();
            ChildModel[] models = new ChildModel[idList.size()];
            int i = 0;
            for (Object modelIdObj : arg.getList().javaIterable()) {
                models[i] = getModel(castToId(modelIdObj));
                i++;
            }
            return models;
        } else {
            throw new ExtensionException("Expected a number or list");
        }
    }

    public static void closeModel(ChildModel model) throws ExtensionException, HaltException {
        model.kill();
        models.remove(model.modelID());
        updateModelMenu();
    }

    public static class SetName implements Command {

        @Override
        public Syntax getSyntax() {
            return SyntaxJ.commandSyntax(new int[] {Syntax.NumberType(), Syntax.StringType()});
        }
        @Override
        public void perform(Argument[] args, Context context) throws LogoException, ExtensionException {
            getModel(args[0].getIntValue()).name_$eq(args[1].getString());
        }
    }

    public static class ModelExists implements Reporter {
        public Syntax getSyntax() {
            return SyntaxJ.reporterSyntax(
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

    public static class AllModels implements Reporter {
        public Syntax getSyntax() {
            return SyntaxJ.reporterSyntax(
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

    public static class UsesLevelSpace implements Reporter {
        @Override
        public Syntax getSyntax() {
            return SyntaxJ.reporterSyntax(new int[] {Syntax.NumberType()}, Syntax.BooleanType());
        }

        @Override
        public Object report(Argument[] args, Context context) throws LogoException, ExtensionException {
            return getModel(args[0].getIntValue()).usesLevelSpace();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
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
    public void runOnce(ExtensionManager em) throws ExtensionException {
        modelManager.updateChildModels(models);

        if (!GraphicsEnvironment.isHeadless() && isMainModel(em)) {
            final JMenuBar menuBar = App.app().frame().getJMenuBar();
            if (menuBar.getComponentIndex(modelManager.guiComponent()) == -1) {
                menuBar.add(modelManager.guiComponent());
            }
        }
    }

    private static void haltChildModels(Map<Integer, ChildModel> models){
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
