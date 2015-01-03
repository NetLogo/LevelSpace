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
        setLayout(new BorderLayout());
        cc = new CommandCenter(ws.workspace(), new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {

            }
        });
        JScrollPane scrollPane = new JScrollPane(
                ws,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                true,
                scrollPane, cc);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(1);
        this.add(splitPane);
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
