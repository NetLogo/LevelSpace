import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.ReporterTask;
import org.nlogo.nvm.Workspace;
import org.nlogo.workspace.AbstractWorkspace;


public abstract class ChildModel {


    private final World parentWorld;

    public ChildModel(World parentWorld){
        this.parentWorld = parentWorld;
        CacheLoader<String, ReporterTask> reporterLoader =
                new CacheLoader<String, ReporterTask>() {
                    public ReporterTask load(String reporterString) {
                        return compileReporterTask(reporterString);
                    }
                };
        reporters =
                CacheBuilder.newBuilder()
                        .build(reporterLoader);

        CacheLoader<String, CommandTask> commandLoader =
                new CacheLoader<String, CommandTask>() {
                    public CommandTask load(String reporterString) {
                        return compileCommandTask(reporterString);
                    }
                };
        commands =
                CacheBuilder.newBuilder()
                        .build(commandLoader);
    }

    abstract public void command(String command) throws ExtensionException;
    abstract public Object report(String reporter) throws ExtensionException;
    public void command(Context context, CommandTask command, Object[] args) throws ExtensionException {
        checkTask(command);
        Agent oldAgent = context.agent;
        context.agent = workspace().world().observer();
        context.agentBit = context.agent.getAgentBit();
        synchronized (workspace().world()) {
            command.perform(context, args);
        }
        context.agent = oldAgent;
        context.agentBit = context.agent.getAgentBit();
    }
    public Object report(Context context, ReporterTask reporter, Object[] args) throws ExtensionException {
        checkTask(reporter);
        Agent oldAgent = context.agent;
        context.agent = workspace().world().observer();
        context.agentBit = context.agent.getAgentBit();
        Object result = null;
        synchronized (workspace().world()) {
            result = reporter.report(context, args);
        }
        context.agent = oldAgent;
        context.agentBit = context.agent.getAgentBit();
        // check if result contains any agents or agentsets
        checkResult(result);
        return result;
    }
    public void checkTask(CommandTask task) throws ExtensionException {
        if (task.procedure().code.length > 0 && task.procedure().code[0].workspace != workspace()) {
            throw new ExtensionException("You can only run a task in the model that it was created in.");
        }
    }
    public void checkTask(ReporterTask task) throws ExtensionException {
        if (task.body().workspace != workspace()) {
            throw new ExtensionException("You can only run a task in the model that it was created in.");
        }
    }

    public void ask(Context context, String command, Object[] actuals) throws ExtensionException{
        command(context, commands.getUnchecked(command), actuals);
    }

    public void ask(Context context, CommandTask task, Object[] actuals) throws ExtensionException {
        command(context, task, actuals);
    }

    public Object of(Context context, String reporter, Object[] actuals) throws ExtensionException{
        return report(context, reporters.getUnchecked(reporter), actuals);
    }

    public Object of(Context context, ReporterTask task, Object[] actuals) throws ExtensionException {
        return report(context, task, actuals);
    }

    public void checkResult(Object reporterResult) throws ExtensionException {
        if (reporterResult instanceof org.nlogo.agent.Agent || reporterResult instanceof AgentSet) {
            throw new ExtensionException("You cannot report agents or agentsets from LevelSpace models.");
        }
        else if (reporterResult instanceof LogoList) {
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

    abstract public String getPath();
    abstract public String getName();
    abstract public void breathe();
    abstract public void setSpeed(double d);
    abstract public void halt();
    abstract public Workspace workspace();
    abstract public LogoList listBreeds();
    abstract public LogoList listBreedsOwns();
    abstract public LogoList listGlobals();

    LoadingCache<String, CommandTask> commands;
    LoadingCache<String, ReporterTask> reporters;
    public int levelsSpaceNumber;

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
    public LogoListBuilder getDescendants() {
        // TODO Auto-generated method stub
        return null;
    }

    public ReporterTask compileReporterTask(String s){
        ReporterTask r = null;
        try {
            r = (ReporterTask)report("task [ " + s + "]");
        } catch (ExtensionException e) {
            e.printStackTrace();
        }
        return r;
    }

    public CommandTask compileCommandTask(String s){
//        LevelsSpace.showMessage("Compiling Command: " + s);
        CommandTask t = null;
        try {
            t = (CommandTask)report("task [ " + s + "]");
        } catch (ExtensionException e) {
            e.printStackTrace();
        }
        return t;
    }


}
