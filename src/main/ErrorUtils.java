import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.nvm.EngineException;

public class ErrorUtils {
    public static ExtensionException handle(ChildModel model, String code, EngineException e) {
        return new ExtensionException("Something went wrong when " + model.getName() + " ran '" + code + "': " + e.getMessage(), e);
    }
    public static ExtensionException handle(ChildModel model, String code, LogoException e) {
        return new ExtensionException("Something went wrong when " + model.getName() + " ran '" + code + "': " + e.getMessage(), e);
    }
    public static ExtensionException handle(ChildModel model, String code, CompilerException e) {
        return new ExtensionException("The model " + model.getName() +" couldn't understand '" + code + "': " + e.getMessage(), e);
    }

    public static ExtensionException bugDetected(Exception e) {
        return new ExtensionException("You've found a bug in LevelSpace! Please report this error!", e);
    }
}
