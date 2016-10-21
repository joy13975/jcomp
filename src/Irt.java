// COMS22201: IR tree construction

import java.util.*;
import java.io.*;
import java.lang.reflect.Array;
import antlr.collections.AST;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

public class Irt {
    // The code below is generated automatically from the ".tokens" file of the
    // ANTLR syntax analysis, using the TokenConv program.
    //
    // CAMLE TOKENS BEGIN [SPECIAL COMMENT TOKEN]
  public static final String[] tokenNames = new String[] {
"NONE", "NONE", "NONE", "NONE", "AMPERSAND", "ASSIGNMENT", "Alphabet", "Alphanumeric", "CLOSEPAREN", "COMMENT", "DIVISION", "DO", "DOT", "Digit", "ELSE", "EQ_TEST", "EXCLAMATION", "FALSE", "IDENTIFIER", "IF", "INTNUM", "LE_TEST", "MINUS", "MULTIPLY", "OPENPAREN", "PLUS", "READ", "REAL", "SEMICOLON", "SKIP", "STRING", "THEN", "TRUE", "WHILE", "WRITE", "WRITELN", "WS"};
  public static final int AMPERSAND=4;
  public static final int ASSIGNMENT=5;
  public static final int Alphabet=6;
  public static final int Alphanumeric=7;
  public static final int CLOSEPAREN=8;
  public static final int COMMENT=9;
  public static final int DIVISION=10;
  public static final int DO=11;
  public static final int DOT=12;
  public static final int Digit=13;
  public static final int ELSE=14;
  public static final int EQ_TEST=15;
  public static final int EXCLAMATION=16;
  public static final int FALSE=17;
  public static final int IDENTIFIER=18;
  public static final int IF=19;
  public static final int INTNUM=20;
  public static final int LE_TEST=21;
  public static final int MINUS=22;
  public static final int MULTIPLY=23;
  public static final int OPENPAREN=24;
  public static final int PLUS=25;
  public static final int READ=26;
  public static final int REAL=27;
  public static final int SEMICOLON=28;
  public static final int SKIP=29;
  public static final int STRING=30;
  public static final int THEN=31;
  public static final int TRUE=32;
  public static final int WHILE=33;
  public static final int WRITE=34;
  public static final int WRITELN=35;
  public static final int WS=36;
    // CAMLE TOKENS END [SPECIAL COMMENT TOKEN]

    //fields
    private enum VarType {IntegerNumber, RealNumber};
    private enum RefType {DefineVar, UseVar};

    private static boolean debugMemScope = false;

    private static class VarRef {
        public final VarType vType;
        public final RefType rType;
        public final IRTree node;
        public final int line;
        public final IRTree root;
        public final String name;
        public final boolean inCond;

        public static VarRef def(VarType vt, IRTree n, IRTree r) {
            if (debugMemScope) logf("Defining var \'%s\' with type %s\n", n.getOp(), vt == VarType.IntegerNumber ? "Integer" : "Real");
            return new VarRef(vt, RefType.DefineVar, n, r);
        }

        public static VarRef use(VarRef lastDef, IRTree n) {
            if (debugMemScope) logf("Using var \'%s\' with type %s\n", n.getOp(), lastDef.vType == VarType.IntegerNumber ? "Integer" : "Real");
            return new VarRef(lastDef.vType, RefType.UseVar, n);
        }

        public int getAddr() {
            return Integer.parseInt(node.getOp());
        }

        public void setAddr(int addr) {
            node.setOp("" + addr);
        }

        private VarRef(VarType vt, RefType rt, IRTree n) {
            vType = vt;
            rType = rt;
            node = n;
            line = statementCount;
            root = null;
            name = node.getOp();
            inCond = cLevel > 0;
        }

        private VarRef(VarType vt, RefType rt, IRTree n , IRTree r) {
            vType = vt;
            rType = rt;
            node = n;
            line = statementCount;
            root = r;
            name = node.getOp();
            inCond = cLevel > 0;
        }
    }

    private static int statementCount = 0;
    private static ArrayList<VarRef> varRefs = new ArrayList<VarRef>();
    private static ArrayList<IRTree> stringAllocs = new ArrayList<IRTree>();
    private static int cLevel = 0;

