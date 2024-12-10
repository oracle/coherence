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
* Base class for and (&&) and or (||).
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public abstract class LogicalExpression extends BinaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a LogicalExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    protected LogicalExpression(Expression left, Token operator, Expression right)
        {
        super(left, operator, right);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "LogicalExpression";
    }
