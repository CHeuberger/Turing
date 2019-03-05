package cfh.turing;

import static java.util.Objects.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

@SuppressWarnings("serial")
public class IdentDialog extends JDialog {

    private static final Font FONT = new Font("monospaced", Font.PLAIN, 12);

    private final Program program;
    private final JTextComponent textComponent;

    private int state;
    private int alternative;

    private JTextArea elementField;
    private TitledBorder elementBorder;

    IdentDialog(JFrame parent, Program program, JTextComponent textComponent) {
        super(parent);
        
        this.program = requireNonNull(program);
        this.textComponent = requireNonNull(textComponent);
        
        state = -1;
        alternative = -1;
        
        elementField = new JTextArea(6, 20);
        elementField.setEditable(false);
        elementField.setFont(FONT);
        
        elementBorder = new TitledBorder("Element");
        
        var scroll = new JScrollPane(elementField);
        scroll.setBorder(elementBorder);
        
        var prev = newJButton("Prev", "show previous element", this::doPrev);
        var next = newJButton("Mext", "show next element", this::doNext);
        
        var buttons = Box.createHorizontalBox();
        buttons.add(Box.createHorizontalGlue());
        buttons.add(prev);
        buttons.add(next);
        buttons.add(Box.createHorizontalGlue());
        
        var panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.PAGE_END);
        
        setAlwaysOnTop(true);
        setModalityType(ModalityType.MODELESS);
        add(panel);
        pack();
        setLocationRelativeTo(parent);
        update();
    }
    
    private void update() {
        Positionable element;
        if (state == -1) {
            element = program;
            elementBorder.setTitle("Program");
        } else if (alternative == -1) {
            element = program.state(state);
            elementBorder.setTitle("State " + state);
        } else {
            element = program.state(state).alternative(alternative);
            elementBorder.setTitle("State " + state + ", Alternative " + alternative);
        }
        elementField.setText(element.toString());
        repaint();
        var pos = element.position();
        textComponent.select(pos.start(), pos.end());
    }
    
    private void doNext(ActionEvent ev) {
        if (state > -1 && alternative < program.state(state).alternativesCount()-1) {
            alternative += 1;
        } else {
            if (state < program.stateCount()-1) {
                state += 1;
                alternative = -1;
            }
        }
        update();
    }
    
    private void doPrev(ActionEvent ev) {
        if (state == -1) {
        } else if (alternative > -1) {
            alternative -= 1;
        } else {
            state -= 1;
            alternative = state == -1 ? -1 : program.state(state).alternativesCount()-1;
        }
        update();
    }
    
    private JButton newJButton(String title, String tooltip, ActionListener listener) {
        var button = new JButton(title);
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
        return button;
    }
}
