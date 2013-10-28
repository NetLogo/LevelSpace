
public abstract class LevelsModelAbstract {
	Object myWS;

	abstract void command(String command);
	abstract Object report(String reporter);
	abstract void kill();
	abstract String getPath();
	abstract String getName();
	abstract void breathe();

}
