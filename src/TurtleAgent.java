import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.app.App;
import org.nlogo.nvm.Workspace.OutputDestination;


public class TurtleAgent implements Agent {
	ModelAgent parentModel;
	org.nlogo.api.Agent theAgent;

	public TurtleAgent(ModelAgent parentModel, org.nlogo.api.Agent theAgent){
		this.parentModel = parentModel;
		this.theAgent = theAgent;
	}
	
	@Override
	public void ask(Argument[] args, Context context)
			throws ExtensionException, LogoException {
		String theCommand = "ask turtle " + Long.toString(theAgent.id()) + " [" + args[1].getString() + "]";
			parentModel.theModel.command(theCommand);
	}

	@Override
	public Object of(String s) throws ExtensionException { 
		// TODO Auto-generated method stub new
		return parentModel.of("[ " + s + " ] of turtle " + Long.toString(theAgent.id()));
	}

}
