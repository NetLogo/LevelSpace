import org.nlogo.api.Argument;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;


public interface Agent {
	public void ask(Argument[] args, org.nlogo.api.Context context) throws ExtensionException, LogoException;
	public Object of(String s) throws ExtensionException;

}
