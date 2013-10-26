import org.nlogo.api.CompilerException;
import org.nlogo.lite.InterfaceComponent;


public class LevelsModelComponent extends LevelsModelAbstract {
	
	final javax.swing.JFrame frame = new javax.swing.JFrame();
    final InterfaceComponent myWS = new InterfaceComponent(frame);	
	String name;
	String path;
	int levelsSpaceNumber;
	
	public LevelsModelComponent(final String url, int levelsSpaceNumber)
	{
		new Runnable() {
			  public void run() {
			    frame.setSize(1000, 700);
			    frame.add(myWS);
			    frame.setVisible(true);
			    try {
			      myWS.open(url);
			    }
			    catch(Exception ex) {
			      ex.printStackTrace();
			    }}};
			    
		path = url;
		
		this.levelsSpaceNumber = levelsSpaceNumber;
		
        // find the name of the model - it is the bit past the last 
        // dash
        int lastDashPosition = url.lastIndexOf("/") + 1;
        name = url.substring(lastDashPosition);
		
	}

	public void command (String command)
	{
		try {
			myWS.command(command);
		} catch (CompilerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	public void kill()
	{
//		myWS.workspace().dispose();
		frame.dispose();
	}
	
	public Object report (String varName)
	{
		Object reportedValue = null;
		try {
			reportedValue = myWS.report(varName);
		} catch (CompilerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return reportedValue;
	}

	public String getName()
	{
		return name;
	}
	
	public String getPath(){
		return path;
	}

	@Override
	void breathe() {
		myWS.workspace().breathe();
		// TODO Auto-generated method stub
		
	}
	

	
	
}
