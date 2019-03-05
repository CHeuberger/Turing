package cfh.turing;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum Command {

    LEFT('L'),
    RIGHT('R'),
    NOP('N'),
    HALT('H');
    
    private final char code;
    
    private Command(char code) {
        this.code = code;
    }
    
    @Override
    public String toString() {
        return Character.toString(code);
    }
    
    public static Command of(char code) throws NoSuchElementException {
        return Arrays.stream(values()).filter(c -> c.code == code).findFirst().orElseThrow(() -> new NoSuchElementException("command '" + code + "'"));
    }
}
