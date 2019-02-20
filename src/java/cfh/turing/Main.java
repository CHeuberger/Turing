package cfh.turing;

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
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main {

    public static void main(String[] args) {
        new Main();
    }
    
    private static final Font FONT = new Font("monospaced", Font.PLAIN, 12);
    
    private static final String PREF_PROG_FILE = "program.file";
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
    
    private Program program = null;
    
    
    private Main() {
        SwingUtilities.invokeLater(this::initGUI);
    }
    
    private void initGUI() {
        programPane = newJTextPane();
        programPane.setPreferredSize(new Dimension(200, -1));
        var code = preferences.get(PREF_CODE, "");
        programPane.setText(code);
        
        var progrScroll = newJScrollPane("Program", programPane);
        
        tapePane = newJTextPane();
        var tape = preferences.get(PREF_TAPE, "");
        tapePane.setText(tape);
        
        var tapeScroll = newJScrollPane("Tape", tapePane);
        
        loadAction = newAction("Load", "load program from file (SHIFT to append)", this::doLoad);
        saveAction = newAction("Save", "save program to file", this::doSave);
        parseAction = newAction("Parse", "parse the program", this::doParse);
        startAction = newAction("Start", "start sprogram", this::doStart);
        
        var controlPane = new JPanel();
        controlPane.setLayout(new GridBagLayout());
        controlPane.add(newJButton(loadAction),  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, CENTER, BOTH, new Insets(2, 2, 2, 2), 0, 0));
        controlPane.add(newJButton(saveAction),  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, CENTER, BOTH, new Insets(2, 2, 2, 2), 0, 0));
        controlPane.add(newJButton(parseAction), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, CENTER, BOTH, new Insets(2, 2, 2, 2), 0, 0));
        controlPane.add(newJButton(startAction), new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, CENTER, BOTH, new Insets(2, 2, 2, 2), 0, 0));
        
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
        } catch (IOException ex) {
            ex.printStackTrace();
            showError(ex, "reading program");
        }
    }
    
    private void doSave(ActionEvent ev) {
    }
    
    private void doParse(ActionEvent ev) {
        var text = CharBuffer.wrap(programPane.getText());
        
        try {
            program = parse(text);
            text.rewind();
            preferences.put(PREF_CODE, text.toString());
            System.out.println(program);  // TODO
        } catch (ParseException ex) {
            System.err.printf("%s at position %d", ex.getClass().getSimpleName(), ex.getErrorOffset());
            showError(ex, "parsing program", "position: " + ex.getErrorOffset());
        }
    }
    
    private void doStart(ActionEvent ev) {
        if (program == null) {
            showError("no parsed program");
            return;
        }
        var worker = new SwingWorker<String, Change>() {
            private int position = 0;
            private int stateIndex = 0;
            @Override
            protected String doInBackground() throws Exception {
                var tape = new StringBuilder(tapePane.getText());
                if (tape.length() == 0 || tape.charAt(0) != '*')
                    throw new Exception("tape must start with '*'");
                preferences.put(PREF_TAPE, tape.toString());
                while (!Thread.interrupted()) {
                    var symbol = tape.charAt(position);
                    var state = program.state(stateIndex);
                    try {
                        var alternative = state.alternative(symbol);
                        tape.setCharAt(position, alternative.replace);
                        publish(new Change(position, alternative));
                        switch (alternative.command) {
                            case HALT: 
                                return tape.toString();
                            case LEFT: 
                                if (--position < 0) throw new Exception("moving left of start");
                                break;
                            case RIGHT:
                                if (++position >= tape.length()) tape.append(' ');
                                break;
                            default:
                                throw new IllegalArgumentException("unhandled command " + alternative.command);
                        }
                        stateIndex += alternative.jump;
                    } catch (NoSuchElementException ex) {
                        throw new Exception(
                                String.format("no alternatives for '%s', position %d, state %d", symbol, position, stateIndex),
                                ex);
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
                try {
                    tapePane.setText(get());
                } catch (InterruptedException ex) {
                    showError(ex, "executing program");
                } catch (ExecutionException ex) {
                    var cause = ex.getCause();
                    showError(cause==null ? ex : cause, "executing program");
                }
            }
        };
        worker.execute();
        // TODO
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
        var state = new State(new Position(text.position()));
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
                    // TODO position
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
                        // TODO position
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
}
