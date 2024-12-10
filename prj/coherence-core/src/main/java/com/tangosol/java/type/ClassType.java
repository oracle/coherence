/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.java.type;


/**
* The class/interface type implementation.
*
* @author cp  2000.10.13
*/
public class ClassType
        extends ReferenceType
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Construct a class type object.
    *
    * @param sSig  the class name (either '.' or '/' delimited) with inner
    *              names '$' delimited
    */
    public ClassType(String sSig)
        {
        super('L' + sSig.replace('.', '/') + ';');
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the class name.
    *
    * @return the class name (e.g. "package.name$inner")
    */
    public String getClassName()
        {
        String sSig = getSignature();
        return sSig.substring(1, sSig.length() - 1).replace('/', '.');
        }

    /**
    * Determine the package containing the class.
    *
    * @return the package name; a 0-length String denotes the unnamed
    *         (JLS 7.4.2) package
    */
    public String getPackageName()
        {
        String sSig  = getSignature();
        String sName = sSig.substring(1, sSig.length() - 1);
        int    ofClz = sName.lastIndexOf('/');
        return ofClz == -1 ? "" : sName.substring(0, ofClz);
        }

    /**
    * Determine the class's local name (the name within the package).
    *
    * @return the local name (e.g. "name$inner" for "package.name$inner")
    */
    public String getLocalName()
        {
        String sSig  = getSignature();
        String sName = sSig.substring(1, sSig.length() - 1);
        return sName.substring(sName.lastIndexOf('/') + 1);
        }

    /**
    * Determine the class's short name (the name in the Java class
    * declaration).
    *
    * @return the short name (e.g. "inner" for "package.name$inner" or
    *         "name" for "package.name")
    */
    public String getShortName()
        {
        String sSig  = getSignature();
        String sName = sSig.substring(1, sSig.length() - 1);
        return sName.substring(Math.max(sName.lastIndexOf('/'),
                                        sName.lastIndexOf('$')) + 1);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the type into its source code form.
    *
    * @return a source code representation of the type
    */
    public String toString()
        {
        // convert "Lpackage/class;" to "package.class"
        String sSig  = getSignature();
        String sName = sSig.substring(1, sSig.length() - 1);
        sName = sName.replace('/', '.');

        /* gg 2002.02.14 - commented out the conversion
        // conversions for inner class names
        //  "package.class$1" remains "package.class$1"
        //  "package.class$inner" becomes "package.class.inner"
        if (sName.indexOf('$') != -1)
            {
            StringBuffer sb = new StringBuffer(sName.length());
            String[]     as = parseDelimitedString(sName, '$');
            char         ch = '.';
            for (int i = 0, c = as.length; i < c; ++i)
                {
                String s = as[i];
                if (s.length() == 0 || !Character.isJavaIdentifierStart(s.charAt(0)))
                    {
                    ch = '$';
                    }
                if (i > 0)
                    {
                    sb.append(ch);
                    }
                sb.append(as[i]);
                }
            sName = sb.toString();
            }
        */
        return sName;
        }
    }