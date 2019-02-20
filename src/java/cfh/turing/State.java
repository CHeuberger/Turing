package cfh.turing;

import static java.util.Objects.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class State implements Positionable {

    final Position position;
    
    private final List<Alternative> alternatives = new ArrayList<>();
    
    State(Position position) {
        this.position = requireNonNull(position);
    }
    
    void add(Alternative alternative) {
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
