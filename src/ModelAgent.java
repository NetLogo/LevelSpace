import org.nlogo.agent.*;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.nvm.ReporterTask;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.api.Task;


public class ModelAgent implements Agent {
	Model model;
	
	public ModelAgent(Model model){
		this.model = model;
	}
	

	@Override
	public void ask(Context parentContext, String command, Object[] args) throws ExtensionException, LogoException{
		ask(parentContext, compile(CommandTask.class, command), args);
	}

	@Override
	public void ask(Context parentContext, CommandTask command, Object[] args) throws ExtensionException, LogoException {
		ask(parentContext, model.workspace().world().observer(), command, args);
	}

	public void ask(Context parentContext, org.nlogo.agent.Agent agent, String command, Object[] args) throws ExtensionException, LogoException {
		ask(parentContext, agent, compile(CommandTask.class, command), args);
	}

	public void ask(Context parentContext, org.nlogo.agent.Agent agent, CommandTask command, Object[] args) throws ExtensionException, LogoException {
		Context context = new Context(parentContext, agent);
		context.agent = agent;
		model.command(context, command, args);
	}

	@Override
	public Object of(Context parentContext, String reporter, Object[] args) throws ExtensionException, LogoException {
		return of(parentContext, compile(ReporterTask.class, reporter), args);
	}

	@Override
	public Object of(Context parentContext, ReporterTask reporter, Object[] args) throws ExtensionException, LogoException {
		return of(parentContext, model.workspace().world().observer(), reporter, args);
	}

	public Object of(Context parentContext, org.nlogo.agent.Agent agent, String reporter, Object[] args) throws ExtensionException, LogoException {
		return of(parentContext, agent, compile(ReporterTask.class, reporter), args);
	}

	public Object of(Context parentContext, org.nlogo.agent.Agent agent, ReporterTask reporter, Object[] args) throws ExtensionException, LogoException {
		return model.report(new Context(parentContext, agent), reporter, args);
	}

	public <T extends Task> T compile(Class<T> taskType, String code) throws ExtensionException {
		Object task = model.report("task [ " + code + " ]");
		if (taskType.isInstance(task)) {
			return taskType.cast(task);
		} else {
			throw new ExtensionException(String.format("Needed a %s but `%s` compiles to a %s.", taskType, code, task.getClass()));
		}
	}
}


