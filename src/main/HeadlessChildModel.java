import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.nlogo.api.*;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.ReporterTask;
import org.nlogo.nvm.Workspace;
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
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        // get an image from the model so we know how big it is
                        final BufferedImage bi = myWS.exportView();
                        // create a new image frame to show what's going on in the model
                        // send it the image to set the size correctly
                        final String aTitle = getName().concat(" (LevelsSpace Model No. ").concat(Integer.toString(levelsSpaceNumber)).concat(")");
                        frame = new ImageFrame(bi, aTitle);
                    }
                });
            } catch (Exception e) {
                // Yes this is bad practice. I'm sorry. Deal with it.
                throw new RuntimeException(e);
            }
        }
    }


    public void updateView()
    {
        if (frame != null && frame.isVisible()){
            // get the image from the workspace
            BufferedImage bi = myWS.exportView();
            // update the frame

            frame.updateImage(bi);
        }

    }

    public void halt(){
        myWS.halt();
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

    public LogoList listBreeds(){
        LogoListBuilder llb = new LogoListBuilder();
        for (String entry : workspace().world().getBreeds().keySet())
        {
            llb.add(entry);
        }
        return llb.toLogoList();
    }

    @Override
    public LogoList listBreedsOwns() {
        LogoListBuilder llb = new LogoListBuilder();
        // TODO Auto-generated method stub
        for (Entry<String, List<String>> entry : workspace().world().program().breedsOwn().entrySet())
        {
            LogoListBuilder tuple  = new LogoListBuilder();
            LogoListBuilder vars = new LogoListBuilder();
            for (String s : entry.getValue()){
                vars.add(s);
            }
            // add turtles own to all of them too
            for (String s: workspace().world().program().turtlesOwn()){
                vars.add(s);
            }
            tuple.add(entry.getKey());
            tuple.add(vars.toLogoList());
            llb.add(tuple.toLogoList());
        }
        return llb.toLogoList();

    }

    @Override
    public LogoList listGlobals() {
        LogoListBuilder llb = new LogoListBuilder();
        for (int i = 0; i < workspace().world().observer().variables.length; i++){
            llb.add(workspace().world().observerOwnsNameAt(i));
        }
        return llb.toLogoList();
    }

}
