import java.util.*;
import java.io.*;

/** Internal errors that should not occur */
class InternalException extends RuntimeException {
    static final long serialVersionUID = 42L;
    InternalException(String mess) { super(mess); }
}

/** Typing analysis errors */
class AnalyseException extends RuntimeException {
    static final long serialVersionUID = 42L;
    AnalyseException(String mess) { super(mess); }
}


/** Wrapper of lexical tokens, for signs and operators */
enum OperatorKind { PLUS, MULT, MOINS, DIV, AND, OR, NOT,
                    SUP, INF, EGAL, TRUE, FALSE };

class Operator {
    OperatorKind theOp;
    private String image;
    Operator(int theO) {
        image = LgConstants.tokenImage[theO];
        switch(theO) {
        case LgConstants.PLUS: theOp=OperatorKind.PLUS; break;
        case LgConstants.MOINS: theOp=OperatorKind.MOINS; break;
        case LgConstants.MULT: theOp=OperatorKind.MULT; break;
        case LgConstants.DIV: theOp=OperatorKind.DIV; break;
        case LgConstants.AND: theOp=OperatorKind.AND; break;
        case LgConstants.OR: theOp=OperatorKind.OR; break;
        case LgConstants.NOT: theOp=OperatorKind.NOT; break;
        case LgConstants.SUP: theOp=OperatorKind.SUP; break;
        case LgConstants.INF: theOp=OperatorKind.INF; break;
        case LgConstants.EGAL: theOp=OperatorKind.EGAL; break;
        case LgConstants.TRUE: theOp=OperatorKind.TRUE; break;
        case LgConstants.FALSE: theOp=OperatorKind.FALSE; break;
        default: throw new InternalException("Unknown operator kind:" +
                                             theO + " " + image);
        }
    }
    public String toString() {
        // the image coming from javacc has quotes. remove them.
        return image.replaceAll("\"", "");
    }
}


/**
 * The mother class, for all the types of nodes in the abstract tree
 */
class MiniLgNode {
    /** A token denoting the node */
    private Token root;

    MiniLgNode(Token t) { root = t; }

    public String toString() { return root.toString(); }
}

/**
 * A complete program
 */
class ProgNode extends MiniLgNode {

    /** The main block of the program */
    private BlocNode b;

    /** The name of the program */
    private String name ;

    /** Constructor
     * @param t the lexical token of the word "program"
     * @param n the lexical token of the program name
     * @param bb the block
     */
    ProgNode(Token t, Token n, BlocNode bb) {
        super(t);
        b = bb;
        name = n.image;
    }

    /**
     * Get the static environment.
     * @return the static environment of declarations for programs
     *   with a single block
     */
    public EnvStatique getDecls() {
        return b.getDecls();
    }

    /**
     * Builds the static environment of the program;
     * @throws AnalyseException is something goes wrong during the analysis
     */
    public void analyse() {
        b.analyse();
        System.out.println("Name and Type analysis ok");
    }

    /**
     * External printable form (here: "program the_name ...")
     */
    public String toString() { return super.toString() + " " + name + "\n" +
                               b.toString(); }

    /**
     * builds the control graph of the program
     * @return the control graph
     */
    public ControlGraph buildControlGraph() {
        ControlGraph g = b.buildControlGraph();
        g.setIdf2Index(b.getDecls());
        return g;
    }

}


/**
 * The block of the program (decls + insts)
 */
class BlocNode extends MiniLgNode {

    /** The declarations of the block */
    private DeclsNode decls;

    /** The sequence of instructions of the block */
    private InstsNode insts;

    /** The static environment(types) of the block */
    private EnvStatique E;

    /** A marker, true if the static analysis has been done */
    private boolean analysisDone = false;

    /** Constructor
     * @param t the lexical token of the word "program"
     * @param d the declarations
     * @param i the instructions
     */
    BlocNode(Token t, DeclsNode d, InstsNode i) {
        super(t);
        decls = d;
        insts = i;
    }

