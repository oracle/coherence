/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.disassembler;

import com.tangosol.dev.compiler.java.*;
import com.tangosol.util.*;
import java.io.*;

public class DoubleConstant extends Constant
    {
    private double m_dflVal;

    public DoubleConstant(DataInput stream)
            throws IOException
        {
        m_dflVal = stream.readDouble();
        }
    
    public double getDouble()
        {
        return m_dflVal;
        }

    public String toString()
        {
        return "Double:  " + m_dflVal;
        }
    }
