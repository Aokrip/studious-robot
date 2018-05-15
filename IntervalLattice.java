import java.util.*;


/**********************************************************/
class Bound {
    private static final Bound MINF = new Bound(0);
    private static final Bound PINF = new Bound(0);

    private int value;

    private Bound(){
        // On ne mets rien ici car il sera utilisé uniquement pour MINF et PINF
    }

    public Bound(int value){
        this.value = value;
    }

    public int getValue(){
        return value;
    }

    private void setValue(int value) {
        this.value = value;
    }

    @java.lang.Override
    public java.lang.String toString() {
        if(this.isPINF()){
            return "PINF";
        }
        else if(this.isMINF()){
            return "MINF";
        }
        else{
            return ""+ value;
        }
    }

    // Méthodes de comparaison
    public boolean isPINF() { return this.equals(Bound.PINF); }
    public boolean isMINF() { return this.equals(Bound.MINF); }

    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        Bound bound = (Bound) object;

        if(this == Bound.PINF && bound == Bound.PINF) return true;
        if(this == Bound.MINF && bound == Bound.MINF) return true;

        if (value != bound.value){
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(Bound b){
        if(this.isPINF() && !b.isPINF()) return 1;
        if(this.isMINF()) return -1;
        if(this.getValue() == b.getValue()) return 0;
        if(this.getValue() > b.getValue()) return 1;
        return -1;
    }

    // Tests
    public boolean equalsZero(){
        return (this.getValue() == 0 && !this.isPINF() && !this.isMINF());
    }

    public boolean positive(){
        return this.getValue() > 0;
    }

    public boolean negative(){
        return !(this.positive());
    }

    public boolean positiveOrNull(){
        return (this.equalsZero() || this.positive());
    }

    public boolean negativeOrNull(){
        return (this.equalsZero() || this.negative());
    }

    // Opérations binaires
    public Bound add(int value){
        if(!this.isMINF() && !this.isPINF()){
            this.setValue(this.getValue() + value);
        }
    }

    public Bound minus(int value){
        if(!this.isMINF() && !this.isPINF()){
            this.setValue(this.getValue() - value);
        }
    }

    // A IMPLEMENTER
}


/**********************************************************/
class Interval {
    // A MODIFIER :
    static final Interval TOP = new Interval(/* */);
    static final Interval BOT = new Interval(/* */);

    // A MODIFIER :
    public boolean equals(Object other) { return /* ... */ true; }

    public boolean isBOT() { return this.equals(Interval.BOT); }
    public boolean isTOP() { return this.equals(Interval.TOP); }
}


/*********************************************************************
 * A pair of intervals
 */
class IntervalPair {

    /** First element of the pair */
    private Interval o1;

    /** Second element of the pair */
    private Interval o2;

    /** constructor from two intervals */
    IntervalPair(Interval o1, Interval o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    /** Get first element */
    Interval getFirst()  { return o1; }

    /** Get second element */
    Interval getSecond() { return o2; }

    public String toString() {
        return "{" + o1 + ", " + o2 + "}";
    }
}


/*********************************************************************
 * A collection of functions for computing in the interval lattice for
 * one numerical variable
 */
class IntervalLattice {

    static Interval union(Interval i1, Interval i2) {
        return new Interval();
    }

    /** function + for intervals */
    static Interval plus(Interval i1, Interval i2) {
        return new Interval();
    }

    /** function - for intervals */
    static Interval minus(Interval i1, Interval i2) {
        return new Interval();
    }

    /** function * for intervals */
    static Interval mult(Interval i1, Interval i2) {
        return new Interval();
    }

    static IntervalPair gt(Interval i1, Interval i2) {
        return new IntervalPair(new Interval(), new Interval());
    }

    static IntervalPair lt(Interval i1, Interval i2) {
        return new IntervalPair(new Interval(), new Interval());
    }

    static IntervalPair geq(Interval i1, Interval i2) {
        return new IntervalPair(new Interval(), new Interval());
    }

    static IntervalPair leq(Interval i1, Interval i2) {
        return new IntervalPair(new Interval(), new Interval());
    }

    static Interval intervalOfConst(int k) {
        return new Interval();
    }

    static Interval P(Interval i) {
        return new Interval();
    }

    public static void main(String args[]) {
        // tests...
    }
}

/************************************************************
 * A vector of intervals, as a representation for a value in
 * the Cartesian product of n Interval lattices.
 * Invariant to be maintained:
 *     if one of the values in the vector is BOT, then all
 *     the values are BOT.
 */
class IntervalVector extends AbstractValue {

    /** The vector of intervals */
    private Interval[] vector;

    /** External printable form */
    public String toString() {
        //String s ="<";
   String s="";
        for (int i = 0; i < vector.length; i++)
            s = s + vector[i] + ' ';
        //s = s + ">";
        return s;
    }

