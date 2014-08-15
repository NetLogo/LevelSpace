import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Can handle any kind of agents.
 */
public class GenericAgentSet<T extends Agent> extends AgentSetAgent<T> {
	private Set<T> agents;

	public GenericAgentSet() {
		agents = new HashSet<T>();
	}

	public GenericAgentSet(Collection<T> agents) {
		agents = new HashSet<T>(agents);
	}

	@Override
	public int size() {
		return agents.size();
	}

	@Override
	public boolean isEmpty() {
		return agents.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return agents.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		return agents.iterator();
	}

	@Override
	public T[] toArray() {
		return toArray((T[]) new Object[this.size()]);
	}

	@Override
	public <T> T[] toArray(T[] ts) {
		return agents.toArray(ts);
	}

	@Override
	public boolean add(T modelAgent) {
		return agents.add(modelAgent);
	}

	@Override
	public boolean remove(Object o) {
		return agents.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> objects) {
		return agents.containsAll(objects);
	}

	@Override
	public boolean addAll(Collection<? extends T> modelAgents) {
		return agents.addAll(modelAgents);
	}

	@Override
	public boolean removeAll(Collection<?> objects) {
		return agents.removeAll(objects);
	}

	@Override
	public boolean retainAll(Collection<?> objects) {
		return agents.retainAll(objects);
	}

	@Override
	public void clear() {
		agents.clear();
	}

	@Override
	public boolean equals(Object o) {
		return agents.equals(o);
	}

	@Override
	public int hashCode() {
		return agents.hashCode();
	}
}
