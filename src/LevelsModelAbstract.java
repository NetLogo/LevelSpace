import org.nlogo.agent.Agent;
import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.World;
import org.nlogo.app.App;
import org.nlogo.nvm.*;

import javax.swing.*;
import java.util.concurrent.*;


public abstract class LevelsModelAbstract {

	abstract public void command(String command) throws CompilerException, LogoException, ExtensionException, ExecutionException;
	abstract public Object report(String reporter) throws ExtensionException, LogoException, CompilerException, ExecutionException;
	public void command(Context context, CommandTask command, Object[] args) throws ExtensionException {
		checkTask(command);
		Agent oldAgent = context.agent;
		context.agent = workspace().world().observer();
		context.agentBit = context.agent.getAgentBit();
		command.perform(context, args);
		context.agent = oldAgent;
		context.agentBit = context.agent.getAgentBit();
	}
	public Object report(Context context, ReporterTask reporter, Object[] args) throws ExtensionException {
		checkTask(reporter);
		Agent oldAgent = context.agent;
		context.agent = workspace().world().observer();
		context.agentBit = context.agent.getAgentBit();
		Object result = reporter.report(context, args);
		context.agent = oldAgent;
		context.agentBit = context.agent.getAgentBit();
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

	abstract public void kill() throws HaltException;
	abstract public String getPath();
	abstract public String getName();
	abstract public void breathe();
	abstract public void setSpeed(double d);
	abstract public void halt();
	abstract public Workspace workspace();

	abstract JFrame frame();

	public boolean usesLevelsSpace() {
		for (Object cm : this.workspace().getExtensionManager().loadedExtensions()) {
			if ("class LevelsSpace".equals(cm.getClass().toString())) {
				return true;
			}
		}
		return false;
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
	 * @param world The world to synchronize on. Should be the main model's world.
	 * @param callable What to run.
	 * @return
	 */
	public <T> T runSafely(final Callable<T> callable) throws HaltException, ExecutionException {
		final World world = App.app().workspace().world();
		final FutureTask<T> reporterTask = new FutureTask<T>(new Callable<T>() {
			@Override
			public T call() throws Exception {
				T result = callable.call();
				synchronized (world) {
					world.notify();
				}
				return result;
			}
		});
		safeExecutor.execute(reporterTask);
		while (!reporterTask.isDone()) {
			synchronized (world) {
				try {
					world.wait(50);
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
		}
	}
}
