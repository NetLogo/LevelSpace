import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ModelAgentSet extends AgentSetAgent<ModelAgent> {
	private Set<ModelAgent> models;

	public ModelAgentSet() {
		models = new HashSet<ModelAgent>();
	}

	public ModelAgentSet(Collection<ModelAgent> models) {
		models = new HashSet<ModelAgent>(models);
	}

	@Override
	public int size() {
		return models.size();
	}

	@Override
	public boolean isEmpty() {
		return models.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return models.contains(o);
	}

	@Override
	public Iterator<ModelAgent> iterator() {
		return models.iterator();
	}

	@Override
	public ModelAgent[] toArray() {
		return toArray(new ModelAgent[this.size()]);
	}

	@Override
	public <T> T[] toArray(T[] ts) {
		return models.toArray(ts);
	}

	@Override
	public boolean add(ModelAgent modelAgent) {
		return models.add(modelAgent);
	}

	@Override
	public boolean remove(Object o) {
		return models.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> objects) {
		return models.containsAll(objects);
	}

	@Override
	public boolean addAll(Collection<? extends ModelAgent> modelAgents) {
		return models.addAll(modelAgents);
	}

	@Override
	public boolean removeAll(Collection<?> objects) {
		return models.removeAll(objects);
	}

	@Override
	public boolean retainAll(Collection<?> objects) {
		return models.retainAll(objects);
	}

	@Override
	public void clear() {
		models.clear();
	}

	@Override
	public boolean equals(Object o) {
		return models.equals(o);
	}

	@Override
	public int hashCode() {
		return models.hashCode();
	}
}
