// COMS22201: Code generation

import java.util.*;
import java.io.*;
import java.lang.reflect.Array;
import antlr.collections.AST;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.*;

public class Cg {
    private static PrintStream ps;
    private static final String labelEndBlock = "lbeb", labelElse = "lbel", labelLoopHead = "lblh", labelNotTail = "lbnt";
    private static int lbebCount = 0, lbelCount = 0, lblhCount = 0, lbntCount = 0;


    public static void program(IRTree irt, PrintStream o) {
        ps = o;
        emit("XOR R0,R0,R0");         // Initialize R0 to 0
        statement(irt);
        emit("HALT");                    // Program must end with HALT
        Memory.dumpData(ps);             // Dump DATA lines: initial memory contents
    }

    private static void statement(IRTree irt) {
        String op = irt.getOp();

        switch (op) {
            case Irt.SEQ: {
                    statement(irt.getSub(0));
                    statement(irt.getSub(1));
                    break;
                }
            case Irt.WRS: {
                    String subOp = irt.getSub(0).getOp();
                    if (subOp.equals(Irt.MEM) && irt.getSub(0).getSub(0).getOp().equals(Irt.CONST)) {
                        String a = irt.getSub(0).getSub(0).getSub(0).getOp();
                        emit("WRS " + a);
                    } else {
                        logf("[!]WRS error: known sub op: %s\n", subOp);
                        error(op);
                    }
                    break;
                }
            case Irt.WR: {
                    String subOp = irt.getSub(0).getOp();
                    String e = subOp.equals(Irt.BOOLE) ? irt.getSub(0).getSub(0).getOp() : expression(irt.getSub(0), 1);
                    emit("WR " + e);
                    break;
                }
            case Irt.WRR: {
                    String subOp = irt.getSub(0).getOp();
                    String e = subOp.equals(Irt.BOOLE) ? irt.getSub(0).getSub(0).getOp() : expression(irt.getSub(0), 1);
                    emit("WRR " + e);
                    break;
                }
            case Irt.MOVE: {
                    String op1 = irt.getSub(0).getOp();

                    if (!op1.equals(Irt.MEM)) {
                        logf("[!]MOVE error: assigning to non-variable..\n");
                        error(op);
                    }

                    String op2 = irt.getSub(1).getOp();

                    //could either be
                    //          var := const
                    //or        var1 := var2
                    //or...     var := exp
                    int tempDataReg = 1;
                    if (op2.equals(Irt.CONST)) {
                        //put source constant int into r2
                        putInteger(tempDataReg, irt.getSub(1).getSub(0).getOp());
                    } else if (op2.equals(Irt.CONSTR)) {
                        //put source constant real into r2
                        putReal(tempDataReg, irt.getSub(1).getSub(0).getOp());
                    } else if (op2.equals(Irt.MEM)) {
                        loadMem(tempDataReg, Integer.parseInt(irt.getSub(1).getSub(0).getSub(0).getOp()));
                    } else if (op2.equals(Irt.BINOP)) {
                        expression(irt.getSub(1), tempDataReg);
                    } else {
                        logf("[!]MOVE error: unknown right hand side op: %s.\n", op2);
                        error(op);
                    }

                    storeMem(tempDataReg, Integer.parseInt(irt.getSub(0).getSub(0).getSub(0).getOp()));
                    break;
                }

            case Irt.RD: {
                    emit("RD R1");
                    storeMem(1, Integer.parseInt(irt.getSub(0).getSub(0).getSub(0).getOp()));
                    break;
                }
            case Irt.DO_NOTHING: {
                    //emit("NOP");
                    break;
                }
            case Irt.CJUMP: {
                    //conditional
                    boolean seq2skip = irt.getSub(2).getOp().equals(Irt.DO_NOTHING);
                    String lbeb = nextLabel(labelEndBlock);
                    String lbel = nextLabel(labelElse);

                    //optimise unused else block
                    genBoolExp(irt.getSub(0), seq2skip ? lbeb : lbel);

                    statement(irt.getSub(1));


                    if (!seq2skip) {
                        emit("JMP " + lbeb);
                        emit(lbel + ":");
                    }

                    statement(irt.getSub(2));
                    emit(lbeb + ":");

                    break;
                }
            case Irt.LOOP: {
                    String lblh = nextLabel(labelLoopHead);
                    String lbeb = nextLabel(labelEndBlock);

                    emit(lblh + ":");
                    genBoolExp(irt.getSub(0), lbeb);
                    statement(irt.getSub(1));
                    emit("JMP " + lblh);
                    emit(lbeb + ":");

                    break;
                }
            default: {
                    logf("[!]statement error: unknown op: %s\n", op);
                    error(op);
                    break;
                }
        }
    }

