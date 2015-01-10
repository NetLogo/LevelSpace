import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.swing.JFrame;

import org.nlogo.api.*;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.workspace.AbstractWorkspace;

public class HeadlessChildModel extends ChildModel {

    HeadlessWorkspace myWS;
    ImageFrame frame;
    int levelsSpaceNumber;

    public HeadlessChildModel(World parentWorld, String path, final int levelsSpaceNumber) throws IOException, CompilerException, LogoException, ExtensionException {
        super(parentWorld);
        this.levelsSpaceNumber = levelsSpaceNumber;
        myWS = HeadlessWorkspace.newInstance();
        myWS.open(path);
        init();
    }

    private void ensureImageFrame() {
        if (frame == null) {
            try {
                frame = runUISafely(new Callable<ImageFrame>() {
                    @Override
                    public ImageFrame call() throws Exception {
                        final BufferedImage bi = myWS.exportView();
                        final String aTitle = getName().concat(" (LevelsSpace Model No. ").concat(Integer.toString(levelsSpaceNumber)).concat(")");
                        return new ImageFrame(bi, aTitle);
                    }
                });
            } catch (Exception e) {
                // Yes this is bad practice. I'm sorry. Deal with it.
                throw new RuntimeException(e);
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
    public AbstractWorkspace workspace() {
        return myWS;
    }

    @Override
    JFrame frame() {
        return frame;
    }

    @Override
    public void show() {
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
