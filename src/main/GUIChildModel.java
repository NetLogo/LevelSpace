import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.nlogo.api.*;
import org.nlogo.lite.InterfaceComponent;
import org.nlogo.nvm.HaltException;
import org.nlogo.window.SpeedSliderPanel;
import org.nlogo.workspace.AbstractWorkspace;


public class GUIChildModel extends ChildModel {

    final JFrame frame = new JFrame();
    InterfaceComponent component;
    int levelsSpaceNumber;
    GUIPanel panel;


    public GUIChildModel(World parentWorld, final String path, final int levelsSpaceNumber)
            throws InterruptedException, ExtensionException, HaltException {
        super(parentWorld);
        this.levelsSpaceNumber = levelsSpaceNumber;

        component = runUISafely(new Callable<InterfaceComponent>() {
            public InterfaceComponent call() throws Exception {
                // For some reason, the IC must be added to the frame before the model is opened.
                new JFrame();
                InterfaceComponent component = new InterfaceComponent(frame);
                panel = new GUIPanel(component);
                frame.add(panel);
                frame.setVisible(true);
                component.open(path);
                // get all components, find the speed slider, and hide it.
                Component[] c = component.workspace().viewWidget.controlStrip.getComponents();
                for (Component co : c) {
                    if (co instanceof SpeedSliderPanel) {
                        co.setVisible(false);
                        ((SpeedSliderPanel) co).setValue(0);
                    }
                }
                frame.setTitle(component.workspace().getModelFileName() + " (LevelsSpace model-id: " + String.valueOf(levelsSpaceNumber) + ")");
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
                                } catch (HaltException e) {
                                    //ignore
                                }
                                break;
                            case 1:
                                hide();
                        }
                    }
                });
                return component;
            }
        });
        init();
    }

    public void setSpeed(double d){
        Component[] c = component.workspace().viewWidget.controlStrip.getComponents();
        for (Component co : c){
            if (co instanceof SpeedSliderPanel){
                ((SpeedSliderPanel) co).setValue((int)d);
            }
        }

    }

    @Override
    public AbstractWorkspace workspace() {
        return component.workspace();
    }

    @Override
    JFrame frame() {
        return frame;
    }
}