    private static String nextLabel(String header) {
        if (header.equals(labelEndBlock)) {
            return header + (lbebCount++);
        } else if (header.equals(labelElse)) {
            return header + (lbelCount++);
        } else if (header.equals(labelLoopHead)) {
            return header + (lblhCount++);
        } else if (header.equals(labelNotTail)) {
            return header + (lbntCount++);
        }{
            return "";
        }
    }

    private static void genBoolExp(IRTree irt, String gotoFail) {
        String op = irt.getOp();

        switch (op) {
            case Irt.BOOLE: {
                    String boolLiteral = irt.getSub(0).getOp();

                    switch (boolLiteral) {
                        case Irt.BOOLTRUE:
                            //do nothing
                            break;

                        case Irt.BOOLFALSE:
                            emit("JMP " + gotoFail);
                            break;

                        default:
                            logf("[!]genBoolExp error: unknown boolean literal: %s\n", boolLiteral);
                            error(boolLiteral);
                    }

                    break;
                }
            case Irt.TESTEXP: {
                    String testOp = irt.getSub(0).getOp();
                    expression(irt.getSub(1), 1);
                    expression(irt.getSub(2), 2);

                    //get diff
                    emit("SUB R1,R2,R1");

                    switch (testOp) {
                        case Irt.LE:
                            emit("BLTZ R1," + gotoFail);
                            break;

                        case Irt.EQ:
                            emit("BNEZ R1," + gotoFail);
                            break;

                        default:
                            logf("[!]genBoolExp error: unknown testexp operation: %s\n", testOp);
                            error(testOp);
                    }

                    break;
                }
            case Irt.AND: {
                    genBoolExp(irt.getSub(0), gotoFail);
                    genBoolExp(irt.getSub(1), gotoFail);
                    break;
                }
            case Irt.NOT: {
                    String lbnt = nextLabel(labelNotTail);
                    genBoolExp(irt.getSub(0), lbnt);
                    emit("JMP " + gotoFail);
                    emit(lbnt + ":");
                    break;
                }
            default:
                logf("[!]genBoolExp error: unknown boolexp op: %s\n", op);
                error(op);
        }
    }

    private static void storeMem(int srcReg, int address) {
        emit("STORE R" + srcReg + ",R0," + address);
    }

    private static void loadMem(int destReg, int address) {
        emit("LOAD R" + destReg + ",R0," + address);
    }

