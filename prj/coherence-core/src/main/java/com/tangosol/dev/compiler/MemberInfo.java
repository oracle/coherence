/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


/**
* The MemberInfo interface represents the information about a method
* or field of a class.
*
* @version 1.00, 01/21/98
* @author  Cameron Purdy
*/
public interface MemberInfo
        extends Info
    {
    /**
    * Determine the type that contains this member.
    *
    * @return the TypeInfo for the containing type
    */
    TypeInfo getTypeInfo();

    /**
    * Determine if the method can be inlined by referencing a field directly
    * or if the field can be inlined by replacing it with a constant.
    *
    * @return true if the member can be inlined
    */
    boolean isInlineable();

    /**
    * Determine if the member must be inlined because it is being optimized
    * out by the compiler context.
    *
    * @return true if the member is being optimized out and must NOT be
    *         referenced
    */
    boolean isInlined();
    }
