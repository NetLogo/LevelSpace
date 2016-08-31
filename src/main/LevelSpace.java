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
import java.util.Set;
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
import org.nlogo.workspace.AbstractWorkspaceScala;
import org.nlogo.window.GUIWorkspace;

import org.nlogo.ls.gui.ModelManager;

public class LevelSpace implements org.nlogo.api.ClassManager {

    // This can be accessed by both the JobThread and EDT (when halting)
    private final Map<Integer, ChildModel> models = new ConcurrentHashMap<Integer, ChildModel>();

    // counter for keeping track of new models
    private int modelCounter = 0;

    public LetPrim letManager = new LetPrim();

    // These need to be cleaned up on unload
    private JMenuItem haltButton;
    private ActionListener haltListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            haltChildModels(models);
        }
    };

    private LSModelManager modelManager = GraphicsEnvironment.isHeadless() ? new HeadlessBackingModelManager() : new BackingModelManager();

    @Override
    public void load(PrimitiveManager primitiveManager) throws ExtensionException {
        primitiveManager.addPrimitive("let", letManager);
        primitiveManager.addPrimitive("ask", new Ask(this));
        primitiveManager.addPrimitive("of", new Of(this));
        primitiveManager.addPrimitive("report", new Report(this));
        primitiveManager.addPrimitive("with", new With(this));
        primitiveManager.addPrimitive("create-models", new CreateModels<HeadlessChildModel>(HeadlessChildModel.class));
        primitiveManager.addPrimitive("create-interactive-models", new CreateModels<GUIChildModel>(GUIChildModel.class));
        primitiveManager.addPrimitive("name-of", new Name(this));
        primitiveManager.addPrimitive("set-name", new SetName(this));
        primitiveManager.addPrimitive("close", new Close(this));
        primitiveManager.addPrimitive("models", new AllModels(this));
        primitiveManager.addPrimitive("model-exists?", new ModelExists(this));
        primitiveManager.addPrimitive("reset", new Reset());
        primitiveManager.addPrimitive("path-of", new Path(this));
        primitiveManager.addPrimitive("display", new UpdateView(this));
        primitiveManager.addPrimitive("show", new Show(this));
        primitiveManager.addPrimitive("hide", new Hide(this));
        primitiveManager.addPrimitive("show-all", new ShowAll(this));
        primitiveManager.addPrimitive("hide-all", new HideAll(this));
        primitiveManager.addPrimitive("uses-level-space?", new UsesLS(this));


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

    public boolean isMainModel(ExtensionManager myEM) {
        return myEM == App.app().workspace().getExtensionManager();
    }

    public ChildModel getModel(int id) throws ExtensionException {
        if (models.containsKey(id)) {
            return models.get(id);
        } else {
            throw new ExtensionException("There is no model with ID " + id);
        }
    }

    public boolean containsModel(int id) {
        return models.containsKey(id);
    }

    public Set<Integer> modelSet() {
        return models.keySet();
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

    public class CreateModels<T extends ChildModel> implements Command {
        private Class<T> modelType;

        private CreateModels(Class<T> modelType) {
            this.modelType = modelType;
        }

        @Override
        public Syntax getSyntax() {
            return SyntaxJ.commandSyntax(
                    new int[] { Syntax.NumberType(), Syntax.StringType(), Syntax.CommandType() | Syntax.RepeatableType()}, 2);
        }

        @Override
        public void perform(Argument args[], Context ctx) throws ExtensionException, org.nlogo.api.LogoException {
            AbstractWorkspaceScala parentWS = (AbstractWorkspaceScala) ctx.workspace();

            String modelPath = getModelPath((ExtensionContext) ctx, args[1].getString());
            try {
                for (int i=0; i < args[0].getIntValue(); i++) {
                    ChildModel model;
                    if (modelType == HeadlessChildModel.class || GraphicsEnvironment.isHeadless() || parentWS.behaviorSpaceRunNumber() != 0) {
                        model = new HeadlessChildModel((AbstractWorkspaceScala) ctx.workspace(), modelPath, modelCounter);
                    } else {
                        model = new GUIChildModel(LevelSpace.this, (AbstractWorkspaceScala) ctx.workspace(), modelPath, modelCounter);
                        Workspace rootWS = App.app().workspace();
                        if (rootWS instanceof GUIWorkspace) {
                            model.setSpeed(((GUIWorkspace) rootWS).updateManager().speed());
                        }
                    }
                    model.workspace().behaviorSpaceRunNumber(parentWS.behaviorSpaceRunNumber());
                    model.workspace().behaviorSpaceExperimentName(parentWS.behaviorSpaceExperimentName());
                    models.put(modelCounter, model);
                    if (args.length > 2) {
                        args[2].getCommand().perform(ctx, new Object[]{(double) modelCounter});
                    }
                    modelCounter++;
                }
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

    public void updateModelMenu() {
        Runnable reportModelOpened = new Runnable() {
            @Override
            public void run() {
                modelManager.updateChildModels(models);
            }
        };
        EventQueue$.MODULE$.invokeLater(reportModelOpened);
    }

    public void reset() throws ExtensionException, HaltException {
        modelCounter = 0;

        for (ChildModel model : models.values()){
            model.kill();
        }
        models.clear();
    }

    public class Reset implements Command {
        public Syntax getSyntax() {
            return SyntaxJ.commandSyntax();
        }
        public void perform(Argument args[], Context context)
                throws org.nlogo.api.LogoException, ExtensionException {
            reset();
        }
    }

    public ChildModel[] toModelList(Argument arg) throws LogoException, ExtensionException {
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

    public void closeModel(ChildModel model) throws ExtensionException, HaltException {
        model.kill();
        models.remove(model.modelID());
        updateModelMenu();
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

    private void haltChildModels(Map<Integer, ChildModel> models){
        for (ChildModel aModel : models.values()){
            aModel.halt();
        }
    }
}