    //IRT Tokens
    public static final String CONST = "CONST", CONSTR = "CONSTR",
                               SEQ = "SEQ", WR = "WR", WRR = "WRR", WRS = "WRS", BINOP = "BINOP", MEM = "MEM", MOVE = "MOVE", RD = "RD",
                               CJUMP = "CJUMP", BOOLE = "BOOLE", LABEL = "LABEL", DO_NOTHING = "DO_NOTHING", LE = "LE_TEST",
                               EQ = "EQ_TEST", AND = "AMPERSAND", NOT = "NOT", BOOLTRUE = "TRUE", BOOLFALSE = "FALSE",
                               TESTEXP = "TESTEXP", LOOP = "LOOP", PLUS_SIGN = "PLUS", MULTIPLY_SIGN = "MULTIPLY",
                               MINUS_SIGN = "MINUS", DIVISON_SIGN = "DIVISION", CONV_ITOR = "CONV_ITOR";

    public static IRTree convert(CommonTree ast) {
        IRTree irt = new IRTree();
        program(ast, irt);

        //allocate ints
        allocateInts();

        //allocate strings at the end
        allocateStrings();

        return irt;
    }

    private static void allocateInts() {
        ArrayList<VarRef> curDefs = new ArrayList<VarRef>();
        ArrayList<Integer> freedAddrs = new ArrayList<Integer>(1);

        for (int i = 0; i < varRefs.size(); i++) {
            VarRef vr = varRefs.get(i);
            String varName = vr.name;
            VarRef curDef = null;
            for (VarRef cd : curDefs) {
                //logf("Looking for curDef of \'%s\', at \'%s\'\n", varName, cd.name);
                if (cd.name.equals(varName)) {
                    curDef = cd;
                    break;
                }
            }

            if (vr.rType == RefType.DefineVar) {
                if (curDef == null) {
                    Integer addr = null;
                    if (freedAddrs.size() > 0) {
                        addr = freedAddrs.get(0);
                        freedAddrs.remove(0);
                    } else if (vr.root.getOp().equals(MOVE)) {
                        String valType = vr.root.getSub(1).getOp();
                        if (valType.equals(CONST)) {
                            if (vr.inCond) {
                                addr = Memory.allocMem(0);
                            } else {
                                addr = Memory.allocMem(Integer.parseInt(vr.root.getSub(1).getSub(0).getOp()));
                                vr.root.setOp(DO_NOTHING);
                            }
                        } else if (valType.equals(CONSTR)) {
                            addr = Memory.calloc(4);
                        } else {
                            addr = Memory.calloc(4);
                        }
                    } else {

                        addr = Memory.calloc(4);
                    }

                    vr.setAddr(addr);
                    curDef = vr;
                    curDefs.add(curDef);
                    //logf("Put def of \'%s\' into curDefs\n", varName);
                } else {
                    vr.setAddr(curDef.getAddr());
                    curDef = vr;
                    curDefs.add(curDef);
                }
            } else if (vr.rType == RefType.UseVar) {
                if (curDef != null) {
                    vr.setAddr(curDef.getAddr());
                } else {
                    errf("Logic error:\nIn allocating integer memory, using variable without having defined it: \'%s\'\n", varName);
                }
            }

            //find next ref; rType == Define then can free now
            boolean canFree = false;
            for (int j = i + 1; j < varRefs.size(); j++) {
                VarRef futureVr = varRefs.get(j);
                if (futureVr.name.equals(varName)) {
                    canFree = futureVr.rType == RefType.DefineVar && !futureVr.inCond;
                    break;
                }
            }

            if (canFree) {
                curDefs.remove(curDef);
                freedAddrs.add(curDef.getAddr());
            }
        }
    }

    private static void allocateStrings() {
        HashMap<String, Integer> stringTable = new HashMap<String, Integer>();
        for (IRTree sirt : stringAllocs) {
            String s = sirt.getOp();
            Integer addr = stringTable.get(s);
            if (addr == null) {
                addr = Memory.allocString(s + "\0");
                stringTable.put(s, addr);
            }

            sirt.setOp(addr.toString());
        }
    }

    public static void program(CommonTree ast, IRTree irt) {
        statements(ast, irt);
    }

    public static void statements(CommonTree ast, IRTree irt) {
        int i;
        Token t = ast.getToken();
        int tt = t.getType();
        if (tt == SEMICOLON) {
            IRTree irt1 = new IRTree();
            IRTree irt2 = new IRTree();
            CommonTree ast1 = astChild(ast, 0);
            CommonTree ast2 = astChild(ast, 1);
            statements(ast1, irt1);
            statements(ast2, irt2);
            irt.setOp(SEQ);
            irt.addSub(irt1);
            irt.addSub(irt2);
        } else {
            statement(ast, irt);
        }
    }

