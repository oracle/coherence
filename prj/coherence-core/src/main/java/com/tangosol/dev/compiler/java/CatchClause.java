/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.ClassConstant;
import com.tangosol.dev.assembler.Begin;
import com.tangosol.dev.assembler.End;
import com.tangosol.dev.assembler.Avar;
import com.tangosol.dev.assembler.Astore;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;
import com.tangosol.util.DeltaSet;

import java.util.Set;
import java.util.Map;


/**
* This class implements a catch clause in the Java try statement.
*
*   CatchClause:
*       catch ( FormalParameter ) Block
*
* @version 1.00, 09/21/98
* @author  Cameron Purdy
*/
public class CatchClause extends Block
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a catch clause for a try statement.
    *
    * @param outer  the enclosing Java statement
    * @param token  the catch token
    */
    public CatchClause(Statement outer, Token token)
        {
        super(outer, token);
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
        DeclarationStatement stmtDecl    = getDeclaration();
        Statement            stmtHandler = getBody();

        // the catch clause has its own variable scope
        DualSet setUBlockVars = new DualSet(setUVars);
        DualSet setFBlockVars = new DualSet(setFVars);

        // pre-compile the exception declaration
        stmtDecl.precompile(ctx, setUBlockVars, setFBlockVars, mapThrown, errlist);

        // verify that the declaration is a single declaration without assignment
        if (!stmtDecl.isSingleDeclaration() || stmtDecl.isAssignmentDeclaration())
            {
            logError(ERROR, CATCH_INVALID, null, errlist);
            }

        // if the exception is final, mark it as assigned
        if (stmtDecl.isFinal())
            {
            setFBlockVars.add(stmtDecl.getVariable());
            }

        // pre-compile the exception handler
        stmtHandler.precompile(ctx, setUBlockVars, setFBlockVars, mapThrown, errlist);

        // the catch statement may have definitely assigned some variables
        setUVars.removeAll(setUBlockVars.getRemoved());

        // no new final variables have been declared for the outer block,
        // but it is possible that final variables that were previously
        // declared for the outer block have since been potentially assigned
        Set setAssigned = setFBlockVars.getAdded();
        if (!setAssigned.isEmpty())
            {
            // some of the assigned final variables may not be from the
            // outer variable scope
            setAssigned.retainAll(getBlock().getVariables());
            if (!setAssigned.isEmpty())
                {
                setFVars.addAll(setAssigned);
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
    protected boolean compileImpl(Context ctx, CodeAttribute code, boolean fReached, ErrorList errlist)
            throws CompilerException
        {
        // compilation for "catch (<decl>) <block>" where <decl> declares
        // variable "e":
        //
        //  start:
        //          begin
        //          [decl]
        //          astore  e
        //          [block]
        //          end
        //  end:

        DeclarationStatement stmtDecl    = getDeclaration();
        Statement            stmtHandler = getBody();
        boolean              fCompletes  = fReached;

        code.add(new Begin());
        fCompletes &= stmtDecl.compile(ctx, code, fReached, errlist);
        code.add(new Astore((Avar) stmtDecl.getVariable().getOp()));
        fCompletes &= stmtHandler.compile(ctx, code, fReached, errlist);
        code.add(new End());

        return fCompletes;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the declaration statement for the caught exception.
    *
    * @return the declaration statement for the catch clause
    */
    public DeclarationStatement getDeclaration()
        {
        // the declaration is the first inner statement
        return (DeclarationStatement) getInnerStatement();
        }

    /**
    * Get the body of the catch clause.
    *
    * @return the body of the catch clause
    */
    public Statement getBody()
        {
        // the body is the second inner statement
        return getInnerStatement().getNextStatement();
        }

    /**
    * Get the data type of the exception which is caught by this clause.
    *
    * @return the exception type caught by this CatchClause
    */
    public DataType getExceptionType()
        {
        return getDeclaration().getTypeExpression().getType();
        }

    /**
    * Get the class constant for the exception caught by this clause.
    *
    * @return the JASM class constant for the exception type
    */
    public ClassConstant getExceptionClass()
        {
        return new ClassConstant(getExceptionType().getClassName());
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "CatchClause";
    }
