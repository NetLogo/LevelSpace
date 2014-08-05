import java.util.AbstractSet;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.ReporterTask;


public abstract class AgentSetAgent<T extends Agent> extends AbstractSet<T> implements Agent {
	@Override
	public void ask(Context parentContext, String command, Object[] args) throws ExtensionException, LogoException {
		for (T agent : this) { agent.ask(parentContext, command, args); }
	}

	@Override
	public void ask(Context parentContext, CommandTask command, Object[] args) throws ExtensionException, LogoException {
		for (T agent : this) { agent.ask(parentContext, command, args); }
	}

	@Override
	public Object of(Context parentContext, String reporter, Object[] args) throws ExtensionException, LogoException {
		LogoListBuilder llb = new LogoListBuilder();
		for (T agent : this){
			llb.add(agent.of(parentContext, reporter, args));
		}
		return llb.toLogoList();
	}

	@Override
	public Object of(Context parentContext, ReporterTask reporter, Object[] args) throws ExtensionException, LogoException {
		LogoListBuilder llb = new LogoListBuilder();
		for (T agent : this){
			llb.add(agent.of(parentContext, reporter, args));
		}
		return llb.toLogoList();
	}
}
