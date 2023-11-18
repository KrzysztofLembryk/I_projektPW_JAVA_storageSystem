package cp2023.solution;

public class Pair <T1, T2>{
    T1 first;
    T2 second;
    public Pair(T1 first, T2 second)
    {
        this.first = first;
        this.second = second;
    }
    T1 getFirst() {return first;}
    T2 getSecond() {return second;}

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof Pair<?,?>)) {
            return false;
        }
        return this.first.equals(((Pair<?, ?>) obj).first) &&
                this.second.equals(((Pair<?, ?>) obj).second);
    }
}
