import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.ReporterTask;


// BCH 8/5/2014: If we see performance problems with iteration, might want to change
// this to a LinkedHashSet.
// Also, I'm not sure it's a good idea to inherit directly from HashSet. Might want to
// just have a HashSet as a field and maybe implement AbstractSet if necessary. Extending
// concrete java collections can have unintended consequences.
public class AgentSetAgent extends HashSet<Agent> implements Agent {
	@Override
	public void ask(Context parentContext, String command, Object[] args) throws ExtensionException, LogoException {
		for (Agent agent : this) { agent.ask(parentContext, command, args); }
	}

	@Override
	public void ask(Context parentContext, CommandTask command, Object[] args) throws ExtensionException, LogoException {
		for (Agent agent : this) { agent.ask(parentContext, command, args); }
	}

	// AH: I am not sure this should be a LogoList, but if we just make it a LogoList by
	// default, it's easy to work with in NetLogo.
	// The alternative is to evaluate what the first model returns, and then
	// cast everything from then on as that. I *think* i prefer the former,
	// although it does mean that people might end up with a LogoList of a bunch of differnet
	// kinds of objects. Which might be good, and might be bad.
	//
	// BCH 8/5/2014: I am sure that it should. No reason not to have it a LogoList. Everything
	// in the LogoList will be wrapped (see ModelAgent's of)

	@Override
	public Object of(Context parentContext, String reporter, Object[] args) throws ExtensionException, LogoException {
		LogoListBuilder llb = new LogoListBuilder();
		for (Agent agent : this){
			llb.add(agent.of(parentContext, reporter, args));
		}
		return llb.toLogoList();
	}

	@Override
	public Object of(Context parentContext, ReporterTask reporter, Object[] args) throws ExtensionException, LogoException {
		LogoListBuilder llb = new LogoListBuilder();
		for (Agent agent : this){
			llb.add(agent.of(parentContext, reporter, args));
		}
		return llb.toLogoList();
	}

	@Override
	public Iterator<Agent> iterator() {
		final Iterator<Agent> superIter = super.iterator();
		return new Iterator<Agent>() {
			private Agent next;
			@Override
			public boolean hasNext() {
				while (superIter.hasNext() && next == null) {
					next = superIter.next();
					if (next instanceof TPLAgent && ((TPLAgent) next).getBaseAgent().id() < 0) {
						superIter.remove();
						next = null;
					}
				}
				return next != null;
			}

			@Override
			public Agent next() {
				hasNext();
				Agent result = next;
				next = null;
				if (result != null) {
					return result;
				} else {
					throw new NoSuchElementException();
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int size() {
		int size = 0;
		for (Agent elem : this) { size++; }
		return size;
	}
}
