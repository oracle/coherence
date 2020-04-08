/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import com.tangosol.dev.assembler.Constant;

import com.tangosol.dev.component.DataType;


/**
* The Info interface represents information about Java types and type
* items.
*
* @version 1.00, 01/13/99
* @author  Cameron Purdy
*/
public interface Info
    {
    /**
    * Get the name of the item.
    *
    * @return the item name
    */
    String getName();

    /**
    * Determine if this item is accessible from the current context.
    *
    * @return true if the item is accessible from this context
    */
    boolean isAccessible();

    /**
    * Determine if the item is public.
    *
    * @return true if the item is public
    */
    boolean isPublic();

    /**
    * Determine if the item is protected.
    *
    * @return true if the item is protected
    */
    boolean isProtected();

    /**
    * Determine if the item is package private.
    *
    * @return true if the item is package private
    */
    boolean isPackage();

    /**
    * Determine if the item is private.
    *
    * @return true if the item is private
    */
    boolean isPrivate();

    /**
    * Determine if the item is abstract.
    *
    * @return true if the item is abstract
    */
    boolean isAbstract();

    /**
    * Determine if the item is static.
    *
    * @return true if the item is static
    */
    boolean isStatic();

    /**
    * Determine if the item is final.
    *
    * @return true if the item is final
    */
    boolean isFinal();

    /**
    * Get the data type of the item.
    *
    * @return the item type or null if not applicable
    */
    DataType getDataType();

    /**
    * Get the constant for the item.
    *
    * @return the constant or null if not applicable
    */
    Constant getConstant();

    /**
    * Add a dependency to the item.  There are two types of dependencies:
    * <ol>
    * <li>Compile - if the item changes, recompile this script
    * <li>Runtime - at runtime, the item is used by this script
    * </ol>
    * By default, a dependeny implies a compile dependency.  Additionally,
    * the caller can specify a runtime dependency.
    *
    * @param fRuntime  true if there is also a dependency at runtime
    * @param iStart    starting line in which the dependency was detected
    * @param ofStart   starting offset of the dependency within the line
    * @param iEnd      ending line in which the dependency was detected
    * @param ofEnd     ending offset of the dependency within the line
    */
    void addDependency(boolean fRuntime, int iStart, int ofStart, int iEnd, int ofEnd)
        throws CompilerException;
    }
