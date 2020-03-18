/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


import com.tangosol.dev.compiler.SimpleScript;

import java.io.EOFException;


/**
* Implements a simple unicode script object for parsing Xml.
*
* @see com.tangosol.dev.compiler.java.UnicodeScript
*
* @version 1.00, 07/16/01
* @author Cameron Purdy
*/
public class XmlScript
        extends SimpleScript
    {
    // ----- constructors  --------------------------------------------------

    /**
    * (Default) Constructs a XmlScript.
    */
    public XmlScript()
        {
        super();
        }

    /**
    * Constructs a XmlScript out of the passed string.
    *
    * @param sScript  the script
    */
    public XmlScript(String sScript)
        {
        super(sScript);
        }
    }
