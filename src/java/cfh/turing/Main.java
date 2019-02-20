package cfh.turing;

import static java.util.Objects.*;
import static java.nio.file.StandardOpenOption.*;
import static java.awt.GridBagConstraints.*;
import static javax.swing.JOptionPane.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main {

    public static void main(String[] args) {
        new Main();
    }
    
    private static final Font FONT = new Font("monospaced", Font.PLAIN, 12);
    
    private static final String PREF_PROG_FILE = "program.file";
    private static final String PREF_TAPE_FILE = "tape.file";
    private static final String PREF_CODE = "code";
    private static final String PREF_TAPE = "tape";
    private final Preferences preferences = Preferences.userNodeForPackage(getClass());
    
    
    private JFrame frame;
    private JTextPane programPane;
    private JTextPane tapePane;
    
    private Action loadAction;
    private Action saveAction;
    private Action parseAction;
    private Action startAction;
    private Action readAction;
    private Action writeAction;
    
    private Program program = null;
    
    
    private Main() {
        SwingUtilities.invokeLater(this::initGUI);
    }
    
    private void initGUI() {
        programPane = newJTextPane();
        programPane.setPreferredSize(new Dimension(200, -1));
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
        
        var progrScroll = newJScrollPane("Program", programPane);
        
        tapePane = newJTextPane();
        var tape = preferences.get(PREF_TAPE, "");
        tapePane.setText(tape);
        
        var tapeScroll = newJScrollPane("Tape", tapePane);
        
        loadAction = newAction("Load", "load program from file (SHIFT to append)", this::doLoad);
        saveAction = newAction("Save", "save program to file (SHIFT to append)", this::doSave);
        parseAction = newAction("Parse", "parse the program", this::doParse);
        startAction = newAction("Start", "start sprogram", this::doStart);
        readAction = newAction("Read", "read file into tape (SHIFT to append)", this::doRead);
        writeAction = newAction("Write", "write tape to file (SHIFT append)", this::doWrite);
        
        var controlPane = new JPanel();
        controlPane.setLayout(new GridBagLayout());
        controlPane.add(newJButton(loadAction),  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, LINE_START, NONE, new Insets(2, 4, 2, 2), 0, 0));
        controlPane.add(newJButton(saveAction),  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, LINE_START, NONE, new Insets(2, 4, 2, 2), 0, 0));
        controlPane.add(newJButton(parseAction), new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, CENTER,     NONE, new Insets(2, 2, 2, 2), 0, 0));
        controlPane.add(newJButton(startAction), new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, CENTER,     NONE, new Insets(2, 2, 2, 2), 0, 0));
        controlPane.add(newJButton(readAction),  new GridBagConstraints(2, 0, 1, 1, 1.0, 0.0, LINE_END,   NONE, new Insets(2, 2, 2, 2), 0, 0));
        controlPane.add(newJButton(writeAction), new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0, LINE_END,   NONE, new Insets(2, 2, 2, 2), 0, 0));
        
        var main = newJPanel();
        main.setLayout(new GridBagLayout());
        main.add(progrScroll, new GridBagConstraints(0, 0, 1, 1, 0.0, 1.0, CENTER, BOTH, new Insets(0, 0, 0, 0), 0, 0));
        main.add(tapeScroll,  new GridBagConstraints(1, 0, 1, 1, 1.0, 1.0, CENTER, BOTH, new Insets(0, 0, 0, 0), 0, 0));
        main.add(controlPane, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, CENTER, BOTH, new Insets(0, 0, 0, 0), 0, 0));
        
        frame = new JFrame();
        frame.setDefaultCloseOperation(frame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(main, BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.validate();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        updateStatus(true);
    }
    
    private void updateStatus(boolean enabled) {
        loadAction.setEnabled(enabled);
        saveAction.setEnabled(enabled);
        parseAction.setEnabled(enabled);
        startAction.setEnabled(enabled && program != null);
        readAction.setEnabled(enabled);
        writeAction.setEnabled(enabled);
    }
    
    private void resetProgram() {
        program = null;
        updateStatus(true);
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
            var code = Files.readString(file.toPath());
            preferences.put(PREF_PROG_FILE, file.getAbsolutePath());
            if ((ev.getModifiers() & ev.SHIFT_MASK) != 0) {
                code = programPane.getText() + code;
            }
            programPane.setText(code);
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
            var tape = Files.readString(file.toPath());
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
        if (append && !file.exists() && showConfirmDialog(frame, new Object[] {file,  "file does not exist, create new?"}, "Confirm", OK_CANCEL_OPTION) != OK_OPTION)
            return;
        if (!append && file.exists() && showConfirmDialog(frame, new Object[] {file,  "file already exists, overwrite?"}, "Confirm", OK_CANCEL_OPTION) != OK_OPTION)
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
        if (append && !file.exists() && showConfirmDialog(frame, new Object[] {file,  "file does not exist, create new?"}, "Confirm", OK_CANCEL_OPTION) != OK_OPTION)
            return;
        if (!append && file.exists() && showConfirmDialog(frame, new Object[] {file,  "file already exists, overwrite?"}, "Confirm", OK_CANCEL_OPTION) != OK_OPTION)
            return;
        
        var tape = tapePane.getText();
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
            updateStatus(true);
            System.out.println();
            System.out.println(program);
        } catch (ParseException ex) {
            System.err.printf("%s at position %d", ex.getClass().getSimpleName(), ex.getErrorOffset());
            showError(ex, "parsing program", "position: " + ex.getErrorOffset());
        }
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
                        alternative = state.alternative(symbol);
                    } catch (NoSuchElementException ex) {
                        throw new StateException(state, 
                                "no alternatives for '%s', position %d, state %d (%s)", symbol, position, stateIndex, ex);
                    }
                    tape.setCharAt(position, alternative.replace);
                    publish(new Change(position, alternative));
                    switch (alternative.command) {
                        case HALT: 
                            return tape.toString();
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
                    stateIndex += alternative.jump;
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
                updateStatus(true);
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
        updateStatus(false);
        worker.execute();
    }

    private Program parse(CharBuffer text) throws ParseException {
        Program program = null;
        while (text.hasRemaining()) {
            var ch = text.get();
            switch (ch) {
                case ' ':
                case '\n':
                case '\r':
                    break;
                case ';':
                    skipComment(text);
                    break;
                case '(':
                    if (program != null)
                        throw new ParseException("program already defined", text.position());
                    program = parseProgram(text);
                    break;
                default:
                    throw new ParseException("expecting program, unrecognized character '" + ch + "'/" + (int)ch, text.position());
            }
        }
        if (program == null)
            throw new ParseException("end of text expecting program", text.position());
        return program;
    }
    
    private Program parseProgram(CharBuffer text) throws ParseException {
        var program = new Program();
        while (text.hasRemaining()) {
            var ch = text.get();
            switch (ch) {
                case ' ':
                case '\n':
                case '\r':
                    break;
                case ';':
                    skipComment(text);
                    break;
                case ')':
                    return program;
                case '(':
                    var state = parseState(text);
                    program.add(state);
                    break;
                default:
                    throw new ParseException("reading program, unrecognized character '" + ch + "'/" + (int)ch, text.position());
            }
        }
        throw new ParseException("unexpected end of text reading program", text.position());
    }
    
    private State parseState(CharBuffer text) throws ParseException {
        Position position = new Position(text.position());
        var state = new State(position);
        while (text.hasRemaining()) {
            var ch = text.get();
            switch (ch) {
                case ' ':
                case '\n':
                case '\r':
                    break;
                case ';':
                    skipComment(text);
                    break;
                case ')':
                    position.end(text.position());
                    return state;
                case '(':
                    var alternative = parseAlternative(text);
                    state.add(alternative);
                    break;
                default:
                    throw new ParseException("reading state, unrecognized character '" + ch + "'/" + (int)ch, text.position());
            }
        }
        throw new ParseException("unexpected end of text reading state", text.position());
    }
    
    private Alternative parseAlternative(CharBuffer text) throws ParseException {
        var position = new Position(text.position());
        char expected = 0;
        char replace = 0;
        Command command = null;
        StringBuilder jumpText = null;
        while (text.hasRemaining()) {
            var ch = text.get();
            switch (ch) {
                case ' ':
                case '\n':
                case '\r':
                    break;
                case ';':
                    skipComment(text);
                    break;
                case ')':
                    if (expected == 0)
                        throw new ParseException("missing expected symbol", text.position());
                    if (replace == 0)
                        throw new ParseException("missing replace symbol", text.position());
                    if (command == null)
                        throw new ParseException("missing command", text.position());
                    if (jumpText == null)
                        throw new ParseException("missing jump distance", text.position());
                    try {
                        var jump = Integer.parseInt(jumpText.toString());
                        position.end(text.position());
                        return new Alternative(position, expected, replace, command, jump);
                    } catch (NumberFormatException ex) {
                        throw (ParseException) new ParseException("invalid jump " + jumpText, text.position()).initCause(ex);
                    }
                default:
                    if (expected == 0) {
                        if (ch == 'B') {
                            expected = ' ';
                            break;
                        }
                        if ("*01".indexOf(ch) != -1) {
                            expected = ch;
                            break;
                        }
                    } else if (replace == 0) {
                        if (ch == 'B') {
                            replace = ' ';
                            break;
                        }
                        if ("*01".indexOf(ch) != -1) {
                            replace = ch;
                            if ((expected == '*') != (replace == '*'))
                                throw new ParseException("invalid replace '" + replace + "'", text.position());
                            break;
                        }
                    } else if (command == null) {
                        try {
                            command = Command.of(ch);
                        } catch (NoSuchElementException ex) {
                            throw (ParseException) new ParseException("invalid command '" + ch + "'", text.position()).initCause(ex);
                        }
                        break;
                    } else if (jumpText == null) {
                        jumpText = new StringBuilder();
                        if (ch == '+')
                            break;
                        if (('0' <= ch && ch <= '9') || ch == '-') {
                            jumpText.append(ch);
                            break;
                        }
                    } else {
                        if ('0' <= ch && ch <= '9') {
                            jumpText.append(ch);
                            break;
                        }
                    }
                    throw new ParseException("reading alternative, unrecognized character '" + ch + "'/" + (int)ch, text.position());
            }
        }
        throw new ParseException("unexpected end of text reading state", text.position());
    }
    
    private void skipComment(CharBuffer text) {
        while (text.hasRemaining() && text.get() != '\n') {
            text.mark();
        }
        text.reset();
    }
    
    private JPanel newJPanel() {
        return new JPanel();
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

    private JTextPane newJTextPane() {
        var pane = new JTextPane();
        pane.setFont(FONT);
        return pane;
    }
    
    private Action newAction(String title, String tooltip, Consumer<ActionEvent> handle) {
        var action = new AbstractAction(title) {
            @Override
            public void actionPerformed(ActionEvent ev) {
                handle.accept(ev);
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
