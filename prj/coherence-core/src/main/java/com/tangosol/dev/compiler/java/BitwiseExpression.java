/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.compiler.java;


import com.tangosol.dev.compiler.CompilerException;
import com.tangosol.dev.compiler.Context;

import com.tangosol.dev.component.DataType;

import com.tangosol.util.ErrorList;

import java.util.Set;
import java.util.Map;


/**
* Base class for bit-and (&), bit-or (|), and bit-xor (^).
*
* @version 1.00, 10/05/98
* @author  Cameron Purdy
*/
public abstract class BitwiseExpression extends BinaryExpression
    {
    // ----- construction ---------------------------------------------------

    /**
    * Construct a BitwiseExpression.
    *
    * @param left      the left expression
    * @param operator  the operator token
    * @param right     the right expression
    */
    protected BitwiseExpression(Expression left, Token operator, Expression right)
        {
        super(left, operator, right);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The class name.
    */
    private static final String CLASS = "BitwiseExpression";

    /**
    * Boolean data type.
    */
    protected static final DataType BOOLEAN = DataType.BOOLEAN;

    /**
    * Integer data type.
    */
    protected static final DataType INT = DataType.INT;

    /**
    * Long data type.
    */
    protected static final DataType LONG = DataType.LONG;
    }
