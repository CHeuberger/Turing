package cfh.turing;

public class Position {

    private int start = -1;
    private int end = -1;
    
    public Position(int start) {
        this.start = start;
    }

    void end(int end) {
        if (this.end != -1)
            throw new IllegalStateException("end already set to " + this.end + ", not changing to " + end);
        
        this.end = end;
    }
    
    @Override
    public String toString() {
        return start + "," + end;
    }
}
