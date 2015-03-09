import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.swing.*;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.nlogo.api.*;
import org.nlogo.api.Task;
import org.nlogo.app.App;
import org.nlogo.nvm.*;
import org.nlogo.nvm.Reporter;
import org.nlogo.prim.*;
import org.nlogo.workspace.AbstractWorkspace;


import scala.collection.JavaConversions;


public abstract class ChildModel {
    private final World parentWorld;
    private JobOwner owner;
    private Procedure reporterRunner;
    private Procedure commandRunner;
    private LoadingCache<String, Reporter> tasks;
    private String name;
    private int modelID;

    public ChildModel(World parentWorld, int modelID) throws ExtensionException {
        this.parentWorld = parentWorld;
        this.modelID = modelID;

        tasks = CacheBuilder.newBuilder().build(new CacheLoader<String, Reporter>() {
            @Override
            public Reporter load(String code) throws ExtensionException {
                try {
                    return compileTaskReporter(code);
                } catch (CompilerException e) {
                    throw ErrorUtils.handle(ChildModel.this, code, e);
                }
            }
        });
    }

    /**
     * This must be called by child class constructors. It initializes things using the model's workspace.
     * @throws ExtensionException
     */
    void init() throws ExtensionException {
        setName(workspace().getModelFileName());
        try {
            reporterRunner = workspace().compileReporter("runresult task [ 0 ]");
            commandRunner = workspace().compileCommands("run task []");
        } catch (CompilerException e) {
            throw new ExtensionException("There is a bug in LevelSpace! Please report! ", e);
        }
        owner = new SimpleJobOwner(getName(), workspace().world.mainRNG, Observer.class);
    }

