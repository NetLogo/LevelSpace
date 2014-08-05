import org.nlogo.agent.AgentSet;

import java.util.Iterator;

public class TPLAgentSet extends AgentSetAgent<TPLAgent> {
	private AgentSet agentSet;
	private ModelAgent parentModel;

	public TPLAgentSet(ModelAgent parentModel, AgentSet agentSet) {
		this.agentSet = agentSet;
		this.parentModel = parentModel;
	}
	@Override
	public int size() {
		return agentSet.count();
	}

	@Override
	public boolean contains(Object agent) {
		return agent != null && agent instanceof TPLAgent
				&& agentSet.contains(((TPLAgent) agent).getBaseAgent());
	}

	@Override
	public boolean equals(Object other) {
		return other != null && other instanceof TPLAgentSet
				&& ((TPLAgentSet) other).agentSet.equalAgentSets(agentSet);
	}

	@Override
	public int hashCode() {
		return agentSet.hashCode();
	}

	private class ASIterator implements Iterator<TPLAgent> {
		AgentSet.Iterator iter;

		public ASIterator(AgentSet.Iterator iter) {
			this.iter = iter;
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public TPLAgent next() {
			return new TPLAgent(parentModel, iter.next());
		}

		@Override
		public void remove() {
			iter.remove();
		}
	}

	@Override
	public Iterator<TPLAgent> iterator() {
		return new ASIterator(agentSet.iterator());
	}

}
