/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.dev.assembler.ClassFile;

import com.tangosol.util.ErrorList;
import com.tangosol.util.StringTable;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import java.util.Enumeration;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;


/**
* ArchivedStorage loads Components, Signatures and resources from a JAR file.
*
* @version 1.00, 03/16/2001
* @author  cp
* @author  gg
*/
public class ArchivedStorage
        extends JarStorage
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a storage object backed by the specified jar.
    *
    * @param sPath  the path of the jar to read components from
    */
    public ArchivedStorage(String sPath)
        {
        super(sPath, null);
        }

    /**
    * Create a storage object backed by the specified jar.
    *
    * @param file  the File object specifying the jar to read
    *              Components from
    */
    public ArchivedStorage(File file)
        {
        super(file, null);
        }


    // ----- Components -----------------------------------------------------

    /**
    * Load the specified Component.
    *
    * @param sName      fully qualified Component Definition name
    * @param fReadOnly  true if the loaded component will be read-only
    * @param errlist    the ErrorList object to log any derivation/
    *                   modification errors to
    *
    * @return the specified Component Definition or null
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public Component loadComponent(String sName, boolean fReadOnly, ErrorList errlist)
            throws ComponentException
        {
        if (sName.length() == 0)
            {
            sName = ROOT;
            }

        try
            {
            DataInputStream stream = loadFile(T_COMPONENT, sName);

            if (stream == null)
                {
                /*
                if (sName == ROOT)
                    {
                    return Component.getRootSuper(this);
                    }
                */
                return Component.isQualifiedNameLegal(sName) ? null : loadSignature(sName);
                }

            Component cd = new Component(stream);

            if (fReadOnly)
                {
                cd.setModifiable(false);
                }
            return cd;
            }
        catch (IOException e)
            {
            throw new ComponentException(e.toString());
            }
        }


    // ----- Java Class Signatures ------------------------------------------

    /**
    * Load the specified Class Signature.
    *
    * @param sName    qualified Java Class Signature (JCS) name
    *
    * @return the specified Class Signature
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public Component loadSignature(String sName)
            throws ComponentException
        {
        try
            {
            DataInputStream stream = loadFile(T_SIGNATURE, sName);

            return stream == null ? null : new Component(stream);
            }
        catch (IOException e)
            {
            throw new ComponentException(e.toString());
            }
        }


    // ----- Classes --------------------------------------------------------

    /**
    * Load the specified generated Java Class.
    *
    * @param sName  fully qualified Java Class name
    *
    * @return the specified Class structure
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public ClassFile loadClass(String sName)
            throws ComponentException
        {
        try
            {
            DataInputStream stream = loadFile(T_CLASS, sName);

            return stream == null ? null : new ClassFile(stream);
            }
        catch (IOException e)
            {
            throw new ComponentException(e.toString());
            }
        }


    // ----- Java -------------------------------------------------------------

    /**
    * Load the source code for the specified (original) Java class.
    *
    * @param sName  fully qualified Java Class name
    *
    * @return the specified Java source code as a String
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public String loadJava(String sName)
            throws IOException
        {
        // TODO
        return null;
        }


    // ----- Resources --------------------------------------------------------

    /**
    * Load the original (before any customization takes place) resource.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public byte[] loadOriginalResource(String sName)
            throws IOException
        {
        // ArchiveStorage is not expected to carry the original resource,
        // only the JarStorage is.
        return null;
        }

    /**
    * Load the Resource Signature.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified Resource Signature as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public byte[] loadResourceSignature(String sName)
            throws IOException
        {
        return loadFileBytes(buildFile(T_RES_SIG, sName));
        }

    /**
    * Load the generated resource.
    *
    * @param sName  fully qualified resource name
    *
    * @return the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public byte[] loadResource(String sName)
            throws IOException
        {
        return loadFileBytes(buildFile(T_RES_BINARY, sName));
        }


    // ---- component management ------------------------------------------

    /**
    * Return a StringTable which contains the names of Component Definitions
    * (CD) that derive from the specified Component Definition
    *
    * @param  sComponent  the qualified CD name
    * @param  fQualify    if set to true, return fully qualified CD names;
    *                     otherwise -- non-qualified names
    *
    * @return StringTable object with Component Definition names as keys
    */
    public StringTable getSubComponents(String sComponent, boolean fQualify)
        {
        ensureContents();

        Object oLocator = getLocator();

        if (sComponent == null || sComponent.length() == 0)
            {
            StringTable tbl = new StringTable();

            sComponent = Component.getRootName();
            if (getJar().getEntry(buildFile(T_COMPONENT, sComponent)) != null)
                {
                tbl.put(sComponent, oLocator);
                }
            return tbl;
            }

        return getNamesFromPackage(m_contents[T_COMPONENT], sComponent, fQualify, '.');
        }

    /**
    * Return a StringTable which contains the names of Component Definitions
    * (CD) that belong to the specified package
    *
    * @param  sPackage  the qualified package name
    * @param  fQualify  if set to true, return fully qualified CD names;
    *                   otherwise -- non-qualified names
    *
    * @return StringTable object with CD names as keys
    */
    public StringTable getPackageComponents(String sPackage, boolean fQualify)
        {
        ensureContents();
        return getNamesFromPackage(m_contents[T_COMPONENT], sPackage, fQualify, '.');
        }

    /**
    * Return a StringTable which contains the names of sub-packages in
    * the specified component package
    *
    * @param sPackage   the qualified package name; pass null to retrieve
    *                   the top level component packages
    * @param fQualify   if set to true, return fully qualified package names;
    *                   otherwise -- non-qualified names
    * @param fSubs      if set to true, returns the entire tree of sub-packages
    *
    * @return StringTable object with package names as keys
    */
    public StringTable getComponentPackages(String sPackage, boolean fQualify, boolean fSubs)
        {
        if (fSubs)
            {
            fQualify = true;
            }

        StringTable tblAll = m_packages[T_COMPONENT];
        if (tblAll == null)
            {
            ensureContents();
            m_packages[T_COMPONENT] = tblAll = ensurePackages(m_contents[T_COMPONENT]);
            }

        return extractPackages(tblAll, sPackage, fQualify, fSubs, '.');
        }

    /**
    * Return a StringTable which contains the names of Java Class Signature
    * (JCS) names that belong to the specified java class package
    * (i.e. "javax.swing")
    *
    * @param sPackage   the qualified package name
    * @param fQualify   if set to true, return fully qualified JCS names;
    *                   otherwise -- non-qualified names
    *
    * @return StringTable object with JCS names as keys
    */
    public StringTable getPackageSignatures(String sPackage, boolean fQualify)
        {
        ensureContents();
        return getNamesFromPackage(m_contents[T_SIGNATURE], sPackage, fQualify, '.');
        }

    /**
    * Return a StringTable which contains the names of sub-packages in
    * the specified java class package
    * (i.e. "javax.swing" is a sub-package of "javax" package)
    *
    * @param sPackage   the qualified package name; pass null to retrieve
    *                   the top level java class packages
    * @param fQualify   if set to true, return fully qualified JCS names;
    *                   otherwise -- non-qualified names
    * @param fSubs      if set to true, returns the entire tree of sub-packages
    *
    * @return StringTable object with package names as keys
    */
    public StringTable getSignaturePackages(String sPackage, boolean fQualify, boolean fSubs)
        {
        StringTable tblAll = m_packages[T_SIGNATURE];
        if (tblAll == null)
            {
            ensureContents();
            m_packages[T_SIGNATURE] = tblAll = ensurePackages(m_contents[T_SIGNATURE]);
            }
        return extractPackages(tblAll, sPackage, fQualify, fSubs, '.');
        }

    /**
    * Return a StringTable which contains the names of resource
    * names that belong to the specified package
    * (i.e. "img/tde")
    *
    * @param sPackage   the qualified package name
    * @param fQualify   if set to true, return fully qualified resource names;
    *                   otherwise -- non-qualified names
    *
    * @return StringTable object with resorce names as keys
    */
    public StringTable getPackageResources(String sPackage, boolean fQualify)
        {
        ensureContents();
        return getNamesFromPackage(m_contents[T_RES_SIG], sPackage, fQualify, '/');
        }

    /**
    * Return a StringTable which contains the names of sub-packages in
    * the specified resource package
    * (i.e. "img/tde" is a sub-package of "img" package)
    *
    * @param sPackage   the qualified package name; pass null to retrieve
    *                   the top level resource packages
    * @param fQualify   if set to true, return fully qualified package names;
    *                   otherwise -- non-qualified names
    * @param fSubs      if set to true, returns the entire tree of sub-packages
    *
    * @return StringTable object with package names as keys
    */
    public StringTable getResourcePackages(String sPackage, boolean fQualify, boolean fSubs)
        {
        StringTable tblAll = m_packages[T_RES_SIG];
        if (tblAll == null)
            {
            ensureContents();
            m_packages[T_RES_SIG] = tblAll = ensurePackages(m_contents[T_RES_SIG]);
            }
        return extractPackages(tblAll, sPackage, fQualify, fSubs, '/');
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Provide the contents of the specified item as a data input stream.
    *
    * @param nType  the enumerated item type
    * @param sName  the name of the item, potentially dot-delimited
    *
    * @return   the file contents as a data input stream; null if the item
    *           does not exist
    */
    protected DataInputStream loadFile(int nType, String sName)
            throws IOException
        {
        JarFile  jar    = getJar();
        String   sEntry = buildFile(nType, sName);
        JarEntry entry  = jar.getJarEntry(sEntry);

        return entry == null ? null :
            new DataInputStream(jar.getInputStream(entry));
        }


    /**
    * Build the path for loading some item from the JAR.
    *
    * @param nType       the item type
    * @param sName       the name of the item, potentially dot-delimited
    *
    * @return the full file path within the JAR
    */
    protected String buildFile(int nType, String sName)
        {
        String sSubDir = SUBDIR[nType];
        String sSuffix = SUFFIX[nType];

        if (nType < T_RES_SIG)
            {
            sName = sName.replace('.', '/');
            }

        return sSubDir + sName + sSuffix;
        }

    /**
    * Make sure an index of the JAR has been loaded.
    */
    protected void ensureContents()
        {
        if (m_contents[T_COMPONENT] != null)
            {
            return;
            }

        StringTable[] contents = m_contents;
        contents[T_COMPONENT  ] = new StringTable();
        contents[T_SIGNATURE  ] = new StringTable();
        contents[T_CLASS      ] = new StringTable();
        contents[T_RES_SIG    ] = new StringTable();
        Object oLocator = getLocator();

        for (Enumeration enmr = getJar().entries(); enmr.hasMoreElements(); )
            {
            JarEntry entry = (JarEntry) enmr.nextElement();
            String   sName = entry.getName();

            for (int i = 0, c = contents.length; i < c; ++i)
                {
                StringTable tblContent = contents[i];
                if (tblContent == null)
                    {
                    continue;
                    }

                if (!sName.endsWith("/") &&
                     sName.startsWith(SUBDIR[i]) && sName.endsWith(SUFFIX[i]))
                    {
                    sName = sName.substring(SUBDIR[i].length(),
                            sName.length() - SUFFIX[i].length());
                    if (sName.length() > 0)
                        {
                        tblContent.put(sName, oLocator);
                        }
                    }
                }
            }
        }

    /**
    * Provide a short human-readable description of the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toString()
        {
        return CLASS + '(' + getJar().getName() + ')';
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "ArchivedStorage";

    /**
    * The super class of Component.
    */
    private static final String ROOT = "Root";

    /**
    * The JAR repository subdirectories for each type.
    */
    private static final String[] SUBDIR;
    static
        {
        SUBDIR = new String[6];
        SUBDIR[T_COMPONENT  ] = "cdb/";
        SUBDIR[T_SIGNATURE  ] = "jcs/";
        SUBDIR[T_CLASS      ] = "classes/";
        SUBDIR[T_JAVA       ] = "java/";
        SUBDIR[T_RES_SIG    ] = "resource/";
        SUBDIR[T_RES_BINARY ] = "classes/";
        }

    /**
    * The JAR repository file extension for Component Definition Binaries.
    */
    private static final String[] SUFFIX;
    static
        {
        SUFFIX = new String[6];
        SUFFIX[T_COMPONENT  ] = ".cdb";
        SUFFIX[T_SIGNATURE  ] = ".jcs";
        SUFFIX[T_CLASS      ] = ".class";
        SUFFIX[T_JAVA       ] = ".java";
        SUFFIX[T_RES_SIG    ] = "";
        SUFFIX[T_RES_BINARY ] = "";
        }

    private StringTable[] m_contents = new StringTable[6];
    private StringTable[] m_packages = new StringTable[6];
    }
