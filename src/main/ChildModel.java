import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import org.nlogo.agent.Agent;
import org.nlogo.api.*;
import org.nlogo.nvm.*;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.Reporter;
import org.nlogo.nvm.ReporterTask;
import org.nlogo.prim.*;
import org.nlogo.workspace.AbstractWorkspace;


public abstract class ChildModel {
    private final World parentWorld;
    private JobOwner owner;
    private Procedure reporterRunner;
    private Procedure commandRunner;
    private LoadingCache<String, Reporter> tasks;

    public ChildModel(World parentWorld) throws ExtensionException {
        this.parentWorld = parentWorld;

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
        try {
            reporterRunner = workspace().compileReporter("runresult task [ 0 ]");
            commandRunner = workspace().compileCommands("run task []");
        } catch (CompilerException e) {
            throw new ExtensionException("There is a bug in LevelSpace! Please report! ", e);
        }
        owner = new SimpleJobOwner(getName(), workspace().world.mainRNG, Observer.class);
    }

    public void command(final Reporter task, final Object[] args) throws ExtensionException, HaltException {
        runSafely(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                workspace().runCompiledCommands(owner, getCommandRunner(task, args));
                return null;
            }
        });
    }

    public Object report(final Reporter task, final Object[] args) throws ExtensionException, HaltException {
        Object result = runSafely(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return workspace().runCompiledReporter(owner, getReporterRunner(task, args));
            }
        });
        checkResult(result);
        return result;
    }

    public void ask(String command, Object[] actuals) throws ExtensionException, HaltException {
        command(tasks.getUnchecked(command), actuals);
    }

    public Object of(String reporter, Object[] actuals) throws ExtensionException, HaltException {
        return report(tasks.getUnchecked(reporter), actuals);
    }

    void checkResult(Object reporterResult) throws ExtensionException {
        if (reporterResult instanceof org.nlogo.agent.Agent || reporterResult instanceof AgentSet) {
            throw new ExtensionException("You cannot report agents or agentsets from LevelSpace models.");
        } else if (reporterResult instanceof LogoList) {
            LogoList resultList = (LogoList)reporterResult;
            for(Object elem : resultList) {
                checkResult(elem);
            }
        }
    }

    final public void kill() throws ExtensionException {
        if(usesLevelsSpace()) {
            Class<?> ls = getLevelSpace();
            if (ls != LevelsSpace.class) {
                try {
                    ls.getMethod("reset").invoke(null);
                } catch (IllegalAccessException e) {
                    throw new ExtensionException("This is a bug in LevelSpace! Please report!", e);
                } catch (NoSuchMethodException e) {
                    throw new ExtensionException("This is a bug in LevelSpace! Please report!", e);
                } catch (InvocationTargetException e) {
                    throw new ExtensionException("This is a bug in LevelSpace! Please report!", e);
                }
            }
        }

        killJobThread();
        killLifeguard();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (frame() != null) {
                    frame().dispose();
                }
            }
        });
    }

    private void killJobThread() {
        try {
            ((AbstractWorkspace) workspace()).jobManager.die();
        } catch (InterruptedException e) {
            // we can safely ignore this I think
        }
    }

    private void killLifeguard() {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getName().equals("Lifeguard")) {
                try {
                    Field outerField = thread.getClass().getDeclaredField("this$0");
                    outerField.setAccessible(true);
                    Object outer = outerField.get(thread);
                    if (outer == workspace()) {
                        thread.interrupt();
                        thread.join();
                    }
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException("There is a bug in LevelSpace! Please report this.", e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("There is a bug in LevelSpace! Please report this.", e);
                } catch (InterruptedException e) {
                    throw new RuntimeException("There is a bug in LevelSpace! Please report this.", e);
                }
            }
        }

    }

    public void halt() {
        workspace().halt();
    }

    public String getPath() {
        return workspace().getModelPath();
    }

    public String getName() {
        return workspace().modelNameForDisplay();
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

    public LogoList listGlobals() {
        LogoListBuilder llb = new LogoListBuilder();

        for (Object var : workspace().world().observer().variables()){
            llb.add(var);
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
    public <T> T runSafely(final Callable<T> callable) throws HaltException, ExtensionException {
        final FutureTask<T> reporterTask = new FutureTask<T>(new Callable<T>() {
            @Override
            public T call() throws Exception {
                T result = callable.call();
                synchronized (parentWorld) {
                    parentWorld.notify();
                }
                return result;
            }
        });
        safeExecutor.execute(reporterTask);
        while (!reporterTask.isDone()) {
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
            return reporterTask.get();
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
}
