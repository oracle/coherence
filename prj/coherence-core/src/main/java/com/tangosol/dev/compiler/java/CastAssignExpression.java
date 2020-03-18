/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.assembler.CodeAttribute;

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Implements the auto-casting version of the Assign (=) expression.  This
* is used to implement the "op=" expressions, such as "+=", "*=", "-=", etc.
*
* The expression "E1 += E2" is parsed as "E1 ~= E1 + E2" which is then
* re-arranged during pre-compile as "E1 = (T-E1) (E1 + E2)" where:
*
*   E1   - assignable and assigned expression
*   E2   - expression that has a value
*   T-E1 - the compile time type of expression E1
*   +=   - any Java "op=" operator
*   +    - the "op" portion of the "op=" operator, for example AddExpression
*   ~=   - a CastAssignExpression
*   =    - an AssignExpression
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public class CastAssignExpression extends AssignExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a CastAssignExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    public CastAssignExpression(Expression left, Token operator, Expression right)
        {
        super(left, operator, right);
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
        // get the sub-expressions
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        // pre-compile the left sub-expression because we need the type
        // (this also checks that it is assigned)
        left = (Expression) left.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // verify that a value can be stored in the left sub-expression
        left.checkAssignable(errlist);

        // cast the right expression to the type of the left sub-expression
        TypeExpression type = new TypeExpression(getBlock(), getOperator(), left.getType());
        right = new CastExpression(getOperator(), right, type);

        // store the sub-expressions
        setLeftExpression (left );
        setRightExpression(right);

        // use special pre-compilation on AssignExpression to avoid pre-
        // compiling the left side twice
        return super.precompile(ctx, setUVars, setFVars, mapThrown, errlist, true);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "CastAssignExpression";
    }