    public void command(final Reporter task, final Object[] args) throws ExtensionException, HaltException {
        runNlogoSafely(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                workspace().runCompiledCommands(owner, getCommandRunner(task, args));
                return null;
            }
        });
    }

    public Object report(final Reporter task, final Object[] args) throws ExtensionException, HaltException {
        Object result = runNlogoSafely(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return workspace().runCompiledReporter(owner, getReporterRunner(task, args));
            }
        });
        checkResult(result);
        return result;
    }

    public void ask(String command, Object[] actuals) throws ExtensionException, HaltException {
        try {
            command(tasks.get(command), actuals);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ExtensionException) {
                throw (ExtensionException) e.getCause();
            } else {
                throw ErrorUtils.handle(this, command, e);
            }
        }
    }

    public Object of(String reporter, Object[] actuals) throws ExtensionException, HaltException {
        try {
            return report(tasks.get(reporter), actuals);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ExtensionException) {
                throw (ExtensionException) e.getCause();
            } else {
                throw ErrorUtils.handle(this, reporter, e);
            }
        }
    }

    void checkResult(Object reporterResult) throws ExtensionException {
        if (reporterResult instanceof org.nlogo.agent.Agent || reporterResult instanceof AgentSet) {
//            throw new ExtensionException("You cannot report agents or agentsets from LevelSpace models.");
        } else if (reporterResult instanceof LogoList) {
            LogoList resultList = (LogoList)reporterResult;
            for(Object elem : resultList) {
                checkResult(elem);
            }
        }
    }

    final public void kill() throws ExtensionException, HaltException {
        // We can't run this synchronously at all. I kept getting freezes when closing/quitting/opening new models
        // through the GUI. It looks like the EDT can't wait for the job thread to die. BCH 1/15/2015
        runLater(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    try {
                        workspace().dispose();
                    } catch (UnsupportedOperationException e) {
                        // In 5.1, you can't do dispose with GUIWorkspace
                        workspace().jobManager.die();
                        workspace().getExtensionManager().reset();
                        // This leaves LifeGuard up, but we're leaking permgen anyway, so whatever
                    }
                } catch (InterruptedException e) {
                    // ok
                }
                return null;
            }
        });

        runUISafely(new Callable<Object>() {
            @Override
            public Object call() {
                if (frame() != null) {
                    frame().dispose();
                }
                return null;
            }
        });
    }

    public void halt() {
        workspace().halt();
    }

    public String getPath() {
        return workspace().getModelPath();
    }

    String getFrameTitle() {
        return name + " (LevelSpace model #" + modelID + ")";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (this.frame() != null) {
            this.frame().setTitle(getFrameTitle());
        }
    }

    abstract public void setSpeed(double d);
    abstract public AbstractWorkspace workspace();

    public LogoList listBreeds() {
        LogoListBuilder llb = new LogoListBuilder();
        for (String entry : workspace().world().getBreeds().keySet())
        {
            llb.add(entry);
        }
        return llb.toLogoList();
    }

    public LogoList listBreedsOwns() {
        LogoListBuilder llb = new LogoListBuilder();
        for (Map.Entry<String, List<String>> entry : workspace().world().program().breedsOwn().entrySet())
        {
            LogoListBuilder tuple  = new LogoListBuilder();
            LogoListBuilder vars = new LogoListBuilder();
            for (String s : entry.getValue()){
                vars.add(s);
            }
            // add turtles own to all of them too
            for (String s: workspace().world().program().turtlesOwn()){
                vars.add(s);
            }
            tuple.add(entry.getKey());
            tuple.add(vars.toLogoList());
            llb.add(tuple.toLogoList());
        }
        return llb.toLogoList();

    }

    public LogoList listObserverProcedures(){
        return null;
    }

    public LogoList listTurtleProcedures(){
        return null;
    }

    public LogoList listGlobals() {
        LogoListBuilder llb = new LogoListBuilder();

        for (int i = 0; i < workspace().world().observer().getVariableCount(); i++){
            llb.add(workspace().world().observer().variableName(i));
        }
        return llb.toLogoList();
    }

    abstract JFrame frame();

    Class<?> getLevelSpace() {
        for (Object cm : this.workspace().getExtensionManager().loadedExtensions()) {
            if ("class LevelsSpace".equals(cm.getClass().toString())) {
                return cm.getClass();
            }
        }
        return null;
    }

    public boolean usesLevelsSpace() {
        if (getLevelSpace() == LevelsSpace.class) {
            System.err.println("same ls");
        }
        return getLevelSpace() != null;
    }

    public void show() {
        frame().setVisible(true);
    }

    public void hide() {
        frame().setVisible(false);
    }

    // Probably only want a single job to run at a time.
    private static Executor safeExecutor = Executors.newSingleThreadExecutor();
    /**
     * Runs the given callable such that it doesn't create a deadlock between
     * the AWT event thread and the JobThread. It does this using a similar
     * technique as ThreadUtils.waitForResponse().
     * @param callable What to run.
     * @return
     */
    public <T> T runNlogoSafely(final Callable<T> callable) throws HaltException, ExtensionException {
        FutureTask<T> task = makeTask(callable);
        safeExecutor.execute(task);
        return waitFor(task);
    }

    public <T> T runUISafely(final Callable<T> callable) throws ExtensionException, HaltException {
        // waitFor is unsafe on the event queue, so if we're on the event queue, just run directly.
        if (EventQueue.isDispatchThread()) {
            try {
                return callable.call();
            } catch (ExtensionException e) {
                throw e;
            } catch (HaltException e) {
                throw e;
            } catch (Exception e) {
                throw new ExtensionException(e);
            }
        } else {
            FutureTask<T> task = makeTask(callable);
            SwingUtilities.invokeLater(task);
            return waitFor(task);
        }
    }

    public void runLater(final Callable<?> callable) throws ExtensionException, HaltException {
        safeExecutor.execute(makeTask(callable));
    }

    private <T> FutureTask<T> makeTask(final Callable<T> callable) {
        return new FutureTask<T>(new Callable<T>() {
            @Override
            public T call() throws Exception {
                T result = callable.call();
                synchronized (parentWorld) {
                    parentWorld.notify();
                }
                return result;
            }
        });
    }

    private <T> T waitFor(final FutureTask<T> task) throws HaltException, ExtensionException {
        while (!task.isDone()) {
            synchronized (parentWorld) {
                try {
                    parentWorld.wait(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new HaltException(false);
                }

            }
        }
        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HaltException(false);
        } catch (ExecutionException e) {
            throw new ExtensionException(e);
        }

    }

    /**
     * Creates a tasked wrapped in a reporter that can then be inserted into `run` or `runresult`.
     * Can be used on both commands and reporters.
     * @param code Command or reporter code in task syntax (with args and stuff).
     * @return The compiled task wrapped in a reporter.
     * @throws CompilerException
     */
    private Reporter compileTaskReporter (String code) throws CompilerException {
        return workspace().compileReporter("task [ " + code + " ]").code[0].args[0].args[0];
    }

    private Reporter makeConstantReporter(Object value) {
        // ConstantParser.makeConstantReporter is private, so had to make my own.
        if (value instanceof Boolean) {
            return new _constboolean((Boolean) value);
        } else if (value instanceof Double) {
            return new _constdouble((Double) value);
        } else if (value instanceof LogoList) {
            return new _constlist((LogoList) value);
        } else if (value instanceof String) {
            return new _conststring((String) value);
        } else {
            throw new IllegalArgumentException(value.getClass().getName());
        }
    }

    private Reporter[] makeArgumentArray(Reporter task, Object[] taskArgs) {
        Reporter[] args = new Reporter[1 + taskArgs.length];
        args[0] = task;
        for (int i=0; i<taskArgs.length; i++) {
            args[i+1] = makeConstantReporter(taskArgs[i]);
        }
        return args;
    }

    private Procedure getReporterRunner(Reporter task, Object[] taskArgs) {
        reporterRunner.code[0].args[0].args = makeArgumentArray(task, taskArgs);
        return reporterRunner;
    }

    private Procedure getCommandRunner(Reporter task, Object[] taskArgs) {
        commandRunner.code[0].args = makeArgumentArray(task, taskArgs);
        return commandRunner;
    }

    public LogoList getProcedures() {
        LogoListBuilder outerLLB = new LogoListBuilder();
        for (String pName : workspace().getProcedures().keySet()){
            LogoListBuilder pList = new LogoListBuilder();
            Procedure p = workspace().getProcedures().get(pName);
            pList.add(pName);
            pList.add(p.tyype.toString());
            pList.add(p.usableBy);
            LogoListBuilder argLLB = new LogoListBuilder();
            // args contains dummies (temp 'lets') so we don't include them.
            // localsCount contains number of lets so we just subtract that
            for (int i = 0; i < p.args.size() - p.localsCount;i++){
                String theString = p.args.get(i);
                argLLB.add(theString);
            }
//            p.syntax().
            pList.add(argLLB.toLogoList());
            outerLLB.add(pList.toLogoList());
        }
        return outerLLB.toLogoList();
    }
}
