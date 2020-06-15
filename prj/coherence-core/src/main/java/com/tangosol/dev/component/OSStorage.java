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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;

import java.util.Enumeration;


/**
* OSStorage loads and saves single-dimension (no versioning, localization,
* or customization) Components from and to an OS file system.
*
* @version 1.00, 02/04/98
* @author  Cameron Purdy
*/
public class OSStorage
        extends BaseStorage
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a storage object backed by the specified path.
    *
    * @param path  the java.lang.String object containing the OS path name
    *              to read/write Components from/to
    */
    public OSStorage(String path)
        {
        this(new File(path));
        }

    /**
    * Create a storage object backed by the specified path.
    *
    * @param path  the java.io.File object containing the OS path to
    *              read/write Components from/to
    */
    public OSStorage(File path)
        {
        boolean fValid;

        if (!path.isAbsolute())
            {
            try
                {
                path = path.getCanonicalFile();
                }
            catch (IOException e) {}
            }

        if (path.exists())
            {
            if (!path.canRead()) // || !path.canWrite())
                {
                throw new IllegalArgumentException(CLASS + ":  Read access required!");
                }
            fValid = path.isDirectory();
            }
        else
            {
            // defer the directory creation
            fValid = path.getParentFile() != null && path.getParentFile().exists();
            }

        if (!fValid)
            {
            throw new IllegalArgumentException(CLASS + ":  Invalid directory:  " + path.getPath());
            }

        m_path = path;

        //ensureStorageDirectories();
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
    *
    */
    public Component loadComponent(String sName, boolean fReadOnly, ErrorList errlist)
            throws ComponentException
        {
        if (sName.length() == 0)
            {
            sName = ROOT;
            }
        /* gg: 2001.4.25 allow JCS modifications

        else if (!Component.isQualifiedNameLegal(sName))
            {
            throw new ComponentException(CLASS +
                ".loadComponent:  Illegal name -- " + sName);
            }
        */

        try
            {
            byte[] ab = loadFile(T_COMPONENT, sName);

            if (ab == null)
                {
                /*
                if (sName == ROOT)
                    {
                    return Component.getRootSuper(this);
                    }
                */
                return Component.isQualifiedNameLegal(sName) ? null : loadSignature(sName);
                }

            Component cd = new Component(
                new DataInputStream(new ByteArrayInputStream(ab)));

            // case-sensitive check
            if (!sName.equals(ROOT) && !sName.equals(cd.getQualifiedName()))
                {
                out("Component case-sensitive check failed:  expected=" +
                    sName + ", actual=" + cd.getQualifiedName());
                return null;
                }

            if (fReadOnly)
                {
                cd.setModifiable(false);
                }
            return cd;
            }
        catch (IOException e)
            {
            out(e);
            throw new ComponentException(e.toString());
            }
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
        try
            {
            if (Trait.DEBUG)
                {
                out();
                out("***OSStorage*** Component before finalizeExtract:");
                cd.dump();
                }

            cd.finalizeExtract(this, errlist);
            if (Trait.DEBUG)
                {
                out();
                out("***OSStorage*** Component after finalizeExtract:");
                cd.dump();
                }

            String sName = cd.getQualifiedName();
            if (sName.length() == 0)
                {
                sName = ROOT;
                }

            if (cd.isDiscardable())
                {
                removeComponent(sName);
                }
            else
                {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                cd.save(new DataOutputStream(stream));

                storeFile(T_COMPONENT, sName, stream.toByteArray());
                }
            }
        catch (IOException e)
            {
            throw new ComponentException(e.toString());
            }
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
        try
            {
            removeFile(T_COMPONENT, sName);
            }
        catch (IOException e)
            {
            throw new ComponentException(e.toString());
            }

        try
            {
            String sClz = Component.isQualifiedNameLegal(sName) ?
                DataType.getComponentClassName(sName) : sName;

            removeFile(T_CLASS, sClz);
            removeFile(T_JAVA,  sClz);
            }
        catch (IOException e) {}
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
            byte[] ab = loadFile(T_SIGNATURE, sName);

            if (ab == null)
                {
                return null;
                }

            Component cd = new Component(new DataInputStream(new ByteArrayInputStream(ab)));

            // case-sensitive check
            if (!sName.equals(cd.getQualifiedName()))
                {
                ///////////////////////////////////////////////////////////////////////////
                // There are valid scenarios where this condition occurs (at least on the
                // NT filing system).
                //
                // For example the Java compiler when evaluating the statement:
                //
                //   char ch = java.awt.event.KeyEvent.VK_ENTER;
                //
                // tries to find the class that this applies to.  In doing so, the compiler
                // first tries to load in order the signatures "java", "java.awt",
                // "java.awt.event", and "java.awt.event.KeyEvent" which is successful.
                //
                // Since there is a class "java.awt.Event", we will encounter this
                // case-sensitivity check when on the NT filing system during the attempt
                // to load the signature "java.awt.event".
                //
                // Because of this, the following message has been removed.
                //
                // out("Signature case-sensitive check failed:  expected=" + sName +
                //     ", actual=" + cd.getQualifiedName());
                ///////////////////////////////////////////////////////////////////////////
                return null;
                }

            return cd;
            }
        catch (IOException e)
            {
            throw new ComponentException(e.toString());
            }
        }

    /**
    * Store the specified generated Java Class Signature.
    *
    * @param cdJCS  the Java Class Signature
    *
    * @exception ComponentException  if an unrecoverable error occurs
    */
    public void storeSignature(Component cdJCS)
            throws ComponentException
        {
        try
            {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            cdJCS.save(new DataOutputStream(stream));
            storeFile(T_SIGNATURE, cdJCS.getName(), stream.toByteArray());
            }
        catch (IOException e)
            {
            throw new ComponentException(e.toString());
            }
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
        // OSStorage is not expected to carry the original classes,
        // only the JarStorage is.
        return null;
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
        try
            {
            String sClass  = sName.replace('.', '/');
            byte[] abClass = loadFile(T_CLASS, sName);

            if (abClass == null)
                {
                return null;
                }

            ClassFile clsf = new ClassFile(new DataInputStream(
                                    new ByteArrayInputStream(abClass)));
            if (clsf.getName().equals(sClass))
                {
                return clsf;
                }
            else
                {
                out("Class case-sensitive check failed:  expected=" + sClass +
                    ", actual=" + clsf.getName());
                return null;
                }
            }
        catch (IOException e)
            {
            throw new ComponentException(e.toString());
            }
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
        try
            {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            clz.save(new DataOutputStream(stream));

            String sClass = clz.getName();
            storeFile(T_CLASS, sClass, stream.toByteArray());

            if (sListing != null)
                {
                storeFile(T_JAVA, sClass, sListing.getBytes());
                }
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
        // OSStorage is not expected to carry the original Java source,
        // only the JarStorage is.
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
        // OSStorage is not expected to carry the original resource,
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
        return loadFile(T_RES_SIG, sName);
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
        if (abData == null) // discardable
            {
            removeResourceSignature(sName);
            }
        else
            {
            storeFile(T_RES_SIG, sName, abData);
            }
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
        removeFile(T_RES_SIG, sName);
        removeFile(T_RES_BINARY, sName);
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
        return loadFile(T_RES_BINARY, sName);
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
        if (abData == null)
            {
            removeFile(T_RES_BINARY, sName);
            }
        else
            {
            storeFile(T_RES_BINARY, sName, abData);
            }
        }


    // ---- component managment --------------------------------------------

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
        Object oLocator = getLocator();

        if (sComponent == null || sComponent.length() == 0)
            {
            StringTable tbl = new StringTable();

            sComponent = Component.getRootName();
            if (existsFile(T_COMPONENT, sComponent))
                {
                tbl.put(sComponent, oLocator);
                }
            return tbl;
            }

        StringTable tblSub = getFiles(T_COMPONENT, sComponent);
        if (tblSub == null)
            {
            return new StringTable();
            }

        if (fQualify && !tblSub.isEmpty())
            {
            Enumeration enmr = tblSub.keys();

            for (tblSub = new StringTable(); enmr.hasMoreElements();)
                {
                tblSub.put(sComponent + '.' + (String) enmr.nextElement(), oLocator);
                }
            }
        return tblSub;
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
        return getSubComponents(sPackage, fQualify);
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

        StringTable tblSub = getSubdirectories(T_COMPONENT, sPackage);
        if (tblSub == null)
            {
            return new StringTable();
            }

        if (fQualify && !tblSub.isEmpty())
            {
            Object oLocator = getLocator();
            Enumeration enmr = tblSub.keys();
            for (tblSub = new StringTable(); enmr.hasMoreElements();)
                {
                String sSubPackage =
                    (sPackage == null ? "" : sPackage + '.') + (String) enmr.nextElement();

                tblSub.put(sSubPackage, oLocator);

                if (fSubs)
                    {
                    tblSub.addAll(getComponentPackages(sSubPackage, true, true));
                    }
                }
            }
        return tblSub;
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
        // persistent Signatures in OSStorage...
        // since we don't ship the UI for cretaing those
        // this could only happen during in-house testing
        StringTable tblJCS = getFiles(T_SIGNATURE, sPackage);

        // customizations for JCSs are stored as Component Definitions
        StringTable tblDelta = getFiles(T_COMPONENT, sPackage);
        if (tblDelta != null)
            {
            if (sPackage == null)
                {
                tblDelta.remove("Root");
                tblDelta.remove("Component");
                }

            if (tblJCS == null)
                {
                tblJCS = tblDelta;
                }
            else
                {
                tblJCS.putAll(tblDelta);
                }
            }

        if (tblJCS == null)
            {
            return new StringTable();
            }

        if (fQualify && sPackage != null && !tblJCS.isEmpty())
            {
            Object oLocator = getLocator();
            Enumeration enmr = tblJCS.keys();
            for (tblJCS = new StringTable(); enmr.hasMoreElements();)
                {
                tblJCS.put(sPackage + '.' + (String) enmr.nextElement(), oLocator);
                }
            }
        return tblJCS;
        }

    /**
    * Return a StringTable which contains the names of sub-packages in
    * the specified java class package
    * (i.e. "javax.swing" is a sub-package of "javax" package)
    *
    * @param sPackage   the qualified package name; pass null to retrieve
    *                   the top level java class packages
    * @param fQualify   if set to true, return fully qualified package names;
    *                   otherwise -- non-qualified names
    * @param fSubs      if set to true, returns the entire tree of sub-packages
    *
    * @return StringTable object with package names as keys
    */
    public StringTable getSignaturePackages(String sPackage, boolean fQualify, boolean fSubs)
        {
        if (fSubs)
            {
            fQualify = true;
            }

        // persistent Signatures in OSStorage...
        // since we don't ship the UI for creating those
        // this could only happen during in-house testing
        StringTable tblSub = getSubdirectories(T_SIGNATURE, sPackage);

        // customizations for JCSs are stored as Component Definitions
        StringTable tblDelta = getSubdirectories(T_COMPONENT, sPackage);
        if (tblDelta != null)
            {
            if (sPackage == null)
                {
                tblDelta.remove("Component");
                }

            if (tblSub == null)
                {
                tblSub = tblDelta;
                }
            else
                {
                tblSub.putAll(tblDelta);
                }
            }

        if (tblSub == null)
            {
            return new StringTable();
            }

        if (fQualify && !tblSub.isEmpty())
            {
            Object oLocator = getLocator();
            Enumeration enmr = tblSub.keys();
            for (tblSub = new StringTable(); enmr.hasMoreElements();)
                {
                String sSubPackage =
                    (sPackage == null ? "" : sPackage + '.') + (String) enmr.nextElement();

                tblSub.put(sSubPackage, oLocator);

                if (fSubs)
                    {
                    tblSub.addAll(getSignaturePackages(sSubPackage, true, true));
                    }
                }
            }
        return tblSub;
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
    * @return StringTable object with JCS names as keys
    */
    public StringTable getPackageResources(String sPackage, boolean fQualify)
        {
        StringTable tblRes = getFiles(T_RES_SIG, sPackage);

        if (tblRes == null)
            {
            return new StringTable();
            }

        if (fQualify && sPackage != null && !tblRes.isEmpty())
            {
            Object oLocator = getLocator();
            Enumeration enmr = tblRes.keys();
            for (tblRes = new StringTable(); enmr.hasMoreElements();)
                {
                tblRes.put(sPackage + '/' + (String) enmr.nextElement(), oLocator);
                }
            }
        return tblRes;
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
        if (fSubs)
            {
            fQualify = true;
            }

        StringTable tblSub = getSubdirectories(T_RES_SIG, sPackage);

        if (tblSub == null)
            {
            return new StringTable();
            }

        if (fQualify && !tblSub.isEmpty())
            {
            Object oLocator = getLocator();
            Enumeration enmr = tblSub.keys();
            for (tblSub = new StringTable(); enmr.hasMoreElements();)
                {
                String sSubPackage =
                    (sPackage == null ? "" : sPackage + '/') + (String) enmr.nextElement();

                tblSub.put(sSubPackage, oLocator);

                if (fSubs)
                    {
                    tblSub.addAll(getResourcePackages(sSubPackage, true, true));
                    }
                }
            }
        return tblSub;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Find out whether or not the specified item is stored in this storage
    *
    * @param nType  the enumerated item type
    * @param sName  the name of the item, potentially dot-delimited
    *
    * @return  true if the item is stored in this storage; false otherwise
    */
    protected boolean existsFile(int nType, String sName)
        {
        try
            {
            return buildFile(nType, sName, false).exists();
            }
        catch (IOException e)
            {
            return false;
            }
        }

    /**
    * Read the contents of the specified item as a byte array.
    *
    * @param nType  the enumerated item type
    * @param sName  the name of the item, potentially dot-delimited
    *
    * @return   the file contents as a byte array; null if the specified
    *           file does not exist
    */
    protected byte[] loadFile(int nType, String sName)
            throws IOException
        {
        File file = buildFile(nType, sName, false);
        if (!file.isFile())
            {
            return null;
            }

        return read(file);
        }

    /**
    * Write the binary contents of the specified item.
    *
    * @param nType   the enumerated item type
    * @param sName   the name of the item, potentially dot-delimited
    * @param abItem  the binary contents to write
    */
    protected void storeFile(int nType, String sName, byte[] abItem)
            throws IOException
        {
        File file = buildFile(nType, sName, true);

        if (file.isFile())
            {
            if (equalsDeep(abItem, read(file)))
                {
                // no change -- no store
                return;
                }

            if (nType == T_COMPONENT && !file.canWrite())
                {
                throw new IOException("Read-only component: "
                    + file);
                }
            file.delete();
            }

        FileOutputStream stream = new FileOutputStream(file);
        try
            {
            stream.write(abItem);
            }
        finally
            {
            try
                {
                stream.close();
                }
            catch (IOException e) {}
            }
        }

    /**
    * Remove the file from the storage.
    *
    * @param nType   the enumerated item type
    * @param sName   the name of the item, potentially dot-delimited
    */
    protected void removeFile(int nType, String sName)
            throws IOException
        {
        File file = buildFile(nType, sName, false);
        if (!file.isFile())
            {
            return;
            }

        File dir = file.getParentFile();

        file.delete();

        // remove all children classes
        if (nType == T_CLASS || nType == T_JAVA)
            {
            File[] aFile = dir.listFiles();

            if (aFile != null)
                {
                String sPref = sName.substring(sName.lastIndexOf('.') + 1) + '$';
                for (int i = 0, c = aFile.length; i < c; i++)
                    {
                    file = aFile[i];

                    if (file.getName().startsWith(sPref))
                        {
                        file.delete();
                        }
                    }
                }
            }

        // clean up all empty parent directories
        while (!dir.equals(m_path))
            {
            if (!emptyDir(dir))
                {
                break;
                }
            dir = dir.getParentFile();
            }
        }

    /**
    * Clean-up the specified directory of all empty subdirectories
    * and remove it if the directory becomes empty
    *
    * @param dir   the directory to check
    *
    * @return true if the directory has been removed; false otherwise
    */
    private boolean emptyDir(File dir)
        {
        azzert(dir.isDirectory(), "Not a directory: " + dir);

        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; ++i)
            {
            File file = files[i];

            // if at least one the file must exist,
            // then the directory itself must exist
            if (!file.isDirectory() || !emptyDir(file))
                {
                return false;
                }
            }

        // remove this directory
        dir.delete();
        return true;
        }

    /**
    * Build the full file path for loading/storing some item from/to the OS
    * repository.
    *
    * @param nType       the item type
    * @param sName       the name of the item, potentially dot-delimited
    * @param fEnsureDir  true if the item is going to be stored and the
    *                    directories therefore may need to be created
    *
    * @return the full file path as a java.io.File object
    */
    protected File buildFile(int nType, String sName, boolean fEnsureDir)
            throws IOException
        {
        sName = SUBDIR[nType] + '/' + sName;
        if (nType < T_RES_SIG)
            {
            sName = sName.replace('.', '/');
            }

        File file = m_path;

        int ofCur = 0;
        int ofSep = sName.indexOf('/');
        while (true)
            {
            if (fEnsureDir && !file.isDirectory() && !file.mkdir())
                {
                throw new IOException("Unable to create directory " + file);
                }

            if (ofSep >= 0)
                {
                file = new File(file, sName.substring(ofCur, ofSep));

                ofCur = ofSep + 1;
                ofSep = sName.indexOf('/', ofCur);
                }
            else
                {
                return new File(file, sName.substring(ofCur) + SUFFIX[nType]);
                }
            }
        }

    /**
    * Build the full directory path for enumerating some item from the OS
    * repository.
    *
    * @param nType       the item type
    * @param sPackage    the name of the package (directory), potentially dot-delimited
    *                    if null, return the top level directory
    *
    * @return   the full file path as a File object; null if the specified
    *           package does not exist
    */
    protected File buildDirectory(int nType, String sPackage)
        {
        if (sPackage == null || sPackage.length() == 0)
            {
            sPackage = SUBDIR[nType];
            }
        else
            {
            sPackage = SUBDIR[nType] + '/' + sPackage;
            }

        if (nType < T_RES_SIG)
            {
            sPackage = sPackage.replace('.', '/');
            }

        File dir   = m_path;
        int  ofCur = 0;
        int  ofSep;
        do
            {
            ofSep = sPackage.indexOf('/', ofCur);

            dir = new File(dir, ofSep >= 0 ?
                    sPackage.substring(ofCur, ofSep) : sPackage.substring(ofCur));
            if (!dir.isDirectory())
                {
                return null;
                }

            ofCur = ofSep + 1;
            } while (ofSep >= 0);

        return dir;
        }

    /**
    * Return the StringTable that contains the files names of the specified type
    * in the specified package (directory) in the OS repository
    *
    * @param nType       the item type
    * @param sPackage    the name of the package, potentially dot-delimited
    *
    * @return   the StringTable object with [non-qualified] file names as keys
    *           and the storage locator objects as elements; null if specified
    *           package does not exist
    */
    protected StringTable getFiles(int nType, String sPackage)
        {
        File dir = buildDirectory(nType, sPackage);
        if (dir == null)
            {
            return null;
            }

        String sSuffix = SUFFIX[nType];
        int    iSuffix = sSuffix.length();

        StringTable tblFiles = new StringTable();
        Object      oLocator = getLocator();

        String[] asName = dir.list();
        int      cNames = asName.length;
        for (int c = 0; c < cNames; ++c)
            {
            String sName = asName[c];
            if (sName.toLowerCase().endsWith(sSuffix))
                {
                // make sure that this file can in fact be
                // accessed with the correct case suffix
                sName = sName.substring(0, sName.length() - iSuffix);
                File file = new File(dir, sName + sSuffix);
                if (file.isFile())
                    {
                    tblFiles.put(sName, oLocator);
                    }
                }
            }

        return tblFiles;
        }

    /**
    * Return the StringTable that contains the sub-package names of the specified
    * type in the specified package (directory) in the OS repository
    *
    * @param nType       the item type
    * @param sPackage    the name of the package, potentially dot-delimited;
    *                    if null, enumerate the top level packages
    *
    * @return the StringTable object with [non-qualified] subdirectory names as keys
    *         and the storage locator objects as elements; null if specified package
    *         does not exist
    */
    protected StringTable getSubdirectories(int nType, String sPackage)
        {
        File dir = buildDirectory(nType, sPackage);
        if (dir == null)
            {
            return null;
            }

        StringTable tblSub   = new StringTable();
        Object      oLocator = getLocator();

        String[] asName = dir.list();
        int      cNames = asName.length;
        for (int i = 0; i < cNames; i++)
            {
            String sName = asName[i];
            File   file  = new File(dir, sName);
            if (file.isDirectory())
                {
                tblSub.put(sName, oLocator);
                }
            }

        return tblSub;
        }

    /**
    * Provide a short human-readable description of the trait.
    *
    * @return a human-readable description of this trait
    */
    public String toString()
        {
        return CLASS + '(' + m_path + ')';
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "OSStorage";

    /**
    * The OS repository subdirectory for Component Definition Binaries.
    */
    private static final int T_COMPONENT   = 0;
    /**
    * The OS repository subdirectory for Java Class Signature.
    */
    private static final int T_SIGNATURE   = 1;
    /**
    * The OS repository subdirectory for Java Classes.
    */
    private static final int T_CLASS       = 2;
    /**
    * The OS repository subdirectory for Java Source (listing).
    */
    private static final int T_JAVA        = 3;
    /**
    * The OS repository subdirectory for Resource Signatures
    */
    private static final int T_RES_SIG     = 4;
    /**
    * The OS repository subdirectory for [resolved] resources
    */
    private static final int T_RES_BINARY  = 5;

    /**
    * The super class of Component.
    */
    private static final String ROOT = "Root";

    /**
    * The OS repository subdirectories for each type.
    */
    private static final String[] SUBDIR;
    static
        {
        SUBDIR = new String[6];
        SUBDIR[T_COMPONENT  ] = "cdb";
        SUBDIR[T_SIGNATURE  ] = "jcs";
        SUBDIR[T_CLASS      ] = "classes";
        SUBDIR[T_JAVA       ] = "java";
        SUBDIR[T_RES_SIG    ] = "resource";
        SUBDIR[T_RES_BINARY ] = "classes";
        }

    /**
    * The OS repository file extension for Component Definition Binaries.
    *
    * Note, the code currently assumes that the suffixes are all
    * lower case.
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

    /**
    * The OS repository root path.
    */
    private File m_path;
    }
