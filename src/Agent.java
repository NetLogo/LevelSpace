import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.ReporterTask;


public interface Agent {
	public void ask(Context parentContext, String command, Object[] args) throws ExtensionException, LogoException;
	public void ask(Context parentContext, CommandTask command, Object[] args) throws ExtensionException, LogoException;
	public Object of(Context parentContext, String reporter, Object[] args) throws ExtensionException, LogoException;
	public Object of(Context parentContext, ReporterTask reporter, Object[] args) throws ExtensionException, LogoException;
}
