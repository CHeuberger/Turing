package cfh.turing;

import static java.util.Objects.*;
import static java.nio.file.StandardOpenOption.*;
import static java.awt.GridBagConstraints.*;
import static javax.swing.JOptionPane.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Element;


public class Main {

    public static void main(String[] args) {
        new Main();
    }
    
    private static final Font FONT = new Font("monospaced", Font.PLAIN, 12);
    private static final Color NORMAL_SELECT = Color.GRAY.brighter();
    private static final Color ERROR_SELECT = Color.RED;
    
    private static final String PREF_PROG_FILE = "program.file";
    private static final String PREF_TAPE_FILE = "tape.file";
    private static final String PREF_CODE = "code";
    private static final String PREF_TAPE = "tape";
    private final Preferences preferences = Preferences.userNodeForPackage(getClass());
    
    
    private JFrame frame;
    private JTextPane programPane;
    private JTextPane tapePane;
    private JTextField output;
    
    private JTextField line;
    private JTextField column;
    private JTextField dot;
    
    private Action loadAction;
    private Action saveAction;
    private Action parseAction;
    private Action identAction;
    private Action startAction;
    private Action readAction;
    private Action writeAction;
    
    private Program program = null;
    
    
    private Main() {
        SwingUtilities.invokeLater(this::initGUI);
    }
    
