package cfh.turing;

import static java.util.Objects.*;
import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.List;

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

    public State state(int index) {
        return states.get(index);
    }
}
