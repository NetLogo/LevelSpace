import java.util.HashSet;

import org.nlogo.api.Argument;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;


public class LevelSpaceAgentSet extends HashSet<Agent> implements Agent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public Object of(String s) throws ExtensionException {
		// AH: I am not sure this should be a LogoList, but if we just make it a LogoList by 
		// default, it's easy to work with in NetLogo.
		// The alternative is to evaluate what the first model returns, and then 
		// cast everything from then on as that. I *think* i prefer the former,
		// although it does mean that people might end up with a LogoList of a bunch of differnet
		// kinds of objects. Which might be good, and might be bad.
		LogoListBuilder llb = new LogoListBuilder();
		for (Agent agent : this){
			llb.add(agent.of(s));
		}
		return llb.toLogoList();
	}

	@Override
	public void ask(Argument[] args, org.nlogo.api.Context context) {
		for (Agent agent : this){
			try {
				agent.ask(args, context);
			} catch (ExtensionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LogoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}
