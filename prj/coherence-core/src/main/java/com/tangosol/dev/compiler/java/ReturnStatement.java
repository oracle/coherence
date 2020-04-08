/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Op;
import com.tangosol.dev.assembler.Begin;
import com.tangosol.dev.assembler.End;
import com.tangosol.dev.assembler.Jsr;
import com.tangosol.dev.assembler.Label;
import com.tangosol.dev.assembler.Return;
import com.tangosol.dev.assembler.Avar;
import com.tangosol.dev.assembler.Ivar;
import com.tangosol.dev.assembler.Lvar;
import com.tangosol.dev.assembler.Fvar;
import com.tangosol.dev.assembler.Dvar;
import com.tangosol.dev.assembler.Astore;
import com.tangosol.dev.assembler.Istore;
import com.tangosol.dev.assembler.Lstore;
import com.tangosol.dev.assembler.Fstore;
import com.tangosol.dev.assembler.Dstore;
import com.tangosol.dev.assembler.Aload;
import com.tangosol.dev.assembler.Iload;
import com.tangosol.dev.assembler.Lload;
import com.tangosol.dev.assembler.Fload;
import com.tangosol.dev.assembler.Dload;
import com.tangosol.dev.assembler.Areturn;
import com.tangosol.dev.assembler.Ireturn;
import com.tangosol.dev.assembler.Lreturn;
import com.tangosol.dev.assembler.Freturn;
import com.tangosol.dev.assembler.Dreturn;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;
import com.tangosol.util.DeltaSet;

import java.util.Set;
import java.util.Map;


/**
* This class implements the return statement.
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class ReturnStatement extends ExitStatement
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a return statement.
    *
    * @param stmt   the statement within which this element exists
    * @param token  the first token of the statement
    */
    public ReturnStatement(Statement stmt, Token token)
        {
        super(stmt, token);
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
    * @exception CompilerException  thrown if an error occurs that should
    *            stop the compilation process
    */
    protected Element precompile(Context ctx, DualSet setUVars, DualSet setFVars, Map mapThrown, ErrorList errlist)
            throws CompilerException
        {
        Expression value    = getExpression();
        DataType   dtReturn = ctx.getMethodInfo().getDataType();

        if (value == null)
            {
            if (dtReturn != DataType.VOID)
                {
                // method is not void but return is missing expression
                logError(ERROR, RETURN_NOTVOID, null, errlist);
                }
            }
        else
            {
            value = (Expression) value.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

            if (dtReturn == DataType.VOID)
                {
                // method is void but return has expression
                logError(ERROR, RETURN_ISVOID, null, errlist);
                }
            else if (value.checkAssignable(ctx, dtReturn, null))
                {
                value = value.convertAssignable(ctx, dtReturn);
                }
            else
                {
                // return value is not assignment compatible with the type
                // of the method
                value.logError(ERROR, RETURN_TYPE, new String[]
                        {value.getType().toString(), dtReturn.toString()}, errlist);
                }

            setExpression(value);
            }

        // JLS 16.2.11:  By convention, we say that V is definitely assigned
        // after any break, continue, return, or throw statement.
        setUVars.clear();
        setFVars.clear();

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
    protected boolean compileImpl(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException
        {
        Expression value = getExpression();

        // for "return;" not contained within guarded statements:
        //          return
        //
        // for "return;" contained within guarded statements:
        //          jsr      lblUnwind[0]       ; innermost finally
        //          jsr      lblUnwind[1]       ; next innermost
        //          ...                         ; and so on
        //          return
        //
        // for "return <expr>;" not contained within guarded statements:
        //          [value]
        //          ?return
        //
        // for "return <expr>;" contained within guarded statements:
        //          [value]
        //          begin
        //          ?var     #temp
        //          ?store   #temp
        //          jsr      lblUnwind[0]       ; innermost finally
        //          jsr      lblUnwind[1]       ; next innermost
        //          ...                         ; and so on
        //          ?load    #temp
        //          end
        //          ?return

        // determine the data type of the return
        char chType = 'V';    // void
        if (value != null)
            {
            chType = value.getType().getTypeString().charAt(0);
            }

        // determine what ops to use (based on type)
        Op opDecl   = null;
        Op opStore  = null;
        Op opLoad   = null;
        Op opReturn = null;
        switch (chType)
            {
            case 'V':
                opReturn = new Return();
                break;

            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
                opDecl   = new Ivar   ();
                opStore  = new Istore ((Ivar) opDecl);
                opLoad   = new Iload  ((Ivar) opDecl);
                opReturn = new Ireturn();
                break;

            case 'J':
                opDecl   = new Lvar   ();
                opStore  = new Lstore ((Lvar) opDecl);
                opLoad   = new Lload  ((Lvar) opDecl);
                opReturn = new Lreturn();
                break;

            case 'F':
                opDecl   = new Fvar   ();
                opStore  = new Fstore ((Fvar) opDecl);
                opLoad   = new Fload  ((Fvar) opDecl);
                opReturn = new Freturn();
                break;

            case 'D':
                opDecl   = new Dvar   ();
                opStore  = new Dstore ((Dvar) opDecl);
                opLoad   = new Dload  ((Dvar) opDecl);
                opReturn = new Dreturn();
                break;

            case 'N':
            case 'L':
            case 'R':
            case '[':
                opDecl   = new Avar   ();
                opStore  = new Astore ((Avar) opDecl);
                opLoad   = new Aload  ((Avar) opDecl);
                opReturn = new Areturn();
                break;

            default:
                throw new IllegalStateException();
            }

        // the return value
        if (value != null)
            {
            value.compile(ctx, code, fReached, errlist);
            }

        // check for unwind clauses that must be executed
        Statement stmt    = getOuterStatement();
        Statement prev    = null;
        boolean   fUnwind = false;
        while (stmt != null)
            {
            // check if the statement is a guarded statement
            // (it is possible that the return is in the finally, in which
            // case the guarded statement is already unwound)
            if (stmt instanceof GuardedStatement && !(prev instanceof FinallyClause))
                {
                Label lblUnwind = ((GuardedStatement) stmt).getUnwindLabel();
                if (lblUnwind != null)
                    {
                    if (!fUnwind && opStore != null)
                        {
                        code.add(new Begin());
                        code.add(opDecl);
                        code.add(opStore);
                        }

                    code.add(new Jsr(lblUnwind));

                    fUnwind = true;
                    }
                }

            prev = stmt;
            stmt = stmt.getOuterStatement();
            }

        if (fUnwind && opLoad != null)
            {
            code.add(opLoad);
            code.add(new End());
            }

        // the return itself
        code.add(opReturn);

        // JLS 14.19: A break, continue, return, or throw statement cannot
        // complete normally.
        return false;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ReturnStatement";
    }
