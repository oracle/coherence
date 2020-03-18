/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


/**
* Represents a Java Virtual Machine "RuntimeVisibleParameterAnnotations" attribute
* 
*
* <p>
* The RuntimeVisibleParameterAnnotations Attribute is defined by the JDK 1.5
* documentation as:
* <p>
* <code><pre>
*   RuntimeVisibleParameterAnnotations_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length;
*       u1 num_parameters;
*           {
*           u2 num_annotations;
*           annotation annotations[num_annotations]
*           } parameter_annotations[num_parameters]
*       }
* </pre></code>
*
* @author  rhl 2008.09.23
*/
public class RuntimeVisibleParameterAnnotationsAttribute
        extends AbstractParameterAnnotationsAttribute
        implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a runtime visible parameter annotations attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected RuntimeVisibleParameterAnnotationsAttribute(VMStructure context)
        {
        super(context, ATTR_RTVISPARAMANNOT);
        }
    }
