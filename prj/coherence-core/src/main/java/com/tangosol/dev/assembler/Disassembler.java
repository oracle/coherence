/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.assembler;


import com.tangosol.util.Base;

import com.tangosol.util.Resources;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;


/**
* Command-line disassembler utility.
*
* @author  1997.07.30  cp   Original programmer (disassembler package)
* @author  1998.01.06  cp   Updating
* @author  2004.05.05  cp   Updating
*/
public class Disassembler
        extends Base
    {
    public static void main(String asArgs[]) throws Throwable
        {
        try
            {
            if (asArgs.length != 1 || asArgs[0] == null)
                {
                showInstructions();
                return;
                }

            String sName       = asArgs[0].trim();
            if (asArgs.length == 0)
                {
                showInstructions();
                return;
                }

            String sFile = sName.replace('.', '/').concat(".class");
            out();
            out("Loading resource: " + sFile);
            InputStream in = null;
            try
                {
                in = Resources.findFileOrResource(sFile, getContextClassLoader()).openStream();
                }
            catch (Exception e)
                {
                out();
                err(e);
                }

            if (in == null)
                {
                out();
                out("Error: Could not load resource.");
                showInstructions();
                return;
                }

            out();
            out("Disassembling:");
            out();

            ClassFile cf = new ClassFile(new DataInputStream(in));
            cf.dump(getOut());

            try
                {
                if (in.read() >= 0)
                    {
                    out("WARNING!!!  Stream not exhausted!!!");
                    }
                }
            catch (EOFException e)
                {
                }
            }
        catch (Throwable t)
            {
            out("Caught \"" + t + "\", stack trace:");
            out(t);
            out("(end stack trace)");
            }
        }

    /**
    * Print command line instructions.
    */
    public static void showInstructions()
        {
        out();
        out("Usage instructions:");
        out();
        out("    java " + Disassembler.class.getName() + " <classname>");
        out();
        out("(Where <classname> is the fully qualified Java class name.)");
        out();
        }
    }
