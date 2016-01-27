import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.swing.JFrame;

import org.nlogo.api.*;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.HaltException;
import org.nlogo.workspace.AbstractWorkspace;

public class HeadlessChildModel extends ChildModel {

    HeadlessWorkspace myWS;
    ImageFrame frame;

    public HeadlessChildModel(World parentWorld, String path, final int levelsSpaceNumber) throws IOException, CompilerException, LogoException, ExtensionException {
        super(parentWorld, levelsSpaceNumber);
        myWS = HeadlessWorkspace.newInstance();
        myWS.open(path);
        init();
    }

    private void ensureImageFrame() throws ExtensionException {
        if (frame == null) {
            try {
                frame = runUISafely(new Callable<ImageFrame>() {
                    @Override
                    public ImageFrame call() throws Exception {
                        final BufferedImage bi = myWS.exportView();
                        return new ImageFrame(myWS.exportView(), getFrameTitle());
                    }
                });
            } catch (Exception e) {
                // Yes this is bad practice. I'm sorry. Deal with it.
                throw ErrorUtils.wrap(this, e);
            }
        }
    }

    public void updateView() {
        if (frame != null && frame.isVisible()){
            // get the image from the workspace
            BufferedImage bi = myWS.exportView();
            // update the frame

            frame.updateImage(bi);
        }
    }

    @Override
    public void ask(String command, Object[] actuals) throws ExtensionException, HaltException {
        super.ask(command, actuals);
        updateView();
    }

    @Override
    public Object of(String reporter, Object[] actuals) throws ExtensionException, HaltException {
        Object result = super.of(reporter, actuals);
        updateView();
        return result;
    }

    @Override
    public AbstractWorkspace workspace() {
        return myWS;
    }

    @Override
    JFrame frame() {
        return frame;
    }

    @Override
    public void show() throws ExtensionException {
        ensureImageFrame();
        super.show();
        updateView();
    }

    @Override
    public void hide() {
        if (frame != null) {
            super.hide();
        }
    }

    @Override
    public void setSpeed(double d){
    }
}