    private void initGUI() {
        programPane = newJTextPane();
        var code = preferences.get(PREF_CODE, "");
        programPane.setText(code);
        programPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                resetProgram();
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                resetProgram();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                resetProgram();
            }
        });
        programPane.addCaretListener(e -> programPane.setSelectionColor(NORMAL_SELECT));
        programPane.setSelectionColor(NORMAL_SELECT);
        
        var noWrap = new JPanel(new BorderLayout());
        noWrap.add(programPane);
        var progScroll = newJScrollPane("Program", noWrap);
        progScroll.setPreferredSize(new Dimension(250, -1));
        
        tapePane = newJTextPane();
        var tape = preferences.get(PREF_TAPE, "");
        
        var tapeScroll = newJScrollPane("Tape", tapePane);
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setLeftComponent(progScroll);
        split.setRightComponent(tapeScroll);
        
        loadAction = newAction("Load", "load program from file (SHIFT to append)", this::doLoad);
        saveAction = newAction("Save", "save program to file (SHIFT to append)", this::doSave);
        parseAction = newAction("Parse", "parse the program", this::doParse);
        identAction = newAction("Ident", "identify the origram parts", this::doIdent);
        startAction = newAction("Start", "start sprogram", this::doStart);
        readAction = newAction("Read", "read file into tape (SHIFT to append)", this::doRead);
        writeAction = newAction("Write", "write tape to file (SHIFT append)", this::doWrite);
        
        var controlPane = new JPanel();
        controlPane.setLayout(new GridBagLayout());
        controlPane.add(newJButton(loadAction),  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, LINE_START, NONE, new Insets(2, 4, 2, 2), 0, 0));
        controlPane.add(newJButton(saveAction),  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, LINE_START, NONE, new Insets(2, 4, 2, 2), 0, 0));
        controlPane.add(newJButton(parseAction), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, CENTER,     NONE, new Insets(2, 2, 2, 4), 0, 0));
        controlPane.add(newJButton(identAction), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, CENTER,     NONE, new Insets(2, 2, 2, 4), 0, 0));
        controlPane.add(newJButton(startAction), new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, CENTER,     NONE, new Insets(2, 4, 2, 2), 0, 0));
        controlPane.add(newJButton(readAction),  new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, LINE_END,   NONE, new Insets(2, 4, 2, 4), 0, 0));
        controlPane.add(newJButton(writeAction), new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0, LINE_END,   NONE, new Insets(2, 2, 2, 4), 0, 0));
        
        output = newJTextField(0);
        output.setEditable(false);
        
        tapePane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
            private void update() {
                String text = tapePane.getText();
                StringBuilder builder = new StringBuilder();
                for (var i = 0; i < text.length(); i++) {
                    var ch = text.charAt(i);
                    switch (ch) {
                        case '*':
                            builder.append(ch);
                            break;
                        case ' ':
                            builder.append(ch);
                            while (i+1 < text.length()) {
                                if (text.charAt(i+1) != ' ')
                                    break;
                                i += 1;
                            }
                            break;
                        case '0':
                        case '1':
                            var j = i + 1;
                            for (; j < text.length(); j++) {
                                ch = text.charAt(j);
                                if (ch != '0' && ch != '1')
                                    break;
                            }
                            int val = Integer.parseInt(text.substring(i, j), 2);
                            builder.append(val);
                            i = j - 1;
                            break;
                        default:
                            break;    
                    }
                }
                output.setText(builder.toString());
            }
        });
        tapePane.setText(tape);
        
        line = newStatusField(6, "line");
        column = newStatusField(4, "column");
        dot = newStatusField(11, "caret position");
        
        programPane.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                Element root = programPane.getDocument().getDefaultRootElement();
                int index = root.getElementIndex(e.getDot());
                line.setText(Integer.toString(index+1));
                column.setText(Integer.toString(e.getDot()-root.getElement(index).getStartOffset()));
                if (e.getMark() < e.getDot()) {
                    dot.setText(e.getMark() + "-" + e.getDot());
                } else if (e.getMark() > e.getDot()) {
                        dot.setText(e.getDot() + "-" + e.getMark());
                } else {
                    dot.setText(Integer.toString(e.getDot()));
                }
            }
        });
        
        var status = Box.createHorizontalBox();
        status.add(line);
        status.add(column);
        status.add(dot);
        status.add(Box.createHorizontalGlue());
        
        var main = newJPanel();
        main.setLayout(new GridBagLayout());
        main.add(split,       new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, CENTER, BOTH, new Insets(4, 4, 2, 4), 0, 0));
        main.add(controlPane, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, CENTER, BOTH, new Insets(2, 4, 2, 4), 0, 0));
        main.add(newJLabel("Output:"), new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, CENTER, BOTH, new Insets(2, 4, 2, 2), 0, 0));
        main.add(output,               new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, CENTER, BOTH, new Insets(2, 2, 2, 4), 0, 0));
        main.add(status, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0, CENTER, BOTH, new Insets(2, 4, 4, 4), 0, 0));
        
        frame = new JFrame();
        frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(main, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.validate();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        updateActions(true);
    }
    
    private void updateActions(boolean enabled) {
        loadAction.setEnabled(enabled);
        saveAction.setEnabled(enabled);
        parseAction.setEnabled(enabled);
        identAction.setEnabled(enabled && program != null);
        startAction.setEnabled(enabled && program != null);
        readAction.setEnabled(enabled);
        writeAction.setEnabled(enabled);
    }
    
    private void resetProgram() {
        program = null;
        programPane.setSelectionColor(NORMAL_SELECT);
        updateActions(true);
    }
    
    private void doLoad(ActionEvent ev) {
        var path = preferences.get(PREF_PROG_FILE, "default.turing");
        var chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Turing Program", "turing"));
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setSelectedFile(new File(path));
        if (chooser.showOpenDialog(frame) != chooser.APPROVE_OPTION)
            return;
        
        var file = chooser.getSelectedFile();
        try {
            var code = new StringBuilder();
            Files.lines(file.toPath()).forEach(line -> code.append(line).append('\n'));
            preferences.put(PREF_PROG_FILE, file.getAbsolutePath());
            if ((ev.getModifiers() & ev.SHIFT_MASK) != 0) {
                code.insert(0, programPane.getText());
            }
            programPane.setText(code.toString());
            resetProgram();
            frame.setTitle(file.getName());
        } catch (IOException ex) {
            ex.printStackTrace();
            showError(ex, "loading program");
        }
    }
    
    private void doRead(ActionEvent ev) {
        var path = preferences.get(PREF_TAPE_FILE, "default.tape");
        var chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Turing Tape", "tape"));
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setSelectedFile(new File(path));
        if (chooser.showOpenDialog(frame) != chooser.APPROVE_OPTION)
            return;
        
        var file = chooser.getSelectedFile();
        try {
            var tape = Files.readString(file.toPath()).trim();
            preferences.put(PREF_TAPE_FILE, file.getAbsolutePath());
            if ((ev.getModifiers() & ev.SHIFT_MASK) != 0) {
                var start = tape.charAt(0)=='*' ? 1 : 0; 
                tape = tapePane.getText() + tape.substring(start);
            }
            tapePane.setText(tape);
        } catch (IOException ex) {
            ex.printStackTrace();
            showError(ex, "loading tape");
        }
    }
    
    private void doSave(ActionEvent ev) {
        var path = preferences.get(PREF_PROG_FILE, "default.turing");
        var chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Turing Program", "turing"));
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setSelectedFile(new File(path));
        if (chooser.showSaveDialog(frame) != chooser.APPROVE_OPTION)
            return;
        
        boolean append = (ev.getModifiers() & ev.SHIFT_MASK) != 0;
        var file = chooser.getSelectedFile();
        if (file.getName().indexOf('.') == -1) {
            file = new File(file.getParentFile(), file.getName() + ".turing");
        }
        if (append && !file.exists() && showConfirm(OK_CANCEL_OPTION, "Confirm", file,  "file does not exist, create new?") != OK_OPTION)
            return;
        if (!append && file.exists() && showConfirm(OK_CANCEL_OPTION, "Confirm", file,  "file already exists, overwrite?") != OK_OPTION)
            return;
        
        var code = programPane.getText();
        try {
            if (append) {
                Files.writeString(file.toPath(), code, WRITE, CREATE, APPEND);
            } else {
                Files.writeString(file.toPath(), code, WRITE, CREATE, TRUNCATE_EXISTING);
            }
            preferences.put(PREF_PROG_FILE, file.getAbsolutePath());
            frame.setTitle(file.getName());
        } catch (IOException ex) {
            ex.printStackTrace();
            showError(ex, "saving program");
        }
    }
    
    private void doWrite(ActionEvent ev) {
        var path = preferences.get(PREF_TAPE_FILE, "default.tape");
        var chooser = new JFileChooser();
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Turing Tape", "tape"));
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setFileSelectionMode(chooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setSelectedFile(new File(path));
        if (chooser.showSaveDialog(frame) != chooser.APPROVE_OPTION)
            return;
        
        boolean append = (ev.getModifiers() & ev.SHIFT_MASK) != 0;
        var file = chooser.getSelectedFile();
        if (file.getName().indexOf('.') == -1) {
            file = new File(file.getParentFile(), file.getName() + ".tape");
        }
        if (append && !file.exists() && showConfirm(OK_CANCEL_OPTION, "Confirm", file,  "file does not exist, create new?") != OK_OPTION)
            return;
        if (!append && file.exists() && showConfirm(OK_CANCEL_OPTION, "Confirm", file,  "file already exists, overwrite?") != OK_OPTION)
            return;
        
        var tape = tapePane.getText().trim();
        try {
            if (append) {
                Files.writeString(file.toPath(), tape, WRITE, CREATE, APPEND);
            } else {
                Files.writeString(file.toPath(), tape, WRITE, CREATE, TRUNCATE_EXISTING);
            }
            preferences.put(PREF_TAPE_FILE, file.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
            showError(ex, "saving program");
        }
    }
    
    private void doParse(ActionEvent ev) {
        var text = CharBuffer.wrap(programPane.getText());
        
        try {
            program = parse(text);
            text.rewind();
            preferences.put(PREF_CODE, text.toString());
            updateActions(true);
            System.out.println();
            System.out.println(program);
        } catch (ParseException ex) {
            int offset = ex.getErrorOffset();
            System.err.printf("%s at position %d", ex.getClass().getSimpleName(), offset);
            programPane.select(offset, offset+1);
            programPane.setSelectionColor(ERROR_SELECT);
            programPane.requestFocus();
            showError(ex, "parsing program", "position: " + offset);
        }
    }
    
    private void doIdent(ActionEvent ev) {
        if (program == null) {
            showError("Error", "program not parsed");
            return;
        }
        
        var dialog = new IdentDialog(frame, program, programPane);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                updateActions(true);
            }
        });
        updateActions(false);
        dialog.setVisible(true);
        programPane.requestFocus();
    }
    
    private void doStart(ActionEvent ev) {
        if (program == null) {
            showError("Error", "program not parsed");
            return;
        }
        if (program.isEmpty()) {
            showError("Error", "empty program");
            return;
        }
        var worker = new SwingWorker<String, Change>() {
            @Override
            protected String doInBackground() throws RunException {
                var tape = new StringBuilder(tapePane.getText());
                if (tape.length() == 0 || tape.charAt(0) != '*')
                    throw new RunException("tape must start with '*'");
                preferences.put(PREF_TAPE, tape.toString());
                
                var position = 0;
                var stateIndex = 0;
                var state = program.state(stateIndex);
                
                while (!Thread.interrupted()) {
                    var symbol = tape.charAt(position);
                    Alternative alternative;
                    try {
                        alternative = state.alternativeFor(symbol);
                    } catch (NoSuchElementException ex) {
                        throw new StateException(state, 
                                "no alternatives for '%s', position %d, state %d (%s)", symbol, position, stateIndex, ex);
                    }
                    tape.setCharAt(position, alternative.replace);
                    publish(new Change(position, alternative));
                    switch (alternative.command) {
                        case HALT: 
                            return tape.toString();
                        case NOP:
                            break;
                        case LEFT: 
                            if (--position < 0) throw new AlternativeException(alternative, "moving left of start");
                            break;
                        case RIGHT:
                            if (++position >= tape.length()) tape.append(' ');
                            break;
                        default:
                            throw new AlternativeException(alternative, "unhandled command \"%s\"", alternative.command);
                    }
                    int old = stateIndex;
                    stateIndex += alternative.jump();
                    try {
                        state = program.state(stateIndex);
                    } catch (NoSuchElementException ex) {
                        throw new AlternativeException(alternative, 
                            "no state at index '%d', position %d, from state %d (%S)", stateIndex, position, old, ex);
                    }
                }
                return tape.toString();
            }
            @Override
            protected void process(List<Change> changes) {
                for (var change : changes) {
                    System.out.printf("%5d: %s%n", change.position, change.alternative);
                }
            }
            @Override
            protected void done() {
                updateActions(true);
                try {
                    tapePane.setText(get());
                } catch (InterruptedException ex) {
                    showError(ex, "executing program");
                } catch (ExecutionException ex) {
                    var cause = ex.getCause();
                    if (cause instanceof RunException) {
                        var e = (RunException) cause;
                        var position = e.position();
                        if (position != null) {
                            programPane.select(position.start(), position.end());
                        }
                    }
                    showError(cause==null ? ex : cause, "executing program");
                }
            }
        };
        updateActions(false);
        worker.execute();
    }

    private Program parse(CharBuffer text) throws ParseException {
        Program prog = null;
        while (text.hasRemaining()) {
            text.mark();
            var ch = text.get();
            switch (ch) {
                case ' ':
                case '\r':
                case '\n':
                    break;
                case ';':
                    while (text.hasRemaining() && text.get() != '\n') {
                        text.mark();
                    }
                    text.reset();
                    break;
                case '(':
                    if (prog != null)
                        throw new ParseException("program already defined", text.position()-1);
                    text.reset();
                    prog = Program.parse(text);
                    break;
                default:
                    throw new ParseException(String.format("expecting program, unrecognized character '%s' (0x%2x)", ch, (int)ch), text.position()-1);
            }
        }
        if (prog == null)
            throw new ParseException("end of text expecting program", text.position()-1);
        return prog;
    }
    
    private JPanel newJPanel() {
        return new JPanel();
    }
    
    private JLabel newJLabel(String text) {
        var label = new JLabel(text);
        return label;
    }
    
    private JButton newJButton(Action action) {
        var button = new JButton(action);
        return button;
    }
    
    private JScrollPane newJScrollPane(String title, Component view) {
        var scroll = new JScrollPane(view);
        scroll.setBorder(new TitledBorder(title));
        return scroll;
    }

    private JTextField newJTextField(int columns) {
        var field = new JTextField(columns);
        field.setFont(FONT);
        return field;
    }
    
    private JTextField newStatusField(int columns, String tooltip) {
        var field = new JTextField(columns);
        field.setBorder(BorderFactory.createEtchedBorder());
        field.setEditable(false);
        field.setFont(FONT);
        field.setHorizontalAlignment(dot.TRAILING);
        field.setMaximumSize(field.getPreferredSize());
        field.setToolTipText(tooltip);
        return field;
    }
    
    private JTextPane newJTextPane() {
        var pane = new JTextPane();
        pane.setFont(FONT);
        return pane;
    }
    
    private Action newAction(String title, String tooltip, ActionListener listener) {
        var action = new AbstractAction(title) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                listener.actionPerformed(ev);
            }
        };
        if (tooltip != null) {
            action.putValue(action.SHORT_DESCRIPTION, tooltip);
        }
        return action;
    }
    
    private void showError(Throwable ex, Object... message) {
        ex.printStackTrace();
        var m = Arrays.copyOf(message, message.length+1);
        m[message.length] = ex.getMessage();
        showError(ex.getClass().getSimpleName(), m);
    }
    
    private void showError(String title, Object... message) {
        showMessageDialog(frame, message, title, ERROR_MESSAGE);
    }
    
    private int showConfirm(int optionType, String title, Object... message) {
        return showConfirmDialog(frame, message, title, optionType);
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    private static class Change {
        
        private final int position;
        private final Alternative alternative;
        
        Change(int position, Alternative alternative) {
            this.position = position;
            this.alternative = alternative;
        }
    }
    
    private static class RunException extends Exception {
        protected RunException(String format, Object... args) {
            super(String.format(format, args));
        }
        Position position() {
            return null;
        }
    }
    
    private static class StateException extends RunException {
        public final State state;
        StateException(State state, String format, Object... args) {
            super(format, args);
            this.state = requireNonNull(state); 
        }
        @Override
        Position position() {
            return state.position;
        }
    }
    
    private static class AlternativeException extends RunException {
        public final Alternative alternative;
        AlternativeException(Alternative alternative, String format, Object... args) {
            super(format, args);
            this.alternative = requireNonNull(alternative); 
        }
        @Override
        Position position() {
            return alternative.position;
        }
    }
}
