import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.Workspace;

import javax.swing.*;


public abstract class LevelsModelAbstract {

	abstract public void command(String command) throws CompilerException, LogoException, ExtensionException;
	abstract public Object report(String reporter) throws ExtensionException, LogoException, CompilerException;
	abstract public void kill() throws HaltException;
	abstract public String getPath();
	abstract public String getName();
	abstract public void breathe();
	abstract public void setSpeed(double d);
	abstract public void halt();
	abstract public Workspace workspace();

	abstract JFrame frame();

	public boolean usesLevelsSpace() {
		for (Object cm : this.workspace().getExtensionManager().loadedExtensions()) {
			if ("class LevelsSpace".equals(cm.getClass().toString())) {
				return true;
			}
		}
		return false;
	}

	public void show() {
		frame().setVisible(true);
	}

	public void hide() {
		frame().setVisible(false);
	}
}
