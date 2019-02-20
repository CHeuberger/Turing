package cfh.turing;

import static java.util.Objects.*;

public class Alternative {

    final Position position;
    final char expected;
    final char replace;
    final Command command;
    final int jump;
    
    Alternative(Position position, char expected, char replace, Command command, int jump) {
        this.position = requireNonNull(position);
        this.expected = expected;
        this.replace = replace;
        this.command = command;
        this.jump = jump;
    }
    
    @Override
    public String toString() {
        return String.format("(%s %s %s %d)", symbol(expected), symbol(replace), command, jump);
    }
    
    private char symbol(char symbol) {
        return symbol == ' ' ? 'B' : symbol;
    }
}
