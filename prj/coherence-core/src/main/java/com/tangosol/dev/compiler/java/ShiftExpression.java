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
* Base class for left (<<), right (>>) and unsigned right (>>>) shifts.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public abstract class ShiftExpression extends BinaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a ShiftExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    protected ShiftExpression(Expression left, Token operator, Expression right)
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

        // pre-compile the sub-expressions
        left  = (Expression) left .precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        right = (Expression) right.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // both sub-expressions must be integral
        if (left.checkIntegral(errlist) & right.checkIntegral(errlist))
            {
            // unary numeric promotion
            left  = left .promoteNumeric();
            right = right.promoteNumeric();
            }

        // store the sub-expressions
        setLeftExpression (left );
        setRightExpression(right);

        // result type is the type of left sub-expression
        setType(left.getType());

        return this;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "ShiftExpression";

    /**
    * The Long data type.
    */
    protected static final DataType LONG = DataType.LONG;
    }
