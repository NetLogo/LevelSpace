import org.nlogo.app.CommandCenter;
import org.nlogo.lite.InterfaceComponent;
import org.nlogo.window.Events;
import org.nlogo.workspace.AbstractWorkspace;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GUIPanel extends JPanel implements Events.OutputEvent.Handler {
    CommandCenter cc;

    public GUIPanel(InterfaceComponent ws){
        this.setLayout(new BorderLayout());
        cc = new CommandCenter(ws.workspace(), new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {

            }
        });
        this.add(BorderLayout.SOUTH, cc);
        this.add(BorderLayout.NORTH, ws);
    }

    @Override
    public void handle(Events.OutputEvent outputEvent) {
        if (outputEvent.clear){
            cc.output().clear();
        }else{
            cc.output().append(outputEvent.outputObject, outputEvent.wrapLines);
        }

    }
}
