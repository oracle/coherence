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
import com.tangosol.util.SimpleEnumerator;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Enumeration;
import java.util.StringTokenizer;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;


/**
* JarStorage loads classes from JAR file and turn them into
* Java Class Signatures. Component Definitions are not handled
* by the JarStorage, ArchivedStorage should be used instead.
*
* @version 1.00, 02/01/99
* @author  cp
* @author  gg
*/
public class JarStorage
        extends BaseStorage
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a storage object backed by the specified jar.
    *
    * @param sFile  the path of the jar to read entries from
    * @param sRoot  relative path within the jar file that should
    *               be used to retrieve the resources
    *
    * Note: sRoot parameter could point to either directory (and must
    * be terminated by "/") or to an entry representing a contained jar.
    */
    public JarStorage(String sFile, String sRoot)
        {
        this(new File(sFile), sRoot);
        }

    /**
    * Create a storage object backed by the specified jar.
    *
    * @param file   the File object specifying the jar to read entries from
    * @param sRoot  relative path within the jar file that should
    *               be used to retrieve the resources
    *
    * Note: sRoot parameter could point to either directory (and must
    * be terminated by "/") or to an entry representing a contained jar.
    */
    public JarStorage(File file, String sRoot)
        {
        if (!(file.exists() && file.isFile()))
            {
            throw new IllegalArgumentException(CLASS + ":  Invalid file:  "
                    + file.toString());
            }

        if (!file.canRead())
            {
            throw new IllegalArgumentException(CLASS + ":  Read access required:  "
                    + file.toString());
            }

        JarFile jar;
        try
            {
            jar = new JarFile(file.getCanonicalFile());
            }
        catch (IOException e)
            {
            throw new IllegalArgumentException(CLASS + ":  Unable to open jar:  "
                    + file.toString() + " (" + e.getMessage() + ")");
            }

        if (sRoot == null)
            {
            sRoot = "";
            }
        else
            {
            // JarFile entries only allow forward slash
            sRoot = sRoot.replace('\\', '/');
            while (sRoot.startsWith("/"))
                {
                sRoot = sRoot.substring(1);
                }

            if (sRoot.length() > 0)
                {
                StringTokenizer tokens = new StringTokenizer(sRoot, "!");
                while (tokens.hasMoreTokens())
                    {
                    String sPath = tokens.nextToken();
                    if (sPath.endsWith("/"))
                        {
                        sRoot = sPath;
                        while (sRoot.startsWith("/"))
                            {
                            sRoot = sRoot.substring(1);
                            }

                        if (tokens.hasMoreTokens())
                            {
                            try
                                {
                                jar.close();
                                }
                            catch (IOException eIgnore) {}

                            throw new IllegalArgumentException(CLASS +
                                ": Illegal path " + sRoot + '!' + tokens.nextToken());
                            }
                        }
                    else
                        {
                        // the entry must be a contained jar
                        JarFile jarInner;
                        try
                            {
                            jarInner = extractJar(jar, sPath);
                            }
                        catch (IOException e)
                            {
                            throw ensureRuntimeException(e);
                            }
                        finally
                            {
                            try
                                {
                                jar.close();
                                }
                            catch (IOException e) {}
                            }

                        jar   = jarInner;
                        sRoot = "";
                        m_fDeleteOnClose = true;
                        }
                    }
                }
            }

        m_jar   = jar;
        m_sRoot = sRoot;
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
        return Component.isQualifiedNameLegal(sName) ? null : loadSignature(sName);
        }

    /**
    * Store the specified Component.
    *
    * @param cd       the Component Definition
    * @param errlist  the ErrorList object to log any derivation/modification
    *                 errors to
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeComponent(Component cd, ErrorList errlist)
            throws ComponentException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }


    /**
    * Remove the specified Component.
    *
    * @param sName fully qualified Component Definition name
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void removeComponent(String sName)
            throws ComponentException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
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
        ClassFile clzf = loadOriginalClass(sName);

        if (clzf == null)
            {
            return null;
            }

        // 2002-07-22 cp - support for parsing parameter names
        String sScript = null;
        try
            {
            sScript = loadJava(sName);
            }
        catch (Exception e)
            {
            }

        return new Component(clzf, sScript);
        }

    /**
    * Store the specified Java Class Signature.
    *
    * @param cdJCS  the Java Class Signature
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeSignature(Component cdJCS)
            throws ComponentException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }


    // ----- Classes --------------------------------------------------------

    /**
    * Load the original (before any customization takes place) Java Class.
    *
    * @param sName  fully qualified Java Class name
    *
    * @return the specified Class structure
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public ClassFile loadOriginalClass(String sName)
            throws ComponentException
        {
        try
            {
            DataInputStream stream = loadFile(sName.replace('.', '/') + ".class");

            return stream == null ? null : new ClassFile(stream);
            }
        catch (IOException e)
            {
            throw new ComponentException(e.toString());
            }
        }

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
        return null;
        }

    /**
    * Store the specified generated Java Class along with its listing
    *
    * @param clz       the generated Class structure to store
    * @param sListing  (optional) the java listing of the class
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeClass(ClassFile clz, String sListing)
            throws ComponentException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
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
        byte[] ab = loadFileBytes(sName.replace('.', '/') + ".java");

        if (ab == null)
            {
            int of = sName.indexOf('$');
            if (of != -1)
                {
                sName = sName.substring(0, of).replace('.', '/');
                ab    = loadFileBytes(sName + ".java");
                }
            }

        return ab == null ? null : new String(ab);
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
        return loadFileBytes(sName);
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
        return loadOriginalResource(sName);
        }

    /**
    * Store the specified Resource Signature.
    *
    * @param sName  fully qualified resource name
    * @param abData the specified Resource Signature as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public void storeResourceSignature(String sName, byte[] abData)
            throws IOException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }

    /**
    * Remove the specified Resource Signature
    *
    * @param sName fully qualified resource name
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public void removeResourceSignature(String sName)
            throws IOException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
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
        return null;
        }

    /**
    * Store the specified resource.
    *
    * @param sName  fully qualified resource name
    * @param abData the specified resource as a byte array
    *
    * @exception IOException  if an unrecoverable error occurs
    */
    public void storeResource(String sName, byte[] abData)
            throws IOException
        {
        throw new UnsupportedOperationException("Read-only storage: " + this);
        }


    // ---- component management --------------------------------------------

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
        return new StringTable();
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
        return new StringTable();
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
        return new StringTable();
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
        return getNamesFromPackage(m_tblClasses, sPackage, fQualify, '.');
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
        StringTable tblAll = m_tblClassPackages;
        if (tblAll == null)
            {
            ensureContents();
            m_tblClassPackages = tblAll = ensurePackages(m_tblClasses);
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
    * @return StringTable object with resource names as keys
    */
    public StringTable getPackageResources(String sPackage, boolean fQualify)
        {
        ensureContents();
        return getNamesFromPackage(m_tblResources, sPackage, fQualify, '/');
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
        StringTable tblAll = m_tblResourcePackages;
        if (tblAll == null)
            {
            ensureContents();
            m_tblResourcePackages = tblAll = ensurePackages(m_tblResources);
            }
        return extractPackages(tblAll, sPackage, fQualify, fSubs, '/');
        }


    /**
    * Close the storage and release all the resources
    */
    public void close()
        {
        // using File.deleteOnExit wouldn't help since without closing the jar
        // the file would not be deleted anyway (see bug id #4171239)
        if (m_jar != null)
            {
            try
                {
                m_jar.close();
                if (m_fDeleteOnClose)
                    {
                    new File(m_jar.getName()).delete();
                    }
                }
            catch (IOException e)
                {
                }
            finally
                {
                m_jar = null;
                }
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * @return the jar file that backs up this JarStorage
    */
    public JarFile getJar()
        {
        return m_jar;
        }

    /**
    * @return the path within the jar file that should be used to retrieve
    * the jar entries.
    */
    public String getRoot()
        {
        return m_sRoot;
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Extract an entry representing a jar file out of the specified jar
    * into a temporary file. Caller is supposed to delete this file
    * when the JarFile object is no longer used.
    *
    * @param jar    the enclosing jar file
    * @param sPath  entry name representing an enclosed jar file
    *
    * @return a JarFile object backed up by a temporary file
    */
    public static JarFile extractJar(JarFile jar, String sPath)
            throws IOException
        {
        JarEntry    entry    = jar.getJarEntry(sPath);
        InputStream streamIn = entry == null ? null : jar.getInputStream(entry);
        if (streamIn == null)
            {
            throw new IOException(CLASS +
                ":  Unable to open contained jar:  " + jar.getName() + "!/" + sPath);
            }

        String sName   = sPath.substring(sPath.lastIndexOf('/') + 1);
        int    of      = sName.lastIndexOf('.');
        String sPrefix = of >= 3 ? sName.substring(0, of) : "tmp";
        String sSuffix = of >= 0 && of < sName.length() - 3 ? sName.substring(of) : ".jar";

        File             fileTemp  = File.createTempFile(sPrefix, sSuffix, null);
        FileOutputStream streamOut = new FileOutputStream(fileTemp);

        try
            {
            int    cbBlock = 4096;
            byte[] ab      = new byte[cbBlock];

            while (true)
                {
                int cb = read(streamIn, ab);
                if (cb <= 0)
                    {
                    break;
                    }
                streamOut.write(ab, 0, cb);
                }
            }
        finally
            {
            streamIn .close();
            streamOut.close();
            }

        return new JarFile(fileTemp);
        }

    /**
    * Provide the contents of the specified item as a data input stream.
    *
    * @param sName  the name of the item, potentially dot-delimited
    *
    * @return   the file contents as a data input stream; null if the item
    *           does not exist
    */
    protected DataInputStream loadFile(String sName)
            throws IOException
        {
        String   sEntry = sName.startsWith("/") ? sName.substring(1) : m_sRoot + sName;
        JarEntry entry  = m_jar.getJarEntry(sEntry);

        return entry == null ? null :
            new DataInputStream(m_jar.getInputStream(entry));
        }

    /**
    * Provide the contents of the specified item as a byte array.
    *
    * @param sName  the entry name to be loaded
    *
    * @return  the file contents as a byte array;null if the item
    *           does not exist
    */
    protected byte[] loadFileBytes(String sName)
            throws IOException
        {
        final long MAX_SIZE = 0x500000L; // 0.5Meg

        String   sEntry = sName.startsWith("/") ? sName.substring(1) : m_sRoot + sName;
        JarEntry entry  = m_jar.getJarEntry(sEntry);

        if (entry == null)
            {
            return null;
            }

        long lSize = entry.getSize();
        if (lSize > MAX_SIZE)
            {
            throw new IOException(CLASS + ".loadFileBytes: " +
                "Entry " + sEntry + " exceeds maximum size");
            }

        InputStream     streamRaw  = m_jar.getInputStream(entry);
        DataInputStream streamData = new DataInputStream(streamRaw);

        // bug Id 4295717: JarInputStream.read(byte[] b,int off, int len) not working properly
        // m_jar.getInputStream(entry).read(ab, 0, cb);

        byte[] ab = read(streamData);

        if (ab.length != lSize)
            {
            throw new EOFException(CLASS + ".loadFileBytes: " +
                "Premature EOF: " + sEntry + " expected=" + lSize + " actual=" + ab.length);
            }
        streamRaw.close();

        return ab;
        }

    /**
    * Make sure an index of the JAR has been loaded.
    */
    protected void ensureContents()
        {
        if (m_tblClasses != null)
            {
            return;
            }

        m_tblClasses   = new StringTable();
        m_tblResources = new StringTable();

        String sPrefix = m_sRoot;

        for (Enumeration enmr = m_jar.entries(); enmr.hasMoreElements();)
            {
            JarEntry entry  = (JarEntry) enmr.nextElement();
            String   sEntry = entry.getName();
            int      iType  = getEntryType(sEntry);

            if (iType != -1)
                {
                if (sEntry.startsWith(sPrefix))
                    {
                    sEntry = sEntry.substring(sPrefix.length());
                    }
                addContentEntry(sEntry, iType);
                }
            }
        }

    /**
    * @return a type of the specified entry; -1 if the entry type is not known
    */
    protected int getEntryType(String sEntry)
        {
        String sPrefix = m_sRoot;

        if (sEntry.startsWith(sPrefix))
            {
            // everything but .class and .java is a resource
            // (except the "META-INF/Manifest.mf" entry)
            if (sEntry.endsWith(".class"))
                {
                return T_CLASS;
                }

            if (!sEntry.endsWith("/") && !sEntry.endsWith("java") &&
                !sEntry.equalsIgnoreCase("META-INF/Manifest.mf"))
                {
                return T_RES_BINARY;
                }
            }
        return -1;
        }

    /**
    * Add the specified entry to the content of the specified type
    */
    protected void addContentEntry(String sEntry, int iType)
        {
        switch (iType)
            {
            case T_CLASS:
                {
                // ".class".length() == 6
                String sClz = sEntry.substring(0, sEntry.length() - 6);
                m_tblClasses.put(sClz, getLocator());
                break;
                }

            case T_RES_BINARY:
                {
                m_tblResources.put(sEntry, getLocator());
                break;
                }
            }
        }

    /**
    * Create a StringTable of packages for the specified table of names
    */
    protected StringTable ensurePackages(StringTable tblNames)
        {
        Object      oLocator = getLocator();
        StringTable tblPkg   = new StringTable();
        String      sPrev    = "";
        for (Enumeration enmr = tblNames.keys(); enmr.hasMoreElements();)
            {
            String sName = (String) enmr.nextElement();

            int    of    = sName.lastIndexOf('/');
            if (of > 0)
                {
                // get name of pkg
                sName = sName.substring(0, of);

                // avoid repetitions
                if (!sName.equals(sPrev))
                    {
                    // make sure that all the sub-packages are registered as well
                    while (true)
                        {
                        of = sName.lastIndexOf('/', of - 1);
                        if (of < 0)
                            {
                            break;
                            }
                        tblPkg.put(sName.substring(0, of), oLocator);
                        }
                    tblPkg.put(sName, oLocator);
                    sPrev = sName;
                    }
                }
            }

        return tblPkg;
        }

    protected StringTable getNamesFromPackage(StringTable tblAll,
                                               String sPackage, boolean fQualify, char chDelim)
        {
        StringTable tbl = new StringTable();

        sPackage = (sPackage == null || sPackage.length() == 0) ? "" : sPackage + '/';
        if (chDelim != '/')
            {
            sPackage = sPackage.replace(chDelim, '/');
            }

        boolean fPreserveAbsoluteName = false;
        if (sPackage.startsWith("/"))
            {
            fPreserveAbsoluteName = true;
            sPackage = sPackage.substring(1);
            }

        int      cchPackage = sPackage.length();
        String[] asNames    = tblAll.stringsStartingWith(sPackage);

        if (asNames.length == 0)
            {
            return tbl;
            }

        Object oLocator = getLocator();

        for (Enumeration enmr = new SimpleEnumerator(asNames); enmr.hasMoreElements();)
            {
            String sName = (String) enmr.nextElement();

            if (sName.indexOf('/', cchPackage) < 0)
                {
                if (!fQualify)
                    {
                    sName = sName.substring(cchPackage);
                    }
                else if (chDelim != '/')
                    {
                    sName = sName.replace('/', chDelim);
                    }

                if (fPreserveAbsoluteName)
                    {
                    sName = "/" + sName;
                    }
                tbl.put(sName, oLocator);
                }
            }

        return tbl;
        }

    protected StringTable extractPackages(StringTable tblAll, String sPackage,
                                           boolean fQualify, boolean fSubs, char chDelim)
        {
        if (fSubs)
            {
            fQualify = true;
            }

        StringTable tblSub = new StringTable();

        sPackage = (sPackage == null || sPackage.length() == 0) ? "" : sPackage + '/';
        if (chDelim != '/')
            {
            sPackage = sPackage.replace(chDelim, '/');
            }

        int      cchPackage = sPackage.length();
        String[] asNames    = tblAll.stringsStartingWith(sPackage);

        if (asNames.length == 0)
            {
            return tblSub;
            }

        Object oLocator = getLocator();

        for (Enumeration enmr = new SimpleEnumerator(asNames); enmr.hasMoreElements();)
            {
            String sName = (String) enmr.nextElement();

            int of = sName.indexOf('/', cchPackage);
            if (of < 0)
                {
                if (!fQualify)
                    {
                    sName = sName.substring(cchPackage);
                    }
                else if (chDelim != '/')
                    {
                    sName = sName.replace('/', chDelim);
                    }

                tblSub.put(sName, oLocator);

                if (fSubs)
                    {
                    tblSub.addAll(extractPackages(tblAll, sName, true, true, chDelim));
                    }
                }
            }

        return tblSub;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Provide a short human-readable description of the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toString()
        {
        return CLASS + '(' + m_jar.getName() + "!/" + m_sRoot + ')';
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "JarStorage";

    /**
    * The component type of entry
    */
    protected static final int T_COMPONENT   = 0;
    /**
    * The Java Class Signature type of entry.
    */
    protected static final int T_SIGNATURE   = 1;
    /**
    * The Java Classes type of entry.
    */
    protected static final int T_CLASS       = 2;
    /**
    * The Java source type of entry.
    */
    protected static final int T_JAVA        = 3;
    /**
    * The Resource Signature type of entry.
    */
    protected static final int T_RES_SIG     = 4;
    /**
    * The [resolved] Resource type of entry.
    */
    protected static final int T_RES_BINARY  = 5;

    /**
    * The JarFile that backs up this JarStorage
    */
    private JarFile m_jar;

    /**
    * The relative path within the jar file that should
    * be used to retrieve the entries. It could point to either directory
    * or an entry representing a contained jar.
    */
    private String m_sRoot;

    /**
    * Specifies whether the jar file is a temporary one
    * and should be removed when this storage is gc'ed
    */
    private transient boolean m_fDeleteOnClose = false;

    // cache tables (all entries are '/'-delimited)
    protected transient StringTable m_tblClasses;
    protected transient StringTable m_tblClassPackages;

    protected transient StringTable m_tblResources;
    protected transient StringTable m_tblResourcePackages;
    }