    public static void statement(CommonTree ast, IRTree irt) {
        CommonTree ast1, ast2, ast3;
        IRTree irt1 = new IRTree(), irt2 = new IRTree(), irt3 = new IRTree();
        Token t = ast.getToken();
        int tt = t.getType();

        switch (tt) {
            case WRITE: {
                    ast1 = astChild(ast, 0);
                    switch (arg(ast1, irt1)) {
                        case "expr": {
                                irt.setOp(WRR);
                                irt.addSub(irt1);
                                break;
                            }
                        case "exp": {
                                irt.setOp(WR);
                                irt.addSub(irt1);
                                break;
                            }
                        case "identifier": {
                                //look for var address in table; error if not found
                                String name = ast1.getToken().getText();
                                VarRef lastDef = findLastDef(name);
                                if (lastDef == null) {
                                    logf("Logic error:\nIn write, uninitialised argument variable: %s\n", name);
                                    error(tt);
                                } else {
                                    irt.setOp(lastDef.vType == VarType.IntegerNumber ? WR : WRR);
                                    IRTree node = new IRTree(name);
                                    varRefs.add(VarRef.use(lastDef, node));
                                    irt.addSub(new IRTree(MEM, new IRTree(CONST, node)));
                                }
                                break;
                            }
                        case "boolexp": {
                                boolexp(ast1, irt1); //translate condition

                                //precomputationnn
                                if (irt1.getOp().equals(BOOLE)) {
                                    String bl = irt1.getSub(0).getOp().toLowerCase();
                                    irt.setOp(WRS);
                                    irt.addSub(new IRTree(MEM, new IRTree(CONST, new IRTree(bl))));
                                    stringAllocs.add(irt.getSub(0).getSub(0).getSub(0));
                                } else {
                                    irt.setOp(CJUMP);

                                    //condition, seq1, seq2
                                    irt.addSub(irt1); //set condition
                                    irt.addSub(new IRTree(WRS, new IRTree(MEM, new IRTree(CONST, new IRTree("true")))));//seq1
                                    stringAllocs.add(irt.getSub(3).getSub(0).getSub(0).getSub(0));
                                    irt.addSub(new IRTree(WRS, new IRTree(MEM, new IRTree(CONST, new IRTree("false")))));//seq2
                                    stringAllocs.add(irt.getSub(4).getSub(0).getSub(0).getSub(0));
                                }
                                break;
                            }
                        default: {
                                irt.setOp(WRS);
                                irt.addSub(irt1);
                                break;
                            }
                    }
                    break;
                }
            case WRITELN: {
                    irt.setOp(WRS);
                    irt.addSub(new IRTree(MEM, new IRTree(CONST, new IRTree("\n"))));
                    stringAllocs.add(irt.getSub(0).getSub(0).getSub(0));
                    break;
                }
            case ASSIGNMENT: {
                    ast1 = astChild(ast, 0);
                    String name1 = ast1.getToken().getText();
                    String type1 = arg(ast1, irt1);
                    ast2 = astChild(ast, 1);
                    String name2 = ast2.getToken().getText();
                    String type2 = arg(ast2, irt2);

                    if (type1.equals("identifier")) {
                        irt.setOp(MOVE);
                        IRTree name1Node = new IRTree(name1);

                        //left hand side
                        irt.addSub(new IRTree(MEM, new IRTree(CONST, name1Node)));

                        if (type2.equals("exp")) {
                            varRefs.add(VarRef.def(VarType.IntegerNumber, name1Node, irt));

                            //right hand side
                            irt.addSub(irt2);
                        } else if (type2.equals("expr")) {
                            varRefs.add(VarRef.def(VarType.RealNumber, name1Node, irt));

                            //right hand side
                            irt.addSub(irt2);
                        } else if (type2.equals("identifier")) {
                            //look for var address in table; error if not found
                            VarRef lastDef2 = findLastDef(name2);
                            if (lastDef2 == null) {
                                logf("Logic error:\nIn assignment, uninitialised right hand side variable: %s\n", name2);
                                error(tt);
                            } else {
                                IRTree name2Node = new IRTree(name2);
                                varRefs.add(VarRef.def(lastDef2.vType, name1Node, irt));
                                varRefs.add(VarRef.use(lastDef2, name2Node));
                                irt.addSub(new IRTree(MEM, new IRTree(CONST, name2Node)));
                            }
                        } else {
                            logf("Logic error:\nIn assignment, unsupported assignment type: %s (%s)\n", name2, type2);
                            error(tt);
                        }
                    } else {
                        logf("Logic error:\nIn assignment, assigning to a non-variable: %s.\n", name1);
                        error(tt);
                    }
                    break;
                }
            case READ: {
                    CommonTree child = astChild(ast, 0);
                    String name = child.getToken().getText();
                    String type = arg(child, irt1);
                    if (type.equals("identifier")) {
                        IRTree nameNode = new IRTree(name);
                        varRefs.add(VarRef.def(VarType.IntegerNumber, nameNode, irt));
                        irt.setOp(RD);
                        irt.addSub(new IRTree(MEM, new IRTree(CONST, nameNode)));
                    } else {
                        logf("Logic error:\nIn read, reading into non-variable: %s\n", name);
                        error(tt);
                    }
                    break;
                }
            case IF: {
                    cLevel++;
                    boolexp(astChild(ast, 0), irt1); //translate condition

                    //precomputationnn
                    if (irt1.getOp().equals(BOOLE)) {
                        String bl = irt1.getSub(0).getOp();
                        switch (bl) {
                            case BOOLTRUE:
                                statements(astChild(ast, 1), irt);//translate seq1
                                break;
                            case BOOLFALSE:
                                statements(astChild(ast, 2), irt);//translate seq2
                                break;
                            default:
                                logf("Logic error:\nIn if, unknown boolean literal: %s\n", bl);
                                error(tt);
                        }
                    } else {
                        statements(astChild(ast, 1), irt2);//translate seq1
                        statements(astChild(ast, 2), irt3);//translate seq2
                        boolean seq1skip = irt2.getOp().equals(DO_NOTHING);

                        irt.setOp(CJUMP);

                        //condition, seq1, seq2
                        irt.addSub(seq1skip ? new IRTree(NOT, irt1) : irt1); //set condition
                        irt.addSub(seq1skip ? irt3 : irt2); //seq1
                        irt.addSub(seq1skip ? irt2 : irt3); //seq2
                    }

                    cLevel--;
                    break;
                }
            case SKIP: {
                    irt.setOp(DO_NOTHING);
                    break;
                }
            case WHILE: {
                    cLevel++;

                    boolexp(astChild(ast, 0), irt1);
                    statements(astChild(ast, 1), irt2);

                    if (irt1.getOp().equals(BOOLE) && irt1.getSub(0).getOp().equals(BOOLFALSE)) {
                        irt.setOp(DO_NOTHING);
                    } else {
                        irt.setOp(LOOP);
                        irt.addSub(irt1);
                        irt.addSub(irt2);
                    }

                    cLevel--;
                    break;
                }
            default: {
                    error(tt);
                }
        }

        statementCount++;
    }

