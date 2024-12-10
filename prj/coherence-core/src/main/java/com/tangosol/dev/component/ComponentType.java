/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.dev.assembler.ClassFile;

import com.tangosol.java.type.ClassType;


/**
* The Component Definition type implementation.
*
* @author cp  2001.05.01
*/
public class ComponentType
        extends ClassType
        implements Constants
    {
    // ----- constructor ----------------------------------------------------

    /**
    * Construct a class type object.
    *
    * @param sComponent  the Component name '.' delimited
    */
    public ComponentType(String sComponent)
        {
        super(getComponentClassName(sComponent));
        m_sName = sComponent;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the component name.
    *
    * @return the ccomponent name (e.g. "Component.Global$Child")
    */
    public String getComponentName()
        {
        return m_sName;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Format the type into its source code form.
    *
    * @return a source code representation of the type
    */
    public String toString()
        {
        return getComponentName();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Return the relocatable class name for the specified component.
    *
    * @param cd  Component Definition
    *
    * @return the relocatable class name for the component ('.'-delimited)
    */
    public static String getComponentClassName(Component cd)
        {
        return getComponentClassName(cd.getQualifiedName());
        }

    /**
    * Return the relocatable class name for the specified component name.
    *
    * @param sName  qualified Component Definition name
    *
    * @return the relocatable class name for the component ('.'-delimited)
    */
    public static String getComponentClassName(String sName)
        {
        return getComponentPackage(sName) + '.' +
               sName.substring(sName.lastIndexOf(GLOBAL_ID_DELIM) + 1);
        }

    /**
    * Return the package name for the specified component.
    *
    * @param cd  Component Definition
    *
    * @return the package name for the component prefixed with
    *         the "relocatable" package name ("."-delimited)
    */
    public static String getComponentPackage(Component cd)
        {
        return getComponentPackage(cd.getQualifiedName());
        }

    /**
    * Return the package name for the specified component name.
    *
    * According to section 7.1 of the Java Language Specification,
    * Second Edition, it is illegal for a package to contain a class
    * or interface type and a subpackage with the same name.
    * To inforce this rule, all global Component Definitions have names
    * that start with an upper case letter and the corresponding packages
    * just have this first letter lower-cased.
    *
    * @param sName  qualified Component Definition name
    *
    * @return the package name for the component ("."-delimited)
    *
    * @see com.tangosol.dev.component.Component#isSimpleNameLegal
    */
    public static String getComponentPackage(String sName)
        {
        azzert(Component.isQualifiedNameLegal(sName), "Illegal component name: " + sName);

        String       sRelocator = ClassFile.Relocator.PACKAGE.replace('/', GLOBAL_ID_DELIM);
        StringBuffer sbPkg      = new StringBuffer(sRelocator.length() + sName.length());
        sbPkg.append(sRelocator);
        
        int ofStart = 0;
        while (true)
            {
            int ofEnd = sName.indexOf(GLOBAL_ID_DELIM, ofStart);
            if (ofEnd > 0)
                {
                sbPkg.append(Character.toLowerCase(sName.charAt(ofStart)))
                     .append(sName.substring(ofStart + 1, ofEnd + 1));
                ofStart = ofEnd + 1;
                }
            else
                {
                int iLen = sbPkg.length();
                return iLen > 0 ? sbPkg.substring(0,  iLen - 1) : "";
                }
            }
        }

    /**
    * Parses a component class name and calculates the component name,
    * where the class name may or may not start with the relocation prefix.
    *
    * @param sClassName the component class name (could be '/' or '.' delimited)
    *
    * @return the component name
    *
    * Note: for synthetic component classes the calculated name will not be
    * a valid component name.
    *
    * @see #getComponentPackage(Component)
    */
    public static String getComponentName(String sClassName)
        {
        String sRelocator = ClassFile.Relocator.PACKAGE.replace('/', GLOBAL_ID_DELIM);

        sClassName = sClassName.replace('/', GLOBAL_ID_DELIM);
        if (sClassName.startsWith(sRelocator))
            {
            sClassName = sClassName.substring(sRelocator.length());
            }

        StringBuffer sbName  = new StringBuffer(sClassName.length());
        int          ofStart = 0;

        while (true)
            {
            int ofEnd = sClassName.indexOf(GLOBAL_ID_DELIM, ofStart);
            if (ofEnd > 0)
                {
                sbName.append(Character.toUpperCase(sClassName.charAt(ofStart)))
                      .append(sClassName.substring(ofStart + 1, ofEnd + 1));
                ofStart = ofEnd + 1;
                }
            else
                {
                sbName.append(sClassName.substring(ofStart));
                return sbName.toString();
                }
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The Component name.
    */
    private String m_sName;
    }