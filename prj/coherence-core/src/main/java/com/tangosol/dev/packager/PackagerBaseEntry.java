/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.packager;


import com.tangosol.util.Base;

import  java.io.File;
import  java.io.Serializable;

import  java.util.ArrayList;
import  java.util.Date;
import  java.util.Properties;
import  java.util.StringTokenizer;


public abstract class PackagerBaseEntry
        extends    Base
        implements Serializable, PackagerEntry
    {
    /**
    * Construct a PackagerBaseEntry.  This constructor is used only by
    * subclass constructors.
    */
    protected PackagerBaseEntry()
        {
        }

    /**
    * Returns a PackagerPath for the PackagerEntry.
    */
    public abstract PackagerPath getPath();

    /**
    * Returns the PackagerEntry as binary data suitable for packager.
    */
    public abstract byte[] getData(ClassLoader classLoader)
            throws PackagerEntryNotFoundException;

    /**
    * Return the time when the entry was last modified.
    */
    public long getModificationTime()
        {
        return(new Date().getTime());
        }

    /**
    * Return a comment for the PackagerEntry.
    */
    public String getComment()
        {
        return null;
        }

    /**
    * Returns true if this entry has to be "secured" in a package
    */
    public boolean isSecured()
        {
        return false;
        }

    /**
    * Return the complete map of attribute names to attribute values for
    * examination or modification.
    */
    public Properties getAttributes()
        {
        return(attributes);
        }

    /**
    * Get the value of the specified attribute.
    */
    public String getAttributeValue(String attributeName)
        {
        return(attributes == null ? null : attributes.getProperty(attributeName));
        }

    /**
    * Get the value of the specified attribute, returning the provided
    * default value if not present.
    */
    public String getAttributeValue(String attributeName, String defaultValue)
        {
        return(attributes == null ? null : attributes.getProperty(attributeName, defaultValue));
        }

    /**
    * Set the value of the specified attribute.
    */
    public void setAttributeValue(String attributeName, String value)
        {
        if (attributes == null)
            {
            attributes = new Properties();
            }
        attributes.setProperty(attributeName, value);
        }


    /**
    * Return a File representing the PackagerEntry by searching the
    * CLASSPATH for a directory that contains the entry by pathname.
    *
    * Note: we use this ONLY to get the modification time (see PackageClassEntry)
    */
    protected File findEntryFile()
        {
        String[] classpathEntries = getClasspathEntries();
        for (int i = 0, n = classpathEntries.length; i < n; i++)
            {
            File classpathEntryFile = new File(classpathEntries[i]);
            if (classpathEntryFile.exists() && classpathEntryFile.isDirectory())
                {
                File entryFile = new File(classpathEntryFile, getPath().getPathName());
                if (entryFile.exists())
                    {
                    return(entryFile);
                    }
                }
            }
        return(null);
        }

    /**
    * Return the directory and archive components of the classpath.
    */
    private static String[] getClasspathEntries()
        {
        if (classpathEntries == null)
            {
            String classpath = System.getProperty("java.class.path");
            StringTokenizer toks = new StringTokenizer(classpath, File.pathSeparator);

            ArrayList elems = new ArrayList();
            while (toks.hasMoreTokens())
                {
                elems.add(toks.nextToken());
                }

            classpathEntries = new String[elems.size()];
            elems.toArray(classpathEntries);
            }

        return(classpathEntries);
        }


    // ----- data members ---------------------------------------------------

    private               Properties      attributes;
    private static        String[]        classpathEntries;
    }
