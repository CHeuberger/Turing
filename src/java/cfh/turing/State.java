package cfh.turing;

import static java.util.Objects.*;
import static java.util.stream.Collectors.*;

import java.nio.CharBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class State implements Positionable {
    
    static State parse(CharBuffer text) throws ParseException {
        int start = text.position();
        if (!text.hasRemaining() || text.get() != '(') 
            throw new ParseException("reading state, missing '('", start);
        
        var position = new Position(start);
        var state = new State(position);
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
                    var alternative = Alternative.parse(text);
                    state.add(alternative);
                    break;
                case ')':
                    position.end(text.position());
                    return state;
                default:
                    throw new ParseException(String.format("reading state, unrecognized character '%s' (0x%2x)", ch, (int)ch), text.position()-1);
            }
        }
        throw new ParseException("unexpected end of text reading state", text.position()-1);
    }

    final Position position;
    
    private final List<Alternative> alternatives = new ArrayList<>();
    
    private State(Position position) {
        this.position = requireNonNull(position);
    }
    
    private void add(Alternative alternative) {
        if (alternatives.stream().anyMatch(a -> a.expected == alternative.expected))
            throw new IllegalArgumentException("alternative duplicated for " + alternative.expected);
        alternatives.add(alternative);
    }
    
    public Alternative alternativeFor(char symbol) throws NoSuchElementException {
        return  alternatives
            .stream()
            .filter(a -> a.expected == symbol)
            .findFirst()
            .get();
    }
    
    public int alternativesCount() {
        return alternatives.size();
    }
    
    public Alternative alternative(int index) {
        if (index < 0 || index >= alternatives.size())
            throw new NoSuchElementException("invalid alternative index: " + index);
        return alternatives.get(index);
    }
    
    @Override
    public Position position() {
        return position;
    }
    
    @Override
    public String toString() {
        return  alternatives
                .stream()
                .map(Alternative::toString)
                .collect(joining("\n  ", "(\n  ", "\n )"));
    }
}
