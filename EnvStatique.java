import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Identifiers */
class Idf {

    /** The lexical token corresponding to the identifier */
    private Token ti;

    /** Constructor from a token */
    Idf(Token t) {
        ti = t;
    }

    /** External printable form */
    public String toString() {
        return ti.toString();
    }

    /** the begin line associated with the token */
    public int beginLine() {
        return ti.beginLine;
    }

    /** Hash method, to guarantee that the hash function used if Idf
     *     is used in maps and Sets is consistent with the notion of
     *     equality for Identifiers
     */
    public int hashCode() {
        return ti.image.hashCode();
    }

    /** Equality test */
    public boolean equals(Object o) {
        // System.out.println("equals idf called");
        return (o instanceof Idf)
            &&(((Idf) o).ti.image.equals(ti.image));
    }
}

/**
 * Types (int or bool)
 */
enum Type { INT, BOOL };

class EnvStatique {
    private Map<Idf, Type> env;

    public EnvStatique() {
        env = new HashMap<Idf, Type>();
    }

    public void set(Idf i, Type t) {
        env.put(i, t);
    }

    public boolean exists(Idf i) {
        return env.containsKey(i);
    }

    public Type getType(Idf i) {
        return env.get(i);
    }

    public Set<Idf> getIdfSet() {
        return env.keySet();
    }

    public int size() {
        return env.keySet().size();
    }

    public String toString() {
        return env.toString();
    }
}