    /**
     * Extern printable form
     */
    public String toString() {
        // ICI IL FAUDRAIT AJOUTER DES INDENTATIONS...
        return
            decls.toString() + "\n" +
            "begin\n" +
            insts.toString() + "\nend\n";
    }

    /**
     * Get the static environment
     * @return a static environment
     */
    public EnvStatique getDecls() {
        if (!analysisDone)
            throw new AnalyseException("Perform type analysis first");
        return E;
    }

    /**
     * Perform the static analysis(types)
     */
    public void analyse() {
        E = decls.theDecls();
        System.out.println("Static environment: " + E + "\n\n\n");
        analysisDone = true;
        insts.analyse(E);
    }

    /**
     * Build the control graph of the block
     */
    public ControlGraph buildControlGraph() {
        return insts.buildControlGraph();
    }


}


// --------------------- Les instructions ------------------------


/**
 * Sequences(lists) of instructions
 */
class InstsNode extends MiniLgNode {

    /** The list of instructions */
    private List<InstNode> insts;

    /** Constructor for empty sequences */
    InstsNode() {
        super(new Token());
        insts = new LinkedList<InstNode>();
    }

    /** Constructor from a list of instructions
     * @param t a lexical token, the last ";" in the sequence
     * @param li the list of instructions
     */
    InstsNode(Token t, List<InstNode> li) { super(t);   insts = li;}

    /**
     * External printable form
     */
    public String toString() {
        String result = "";
        Iterator<InstNode> i = insts.iterator();
        while (i.hasNext()) {
            result = result + i.next() + ";\n";
        }
        result = result + "\n";
        return result;
    }

    /**
     * Perform the static analysis(types)
     */
    public void analyse(EnvStatique E) {
        Iterator<InstNode> i = insts.iterator();
        while (i.hasNext()) {
            InstNode in = i.next();
            in.analyse(E);
        }
    }

    /**
     * Build the control graph of the sequence of instructions
     */
    public ControlGraph buildControlGraph() {

        ControlGraph current = new ControlGraph();
        Iterator<InstNode> i = insts.iterator();
        while (i.hasNext()) {
            InstNode in = i.next();
            ControlGraph g = in.buildControlGraph() ;
            current.addLast(g);
        }
        return current;
    }



}


/** One instruction */
abstract class InstNode  extends MiniLgNode {

    InstNode(Token t) { super(t);}

    /**
     * Perform the static analysis(types)
     * @param E a static environment from the upper level
     */
    public abstract void analyse(EnvStatique E);

    /** Build the control graph of the sequence of instructions */
    public abstract ControlGraph buildControlGraph();


}


/** The assignment instruction */
class AffNode extends  InstNode {

    /** The assigned identifier */
    Idf affecte;

    /** The expression assigned to the identifier */
    private ExprNode expr;

    /** A lexical token, that of ":=" */
    private Token assign;

    /** Constructor */
    AffNode(Token t, Token idf, ExprNode e) {
        super(t);  assign = t;
        affecte = new Idf(idf);
        expr = e;
    }

    /** Get the identifier */
    public Idf getIdf() { return affecte;}

    /** Get the expression */
    public ExprNode getExpr() { return expr; }

    public String toString() { return affecte + ":=" + expr; }

    /** Static analysis(types)
     * @param E static environment
     * @throws AnalyseException when there is a typing error
     */
    public  void analyse(EnvStatique E) {
        if (! E.exists(affecte))
            throw new AnalyseException
                ("Undeclared identifier " + affecte + " line " +
                 affecte.beginLine());
        if (expr.theType(E)!= E.getType(affecte))
            throw new AnalyseException
                ("Incompatible types in assignment" + " line " +
                 assign.beginLine + " column " + assign.beginColumn);
    }

    /** Build the control graph of the sequence of the assignment */
    public  ControlGraph buildControlGraph() {
        return new ControlGraph(this);
    }


}


/** The conditional instruction */
class CondNode extends  InstNode {

    /** A lexical token, that of the word "if" */
    private Token theif;

