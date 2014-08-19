import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.swing.JFrame;

import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.World;
import org.nlogo.app.App;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.ReporterTask;
import org.nlogo.nvm.Workspace;


public abstract class Model {

	abstract public void command(String command) throws ExtensionException;
	abstract public Object report(String reporter) throws ExtensionException;
	public void command(Context context, CommandTask command, Object[] args) throws ExtensionException {
		checkTask(command);
		synchronized (workspace().world()) {
			command.perform(context, args);
		}
	}
	public Object report(Context context, ReporterTask reporter, Object[] args) throws ExtensionException {
		checkTask(reporter);
		Object result = null;
		synchronized (workspace().world()) {
			result = reporter.report(context, args);
		}
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
	
	public int levelsSpaceNumber;


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
	public <T> T runSafely(final Callable<T> callable) throws HaltException, ExtensionException {
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
		} catch (ExecutionException e) {
			throw new ExtensionException(e);
		}
	}
	public LogoListBuilder getDescendants() {
		// TODO Auto-generated method stub
		return null;
	}

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
		for (Entry<String, List<String>> entry : workspace().world().program().breedsOwn().entrySet())
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
		for (int i = 0; i < workspace().world().observer().getVariableCount(); i++){
			llb.add(workspace().world().observer().variableName(i));
		}
		return llb.toLogoList();
	}

}
