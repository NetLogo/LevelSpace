package org.nlogo.ls;

import org.nlogo.api.Argument;
import org.nlogo.api.Command;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultClassManager;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.ExtensionManager;
import org.nlogo.api.ImportErrorHandler;
import org.nlogo.api.LogoException;
import org.nlogo.api.PrimitiveManager;
import org.nlogo.app.App;
import org.nlogo.app.ToolsMenu;
import org.nlogo.awt.EventQueue$;
import org.nlogo.core.CompilerException;
import org.nlogo.core.LogoList;
import org.nlogo.core.Syntax;
import org.nlogo.core.SyntaxJ;
import org.nlogo.nvm.ExtensionContext;
import org.nlogo.nvm.HaltException;
import org.nlogo.window.GUIWorkspace;
import org.nlogo.workspace.AbstractWorkspaceScala;

import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class LevelSpace extends DefaultClassManager {

    private static boolean isHeadless() {
      return GraphicsEnvironment.isHeadless() || Objects.equals(System.getProperty("org.nlogo.preferHeadless"), "true");
    }

    // This can be accessed by both the JobThread and EDT (when halting)
    private final Map<Integer, org.nlogo.ls.ChildModel> models = new ConcurrentHashMap<>();

    // counter for keeping track of new models
    private int modelCounter = 0;

    public LetPrim letManager = new LetPrim();

    // These need to be cleaned up on unload
    private JMenuItem haltButton;
    private ActionListener haltListener = ignored -> haltChildModels();

    private LSModelManager modelManager = isHeadless() ? new HeadlessBackingModelManager() : new BackingModelManager();

    @Override
    public void load(PrimitiveManager primitiveManager) throws ExtensionException {
        primitiveManager.addPrimitive("let", letManager);
        primitiveManager.addPrimitive("ask", new Ask(this));
        primitiveManager.addPrimitive("of", new Of(this));
        primitiveManager.addPrimitive("report", new Report(this));
        primitiveManager.addPrimitive("with", new With(this));
        primitiveManager.addPrimitive("create-models", new CreateModels<>(HeadlessChildModel.class));
        primitiveManager.addPrimitive("create-interactive-models", new CreateModels<>(GUIChildModel.class));
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


        if (!isHeadless()) {
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

    public List<Integer> modelList() {
        List<Integer> modelList = new ArrayList<>(models.keySet());
        Collections.sort(modelList);
        return modelList;
    }

    private static int castToId(Object id) throws ExtensionException {
        if (id instanceof Number) {
            return ((Number) id).intValue();
        } else {
            throw new ExtensionException("Expected a model ID but got: " + id);
        }
    }


    @Override
    public void unload(ExtensionManager em) throws ExtensionException {
        if (!isHeadless() && isMainModel(em)) {
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
                    if (modelType == HeadlessChildModel.class || isHeadless() || parentWS.behaviorSpaceRunNumber() != 0) {
                        model = new HeadlessChildModel((AbstractWorkspaceScala) ctx.workspace(), modelPath, modelCounter);
                    } else {
                        model = new GUIChildModel(LevelSpace.this, ctx.workspace(), modelPath, modelCounter);
                        GUIWorkspace rootWS = App.app().workspace();
                        model.setSpeed(rootWS.updateManager().speed());
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

    private void updateModelMenu() {
        Runnable reportModelOpened = () -> modelManager.updateChildModels(models);
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
            for (Object modelIDObj : arg.getList().javaIterable()) {
                models[i] = getModel(castToId(modelIDObj));
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
    public void importWorld(List<String[]> arg0, ExtensionManager arg1,
                            ImportErrorHandler arg2) throws ExtensionException {
        // TODO
    }

    @Override
    public void runOnce(ExtensionManager em) throws ExtensionException {
        modelManager.updateChildModels(models);

        if (!isHeadless() && isMainModel(em)) {
            final JMenuBar menuBar = App.app().frame().getJMenuBar();
            if (menuBar.getComponentIndex(modelManager.guiComponent()) == -1) {
                menuBar.add(modelManager.guiComponent());
            }
        }
    }

    private void haltChildModels(){
        for (ChildModel aModel : models.values()){
            aModel.halt();
        }
    }
}