    private static String expression(IRTree irt, int destReg) {
        String op = irt.getOp();

        if (op.equals(Irt.CONST)) {
            String t = irt.getSub(0).getOp();
            putInteger(destReg, t);
        } else if (op.equals(Irt.CONSTR)) {
            String t = irt.getSub(0).getOp();
            putReal(destReg, t);
        } else if (op.equals(Irt.MEM)) {
            loadMem(destReg, Integer.parseInt(irt.getSub(0).getSub(0).getOp()));
        } else if (op.equals(Irt.CONV_ITOR)) {
            expression(irt.getSub(0), destReg);
            emit("ITOR R" + destReg + ",R" + destReg);
        } else if (op.equals(Irt.BINOP)) {
            int srcReg1 = destReg;
            int srcReg2 = destReg + 1;
            String opr1 = irt.getSub(1).getOp(), opr2 = irt.getSub(2).getOp();
            String operation = irt.getSub(0).getOp();
            String action = "";

            switch (operation) {
                case Irt.PLUS_SIGN:
                    action = "ADD";
                    break;

                case Irt.MINUS_SIGN:
                    action = "SUB";
                    break;

                case Irt.MULTIPLY_SIGN:
                    action = "MUL";
                    break;

                case Irt.DIVISON_SIGN:
                    action = "DIV";
                    break;

                case Irt.PLUS_SIGN + "R":
                    action = "ADDR";
                    break;

                case Irt.MINUS_SIGN + "R":
                    action = "SUBR";
                    break;

                case Irt.MULTIPLY_SIGN + "R":
                    action = "MULR";
                    break;

                case Irt.DIVISON_SIGN + "R":
                    action = "DIVR";
                    break;

                default:
                    logf("[!]expression->BINOP error: unknown operaton: \"%s\"\n", operation);
                    error(op);
            }

            if (opr1.equals(Irt.CONST)) {
                int const1 = Integer.parseInt(irt.getSub(1).getSub(0).getOp());

                if (opr2.equals(Irt.CONST)) {
                    //optimise by pre-calculating
                    int const2 = Integer.parseInt(irt.getSub(2).getSub(0).getOp());

                    Integer result = new Integer(0);

                    switch (operation) {
                        case Irt.PLUS_SIGN:
                            result = const1 + const2;
                            break;

                        case Irt.MINUS_SIGN:
                            result = const1 - const2;
                            break;

                        case Irt.MULTIPLY_SIGN:
                            result = const1 * const2;
                            break;

                        case Irt.DIVISON_SIGN:
                            result = const1 / const2;
                            break;
                        default:
                            logf("[!]expression->BINOP->CONST error: unknown operaton: %s\n", operation);
                            error(op);
                    }

                    putInteger(destReg, result.toString());
                } else if (opr2.equals(Irt.CONSTR)) {
                    //optimise by pre-calculating
                    double const2 = Double.parseDouble(irt.getSub(2).getSub(0).getOp());

                    Double result = new Double(0);
                    if (operation.contains(Irt.PLUS_SIGN)) {
                        result = const1 + const2;
                    } else if (operation.contains(Irt.MINUS_SIGN)) {
                        result = const1 - const2;
                    } else if (operation.contains(Irt.MULTIPLY_SIGN)) {
                        result = const1 * const2;
                    } else if (operation.contains(Irt.DIVISON_SIGN)) {
                        result = const1 / const2;
                    } else {
                        logf("[!]expression->BINOP->CONST error: unknown operaton: %s\n", operation);
                        error(op);
                    }

                    putReal(destReg, result.toString());
                } else {
                    expression(irt.getSub(2), srcReg2);

                    if (action.contains("R")) {
                        emit(action + " R" + destReg + ",R" + srcReg2 + "," + const1);
                    } else {
                        action += "I";
                        if (action.equals("SUBI")) {
                            putInteger(destReg, "" + const1);
                            emit("SUB R" + destReg + ",R" + destReg + ",R" + srcReg2);
                        } else if (action.equals("DIVI")) {
                            putInteger(destReg, "" + const1);
                            emit("DIVI R" + destReg + ",R" + destReg + ",R" + srcReg2);
                        } else {
                            emit(action + " R" + destReg + ",R" + srcReg2 + "," + const1);
                        }
                    }
                }
            } else if (opr1.equals(Irt.CONSTR)) {
                double const1 = Double.parseDouble(irt.getSub(1).getSub(0).getOp());

                if (opr2.equals(Irt.CONST) || opr2.equals(Irt.CONSTR)) {
                    //optimise by pre-calculating
                    double const2 = Double.parseDouble(irt.getSub(2).getSub(0).getOp());

                    Double result = new Double(0);
                    if (operation.contains(Irt.PLUS_SIGN)) {
                        result = const1 + const2;
                    } else if (operation.contains(Irt.MINUS_SIGN)) {
                        result = const1 - const2;
                    } else if (operation.contains(Irt.MULTIPLY_SIGN)) {
                        result = const1 * const2;
                    } else if (operation.contains(Irt.DIVISON_SIGN)) {
                        result = const1 / const2;
                    } else {
                        logf("[!]expression->BINOP->CONST error: unknown operaton: %s\n", operation);
                        error(op);
                    }

                    putReal(destReg, result.toString());
                } else {
                    expression(irt.getSub(2), srcReg2);
                    emit(action + " R" + destReg + ",R" + srcReg2 + "," + const1);
                }
            } else if (opr2.equals(Irt.CONSTR)) {
                Double const2 = Double.parseDouble(irt.getSub(2).getSub(0).getOp());
                expression(irt.getSub(1), srcReg1);

                putReal(srcReg2, const2.toString());
                emit(action + " R" + destReg + ",R" + srcReg1 + ",R" + srcReg2);
            } else if (opr2.equals(Irt.CONST)) {
                Integer const2 = Integer.parseInt(irt.getSub(2).getSub(0).getOp());
                expression(irt.getSub(1), srcReg1);

                if (action.contains("R")) {
                    toReal(srcReg2, const2.toString());
                    emit(action + " R" + destReg + ",R" + srcReg1 + ",R" + srcReg2);
                } else {
                    action += "I";
                    emit(action + " R" + destReg + ",R" + srcReg1 + "," + const2);
                }

            } else {
                expression(irt.getSub(1), srcReg1);
                expression(irt.getSub(2), srcReg2);

                emit(action + " R" + destReg + ",R" + srcReg1 + ",R" + srcReg2);
            }
        } else {
            logf("[!]expression error: unknown op: %s\n", op);
            error(op);
        }

        return "R" + destReg;
    }

    private static void toReal(int destReg, String intConst) {
        putInteger(destReg, intConst);
        emit("ITOR R" + destReg + "," + destReg);
    }

    private static void putReal(int destReg, String realConst) {
        //add constant to R0, put result in R1
        emit(String.format("MOVIR R" + destReg + ",%.5f", Double.parseDouble(realConst)));
    }

    private static void putInteger(int destReg, String intConst) {
        //add constant to R0, put result in R1
        emit("ADDI R" + destReg + ",R0," + intConst);
    }

    private static void logf(String format, Object... args) {
        System.out.printf(format, args);
    }

    private static void emit(String s) {
        ps.println(s);
    }

    private static void error(String op) {
        System.out.println("CG error: " + op);
        System.exit(1);
    }
}
