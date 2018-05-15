import java.util.*;


abstract class AbstractValue {
    abstract  public String toString();
    abstract  public void union(AbstractValue v);
    abstract  public AbstractValue copyChangeAssign(int i, AbstractValue a);
    abstract  public AbstractValue copyChangeCond(int i, int j,
                                                  Operator op, boolean neg);
    abstract public boolean isBottom();
}


/**
 * Information attached to the control points for the static analysis
 * The variables are identified by their names,
 * The class maintains the correspondence between names
 * and indices in the lattice
 */
class InfoAttached {

    /** the graph to which this info is attached */
    ControlGraph  theCG;
    AbstractValue theOld;
    AbstractValue theNew;

    public String toString() {
        return theOld.toString();
    }

    /**
     * @param cg the graph to which this info is attached
     * @param initOld initNew the initial value of the old (resp. new)
     *        abstract value (either TOP or BOT)
     */
    InfoAttached(ControlGraph cg,
                 AbstractValue initOld,
                 AbstractValue initNew) {
        theCG = cg;
        theOld = initOld;
        theNew = initNew;
    }

    public void setNew(AbstractValue newValue) {
        theNew = newValue;
    }

    /**
     * the old abstravt value
     */
    public AbstractValue getAbstractValue() {
        return theOld;
    }

    /**
     * @return true if a change exists between old and new,
     *  false otherwise.
     */
    public boolean commit(AbstractValue reinitNew) {
        boolean answer;
        if(theOld.equals(theNew)){
            answer = false ;
        }
        else {
            answer = true;
        }
        theOld = theNew;
        theNew = reinitNew;

        return answer;
    }

    /**
     * accumulate values by union
     * @param plus a new  abstract value to be cumulated by union with the
     * current New abstract value.
     */
    public void accumulateNew(AbstractValue plus) {
        theNew.union(plus);
    }

    /**
     * a special type of accumulateNew, when we copy
     * the info attached to a source state, without modification
     * into the info attached to the target state.
     */
    public void accumulateNew(InfoAttached ia) {
        theNew.union(ia.theOld);
    }

    /**
     * accumulate in new vector the information coming from
     * the knowledge of a new info  for the variable ii (assignment)
     */

    public void accumulateNew(InfoAttached source, Idf ii, AbstractValue av) {
        theNew.union(source.theOld.copyChangeAssign(theCG.indexOfIdf(ii), av));
    }



    /**
     * accumulate in new vector the information coming from
     * the knowledge of the condition  neg (lhs op rhs)
     */

    public void accumulateNew(InfoAttached source, Idf lhs, Idf rhs,
                              Operator op, boolean neg) {
        theNew.union
            (source.theOld.copyChangeCond(theCG.indexOfIdf(lhs),
                                          theCG.indexOfIdf(rhs), op, neg));
    }
}