    /** Constructor from an array
     * @param v a vector of intervals
     */
    public IntervalVector(Interval[] v) {
        vector = new Interval[v.length];
        for (int i = 0; i < v.length; i++)
            vector[i] = v[i];
    }

    /**
     * Union of this with another interval vector
     * @param a another interval vector, should be in canonical form
     * (BOT everywhere or nowhere)
     */
    public void union(AbstractValue a) {
        IntervalVector v = (IntervalVector) a;
        for (int i = 0; i < vector.length; i++)
            vector[i] = IntervalLattice.union(vector[i], v.vector[i]);
    }

    /**
     * Equality for interval vectors
     * @param other another interval vector to be compared to this
     * equals works on the canonical form:
     * BOT is assumed to be nowhere or everywhere
     */
    public boolean equals(Object other) {
        if (!(other instanceof  IntervalVector)) return false;
        IntervalVector othervector = (IntervalVector) other;
        for (int i = 0; i < vector.length; i++)
            if (!vector[i].equals(othervector.vector[i]))
                return false;
        return true;
    }

    /**
     * Constructor from an int that gives the size; vector is
     * initialized to Bottom
     * @param n the size
     */
    public IntervalVector(int n) {
        this(n, Interval.BOT);
    }

    /**
     * Constructor from an int that gives the size, and an interval to
     * be used as initial value
     * @param n the size
     * @param in the interval to be used as initial value
     */
    public IntervalVector(int n, Interval in) {
        vector = new Interval[n];
        // initial value is canonical
        for (int i = 0; i < vector.length; i++)
            vector[i] = in;
    }

    /**
     * get the interval at rank
     * @param i the rank in the vector
     * @return an interval
     */
    public Interval get(int i) {
         return vector[i] ;
    }

    /**
     * Build a new vector from this, by changing only one interval,
     * according to the effect of an assignment x:= expr(of interval in)
     * @param i the rank of the variable x whose interval is modified
     * @param a the new interval of this variable
     * @return the new interval vector, in canonical form
     */
    public IntervalVector copyChangeAssign(int i, AbstractValue a) {
        IntervalVector v = (IntervalVector) a;
        Interval in = v.vector[i];

        // trace -----------------------------------
        System.out.println("copyChangeAssign" + this.toString()
                           + " " + i + " " + in);
        // end trace

        Interval[] copy = new Interval[vector.length];

        if (in.isBOT())
            for (int j = 0; j < vector.length; j++)  copy[j] = Interval.BOT;
        else {
            // COPY FIRST
            for (int j = 0; j < vector.length; j++) {
                copy[j] = vector[j];
            }
            // THEN
            if (vector[0].isBOT()) {
                // NO MORE CHANGE, LEAVE bot EVERYWHERE
            }
            else {
                copy[i] = in;
            }
        }

        IntervalVector result = new IntervalVector(copy);
        System.out.println("returns " + result);
        return result;
    }

    /**
     * Build a new vector from this, by changing two intervals at a
     * time, according to the effect of a condition x # y
     * @param i j ranks of the two variables whose intervals are
     * changed
     * @param op the operator(either < or >)
     * @param neg true if simple expr x#y, false if expression !(x#y)
     * @return a new canonical interval vector in which the
     *  information build from 'vector' and 'i op j' and neg is
     *  written.
     */
    public IntervalVector copyChangeCond(int i, int j, Operator op, boolean neg) {

        // trace ------------------------------------------------
        System.out.println("copyChangeCond " + this.toString() + i + op + j
                           + neg);
        // end trace --------------------------------------------
        Interval[] copy = new Interval[vector.length];
        Interval lhinter = vector[i];
        Interval rhinter = vector[j];
        IntervalPair ip;
        if (op.theOp == OperatorKind.INF) {
            if (neg) {
                System.out.println("copyChangeCond LT");
                ip = IntervalLattice.lt(lhinter, rhinter);
                System.out.println(ip);
            }
            else {
                System.out.println("copyChangeCond GEQ");
                ip = IntervalLattice.geq(lhinter, rhinter);
                System.out.println(ip);
            }
        }
        else {
            // then, necessarily LgConstants.SUP
            if (neg) {
                ip = IntervalLattice.gt(lhinter, rhinter);
            }
            else {
                ip = IntervalLattice.leq(lhinter, rhinter);
            }
        }

        if (ip.getFirst().isBOT() || ip.getSecond().isBOT()) {
            for (int k = 0; k < vector.length; k++)  copy[k] = Interval.BOT;
        }
        else {
            for (int k = 0; k < vector.length; k++) {
                copy[k] = vector[k];
            }
            copy[i] = ip.getFirst();
            copy[j] = ip.getSecond();
        }
        return new IntervalVector(copy);
    }

    public boolean isBottom() {
        return vector[0].isBOT();
    }
}