    /** The expression that serves as condition */
    ExprNode thecond;

    /** The sequences of instructions for the then and the else parts */
    InstsNode thenpart, elsepart;

    /** Constructor */
    CondNode(Token t, ExprNode c, InstsNode th, InstsNode el) {
        super(t); theif = t;
        thenpart = th;
        elsepart = el;
        thecond = c;
    }

    public String toString() {
        return "if (" + thecond + ") then\n" +
            thenpart + "\nelse\n " + elsepart + "\nendif";
    }

    /** Static analysis(types)
     * @param E static environment
     * @throws AnalyseException when there is a typing error
     */
    public  void analyse(EnvStatique E) {
        if (thecond.theType(E)!=Type.BOOL)
            throw new AnalyseException
                ("Condition should be boolean in IF " + " line " +
                 theif.beginLine + " column " + theif.beginColumn);

        thenpart.analyse(E);
        if (elsepart != null) elsepart.analyse(E);
    }

    /** Build the control graph of the conditional instruction
     */
    public  ControlGraph buildControlGraph() {
        ControlGraph gtrue = thenpart.buildControlGraph();
        if (elsepart != null) {
            ControlGraph gfalse = elsepart.buildControlGraph();
            return new ControlGraph(gtrue, gfalse, thecond);
        }
        else {
            return new ControlGraph(gtrue, thecond, false);
        }
    }


}


/** The loop instruction */
class WhileNode extends  InstNode {

    /** A lexical token, that of the word "while" */
    private Token thewhile;

    /** The expression that serves as the condition */
    ExprNode thecond;

    /** The list of instructions which is the body of the loop */
    InstsNode body;

    /** Constructor */
    WhileNode(Token t, ExprNode c, InstsNode b) {
        super(t); thewhile = t;
        body = b;
        thecond = c;
    }

    public String toString() {
        return "while (" + thecond + ") loop \n" + body + "\nendloop";
    }

    /** Static analysis(types)
     * @param E static environment
     * @throws AnalyseException when there is a typing error
     */
    public  void analyse(EnvStatique E) {
        if (thecond.theType(E)!=Type.BOOL)
            throw new AnalyseException
                ("Condition should be boolean in WHILE " + " line " +
                 thewhile.beginLine + " column " + thewhile.beginColumn);

        body.analyse(E);
    }

    /** Build the control graph of the loop instruction */
    public  ControlGraph buildControlGraph() {
        ControlGraph cb = body.buildControlGraph();
        return new ControlGraph(cb, thecond, true);
    }


}

/** The READ instruction */
class ReadNode extends  InstNode {

    /** The identifier left-hand-side of the assignment */
    private Idf read;

    /** The type of the variable read, computed by the static analysis */
    private Type t;

    /** A lexical token, that of the word "read" */
    private Token tt;

    /** A lexical token, that of the identifier */
    private Token tr;

    /** Constructor */
    ReadNode(Token t, Token r) {
        super(t);
        tt = t;
        tr = r;
        read = new Idf(r);
    }

    public String toString() {
        return "read(" + read + ")";
    }

    /** Static analysis(types)
     * @param E static environment
     * @throws AnalyseException when the identifier is not declared
     */
    public  void analyse(EnvStatique E) {
        if (!E.exists(read))
            throw new AnalyseException
                ("Undeclared identifier " + read + " line " +
                 read.beginLine());
        t = E.getType(read);
    }

    /** Build the control graph of the sequence of the read instruction */
    public  ControlGraph buildControlGraph() {
        // A READ instruction is similar to an assigment, where the
        // lhs is the identifier, and the rhs is a null expression.
        // We use the special constructor for READ
        return new ControlGraph(new AffNode(tt, tr, new ExprNode(tt)));
    }

}


/** The write instruction */
class WriteNode extends  InstNode {

    /** The expression to be written */
    private ExprNode written;

    /** A lexical token, that of the word "write" */
    private Token tt;

    /** Constructor */
    WriteNode(Token t, ExprNode w) {
        super(t);
        tt = t;
        written = w;
    }

