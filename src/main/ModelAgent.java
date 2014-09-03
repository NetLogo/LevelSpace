import org.nlogo.agent.AgentSet;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.api.Task;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.ReporterTask;


public class ModelAgent implements Agent {
	Model model;
	double who;
	
	public ModelAgent(Model model, double who){
		this.model = model;
		this.who = who;
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
		Context context = new Context(parentContext, agent);
		context.agent = agent;
		return wrap(model.report(context, reporter, args));
	}

	public <T extends Task> T compile(Class<T> taskType, String code) throws ExtensionException {
		Object task = model.report("task [ " + code + " ]");
		if (taskType.isInstance(task)) {
			return taskType.cast(task);
		} else {
			throw new ExtensionException(String.format("Needed a %s but `%s` compiles to a %s.", taskType, code, task.getClass()));
		}
	}

	public Object wrap(Object reporterResult) {
		if (reporterResult instanceof org.nlogo.agent.Agent) {
			return new TPLAgent(this, (org.nlogo.agent.Agent) reporterResult);
		} else if (reporterResult instanceof LogoList) {
			LogoList result = (LogoList) reporterResult;
			LogoListBuilder wrappedList = new LogoListBuilder();
			for(Object elem : result) {
				wrappedList.add(wrap(elem));
			}
			return wrappedList.toLogoList();
		} else if (reporterResult instanceof AgentSet) {
			return new TPLAgentSet(this, (AgentSet) reporterResult);
		}else {
			return reporterResult;
		}
	}

	public LogoList allInfo() {
		LogoListBuilder allModelInfo = new LogoListBuilder();
		LogoListBuilder baseInfo = new LogoListBuilder();
		baseInfo.add(this);
		baseInfo.add(who);
		baseInfo.add(model.getPath());
		allModelInfo.add(baseInfo.toLogoList());
		allModelInfo.add(model.listGlobals());
		allModelInfo.add(model.listBreedsOwns());
		return allModelInfo.toLogoList(); 
	}
	
	public double who(){
		return who;
	}
	
}


