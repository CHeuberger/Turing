package cfh.turing;

import static java.util.Objects.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class Program {

    private final List<State> states = new ArrayList<>();
    
    void add(State state) {
        states.add(requireNonNull(state));
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