    private static String toHex(int num, int length) {
        //String hex = new StringBuilder(Integer.toHexString(num)).reverse().toString();
        String hex = Integer.toHexString(num);
        while (hex.length() < length)
            hex = "0" + hex;
        if (hex.length() > length) {
            logf("Declared constant integer too large\n");
            error(0);
        }
        return hex;
    }

    private static void boolexp(CommonTree ast, IRTree irt) {
        Token t = ast.getToken();
        int tt = t.getType();
        String tx = tokenNames[tt];
        if (tt == TRUE || tt == FALSE) {
            irt.setOp(BOOLE);
            irt.addSub(new IRTree(tx));
        } else if (tt == EQ_TEST || tt == LE_TEST) {
            IRTree irte1 = new IRTree(), irte2 = new IRTree();;
            expression(astChild(ast, 0), irte1); //e1
            expression(astChild(ast, 1), irte2); //e2

            //precomputationnn
            if (irte1.getOp().equals(CONST) && irte2.getOp().equals(CONST)) {
                irt.setOp(BOOLE);
                switch (tt) {
                    case LE_TEST:
                        irt.addSub(new IRTree(
                                       Integer.parseInt(irte1.getSub(0).getOp()) <= Integer.parseInt(irte2.getSub(0).getOp()) ?
                                       BOOLTRUE : BOOLFALSE)
                                  );
                        break;
                    case EQ_TEST:
                        irt.addSub(new IRTree(
                                       Integer.parseInt(irte1.getSub(0).getOp()) == Integer.parseInt(irte2.getSub(0).getOp()) ?
                                       BOOLTRUE : BOOLFALSE)
                                  );
                        break;
                    default:
                }
            } else {
                irt.setOp(TESTEXP);
                irt.addSub(new IRTree(tx)); //op
                irt.addSub(irte1);
                irt.addSub(irte2);
            }
        } else if (tt == AMPERSAND) {
            IRTree irtbe1 = new IRTree(), irtbe2 = new IRTree();
            boolexp(astChild(ast, 0), irtbe1);
            boolexp(astChild(ast, 1), irtbe2);

            //precomputationnn
            if (irtbe1.getOp().equals(BOOLE) && irtbe2.getOp().equals(BOOLE)) {
                irt.setOp(BOOLE);
                irt.addSub(
                    new IRTree(
                        irtbe1.getSub(0).getOp().equals(BOOLTRUE) && irtbe2.getSub(0).getOp().equals(BOOLTRUE) ?
                        BOOLTRUE : BOOLFALSE
                    ));
            } else {
                irt.setOp(AND);
                irt.addSub(irtbe1);
                irt.addSub(irtbe2);
            }
        } else if ( tt == EXCLAMATION) {
            IRTree irtbe = new IRTree();
            boolexp(astChild(ast, 0), irtbe);

            //precomputationnn
            if (irtbe.getOp().equals(BOOLE)) {
                irt.setOp(BOOLE);
                irt.addSub(new IRTree(irtbe.getSub(0).getOp().equals(BOOLTRUE) ? BOOLFALSE : BOOLTRUE));
            } else {
                irt.setOp(NOT);
                irt.addSub(irtbe);
            }
        } else {
            logf("Logic error:\nIn boolean expression, unknown operation: %s\n", tokenNames[tt]);
            error(tt);
        }
    }

