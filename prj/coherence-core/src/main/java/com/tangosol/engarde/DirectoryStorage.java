/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.engarde;


import com.tangosol.util.Base;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.StringTable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Enumeration;

import java.util.jar.JarFile;


/**
* Directory tree that contains a Java application components. 
*
* @version 1.00 10/08/01
* @author gg
*/
public class DirectoryStorage
        extends    Base
        implements ApplicationReader, ApplicationWriter
    {
    /**
    * Construct a new DirectoryStorage
    *
    * @param dirRoot  root directory
    * @param iAccess  access mode could be one of of the following values:
    *                 ACCESS_READ, ACCESS_WRITE or ACCESS_READ | ACCESS_WRITE
    */
    public DirectoryStorage(File dirRoot, int iAccess)
            throws IOException
        {
        azzert((iAccess & (ACCESS_READ | ACCESS_WRITE)) != 0, "Illegal access mode: " + iAccess);
        
        if (dirRoot == null || !dirRoot.isDirectory())
            {
            throw new IOException("Not a directory: " + dirRoot);
            }

        m_dirRoot     = dirRoot.getCanonicalFile();
        m_fAllowRead  = (iAccess & ACCESS_READ)  != 0;
        m_fAllowWrite = (iAccess & ACCESS_WRITE) != 0;
        }

    /**
    * Get the root directory
    */
    public File getRoot()
        {
        return m_dirRoot;
        }

    /**
    * Provide a human-readable description of the storage.
    *
    * @return a string describing the property
    */
    public String toString()
        {
        return CLASS + ": " + m_dirRoot;
        }

    // ---- ApplicationReader interface ---------------------------------------

    /**
    * Return an input stream for reading the content of the specified
    * application entry.
    *
    * @param  entry the application entry
    *
    * @return an input stream for reading the content of the specified
    *         application entry or null if not found
    * 
    * @exception IOException if an I/O error has occurred
    */
    public InputStream getInputStream(ApplicationEntry entry)
            throws IOException
        {
        if (!m_fAllowRead)
            {
            throw new IllegalStateException(
                "ApplicationReader operation is not allowed");
            }

        File file = new File(m_dirRoot, entry.getName());
        
        return file.isFile() ? (InputStream)
            new FileInputStream(file) :  new ByteArrayInputStream(new byte[0]);
        }

    /**
    * Return an ApplicationReader that represents an application contained
    * within this application as a specified application entry
    *
    * @param  sName name of the application entry
    *
    * @return an ApplicationReader object for reading the content of 
    *         the contained application or null if not found
    * 
    * @exception IOException if an I/O error has occurred
    */
    public ApplicationReader extractApplication(String sName)
            throws IOException
        {
        if (!m_fAllowRead)
            {
            throw new IllegalStateException(
                "ApplicationReader operation is not allowed");
            }

        File file = new File(m_dirRoot, sName);
        if (file.exists())
            {
            return file.isDirectory() ? (ApplicationReader)
                new DirectoryStorage(file, 
                    m_fAllowWrite ? ACCESS_READ | ACCESS_WRITE : ACCESS_READ) :
                new JarStorage(new JarFile(file.getCanonicalFile()));
            }
        return null;
        }


    /**
    * Return an ApplicationEntry object for the given name
    *
    * @param sName  name of the application entry
    *
    * @return ApplicationEntry object for the given entry name or
    *         null if not found
    */
    public ApplicationEntry getEntry(String sName)
        {
        if (!m_fAllowRead)
            {
            throw new IllegalStateException(
                "ApplicationReader operation is not allowed");
            }

        File file = new File(m_dirRoot, sName);

        return file.exists() ? new Entry(sName, file.isDirectory()) : null;
        }

    /**
    * Return an enumeration of the application entries
    *
    * @return Enumeration of ApplicationEntry objects
    */
    public Enumeration entries()
        {
        if (!m_fAllowRead)
            {
            throw new IllegalStateException(
                "ApplicationReader operation is not allowed");
            }
        
        StringTable tbl = new StringTable();

        collectEntries(m_dirRoot, tbl);

        return tbl.elements();
        }

    private void collectEntries(File dir, StringTable tbl)
        {
        File[] aFile = dir.listFiles();
        if (aFile != null)
            {
            String sRoot   = m_dirRoot.getPath() + File.separatorChar;
            int    cchRoot = sRoot.length();
            for (int i = 0, c = aFile.length; i < c; i++)
                {
                File   file  = aFile[i];
                String sPath = file.getPath();

                azzert(sPath.startsWith(sRoot));

                sPath = sPath.substring(cchRoot).replace(File.separatorChar, '/');
                if (file.isFile())
                    {
                    tbl.put(sPath, new Entry(sPath, false));
                    }
                else if (file.isDirectory())
                    {
                    tbl.put(sPath, new Entry(sPath, true));
                    collectEntries(file, tbl);
                    }
                }
            }
        }

    /**
    * Close the application reader and release all used resources
    */
    public void close()
        {
        m_fAllowRead = false;

        if (m_fAllowWrite)
            {
            try
                {
                closeEntry();
                }
            catch (IOException e) {}

            m_fAllowWrite = false;
            }
        }


    // ---- ApplicationWriter interface ---------------------------------------

    /**
    * Create a new application entry with all attributes of the specified
    * entry and prepare to start writing the entry data.
    * Close the current entry if still active.
    *
    * @param entry an application entry
    *
    * @return a newly created AppplicationEntry
    *
    * @exception  IOException if an I/O error has occurred
    */
    public ApplicationEntry createEntry(ApplicationEntry entry)
            throws IOException
        {
        if (!m_fAllowWrite)
            {
            throw new IllegalStateException(
                "ApplicationWriter operation is not allowed");
            }

        Entry entryCurr = m_currEntry;
        if (entryCurr != null)
            {
            closeEntry();
            }

        entryCurr = ensureEntry(entry.getName(),
            entry instanceof Entry && ((Entry) entry).isDirectory());

        entryCurr.setTime(entry.getTime());
        
        return entryCurr;
        }

    /**
    * Create a new application entry with a given name and prepare to start
    * writing the entry data.  Close the current entry if still active.
    *
    * @param sName the application entry name
    *
    * @return a newly created AppplicationEntry
    *
    * @exception  IOException if an I/O error has occurred
    */
    public ApplicationEntry createEntry(String sName)
            throws IOException
        {
        if (!m_fAllowWrite)
            {
            throw new IllegalStateException(
                "ApplicationWriter operation is not allowed");
            }

        Entry entry = m_currEntry;
        if (entry != null)
            {
            closeEntry();
            }

        return ensureEntry(sName, sName.endsWith("/"));
        }

    private Entry ensureEntry(String sName, boolean fDir)
            throws IOException
        {
        // the entry could refer to a symbolic link; resolve the actual
        // (canonical) file before attempting to create parent directories
        File file = new File(m_dirRoot, sName).getCanonicalFile();
        File dir  = file.getParentFile();

        if (!dir.exists() && !dir.mkdirs())
            {
            throw new IOException(CLASS + 
                ": Failure to create directory: " + dir);
            }

        m_currOut = fDir ? NullImplementation.getOutputStream() : 
            new FileOutputStream(file);
        m_currEntry = new Entry(sName, fDir);

        return m_currEntry;
        }

    /**
    * Write an array of bytes to the current application entry data.
    * This method will block until all the bytes are written.
    *
    * @param ab  the data to be written
    * @param of  the offset into the array
    * @param cb  the number of bytes to write
    * 
    * @exception  IOException if an I/O error has occurred
    */
    public void writeEntryData(byte[] ab, int of, int cb)
            throws IOException
        {
        if (!m_fAllowWrite)
            {
            throw new IllegalStateException(
                "ApplicationWriter operation is not allowed");
            }

        if (m_currOut == null)
            {
            throw new IllegalStateException("Request is out of sequence");
            }
        
        if (m_currEntry.isDirectory() && cb > 0)
            {
            throw new IllegalStateException(
                "Illegal write operation to a directory: " + m_currEntry);
            }

        if (cb > 0)
            {
            m_currOut.write(ab, of, cb);
            }
        }   

    /**
    * Close the current application entry.
    *
    * @exception  IOException if an I/O error has occurred
    */
    public void closeEntry()
            throws IOException
        {
        if (!m_fAllowWrite)
            {
            throw new IllegalStateException(
                "ApplicationWriter operation is not allowed");
            }

        Entry entry = m_currEntry;

        if (entry != null)
            {
            m_currOut.close();
            entry.getFile().setLastModified(entry.getTime());

            m_currOut   = null;
            m_currEntry = null;
            }
        }

    // ---- data fields and constants ----------------------------------------

    /**
    * Specifies whether or not the "read" operations are allowed
    */
    private boolean m_fAllowRead;

    /**
    * Specifies whether or not the "write" operations are allowed
    */
    private boolean m_fAllowWrite;

    /**
    * Specifies the root directory of this storage
    */
    private File    m_dirRoot;

    /**
    * Specifies the currently "active" entry
    */
    private Entry   m_currEntry;
    
    /**
    * Specifies the currently "active" file output stream
    */
    private OutputStream m_currOut;

    /**
    * ACCESS_READ denotes the read only mode of operations
    */
    public static final int ACCESS_READ  = 1;

    /**
    * ACCESS_WRITE denotes the read only mode of operations
    */
    public static final int ACCESS_WRITE = 2;
    
    /**
    * The name of this class.
    */
    private static final String CLASS = "DirectoryStorage";


    // ---- inner class -------------------------------------------------------
    
    public class Entry
            implements ApplicationEntry
        {
        /**
        * Construct a new entry for the specified path
        */
        protected Entry(String sPath, boolean fDir)
            {
            azzert(sPath != null);
            m_sPath = sPath;
            m_fDir  = fDir;
            }

        /**
        * Return the file object for this entry.
        *
        * @return the name of the entry
        */
        public File getFile()
            {
            return new File(m_dirRoot, m_sPath);
            }

        /**
        * Check whether or not this entry represents a directory.
        *
        * @return true iff this entry represents a directory
        */
        public boolean isDirectory()
            {
            return m_fDir;
            }

        // ---- ApplicationEntry interface ------------------------------------

        /**
        * Return the name of the entry.
        *
        * @return the name of the entry
        */
        public String getName()
            {
            return m_sPath;
            }

        /**
        * Return the modification time of the entry, or -1 if not specified.
        *
        * @return the modification time of the entry, or -1 if not specified
        */
        public long getTime()
            {
            return m_lTime == 0 ? getFile().lastModified() : m_lTime;
            }

        /**
        * Set the modification time of the entry.
        *
        * @param lTime the entry modification time in number of milliseconds
        *		       since the epoch
        */
        public void setTime(long lTime)
            {
            m_lTime = lTime;
            }

        /**
        * Return the [uncompressed] size of the entry data, or -1 if not known.
        *
        * @return the size of the entry data, or -1 if not known
        */
        public long getSize()
            {
            return getFile().length();
            }


        /**
        * Set the [uncompressed] size of the entry data.
        *
        * @param lSize the size in bytes
        */
        public void setSize(long lSize)
            {
            // no op
            }

        // ----- Object methods -------------------------------------------

        /**
        * Provide a human-readable description of the storage.
        *
        * @return a string describing the property
        */
        public String toString()
            {
            return "DirectoryStorage$Entry: " + getFile();
            }

        // ---- data fields --------------------------------------------------

        /**
        * Path relative to the storage root representing this Entry
        */
        private String m_sPath;

        /**
        * Directory indicator.
        */
        private boolean m_fDir;

        /**
        * Cached value of time attribute
        */
        private long m_lTime;
        }
    }
