import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.nlogo.api.*;
import org.nlogo.lite.InterfaceComponent;
import org.nlogo.nvm.CommandTask;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.HaltException;
import org.nlogo.nvm.ReporterTask;
import org.nlogo.nvm.Workspace;
import org.nlogo.window.SpeedSliderPanel;
import org.nlogo.workspace.AbstractWorkspace;


public class GUIChildModel extends ChildModel {

    final javax.swing.JFrame frame = new javax.swing.JFrame();
    final InterfaceComponent myWS = new InterfaceComponent(frame);
    final int levelsSpaceNumber;
    GUIPanel panel;


    public GUIChildModel(World parentWorld, final String path, final int levelsSpaceNumber)
            throws InterruptedException, InvocationTargetException, ExtensionException {
        super(parentWorld);
        this.levelsSpaceNumber = levelsSpaceNumber;
        // find the name of the model - it is the bit past the last dash

        final Exception[] ex = new Exception[] { null };

        SwingUtilities.invokeAndWait(
                new Runnable() {
                    public void run() {
                        // For some reason, the IC must be added to the frame before the model is opened.
                        panel = new GUIPanel(myWS);
                        frame.add(panel);
                        frame.setVisible(true);
                        try {
                            myWS.open(path);
                            init();
                        } catch (Exception e) {
                            ex[0] = e;
                        }
                        // get all components, find the speed slider, and hide it.
                        Component[] c = myWS.workspace().viewWidget.controlStrip.getComponents();
                        for (Component co : c){
                            if (co instanceof SpeedSliderPanel){
                                co.setVisible(false);
                                ((SpeedSliderPanel) co).setValue(0);
                            }
                        }
                        frame.setTitle(getName() + " (LevelsSpace model-id: " + String.valueOf(levelsSpaceNumber) + ")");
                        frame.pack();
                        // Make sure that the model doesn't close if people accidentally click the close button
                        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                        // Adding window listener so that the model calls the method that removes it from
                        // the extension if closed.
                        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                                Object[] options = {"Close Model", "Run in Background", "Cancel"};
                                int n = JOptionPane.showOptionDialog(frame,
                                        "Close the model, run it in the background, or do nothing?",
                                        null, JOptionPane.YES_NO_CANCEL_OPTION,
                                        JOptionPane.QUESTION_MESSAGE,
                                        null,
                                        options,
                                        options[2]);
                                switch (n) {
                                    case 0:
                                        try {
                                            LevelsSpace.closeModel(levelsSpaceNumber);
                                        } catch (ExtensionException e) {
                                            throw new RuntimeException(e);
                                        }
                                        break;
                                    case 1:
                                        hide();
                                }
                            }
                        });
                    }});
        if (ex[0] != null){
            frame.dispose();
            Exception e = ex[0];
            throw new ExtensionException(e.getMessage());
        }
    }

    public void setSpeed(double d){
        Component[] c = myWS.workspace().viewWidget.controlStrip.getComponents();
        for (Component co : c){
            if (co instanceof SpeedSliderPanel){
                ((SpeedSliderPanel) co).setValue((int)d);
            }
        }

    }

    @Override
    public AbstractWorkspace workspace() {
        return myWS.workspace();
    }

    @Override
    JFrame frame() {
        return frame;
    }
}
