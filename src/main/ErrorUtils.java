import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;

public class ErrorUtils {
    public static ExtensionException wrap(ChildModel model, Exception e) {
        String fmt = "Model %d (%s) encountered an error: %s";
        return new ExtensionException(String.format(fmt, model.getModelID(), model.getName(), e.getMessage()), e);
    }

    public static void checkForLogoException(ChildModel model) throws ExtensionException {
        LogoException ex = model.workspace().lastLogoException();
        if (ex != null) {
            model.workspace().clearLastLogoException();
            throw ErrorUtils.wrap(model, ex);
        }
    }

    public static ExtensionException bugDetected(Exception e) {
        return new ExtensionException("You've found a bug in LevelSpace! Please report this error!", e);
    }
}
