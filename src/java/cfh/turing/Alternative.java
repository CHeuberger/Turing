package cfh.turing;

import static java.util.Objects.*;

import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.HashMap;
import java.util.NoSuchElementException;

public class Alternative implements Positionable {
    
    static Alternative parse(CharBuffer text) throws ParseException {
        int start = text.position();
        if (!text.hasRemaining() || text.get() != '(') 
            throw new ParseException("reading alternative, missing '('", start);

        char expected = 0;
        char replace = 0;
        Command command = null;
        StringBuilder jumpText = null;
        while (text.hasRemaining()) {
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
                case ')':
                    if (expected == 0)
                        throw new ParseException("missing expected symbol", text.position()-1);
                    if (replace == 0)
                        throw new ParseException("missing replace symbol", text.position()-1);
                    if (command == null)
                        throw new ParseException("missing command", text.position()-1);
                    if (jumpText == null)
                        throw new ParseException("missing jump distance", text.position()-1);
                    var position = new Position(start);
                    position.end(text.position());
                    if (jumpText.charAt(0) == '"') 
                        return new Alternative(position, expected, replace, command, jumpText.toString().substring(1));
                    int jump;
                    try {
                        jump = Integer.parseInt(jumpText.toString());
                    } catch (NumberFormatException ex) {
                        throw (ParseException) new ParseException("invalid jump " + jumpText, text.position()-1).initCause(ex);
                    }
                    return new Alternative(position, expected, replace, command, jump);
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
                                throw new ParseException("invalid replace '" + replace + "'", text.position()-1);
                            break;
                        }
                    } else if (command == null) {
                        try {
                            command = Command.of(ch);
                        } catch (NoSuchElementException ex) {
                            throw (ParseException) new ParseException("invalid command '" + ch + "'", text.position()-1).initCause(ex);
                        }
                        break;
                    } else if (jumpText == null) {
                        jumpText = new StringBuilder();
                        if (ch == '"') {
                            var startPosition = text.position();
                            jumpText.append(ch);
                            while (text.hasRemaining()) {
                                var c = text.get();
                                if (c == '"' )
                                    break;
                                if (c == '\n' || c == '\r')
                                    throw new ParseException("label not terminated at end of line", startPosition);
                                jumpText.append(c);
                            }
                            break;
                        } else if (('0' <= ch && ch <= '9') || ch == '+' || ch == '-') {
                            jumpText.append(ch);
                            break;
                        }
                    } else {
                        if ('0' <= ch && ch <= '9') {
                            jumpText.append(ch);
                            break;
                        }
                    }
                    throw new ParseException(String.format("reading alternative, unrecognized character '%s' (0x%2x)", ch, (int)ch), text.position()-1);
            }
        }
        throw new ParseException("unexpected end of text reading alternative", text.position()-1);
    }

    final Position position;
    final char expected;
    final char replace;
    final Command command;
    private int jump;
    private String label;
    
    private Alternative(Position position, char expected, char replace, Command command, int jump) {
        this.position = requireNonNull(position);
        this.expected = expected;
        this.replace = replace;
        this.command = command;
        this.jump = jump;
        label = null;
    }
    
    private Alternative(Position position, char expected, char replace, Command command, String label) {
        this.position = requireNonNull(position);
        this.expected = expected;
        this.replace = replace;
        this.command = command;
        jump = Integer.MIN_VALUE;
        this.label = requireNonNull(label);
    }
    
    @Override
    public Position position() {
        return position;
    }
    
    public int jump() {
        if (jump == Integer.MIN_VALUE)
            throw new IllegalStateException("label not resolved: \"" + label + "\"");
        return jump;
    }
    
    private char symbol(char symbol) {
        return symbol == ' ' ? 'B' : symbol;
    }

    void resolve(int state, HashMap<String, Integer> labels) throws ParseException {
        if (label != null) {
            if (labels.containsKey(label)) {
                jump = labels.get(label) - state;
            } else {
                throw new ParseException("unknown label \"" + label + "\"", position.end());
            }
        }
    }
    
    @Override
    public String toString() {
        return label == null 
            ? String.format("(%s %s %s %d)", symbol(expected), symbol(replace), command, jump)
                : String.format("(%s %s %s \"%s\"(%d))", symbol(expected), symbol(replace), command, label, jump);
    }
}