    private static CommonTree astChild(CommonTree ast, int childIndex) {
        return (CommonTree) ast.getChild(childIndex);
    }

    private static VarRef findLastDef(String varName) {
        for (int i = varRefs.size() - 1; i > -1; i--) {
            VarRef vr = varRefs.get(i);
            if (vr.name.equals(varName) && vr.rType == RefType.DefineVar)
                return vr;
        }
        return null;
    }

    private static void logf(String format, Object... args) {
        System.err.printf(format, args);
    }

    private static void errf(String format, Object... args) {
        logf(format, args);
        System.exit(1);
    }

    public static String arg(CommonTree ast, IRTree irt) {
        Token t = ast.getToken();
        int tt = t.getType();
        if (tt == STRING) {
            String tx = t.getText();
            irt.setOp(MEM);
            irt.addSub(new IRTree(CONST, new IRTree(tx)));
            stringAllocs.add(irt.getSub(0).getSub(0));
            return "string";
        } else if (tt == IDENTIFIER) {
            return "identifier";
        } else if (Arrays.asList(new Integer[] {TRUE, FALSE, EXCLAMATION, AMPERSAND, LE_TEST, EQ_TEST}).contains(tt)) {
            return "boolexp";
        } else {
            VarType vt = expression(ast, irt);
            return vt == VarType.IntegerNumber ? "exp" : "expr";
        }
    }

