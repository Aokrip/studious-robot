import java.util.*;
import java.io.*;




enum NatureTransition { COND, ASSIGN };


abstract class TransitionLabel {
    abstract NatureTransition getNature();

    abstract String toDotFile();


    abstract void computeAbstractIntervals(InfoAttached infoSource,
                                           InfoAttached infoTarget,
                                           EnvStatique ES);
}


class CondTransition extends TransitionLabel {
    static CondTransition condTRUE =
        new CondTransition(ExprNode.buildTrueNode(),true);


    ExprNode theCond;
    NatureTransition getNature() { return NatureTransition.COND; }

    CondTransition(ExprNode c, boolean normal_or_neg) {
        if (normal_or_neg)
            theCond = c;
        else
            theCond = ExprNode.buildNotNode (c);
    }



    public void computeAbstractIntervals(InfoAttached infoSource,
                                         InfoAttached infoTarget,
                                         EnvStatique ES){


        // works only for "simple" conditions
        // for others, just ignore the condition
        // (instead of computing source.info inter cond, keep source.info,
        // which is safe).
        ExprNode.SimpleExpr se = theCond.getSimpleExpr();

        if (se == null) {
            System.out.println("cond pas simple : " + theCond + " \n");
            infoTarget.accumulateNew(infoSource);
        }
        else {
            infoTarget.accumulateNew(infoSource, se.lhs, se.rhs,
                                     se.sign, se.neg);
        }
    }

    public String toString() {
        return "["+ theCond + "]";
    }

    public String toDotFile() {
        return theCond+"";
    }
}


class AssignTransition extends TransitionLabel {

    AffNode theAssign;

    NatureTransition getNature() { return NatureTransition.ASSIGN; }

    AssignTransition(AffNode a) { theAssign = a; }

    public String toString() {
        return "["+ theAssign + "]";
    }

    public String toDotFile() {
        return theAssign+"" ;
    }


    public void computeAbstractIntervals(InfoAttached infoSource,
                                         InfoAttached infoTarget,
                                         EnvStatique ES){

        // Ignore transitions that assign non integer variables.
        if (ES.getType(theAssign.getIdf()) != Type.INT) {
            infoTarget.accumulateNew(infoSource);
        }
        else {
            Interval es;
            // Compute the interval to be associated with
            // the variable assigned ii.
            es = intervalOfExpr(ES, infoSource, theAssign.getExpr());

            // Propagate the information change for ii, to target state
            Idf ii = theAssign.getIdf();
            infoTarget.accumulateNew
                (infoSource, ii,
                 new IntervalVector(infoSource.theCG.getNumberOfIntIdfs(),es));
        }
    }

    /** Compute the interval of an expression
     * @param expr the expression
     * @return the interval of the expression according to the intervals of
     *        the variables that occur in it (according to
     *        theOldIntervals)
     * @throws InternalException if it is not an arithmetic expression
     */
    public Interval intervalOfExpr(EnvStatique ES,
                               InfoAttached info,
                               ExprNode expr) {

        ExprNode fg = expr.fg;
        ExprNode fd = expr.fd;
        Idf idf = expr.idf;
        Operator operator = expr.operator;

        switch(expr.theKind){
        case READ:
            return Interval.TOP ;
        case BOOLCONST:
            throw new InternalException
                ("This should be an arithmetic expression");
        case INTCONST:
            return IntervalLattice.intervalOfConst(expr.value);
        case IDF:
            if (ES.getType(idf) != Type.INT)
                throw new InternalException
                    ("This should be an arithmetic expression");
            else
                return ((IntervalVector)info.getAbstractValue()).get
                    (info.theCG.indexOfIdf(idf));
        case UNARY:
        // les operateurs unaires, forcement bool (pas de - unaire dans Lg.jj)
            throw new InternalException
                ("This should be an arithmetic expression");

        case BINARY:
            Interval s1 = intervalOfExpr(ES, info, fg);
            Interval s2 = intervalOfExpr(ES, info, fd);

            switch(operator.theOp) {
            case EGAL:
            case INF:
            case SUP:
            case OR:
            case AND:
                throw new InternalException
                    ("This should be an arithmetic expression");
            case DIV:
                throw new InternalException
                    ("This should be a SIMPLE arithmetic expression");
            case MULT:
                return IntervalLattice.mult(s1, s2);
            case MOINS:
                return IntervalLattice.minus(s1, s2);
            case PLUS:
                return IntervalLattice.plus(s1, s2);
            default:
                throw new InternalException("Should not get there!");
            }
            default:
                throw new InternalException("Should not get there!");
        }
    }

}


