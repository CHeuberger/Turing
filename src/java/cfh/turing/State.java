package cfh.turing;

import static java.util.Objects.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class State {

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
    
    @Override
    public String toString() {
        return  alternatives
                .stream()
                .map(Alternative::toString)
                .collect(joining("\n  ", "(\n  ", "\n )"));
    }

    public Alternative alternative(char symbol) throws NoSuchElementException {
        return  alternatives
                .stream()
                .filter(a -> a.expected == symbol)
                .findFirst()
                .get();
    }
}
