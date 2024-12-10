/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;


/**
* Represents a Java Virtual Machine "synthetic" attribute which specifies
* that a member (i.e. field or method) is "tool generated" (i.e. will not
* be found by that exact name in the source).  This attribute can also
* apply to a class.
* <p>
* The Synthetic Attribute is defined by the JDK 1.1 documentation as:
* <p>
* <code><pre>
*   Synthetic_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length; (=0)
*       }
* </pre></code>
*
* @version 0.50, 05/18/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class SyntheticAttribute extends Attribute implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a synthetic attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected SyntheticAttribute(VMStructure context)
        {
        super(context, ATTR_SYNTHETIC);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "SyntheticAttribute";
    }
