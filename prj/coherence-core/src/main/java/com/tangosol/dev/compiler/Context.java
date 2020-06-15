/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import com.tangosol.dev.assembler.CodeAttribute;
import com.tangosol.dev.assembler.Field;
import com.tangosol.dev.assembler.FieldConstant;
import com.tangosol.dev.assembler.Method;
import com.tangosol.dev.assembler.MethodConstant;

import com.tangosol.dev.component.DataType;

import java.util.Enumeration;


/**
* This interface represents the context within which a compiler is
* operating.  A context is used to resolve names which cannot be
* determined by the compiler itself.
*
* @version 1.00, 09/14/98
* @author  Cameron Purdy
*/
public interface Context
    {
    // ----- environment ----------------------------------------------------

    /**
    * Generally speaking, compile types are "debug" and "non-debug".  More
    * specific optimizations can be toggled in a compiler-specific manner
    * by providing compiler-specific options.
    *
    * @return  true if the compile type is "debug"
    */
    boolean isDebug();

    /**
    * The compiler can be used to check the script for syntax and semantic
    * errors without producing code.
    *
    * @return  true if the compiler is only being used to check the script
    */
    boolean isCheck();

    /**
    * Get a compiler-specific option.
    *
    * @param sOption   the name of a compiler-specific option in the form
    *                  <language>.<option>, for example "javascript.casesens"
    * @param sDefault  the default value for the option; may be null
    *
    * @return  the String value for the option or the default if the option
    *          is not specified
    */
    String getOption(String sOption, String sDefault);


    // ----- target method --------------------------------------------------

    /**
    * Get the method information for the method that the script is being
    * compiled into.
    *
    * @return the method information for the script being compiled
    */
    MethodInfo getMethodInfo();

    /**
    * Determine the super for the method.  This is the method to call when
    * the script invokes its super implementation.
    *
    * The super method is the method to call when "super.foo()" is called
    * (assuming this method is the context and the context is foo()).  As
    * always, the super may be a "forced inline" which is actually a field,
    * and that field may in turn be a "forced inline" which may be a value.
    *
    * @return the method information for the super of the method
    */
    MethodInfo getSuperInfo();

    /**
    * Get the code attribute to compile the script into.
    *
    * @return the target code attribute for the script; (null if isCheck)
    */
    CodeAttribute getCode();


    // ----- synthetic member allocation ------------------------------------

    /**
    * Create a synthetic field required by the script.
    *
    * @return a new synthetic field
    */
    Field addSyntheticField(DataType dt);

    /**
    * Create a synthetic method required by the script.
    *
    * @param adt  an array of data types, [0] for the return type and
    *             [1..n] for the parameters
    *
    * @return a new synthetic field
    */
    Method addSyntheticMethod(DataType[] adt);

    /**
    * Get a synthetic helper method which will supply an instance of
    * java.lang.Class for the specified class.  The context returns a
    * method constant to a static Method (signature "()Ljava/lang/Class;")
    * which provides the requested class.
    *
    * This supports the functionality of the CLASS$ construct in the
    * classes compiled by JAVAC.
    *
    * @param dt  the datatype for the class instance to obtain
    *
    * @return the static method constant to use to obtain the specified class
    */
    MethodConstant getClassForName(DataType dt);


    // ----- import resolution ----------------------------------------------

    /**
    * Register an import name.
    *
    * @param sImport  the import name
    * @param dt       the imported class or component
    *
    * @return true if the import name was already used (and therefore if the
    *         imported type over-wrote the preious value)
    */
    boolean addImport(String sImport, DataType dt);

    /**
    * Get imported class/component by name.
    *
    * @param sImport  the import name
    *
    * @return the imported DataType or null
    */
    DataType getImport(String sImport);

    /**
    * Enumerate all imports.
    *
    * @return an enumeration of import names
    */
    Enumeration importNames();


    // ----- packages and classes -------------------------------------------

    /**
    * Get the specified package.
    *
    * @param sPkg  the dot-delimited package name (or null for the top-level
    *              package)
    *
    * @return the specified package or null
    */
    PackageInfo getPackageInfo(String sPkg);

    /**
    * Get the specified type.
    *
    * @param sType  the qualified (dot-delimited) type name
    *
    * @return the specified type info or null
    */
    TypeInfo getTypeInfo(String sType);

    /**
    * Get the specified type.
    *
    * @param dt  the data type
    *
    * @return the specified type info or null
    */
    TypeInfo getTypeInfo(DataType dt);


    // ----- context-related caching ----------------------------------------

    /**
    * Store a named object with the context for later retrieval.
    *
    * @param sName  the name to give the object
    * @param oItem  the object to store
    */
    void put(String sName, Object oItem);

    /**
    * Retrieve a previously stored object from the context.
    *
    * @param sName  the name that the object was stored under.
    *
    * @return  the object that was stored (or null if no object was stored
    *          using that name)
    */
    Object get(String sName);
    }