class Transition {
    TransitionLabel theLabel;
    ControlPoint    theTarget;

    Transition(TransitionLabel l, ControlPoint c) {
        theLabel = l;
        theTarget = c;
    }

    public String toString() {
        return "---" + theLabel + "---->" + theTarget;
    }
    public String toDotFile() {
        return theTarget + "[label=\"" + theLabel.toDotFile() + "\"]";
    }


    public void computeAbstractIntervals(InfoAttached info, EnvStatique ES) {
        // info is the info attached to the source state
        theLabel.computeAbstractIntervals(info, theTarget.getInfo(), ES);
    }
}


/**
 * A control point in the control graph
 */
class ControlPoint {
    static int counter = 0;
    String name;
    int number;
    InfoAttached info;
    ArrayList<Transition>  theTransitions;

    ControlPoint(String subname) {
        theTransitions = new ArrayList<Transition>();
        name = subname+counter;
        number=counter;
        counter++;
    }

    public InfoAttached getInfo() { return info; }

    public int getNumber() { return number; }

    public String toString() {
        return
            "\"" + name +
            " " + info +
            "\"";
    }

    public String toDotFile() {
        String st = "";
        Iterator<Transition> i = theTransitions.iterator();
        while (i.hasNext()) {
            st = st + this + "->" + i.next().toDotFile();
            st += "\n";
        }
        return st;
    }

    public String printTransitions() {
        String st = "";
        Iterator<Transition> i = theTransitions.iterator();
        while (i.hasNext()) {
            st = st + i.next();
            st += "\n";
        }
        return st;
    }

    void attach(InfoAttached info) { this.info = info;}


    void addCondTransition(CondTransition ct, ControlPoint t) {
        theTransitions.add(new Transition(ct, t));
    }

    void addAssignTransition(AssignTransition at, ControlPoint t) {
        theTransitions.add(new Transition(at,t));
    }

    // Add to this the transitions sourced in cp.
    void mergeTransitions(ControlPoint cp) {
        for (Transition t: cp.theTransitions)
            theTransitions.add (t);
    }
}


/**
 * The control graph: one entry point, one exit point
 * among the set of control points
 */
class ControlGraph {
    List<ControlPoint> theControlPoints;
    ControlPoint entry, exit, error;

    boolean errorIsReachable(){
        return error != null && !error.info.getAbstractValue().isBottom();
    }

    /** The textual form of the result array */
    String resultText = "";

    /** Maps an identifier to an index in the vector of intervals */
    private Map<Idf,Integer> idf2index;

    public  int indexOfIdf(Idf i) {
        // System.out.println("-------------- idf = " + i);
        // System.out.println("-------------- idf2index = " + idf2index);
        return idf2index.get(i).intValue();
    }
    public  int getNumberOfIntIdfs() { return numberOfIntIdfs; }
    private int numberOfIntIdfs = 0;

    public void setIdf2Index(EnvStatique theES) {
        // count the variables of type int in the static envt theES
        // and build the correspondence between
        // identifiers and indices.
        numberOfIntIdfs = 0;
        idf2index = new HashMap<Idf, Integer>();
        for (Idf ii: theES.getIdfSet()){
            if (theES.getType(ii)==Type.INT) {
                idf2index.put(ii, new Integer(numberOfIntIdfs));
                numberOfIntIdfs ++;
            }
        }
    }

    ControlGraph() {
        entry = new ControlPoint("Entry");
        exit = entry;
        theControlPoints = new ArrayList<ControlPoint>();
        theControlPoints.add(entry);
    }



    private void buildOneResultLine(int pass, int header_footer_inside) {
        if (header_footer_inside == 0) {
            // header
            resultText+= "\\[\\begin{array}{|c|";
            for (ControlPoint cp: theControlPoints) resultText+= "c|";
            resultText+= "}\n \\hline\n";
            resultText+= " CP  " ;
            for (ControlPoint cp: theControlPoints)
                resultText+= "& " + cp.getNumber();
            resultText+= "\\\\\\hline \n";
        }
        else if (header_footer_inside == 2) {
            // footer
            resultText+= "\\hline\n\\end{array}\\]\n";
            }
        else {
            // inside
            resultText += pass ;
            for (ControlPoint cp: theControlPoints)
                resultText+= " & " + cp.getInfo();
            resultText += "\\\\\\hline\n";
        }
    }

