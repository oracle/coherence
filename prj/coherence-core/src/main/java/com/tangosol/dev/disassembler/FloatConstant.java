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

public class FloatConstant extends Constant
    {
    private float m_flVal;

    public FloatConstant(DataInput stream)
            throws IOException
        {
        m_flVal = stream.readFloat();
        }
    
    public float getFloat()
        {
        return m_flVal;
        }

    public String toString()
        {
        return "Float:  " + m_flVal;
        }
    }
