package cfh.turing;

import java.util.Arrays;

public enum Command {

    LEFT('L'),
    RIGHT('R'),
    HALT('H');
    
    private final char code;
    
    private Command(char code) {
        this.code = code;
    }
    
    @Override
    public String toString() {
        return Character.toString(code);
    }
    
    public static Command of(char code) {
        return Arrays.stream(values()).filter(c -> c.code == code).findFirst().get();
    }
}