    public void computeAbstractIntervals(EnvStatique ES) {
        // trace
        System.out.println(ES);
        // ----------------------------------------

        // init: attach BOT values for all integer variables,
        // except in the initial state where the old value is set to TOP
        // (As if the first step  of the fix-point computation was already done)
        for (ControlPoint cp: theControlPoints) {
            cp.attach(new InfoAttached(this,
                                       new IntervalVector(numberOfIntIdfs),
                                       new IntervalVector(numberOfIntIdfs)));
        }
        entry.attach
            (new InfoAttached(this,
                              new IntervalVector(numberOfIntIdfs, Interval.TOP),
                              new IntervalVector(numberOfIntIdfs)));

        // on travaille en lisant dans theIntervals et en
        // ecrivant dans theNewIntervals et a la fin on commit. theIntervals
        // est initialise a BOT, et l'autre a BOT, pour pouvoir faire
        // des unions au fur et a mesure dans le parcours de graphe en
        // avant : A -> B on fait l'union avec la valeur deja la, qui
        // avait ete placee par une transition C -> B vue avant.


        System.out.println("============= INIT ===========================");
        System.out.println(this);
        int pass = 0;

        // print the head of the result tab
        buildOneResultLine(pass,0);
        // print one line of the results
        buildOneResultLine(pass,1);

        // Kleene iteration, with the test for stabilization
        while (true) {
            System.out.println("============= Pass nb " + ++pass +
                               " ============" );

            // Go through all transitions, and propagate
            // info from source to target point
            for (ControlPoint cp: theControlPoints) {
                for (Transition tr: cp.theTransitions) {
                    tr.computeAbstractIntervals(cp.info, ES);
                }
            }

            // The initial state has not been treated by the previous loop,
            // because it's the target of no transition.
            // set to TOP, for all passes in the loop
            entry.info.setNew(new IntervalVector(numberOfIntIdfs,
                                                 Interval.TOP));

            // all control points have a new value for this pass,
            // can commit for all points (testing whether something has changed):
            boolean change = false;
            for (ControlPoint cp: theControlPoints) {
                if(cp.info.commit(new IntervalVector(numberOfIntIdfs))) {
                    change = true;
                }
            }

            // print one line of the results
            buildOneResultLine(pass,1);

                  System.out.println(this);
            if (! change) break;
            // In case you need to stop an infinite loop, try something like:
            // if (pass > 32) break;
        }

        // print the tail of the result tab
        buildOneResultLine(pass,2);

    }

    public String toResultFile() {
        return resultText;
    }

    public String toDotFile() {
        Iterator<ControlPoint> i = theControlPoints.iterator();
        String st = "digraph g {\n";
        while(i.hasNext()) {
            ControlPoint cp = i.next();
            st = st + cp.toDotFile();
            st = st + "\n";
        }
        st += "\n}\n";
        return st;
   }

    public String toString() {
        Iterator<ControlPoint> i = theControlPoints.iterator();
        String st = "";
        while (i.hasNext()) {
            ControlPoint cp = i.next();
            //st = st + "Control point "  + cp + " has info attached  "
            //+ cp.info + "\n";
            st = st + "Control point " + cp + " has transitions:" + "\n";
            st = st + cp.printTransitions ();
            st = st + "\n\n\n";
        }
        return st;
    }



    // Append the graph g "at the end" of this
    // by merging the exit point of this with the entry point of g
    // to avoid the TRUE transitions that make the connection.
    void addLast(ControlGraph g) {
        theControlPoints.addAll(g.theControlPoints);

        // add the transitions sourced in g.entry to the transitions of exit
        exit.mergeTransitions (g.entry);
        // and then try to remove g.entry. If g.entry has entering transitions in g
        // they should be re-routed to exit.
        for(ControlPoint cp: g.theControlPoints)
            for(Transition t: cp.theTransitions)
                if (t.theTarget == g.entry)
                    t.theTarget = exit;

        theControlPoints.remove(g.entry);

        exit = g.exit;
        // ok, there's at most one ASSERT statement in the program,
        // hence there cannot be an error state in g, and also in this.
        if (g.error != null) error = g.error;
    }




    ControlGraph(AssertNode an) {
        entry = new ControlPoint("Assert_before");
        exit = new ControlPoint("Assert_after");
        error =  new ControlPoint("ERROR");
        theControlPoints = new ArrayList<ControlPoint>();
        theControlPoints.add(entry);
        theControlPoints.add(exit);
        theControlPoints.add(error);

        generateCondition(an.getExpr(), entry, exit, error, theControlPoints);
    }

    // contructor for single-transition graph, made of a TRUE expression.
    // used for the write expressions.
    ControlGraph(boolean dummy) {
        entry = new ControlPoint("Write_before");
        exit  = new ControlPoint("Write_after");
         theControlPoints = new ArrayList<ControlPoint>();
        theControlPoints.add(entry);
        theControlPoints.add(exit);
        entry.addCondTransition(CondTransition.condTRUE, exit);
   }

