package cfh.turing;

import static java.util.Objects.*;
import static java.util.stream.Collectors.*;

import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;


public class Program implements Positionable {
    
    static Program parse(CharBuffer text) throws ParseException {
        int start = text.position();
        if (!text.hasRemaining() || text.get() != '(') 
            throw new ParseException("reading program, missing '('", start);
        
        var position = new Position(start);
        var program = new Program(position);
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
                    text.reset();
                    var state = State.parse(text);
                    program.add(state);
                    break;
                case ')':
                    position.end(text.position());
                    return program;
                default:
                    throw new ParseException(String.format("reading program, unrecognized character '%s' (0x%2x)", ch, (int)ch), text.position());
            }
        }
        throw new ParseException("unexpected end of text reading program", text.position());
    }

    final Position position;
    
    private final List<State> states = new ArrayList<>();
    
    private Program(Position position) {
        this.position = requireNonNull(position);
    }
    
    private void add(State state) {
        states.add(requireNonNull(state));
    }
    
    @Override
    public Position position() {
        return position;
    }
    
    @Override
    public String toString() {
        return  states
                .stream()
                .map(State::toString)
                .collect(joining("\n ", "(\n ", "\n)"));
    }

    public int stateCount() {
        return states.size();
    }
    
    public State state(int index) throws NoSuchElementException {
        if (index < 0 || index >= states.size())
            throw new NoSuchElementException("invalid state index: " + index);
        return states.get(index);
    }

    public boolean isEmpty() {
        return states.isEmpty();
    }
}