    public String toString() {
        return "write(" + written + ")";
    }

    /** Static analysis(types)
     * @param E static environment
     */
    public  void analyse(EnvStatique E) {
        written.theType(E);
    }

    /** Build the control graph of the sequence of the write instruction */
    public  ControlGraph buildControlGraph() {
        // A WRITE instruction is similar to an assigment, where the
        // lhs is a dummy variable, and the rhs is the expression to be written
        // but this is useless for the analysis, and creating a dummy variable is a bit
        // complex (with its type, ...). So we replace a write by a "true" transition
        // Use the constructor with a dummy bool param for that.
        return new ControlGraph(true);
    }


}

/** The assert instruction */
class AssertNode extends  InstNode {

    /** The expression under assertion */
    private ExprNode asserted;

    /** A lexical token, that of the word "assert" */
    private Token tt;

   /** Get the expressions */
    public ExprNode getExpr() { return asserted; }

    /** A lexical token, that of the word "assert" */
    Token theAssert;

    /** Constructor */
    AssertNode(Token t, ExprNode a) {
        super(t);
        tt = t;
        asserted = a;
    }

    public String toString() {
        return "assert(" + asserted + ")";
    }

    /** Static analysis (types)
     * @param E static environment
     * @throws AnalyseException when there is a typing error
     */
    public  void analyse(EnvStatique E) {
        if (asserted.theType(E) != Type.BOOL)
            throw new AnalyseException
                ("Condition should be boolean in ASSERT " + " line " +
                 theAssert.beginLine + " column " + theAssert.beginColumn);
    }

    /** Build the control graph of the assert instruction */
    public  ControlGraph buildControlGraph() {
        return new ControlGraph(this);
    }


}


//////////////////////////////////////////////////////////////////////////


/** Expressions */
class ExprNode extends MiniLgNode {

    enum Kind { BINARY, UNARY, BOOLCONST, INTCONST, IDF, READ };

    /** The kind of the expression */
    Kind theKind;

    /** The lexical token of the operator or all constants*/
    private Token operatorToken ;

    /** The operator built from the lexical token (including bool constants)*/
    Operator operator;

    /** The integer value built from the lexical token, when it'a an int */
    int value ;

    /** An identifier, may be null */
    Idf idf = null;

    /** Two sub-expressions, may be null */
    ExprNode fg, fd = null;

    /** Constructor for leaves (int constants)
     * @param t the token of the constant
     * @param the corresponding integer
     */
    ExprNode(Token t, int v) {
        super(t); operatorToken = t;
        value = v;        theKind=Kind.INTCONST;
    }

    /** Constructor for leaves (bool constants only)
     * @param t the token of the operator or constant
     * @param b the value (redundant with t, but needed for the choice
     *          of the constructor)
     */
    ExprNode(Token t, boolean b) {
        super(t); operatorToken = t;
        operator = new Operator(t.kind);         theKind=Kind.BOOLCONST;
    }

    /** Constructor for the rhs of new expressions made from "read(x)",
     *  transformed into x:=? */
    ExprNode (Token t) {
        super(t); operatorToken = t;
        theKind=Kind.READ;
    }


    /** Constructor for identifiers
     * @param t the token of the identifier
     * @param i the idenfifier
     */
    ExprNode(Token t, Idf i) {
        super(t); operatorToken=t;        theKind=Kind.IDF;
        idf = i;
        fg = null;
        fd = null;
    }

    /** Constructor for unary expressions
     * @param t the token of the operator
     * @param g the sub-expressions
     */
    ExprNode(Token t, ExprNode g) {
        super(t); operatorToken=t;        theKind=Kind.UNARY;
        operator = new Operator(t.kind);
        fg = g;
        fd = null;
    }

    /** Constructor for binary expressions
     * @param t the token of the operator
     * @param g d the sub-expressions
     */
    ExprNode(Token t, ExprNode g, ExprNode d) {
        super(t); operatorToken=t;        theKind=Kind.BINARY;
        operator = new Operator(t.kind);
        fg = g; fd = d;
    }

