/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Constants;
import com.tangosol.dev.assembler.IntConstant;
import com.tangosol.dev.assembler.LongConstant;
import com.tangosol.dev.assembler.FloatConstant;
import com.tangosol.dev.assembler.DoubleConstant;
import com.tangosol.dev.assembler.FieldConstant;
import com.tangosol.dev.assembler.Op;
import com.tangosol.dev.assembler.OpDeclare;
import com.tangosol.dev.assembler.OpLoad;
import com.tangosol.dev.assembler.OpStore;
import com.tangosol.dev.assembler.Ivar;
import com.tangosol.dev.assembler.Lvar;
import com.tangosol.dev.assembler.Fvar;
import com.tangosol.dev.assembler.Dvar;
import com.tangosol.dev.assembler.Iload;
import com.tangosol.dev.assembler.Lload;
import com.tangosol.dev.assembler.Fload;
import com.tangosol.dev.assembler.Dload;
import com.tangosol.dev.assembler.Istore;
import com.tangosol.dev.assembler.Lstore;
import com.tangosol.dev.assembler.Fstore;
import com.tangosol.dev.assembler.Dstore;
import com.tangosol.dev.assembler.Iconst;
import com.tangosol.dev.assembler.Lconst;
import com.tangosol.dev.assembler.Fconst;
import com.tangosol.dev.assembler.Dconst;
import com.tangosol.dev.assembler.Iadd;
import com.tangosol.dev.assembler.Ladd;
import com.tangosol.dev.assembler.Fadd;
import com.tangosol.dev.assembler.Dadd;
import com.tangosol.dev.assembler.Isub;
import com.tangosol.dev.assembler.Lsub;
import com.tangosol.dev.assembler.Fsub;
import com.tangosol.dev.assembler.Dsub;
import com.tangosol.dev.assembler.Baload;
import com.tangosol.dev.assembler.Caload;
import com.tangosol.dev.assembler.Saload;
import com.tangosol.dev.assembler.Iaload;
import com.tangosol.dev.assembler.Laload;
import com.tangosol.dev.assembler.Faload;
import com.tangosol.dev.assembler.Daload;
import com.tangosol.dev.assembler.Bastore;
import com.tangosol.dev.assembler.Castore;
import com.tangosol.dev.assembler.Sastore;
import com.tangosol.dev.assembler.Iastore;
import com.tangosol.dev.assembler.Lastore;
import com.tangosol.dev.assembler.Fastore;
import com.tangosol.dev.assembler.Dastore;
import com.tangosol.dev.assembler.Iinc;
import com.tangosol.dev.assembler.I2b;
import com.tangosol.dev.assembler.I2c;
import com.tangosol.dev.assembler.I2s;
import com.tangosol.dev.assembler.Dup;
import com.tangosol.dev.assembler.Dup2;
import com.tangosol.dev.assembler.Dup_x1;
import com.tangosol.dev.assembler.Dup2_x1;
import com.tangosol.dev.assembler.Dup_x2;
import com.tangosol.dev.assembler.Dup2_x2;
import com.tangosol.dev.assembler.Getfield;
import com.tangosol.dev.assembler.Putfield;
import com.tangosol.dev.assembler.Getstatic;
import com.tangosol.dev.assembler.Putstatic;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* The pre- and post-increment and -decrement expressions.
*
* @version 1.00, 01/09/98
* @author  Cameron Purdy
*/
abstract public class IncExpression extends UnaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a IncExpression.
    *
    * @param operator  the operator token
    * @param expr      the sub-expression
    */
    protected IncExpression(Token operator, Expression expr)
        {
        super(operator, expr);
        }


    // ----- code generation ------------------------------------------------

    /**
    * Perform semantic checks, parse tree re-organization, name binding,
    * and optimizations.
    *
    * @param ctx        the compiler context
    * @param setUVars   the set of potentially unassigned variables
    * @param setFVars   the set of potentially assigned final variables
    * @param mapThrown  the set of potentially thrown checked exceptions
    * @param errlist    the error list
    *
    * @return the resulting language element (typically this)
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected Element precompile(Context ctx, DualSet setUVars, DualSet setFVars, Map mapThrown, ErrorList errlist)
            throws CompilerException
        {
        // get the sub-expression
        Expression expr = getExpression();

        // sub-expression must be assignable
        if (expr.checkAssignable(errlist))
            {
            // pre-compile the sub-expression
            expr = (Expression) expr.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

            // sub-expression must be numeric type
            expr.checkNumeric(errlist);
            }

        // store the sub-expression
        setExpression(expr);

        // the result type is the type of the sub-expression
        setType(expr.getType());

        if (expr instanceof VariableExpression)
            {
            Variable var = ((VariableExpression) expr).getVariable();

            // verify variable is definitely assigned
            if (setUVars.contains(var))
                {
                // variable is potentially unassigned
                logError(ERROR, VAR_UNASSIGNED, new String[] {var.getName()}, errlist);
                }

            // JLS 16.1.15 Operators ++ and --
            // V is definitely assigned after a preincrement, predecrement,
            // postincrement, or postdecrement expression iff either the
            // operand expression is V or V is definitely assigned after
            // the operand expression.
            setUVars.remove(var);
            if (var.isFinal())
                {
                setFVars.add(var);
                }
            }

        return this;
        }

    /**
    * Perform final optimizations and code generation.
    *
    * @param ctx       the compiler context
    * @param code      the assembler code attribute to compile to
    * @param fReached  true if this language element is reached (JLS 14.19)
    * @param errlist   the error list to log errors to
    *
    * @return true if the element can complete normally (JLS 14.1)
    *
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected boolean compile(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException
        {
        Expression expr  = getExpression();

        char    chType   = expr.getType().getTypeString().charAt(0);
        boolean fDiscard = isDiscarded();
        boolean fWide    = (chType == 'J' || chType == 'D');

        boolean fSub  = (this instanceof PreDecExpression || this instanceof PostDecExpression);
        boolean fPre  = (this instanceof PreIncExpression || this instanceof PreDecExpression);
        boolean fPost = !fPre;

        // compilation of "++var":
        //      ?load var       // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //      ?const_1        // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //      ?add            // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //                      // ?sub if pre-dec
        //      i2?             // b for byte, c for char, s for short
        //                      // (n/a for int/long/float/double)
        //      dup?            // if not discarded; dup2 for long/double,
        //                      // dup for byte/char/short/int/float
        //      ?store var      // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //
        // optimization of "++var" for int:
        //      iinc var,1      // -1 if pre-dec
        //      iload var       // if not discarded
        //
        // compilation of "var++":
        //      ?load var       // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //      dup?            // if not discarded; dup2 for long/double,
        //                      // dup for byte/char/short/int/float
        //      ?const_1        // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //      ?add            // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //                      // ?sub if pre-dec
        //      i2?             // b for byte, c for char, s for short
        //                      // (n/a for int/long/float/double)
        //      ?store var      // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //
        // optimization of "++var" for int:
        //      iload var       // if not discarded
        //      iinc var,1      // -1 if pre-dec
        //
        // Note:  The only difference between "++<expr>" and "<expr>++" is
        // the location of the dup when the result is not discarded.  This
        // holds true for fields and arrays as well as with variables.  For
        // postfix increment and decrement in which the result is not disc-
        // arded, the dup instruction is moved from after the conversion to
        // before the constant increment of 1.
        //
        // compilation of "++field":
        //      getstatic field
        //      ?const_1        // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //      ?add            // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //                      // ?sub if pre-dec
        //      i2?             // b for byte, c for char, s for short
        //                      // (n/a for int/long/float/double)
        //      dup?            // if not discarded; dup2 for long/double,
        //                      // dup for byte/char/short/int/float
        //      putstatic field
        //
        // compilation of "++ref.field":
        //      <ref>
        //      dup
        //      getfield field
        //      ?const_1        // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //      ?add            // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //                      // ?sub if pre-dec
        //      i2?             // b for byte, c for char, s for short
        //                      // (n/a for int/long/float/double)
        //      dup?_x1         // if not discarded; dup2_x1 for long/double,
        //                      // dup_x1 for byte/char/short/int/float
        //      putfield field
        //
        // compilation of "++array[index]":
        //      <array>
        //      <index>
        //      dup2
        //      ?aload          // b for byte, c for char, s for short, i for int,
        //                      // l for long, f for float, d for double
        //      ?const_1        // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //      ?add            // i for byte/char/short/int, l for long,
        //                      // f for float, d for double
        //                      // ?sub if pre-dec
        //      i2?             // b for byte, c for char, s for short
        //                      // (n/a for int/long/float/double)
        //      dup?_x2         // if not discarded; dup2_x2 for long/double,
        //                      // dup_x2 for byte/char/short/int/float
        //      ?astore         // b for byte, c for char, s for short, i for int,
        //                      // l for long, f for float, d for double

        // specific handling for each potential expression class
        Op opFinal = null;
        Op opDup   = null;
        if (expr instanceof VariableExpression)
            {
            OpDeclare opVar   = ((VariableExpression) expr).getVariable().getOp();
            OpLoad    opLoad  = null;
            OpStore   opStore = null;

            switch (chType)
                {
                case 'B':
                case 'C':
                case 'S':
                    opLoad  = new Iload ((Ivar) opVar);
                    opStore = new Istore((Ivar) opVar);
                    break;
                case 'I':
                    {
                    // optimization for int vars
                    Ivar var    = (Ivar) opVar;

                    if (!fDiscard && fPost)
                        {
                        code.add(new Iload(var));
                        }

                    code.add(new Iinc(var, fSub ? (short) -1 : 1));

                    if (!fDiscard && fPre)
                        {
                        code.add(new Iload(var));
                        }
                    }
                    return fReached;
                case 'J':
                    opLoad  = new Lload ((Lvar) opVar);
                    opStore = new Lstore((Lvar) opVar);
                    break;
                case 'F':
                    opLoad  = new Fload ((Fvar) opVar);
                    opStore = new Fstore((Fvar) opVar);
                    break;
                case 'D':
                    opLoad  = new Dload ((Dvar) opVar);
                    opStore = new Dstore((Dvar) opVar);
                    break;
                }

            // set-up code
            code.add(opLoad);

            if (!fDiscard)
                {
                opDup = fWide ? (Op) new Dup2() : new Dup();
                }

            opFinal = opStore;
            }
        else if (expr instanceof FieldAccessExpression)
            {
            FieldAccessExpression exprField = (FieldAccessExpression) expr;
            FieldConstant         field     = exprField.getFieldConstant();
            boolean               fStatic   = exprField.isStatic();

            // load field
            if (fStatic)
                {
                code.add(new Getstatic(field));

                opDup   = fWide ? (Op) new Dup2() : new Dup();
                opFinal = new Putstatic(field);
                }
            else
                {
                // compile reference expression
                exprField.getExpression().compile(ctx, code, fReached, errlist);

                // make a copy (it will be used by the Putfield op)
                code.add(new Dup());

                code.add(new Getfield(field));

                opDup   = fWide ? (Op) new Dup2_x1() : new Dup_x1();
                opFinal = new Putfield(field);
                }
            }
        else if (expr instanceof ArrayAccessExpression)
            {
            Op opLoad  = null;
            Op opStore = null;
            switch (chType)
                {
                case 'B':
                    opLoad  = new Baload ();
                    opStore = new Bastore();
                    break;
                case 'C':
                    opLoad  = new Caload ();
                    opStore = new Castore();
                    break;
                case 'S':
                    opLoad  = new Saload ();
                    opStore = new Sastore();
                    break;
                case 'I':
                    opLoad  = new Iaload ();
                    opStore = new Iastore();
                    break;
                case 'J':
                    opLoad  = new Laload ();
                    opStore = new Lastore();
                    break;
                case 'F':
                    opLoad  = new Faload ();
                    opStore = new Fastore();
                    break;
                case 'D':
                    opLoad  = new Daload ();
                    opStore = new Dastore();
                    break;
                }

            // set-up code
            ArrayAccessExpression exprElement = (ArrayAccessExpression) expr;
            exprElement.getArray().compile(ctx, code, fReached, errlist);
            exprElement.getIndex().compile(ctx, code, fReached, errlist);
            code.add(new Dup2());
            code.add(opLoad);

            if (!fDiscard)
                {
                opDup = fWide ? (Op) new Dup2_x2() : new Dup_x2();
                }

            opFinal = opStore;
            }
        else
            {
            throw new IllegalStateException();
            }

        Op opConst = null;
        Op opAdd   = null;
        Op opConv  = null;
        switch (chType)
            {
            case 'B':
                opConst = new Iconst(CONSTANT_ICONST_1);
                opAdd   = fSub ? (Op) new Isub() : new Iadd();
                opConv  = new I2b();
                break;

            case 'C':
                opConst = new Iconst(CONSTANT_ICONST_1);
                opAdd   = fSub ? (Op) new Isub() : new Iadd();
                opConv  = new I2c();
                break;

            case 'S':
                opConst = new Iconst(CONSTANT_ICONST_1);
                opAdd   = fSub ? (Op) new Isub() : new Iadd();
                opConv  = new I2s();
                break;

            case 'I':
                opConst = new Iconst(CONSTANT_ICONST_1);
                opAdd   = fSub ? (Op) new Isub() : new Iadd();
                break;

            case 'J':
                opConst = new Lconst(CONSTANT_LCONST_1);
                opAdd   = fSub ? (Op) new Lsub() : new Ladd();
                break;

            case 'F':
                opConst = new Fconst(CONSTANT_FCONST_1);
                opAdd   = fSub ? (Op) new Fsub() : new Fadd();
                break;

            case 'D':
                opConst = new Dconst(CONSTANT_DCONST_1);
                opAdd   = fSub ? (Op) new Dsub() : new Dadd();
                break;

            default:
                throw new IllegalStateException();
            }

        // non-discarded <expr>++ and <expr>--
        if (fPost && opDup != null)
            {
            code.add(opDup);
            }

        // add (or subtract) one
        code.add(opConst);
        code.add(opAdd);

        // convert result if necessary
        if (opConv != null)
            {
            code.add(opConv);
            }

        // non-discarded ++<expr> and --<expr>
        if (fPre && opDup != null)
            {
            code.add(opDup);
            }

        // store incremented value
        if (opFinal != null)
            {
            code.add(opFinal);
            }

        // normal completion possible if reachable
        return fReached;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "IncExpression";

    private static final IntConstant    CONSTANT_ICONST_1 = Constants.CONSTANT_ICONST_1;
    private static final LongConstant   CONSTANT_LCONST_1 = Constants.CONSTANT_LCONST_1;
    private static final FloatConstant  CONSTANT_FCONST_1 = Constants.CONSTANT_FCONST_1;
    private static final DoubleConstant CONSTANT_DCONST_1 = Constants.CONSTANT_DCONST_1;
    }
