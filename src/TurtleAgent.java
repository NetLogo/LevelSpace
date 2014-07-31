import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;


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
		// TODO Auto-generated method stub
		

	}

	@Override
	public Object of(String s) throws ExtensionException { 
		// TODO Auto-generated method stub new
		return parentModel.of("[ " + s + " ] of turtle " + Long.toString(theAgent.id()));
	}

}