    public String toString() {
        switch(theKind){
        case BOOLCONST:  return operator.toString();
        case INTCONST:   return value+"";
        case IDF: return idf.toString();
        case UNARY:
            // le seul operator unaire est NOT, pour l'instant
            // et operator.image produit des "" autour.
            // return operator.image + " " + fg;
            return "! " + fg;
        case BINARY:
            return "(" + fg + operator + fd + ")";
        case READ:
            return "read";
        default: throw new InternalException("Should not get there");
        }
    }

    /**
     * result for getSimpleExpr when the expression has the expected form
     *(null otherwise).
     */
    class SimpleExpr extends ExprNode {

        /** the two identifiers */
        Idf lhs, rhs;

        /** the sign */
        Operator sign;

        /** a boolean, true if the expression is simply x#y, false if
         * it is !(x#y) */
        boolean neg;

        SimpleExpr(Idf l, Idf r, Token s, boolean n) {
            super(s);
            sign = new Operator(s.kind);
            lhs = l; rhs = r; neg = n;
        }

        void invertNeg() { neg = !neg; }
    }

    /**
     * Determines whether an expression is "simple", and return its elements
     * <p>
     * An expression is "simple" iff it has one of the following forms:
     * x#y , where # is either < or >
     * !(x#y), where # is either < or >
     * !(!(! ...(x#y)))
     * @return null if it is not a simple expression, the simple
     * expression otherwise
     */
    public SimpleExpr getSimpleExpr() {

        // trace ------------------------------------------
        //System.out.println("getSimpleExpr " + operator + " " + idf + " "
        //                   + fg + " " + fd);
        // end trace --------------------------------------

        // Assume : appelee pour une expression de comparaison < ou > sur
        // des expressions reduites a des idfs.
        // Sinon : on renvoie null(pour etre testable ailleurs)


        switch(theKind){
        case READ:
        case BOOLCONST:
        case INTCONST:
        case IDF:
             return null;
        case UNARY:
            if (operator.theOp != OperatorKind.NOT) {
                //System.out.println("     returns null (unaire)");
                return null;
            }
            else {
                SimpleExpr se = fg.getSimpleExpr();
                if (se != null) se.invertNeg();
                //System.out.println("     returns NOT NULL");
                return se;
            }
        case BINARY:
            // expressions binaires
            // les deux fils doivent etre des idfs, et l'operateur
            // doit etre LgConstants.INF ou LgConstants.SUP

            if (((operator.theOp != OperatorKind.INF) &&
                 (operator.theOp != OperatorKind.SUP)) ||
                (fg.fg != null) || (fg.fd != null) || (fg.idf == null) ||
                (fd.fg != null) || (fd.fd != null) || (fd.idf == null) ) {
                //System.out.println("     returns null (binaire)");
                return null;
            }
            else {
                //System.out.println("     returns NOT null");
                return new SimpleExpr(fg.idf, fd.idf, operatorToken, true);
            }
         default: throw new InternalException("Should not get there");
        }
    }




