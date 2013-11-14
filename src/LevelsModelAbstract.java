import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;


public abstract class LevelsModelAbstract {
	Object myWS;

	abstract void command(String command) throws CompilerException, LogoException, ExtensionException;
	abstract Object report(String reporter) throws ExtensionException, LogoException, CompilerException;
	abstract void kill();
	abstract String getPath();
	abstract String getName();
	abstract void breathe();

}
