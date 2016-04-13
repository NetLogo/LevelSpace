package org.nlogo.ls;

import java.awt.*;
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
import org.nlogo.core.*;
import org.nlogo.nvm.*;
import org.nlogo.nvm.Reporter;
import org.nlogo.prim.*;
import org.nlogo.api.Workspace;
import org.nlogo.workspace.AbstractWorkspaceScala;

import scala.Function1;
import scala.collection.Seq;
import scala.Tuple2;
import scala.runtime.BoxedUnit;
import scala.runtime.AbstractFunction1;

public abstract class ChildModel {
    private final World parentWorld;
    private JobOwner owner;
    private Procedure reporterRunner;
    private Procedure commandRunner;
    private LoadingCache<String, Reporter> tasks;
    private String name;
    private int modelID;
    private NotifyingJob lastJob;

    private Evaluator evaluator;

    public ChildModel(World parentWorld, int modelID) throws ExtensionException {
        this.parentWorld = parentWorld;
        this.modelID = modelID;

        tasks = CacheBuilder.newBuilder().build(new CacheLoader<String, Reporter>() {
            @Override
            public Reporter load(String code) throws ExtensionException {
                try {
                    return compileTaskReporter(code);
                } catch (CompilerException e) {
                    ErrorUtils.wrap(ChildModel.this, e);
                    return null;
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
        owner = new SimpleJobOwner(name(), workspace().world().mainRNG(), AgentKindJ.Observer());
        evaluator = new Evaluator(name(), workspace());
    }

    public Evaluator evaluator() {
        return evaluator;
    }

    public Function1<World, BoxedUnit> ask(String code, Seq<Tuple2<String, Object>> lets, Seq<Object> args) throws ExtensionException {
        try {
            final Function1<World, BoxedUnit> future = evaluator().command(code, lets, args);
            return ErrorUtils.handle(this, future);
        } catch (Exception e) {
            ErrorUtils.wrap(this, e);
            return null;
        }
    }

    public Function1<World, Object> of(String code, Seq<Tuple2<String, Object>> lets, Seq<Object> args) throws ExtensionException {
        try {
            final Function1<World, Object> future = evaluator().report(code, lets, args);
            return ErrorUtils.handle(this, future);
        } catch (Exception e) {
            ErrorUtils.wrap(this, e);
            return null;
        }
    }

    final public void kill() throws ExtensionException, HaltException {
        // We can't run this synchronously at all. I kept getting freezes when closing/quitting/opening new models
        // through the GUI. It looks like the EDT can't wait for the job thread to die. BCH 1/15/2015

        // workspace.dispose() calls jobManager.die()
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    workspace().dispose();
                } catch (InterruptedException e) {
                    // fine
                }
            }
        }).start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (frame() != null) {
                    frame().dispose();
                }
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

    public int getModelID() {
        return modelID;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (this.frame() != null) {
            this.frame().setTitle(getFrameTitle());
        }
    }

    abstract public void setSpeed(double d);
    abstract public AbstractWorkspaceScala workspace();
    abstract JFrame frame();

    Class<?> getLevelSpace() {
        for (Object cm : this.workspace().getExtensionManager().loadedExtensions()) {
            if ("class LevelSpace".equals(cm.getClass().toString())) {
                return cm.getClass();
            }
        }
        return null;
    }

    public boolean usesLevelSpace() {
        if (getLevelSpace() == LevelSpace.class) {
            System.err.println("same ls");
        }
        return getLevelSpace() != null;
    }

    public void show() throws ExtensionException {
        frame().setVisible(true);
    }

    public void hide() {
        frame().setVisible(false);
    }

    public <T> T runUISafely(final Callable<T> callable) throws ExtensionException, HaltException {
        // waitFor is unsafe on the event queue, so if we're on the event queue, just run directly.
        if (SwingUtilities.isEventDispatchThread()) {
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
            if (e.getCause() != null && e.getCause() instanceof ExtensionException) {
                throw (ExtensionException) e.getCause();
            } else {
                throw new ExtensionException(e);
            }
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
        return workspace().compileReporter("task [ " + code + " ]").code()[0].args[0];
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
        reporterRunner.code()[0].args[0].args = makeArgumentArray(task, taskArgs);
        return reporterRunner;
    }

    private Procedure getCommandRunner(Reporter task, Object[] taskArgs) {
        commandRunner.code()[0].args = makeArgumentArray(task, taskArgs);
        return commandRunner;
    }
}