    /** Compute the type of the expression
     * @param E a static environment(types)
     * @return the type of the expression, according to the types of
     * the variables that appear in it
     * @throws AnalyseException if there is a typing error
     */
    public Type theType(EnvStatique E) {
        //System.err.println("Expression typing " + this);

        switch(theKind){
        case READ:
            // the type analysis is made on the AST, before read nodes
            // are translated into special assignments.
            throw new InternalException("Should not get there");
        case BOOLCONST: return Type.BOOL ;
        case INTCONST:  return Type.INT ;
        case IDF:
            if (! E.exists(idf))
                throw new AnalyseException
                    ("Undeclared identifier " + idf + " line " +
                     idf.beginLine());
            else
                return E.getType(idf);
        case UNARY:
            if (operator.theOp== OperatorKind.NOT)
                if (fg.theType(E)!=Type.BOOL)
                    throw new AnalyseException
                        ("Incompatible type" + operator + " line " +
                         operatorToken.beginLine);
                else return Type.BOOL;
            else
                return Type.INT; // should not occur
        case BINARY:
            Type t1 = fg.theType(E);
            Type t2 = fd.theType(E);
            switch(operator.theOp) {
            case EGAL:
                if (t1!=t2)
                    throw new AnalyseException
                        ("incompatible types for" + operator + " line "
                         + operatorToken.beginLine + " column "
                         + operatorToken.beginColumn);
                return Type.BOOL;

            case INF:
            case SUP:
                if (!(t1==Type.INT && t2==Type.INT))
                    throw new AnalyseException
                        ("incompatible types for" + operator + " line "
                         + operatorToken.beginLine + " column "
                         + operatorToken.beginColumn);
                return Type.BOOL;

            case OR:
            case AND:
                if (!(t1==Type.BOOL && t2==Type.BOOL))
                    throw new AnalyseException
                        ("incompatible types for" + operator + " line "
                         + operatorToken.beginLine + " column "
                         + operatorToken.beginColumn);
                return t1;

            case DIV:
            case MULT:
            case PLUS:
            case MOINS:
                if (!(t1==Type.INT && t2==Type.INT))
                    throw new AnalyseException
                        ("incompatible types for" + operator + " line "
                         + operatorToken.beginLine + " column "
                         + operatorToken.beginColumn);
                return t1;
            default:
                System.err.println("Argh!");
                return Type.INT;
            }
        default: throw new InternalException("Should not get there");
        }
    }

    public static ExprNode buildNotNode(ExprNode c) {
        return  new ExprNode(Token.newToken(LgConstants.NOT,
                                            LgConstants.tokenImage
                                                [LgConstants.NOT]),
                             c);
    }

    public static ExprNode buildTrueNode() {
        return new ExprNode(Token.newToken(LgConstants.TRUE,
                                           LgConstants.tokenImage
                                               [LgConstants.TRUE]),
                            true);
    }
}



/** Declarations */
class DeclsNode extends MiniLgNode {

    private List<DeclNode> decls;

    DeclsNode(Token t, List<DeclNode> ld) {
        super(t);
        decls = ld;
    }

    public String toString() {
        String result="";
        for (DeclNode dc: decls) {
            result+=dc.toString() ;
        }
        return result;
    }

    /**
     * Builds the symbol table
     * @throws AnalyseException if there is a double declaration of a variable
     * @return a static environment(types)
     */
    public EnvStatique theDecls() {
        EnvStatique ts = new EnvStatique();
        Iterator<DeclNode> i = decls.iterator();
        while (i.hasNext()) {
            DeclNode d = i.next();
            TypeNode ty = d.type;
            List<Idf> li = d.lidfs;
            Iterator<Idf> j = li.iterator();
            while (j.hasNext()) {
                Idf idf = j.next();
                if (ts.exists(idf))
                    throw new AnalyseException
                        (idf + " is multiply defined, line "
                         + idf.beginLine());
                ts.set(idf, ty.getType());
            }
        }
        return ts;
    }
}


/** One declaration */
class DeclNode  extends MiniLgNode {

    /** The type of the declaratio n */
    TypeNode type;

    /** the lists of identifiers declared with this type */
    List<Idf> lidfs;

    /** Constructor */
    DeclNode(Token t, List<Idf> li, TypeNode ty) {
        super(t);
        lidfs = li;
        type = ty;
    }

    public String toString() {
        String result = "";
        boolean first = true;
        for (Idf i: lidfs) {
            result+= (!first?",":"") + i.toString() ;
            first = false ;
        }
        return result + ":" + type + ";\n";
    }
}


/** One type */
class TypeNode  extends MiniLgNode {

    /** the type */
    private Type type;

    /** Constructor */
    TypeNode(Token t) {
        super(t);
        if (t.image.equals("integer"))
            type = Type.INT;
        else if (t.image.equals("boolean"))
            type = Type.BOOL;
    }

    public String toString() { return type.toString(); }

    /** Get the type */
    public Type getType() { return type; }
}
