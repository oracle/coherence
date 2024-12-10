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

import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Base class for less than (<), not less than (>=), greater than (>), not
* greater than (<=), and the castability (instanceof) relational operators.
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public abstract class RelationalExpression extends BinaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a RelationalExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    protected RelationalExpression(Expression left, Token operator, Expression right)
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
        // note:  this method must be overridden for instanceof

        // get the sub-expressions
        Expression left  = getLeftExpression();
        Expression right = getRightExpression();

        // pre-compile the sub-expressions
        left  = (Expression) left .precompile(ctx, setUVars, setFVars, mapThrown, errlist);
        right = (Expression) right.precompile(ctx, setUVars, setFVars, mapThrown, errlist);

        // both sub-expressions must be numeric
        if (left.checkNumeric(errlist) & right.checkNumeric(errlist))
            {
            // binary numeric promotion
            left  = left .promoteNumeric(right);
            right = right.promoteNumeric(left );
            }

        // store the sub-expressions
        setLeftExpression (left );
        setRightExpression(right);

        setType(BOOLEAN);

        return this;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "RelationalExpression";

    /**
    * Boolean data type.
    */
    protected static final DataType BOOLEAN = DataType.BOOLEAN;

    /**
    * The assembly constant int value of boolean true.
    */
    protected static final IntConstant TRUE  = Constants.CONSTANT_ICONST_1;

    /**
    * The assembly constant int value of boolean false.
    */
    protected static final IntConstant FALSE = Constants.CONSTANT_ICONST_0;
    }
