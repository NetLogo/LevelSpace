import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.ReporterTask;


/**
 * Agent wrapper for turtle, patches, and links.
 */
public class TPLAgent implements Agent {
	private ModelAgent parentModel;
	private org.nlogo.agent.Agent agent;

	public TPLAgent(ModelAgent parentModel, org.nlogo.agent.Agent agent){
		this.parentModel = parentModel;
		this.agent = agent;
	}
	
	@Override
	public void ask(org.nlogo.nvm.Context parentContext, String command, Object[] args) throws ExtensionException, LogoException {
		parentModel.ask(parentContext, agent, command, args);
	}

	@Override
	public void ask(org.nlogo.nvm.Context parentContext, CommandTask command, Object[] args) throws ExtensionException, LogoException {
		parentModel.ask(parentContext, agent, command, args);

	}

	@Override
	public Object of(org.nlogo.nvm.Context parentContext, String reporter, Object[] args) throws ExtensionException, LogoException {
		return parentModel.of(parentContext, agent, reporter, args);
	}

	@Override
	public Object of(org.nlogo.nvm.Context parentContext, ReporterTask reporter, Object[] args) throws ExtensionException, LogoException {
		return parentModel.of(parentContext, agent, reporter, args);
	}

	public org.nlogo.agent.Agent getBaseAgent() {
		return agent;
	}

	public ModelAgent getModel() {
		return parentModel;
	}

	@Override
	public boolean equals(Object other) {
		return other != null &&
				other instanceof TPLAgent &&
				((TPLAgent) other).agent.equals(agent);
	}
	
	@Override
	public int hashCode() {
		// Could change this to incorporate the parentModel hashCode, but that seems silly.
		// Memory addresses for agents will obviously be unique across all models and I believe
		// that's the default hashCode.
		// BCH 8/5/2014
		return agent.hashCode();
	}
}
