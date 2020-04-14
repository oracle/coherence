/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import com.tangosol.util.ErrorList;


/**
* This interface represents a script compiler.
*
* The default (no-parameter) constructor is required to be available (public)
* for a compiler, since compilers are located and loaded dynamically by
* inferring the compiler class name from the language name, and then
* instantiating the compiler dynamically.  As a result, the constructor must
* be predictable, and the no-parameter constructor is the easiest to use.
*
* A side-effect of the default constructor is that the compiler has no state
* after it is created.  Therefore the compile method acts as both an
* initializer and a command to compile.  If the compiler has global state
* (uses fields to store its state) then the compile method must be
* synchronized in case the compilation process is multi-threaded.
*
* The compiler may be re-used (multiple calls to compile) or may be used only
* to compile a single script.
*
* @version 1.00, 09/04/98
* @author  Cameron Purdy
*/
public interface Compiler
    {
    /**
    * Compile the passed script.
    *
    * @param ctx      the script compilation context
    * @param sScript  the script to compile (as a string)
    * @param errlist  the error list to log to
    *
    * @exception CompilerException  thrown if the compilation of this script
    *            fails
    */
    void compile(Context ctx, String sScript, ErrorList errlist)
        throws CompilerException;
    }