    public static VarType expression(CommonTree ast, IRTree irt) {
        Token t = ast.getToken();
        int tt = t.getType();
        switch (tt) {
            case INTNUM: {
                    IRTree irt1 = new IRTree();
                    constant(ast, irt1);
                    irt.setOp(CONST);
                    irt.addSub(irt1);
                    return VarType.IntegerNumber;
                }
            case REAL: {
                    IRTree irt1 = new IRTree();
                    constant(ast, irt1);
                    irt.setOp(CONSTR);
                    irt.addSub(irt1);
                    return VarType.RealNumber;
                }
            case PLUS:
            case MINUS:
            case MULTIPLY:
            case DIVISION: {
                    IRTree irt1 = new IRTree(), irt2 = new IRTree();
                    VarType vt1 = expression(astChild(ast, 0), irt1);
                    VarType vt2 = expression(astChild(ast, 1), irt2);
                    String op1 = irt1.getOp(), op2 = irt2.getOp();

                    if (vt1 == VarType.RealNumber || vt2 == VarType.RealNumber) {
                        if (op1.contains(CONST) && op2.contains(CONST)) {
                            //precompute!!
                            irt.setOp(CONSTR);
                            Double result = new Double(0);
                            double constr1 = Double.parseDouble(irt1.getSub(0).getOp());
                            double constr2 = Double.parseDouble(irt2.getSub(0).getOp());

                            switch (tt) {
                                case PLUS:
                                    result = constr1 + constr2;
                                    break;
                                case MINUS:
                                    result = constr1 - constr2;
                                    break;
                                case MULTIPLY:
                                    result = constr1 * constr2;
                                    break;
                                case DIVISION: {
                                        if (constr2 == 0) {
                                            logf("Logic error:\nDivision by zero\n");
                                            error(tt);
                                        }
                                        result = constr1 / constr2;
                                    }
                                default:
                                    break;
                            }

                            irt.addSub(new IRTree(result.toString()));
                        } else {
                            irt.setOp(BINOP);
                            irt.addSub(new IRTree(tokenNames[tt] + "R"));
                            if (vt1 != VarType.RealNumber) {
                                IRTree irt_conv = new IRTree();
                                irt_conv.setOp(CONV_ITOR);
                                irt_conv.addSub(irt1);
                                irt.addSub(irt_conv);
                                irt.addSub(irt2);
                            } else if (vt2 != VarType.RealNumber) {
                                IRTree irt_conv = new IRTree();
                                irt_conv.setOp(CONV_ITOR);
                                irt_conv.addSub(irt2);
                                irt.addSub(irt1);
                                irt.addSub(irt_conv);
                            } else {
                                irt.addSub(irt1);
                                irt.addSub(irt2);
                            }

                            if (tt == DIVISION) {
                                String rhsType = irt.getSub(2).getOp();
                                if (rhsType.equals(CONST) || rhsType.equals(CONSTR)) {
                                    if (Double.parseDouble(irt.getSub(2).getSub(0).getOp()) == 0) {
                                        logf("Logic error:\nDivision by zero\n");
                                        error(tt);
                                    }
                                }
                            }
                        }
                        return VarType.RealNumber;
                    } else {
                        if (op1.equals(CONST) && op2.equals(CONST)) {
                            //precompute!!
                            irt.setOp(CONST);
                            Integer result = 0;
                            Integer const1 = Integer.parseInt(irt1.getSub(0).getOp());
                            Integer const2 = Integer.parseInt(irt2.getSub(0).getOp());

                            switch (tt) {
                                case PLUS:
                                    result = const1 + const2;
                                    break;
                                case MINUS:
                                    result = const1 - const2;
                                    break;
                                case MULTIPLY:
                                    result = const1 * const2;
                                    break;
                                case DIVISION: {
                                        if (const2 == 0) {
                                            logf("Logic error:\nDivision by zero\n");
                                            error(tt);
                                        }
                                        result = const1 / const2;
                                    }
                                default:
                                    break;
                            }

                            irt.addSub(new IRTree(result.toString()));
                        } else {
                            irt.setOp(BINOP);
                            irt.addSub(new IRTree(tokenNames[tt]));
                            irt.addSub(irt1);
                            irt.addSub(irt2);
                            if (tt == DIVISION) {
                                String rhsType = irt.getSub(2).getOp();
                                if (rhsType.equals(CONST) || rhsType.equals(CONSTR)) {
                                    if (Double.parseDouble(irt.getSub(2).getSub(0).getOp()) == 0) {
                                        logf("Logic error:\nDivision by zero\n");
                                        error(tt);
                                    }
                                }
                            }
                        }
                        return VarType.IntegerNumber;
                    }
                }
            case IDENTIFIER: {
                    String varName = t.getText();
                    VarRef lastDef = findLastDef(varName);
                    if (lastDef == null) {
                        logf("Logic error:\nExpression contains uninitialised variable: %s\n", varName);
                        error(tt);
                    } else {
                        irt.setOp(MEM);
                        IRTree nameNode = new IRTree(varName);
                        varRefs.add(VarRef.use(lastDef, nameNode));
                        irt.addSub(new IRTree(CONST, nameNode));
                    }
                    return lastDef.vType;
                }
            default: {
                    logf("Logic error:\nExpression error on \"%s\"\n", t.getText());
                    error(tt);
                    return null;
                }
        }
    }

    public static void constant(CommonTree ast, IRTree irt) {
        Token t = ast.getToken();
        int tt = t.getType();
        if (tt == INTNUM || tt == REAL) {
            String tx = t.getText();
            irt.setOp(tx);
        } else {
            error(tt);
        }
    }

    private static void error(int tt) {
        System.out.println("IRT error: " + tokenNames[tt]);
        System.exit(1);
    }
}
