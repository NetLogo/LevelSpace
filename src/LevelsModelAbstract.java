import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.Workspace;


public abstract class LevelsModelAbstract {
	Workspace myWS;

	abstract void command(String command) throws CompilerException, LogoException, ExtensionException;
	abstract Object report(String reporter) throws ExtensionException, LogoException, CompilerException;
	abstract void kill() throws HaltException;
	abstract String getPath();
	abstract String getName();
	abstract void breathe();
	abstract void setSpeed(double d);
	abstract void halt();
}