    ControlGraph(AffNode an) {
        entry = new ControlPoint("Assign_before");
        exit  = new ControlPoint("Assign_after");
        theControlPoints = new ArrayList<ControlPoint>();
        theControlPoints.add(entry);
        theControlPoints.add(exit);
        entry.addAssignTransition(new AssignTransition(an), exit);
    }

    ControlGraph(ControlGraph g, ExprNode ec, boolean while_or_if) {
        theControlPoints = new ArrayList<ControlPoint>();
        theControlPoints.addAll(g.theControlPoints);
        error = g.error;
        if (while_or_if) {
            // it's a While
            entry = new ControlPoint("While_entry");
            theControlPoints.add(entry);

            exit = new ControlPoint("While_exit");
            theControlPoints.add(exit);

            g.exit.addCondTransition(CondTransition.condTRUE, entry);
            generateCondition(ec, entry, g.entry,  exit, theControlPoints);
        }
        else {
            // it's an IF without Else
            entry = new ControlPoint("IF_entry");
            theControlPoints.add(entry);
            exit = new ControlPoint("IF_exit");
            theControlPoints.add(exit);

            g.exit.addCondTransition(CondTransition.condTRUE, exit);
            generateCondition(ec, entry, g.entry,  exit, theControlPoints);
        }
    }

    ControlGraph(ControlGraph gtrue, ControlGraph gfalse, ExprNode ec) {
        // it's a If-then-else
        theControlPoints = new ArrayList<ControlPoint>();
        entry = new ControlPoint("IF_entry");
        theControlPoints.add(entry);
        exit = new ControlPoint("IF_exit");
        theControlPoints.add(exit);

        theControlPoints.addAll(gtrue.theControlPoints);
        theControlPoints.addAll(gfalse.theControlPoints);

        if(gtrue.error != null) error = gtrue.error;
        else error = gfalse.error;

        // generate a part of the graph which tests the condition
        // ec. Whenever we are sure it is true (resp. false),
        // we branch to gtrue.entry (resp. gfalse.entry)
        // the graph is connected to the begin point.
        // we also pass the list of control points, for the auxiliary
        // points to  be added to it.
        generateCondition(ec, entry, gtrue.entry,  gfalse.entry,
                          theControlPoints);

        gtrue.exit.addCondTransition (CondTransition.condTRUE, exit);
        gfalse.exit.addCondTransition(CondTransition.condTRUE, exit);

    }

    /**
     * Generate a part of the graph that tests condition ec,
     * by enrolling the boolean combinations into branches.
     * <p>
     * Whenever the condition is guaranteed to be true (resp. false),
     * a connection to tt is made (resp. to ff).
     * All the auxiliary points are added to cp.
     * the while graph is connected to the "begin" entry point.
     * @param ec a Boolean expression
     * @param tt ff entry points of graphs for the code to reach when the
     *   condition is true, resp. false
     * @param cp a list of control points to which auxiliary
     *   points should be added
     */
    void generateCondition
        (ExprNode ec,
         ControlPoint begin,
         ControlPoint tt, ControlPoint ff,
         List<ControlPoint> cp) {

        if ((ec.theKind==ExprNode.Kind.BINARY) &&
            (ec.operator.theOp == OperatorKind.OR)) {
            // ec = ec1 or ec2
            ExprNode ec1 = ec.fg;
            ExprNode ec2 = ec.fd;
            ControlPoint aux = new ControlPoint("aux");
            cp.add(aux);
            generateCondition(ec1, begin, tt, aux, cp);
            generateCondition(ec2, aux, tt, ff, cp);
        }
        else if ((ec.theKind==ExprNode.Kind.BINARY) &&
                 (ec.operator.theOp == OperatorKind.AND)) {
            // ec = ec1 and ec2
            ControlPoint aux = new ControlPoint("aux");
            cp.add(aux);
            ExprNode ec1 = ec.fg;
            ExprNode ec2 = ec.fd;
            generateCondition(ec1, begin, aux, ff, cp);
            generateCondition(ec2, aux, tt, ff, cp);
        }
        else if ((ec.theKind==ExprNode.Kind.UNARY) &&
                 (ec.operator.theOp == OperatorKind.NOT)) {
            // ec = not ecn
            ExprNode ecn =  ec.fg;
            generateCondition(ecn, begin, ff, tt, cp);
        }
        else {
            // non-boolean-composed cond, as before
            begin.addCondTransition(new CondTransition(ec, true),  tt);
            begin.addCondTransition(new CondTransition(ec, false), ff);
        }
    }
}
