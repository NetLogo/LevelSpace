import java.util.HashSet;

import org.nlogo.api.AgentSet;
import org.nlogo.api.Argument;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoList;
import org.nlogo.api.LogoListBuilder;


public class AgentSetAgent extends HashSet<Agent> implements Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AgentSetAgent(){
		super();
	}

	public AgentSetAgent(LogoList aList) throws ExtensionException {
		unpackAndAdd(aList);
	}
	
	public AgentSetAgent(AgentSet logoAgentSet, ModelAgent aModel){
		
	}

	void unpackAndAdd(LogoList aList) throws ExtensionException{
		for (Object anObject : aList.toArray()){
			if (anObject instanceof TurtleAgent){
				this.add((Agent) anObject);
			}
			else if (anObject instanceof AgentSetAgent){
				this.addAll((AgentSetAgent)anObject);
			}
			else if (anObject instanceof LogoList){
				unpackAndAdd((LogoList)anObject);
			}
			else{
				throw new ExtensionException("You can only send LSAgents, LSAgentsets and LogoLists to ls:turtle-set");
			}
		}

	}

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

	public AgentSetAgent with (String s) throws ExtensionException{
		AgentSetAgent returnSet = new AgentSetAgent();
		for (Agent anAgent : this){
			Object agentValue = anAgent.of(s);
			if (agentValue instanceof Boolean){
				if ((Boolean)agentValue){
					returnSet.add(anAgent);
				}
			}else{
				throw new ExtensionException("ls:with only accepts reporter booleans. The string " + s +
						"did not return a boolean.");
			}
		}
		return returnSet;
	}
}
