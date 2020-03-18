/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.engarde;


import com.tangosol.util.Base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Enumeration;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
* Jar file that contains a Java application components. 
*
* @version 1.00 10/08/01
* @author gg
*/
public class JarStorage
        extends    Base
        implements ApplicationReader, ApplicationWriter
    {
    /**
    * Construct a new JarStorage reader
    *
    * @param jarFile  JarFile object wrapped by this JarStorage
    */
    public JarStorage(JarFile jarFile)
            throws IOException
        {
        azzert(jarFile != null);
        
        m_jar        = jarFile;
        m_fAllowRead = true;
        }

    /**
    * Construct a new JarStorage writer
    *
    * @param fileJar   file object this JarStorage writer is writing to
    * @param manifest  manifest to use while creating a new jar
    */
    public JarStorage(File fileJar, Manifest manifest)
            throws IOException
        {
        FileOutputStream stream = new FileOutputStream(fileJar);

        m_fileOut = fileJar;
        m_jarOut  = manifest == null ?
            new JarOutputStream(stream) : new JarOutputStream(stream, manifest);
        m_fAllowWrite = true;
        }


    /**
    * Get the JarFile object for the Java archive (reader) represented
    * by this JarStorage
    */
    public JarFile getJar()
        {
        return m_jar;
        }

    /**
    * Get the JarOutputStream object for the Java archive (writer) represented
    * by this JarStorage
    */
    public JarOutputStream getJarOut()
        {
        return m_jarOut;
        }

    /**
    * Provide a human-readable description of the storage.
    *
    * @return a string describing the property
    */
    public String toString()
        {
        return "JarStorage" +
            (m_jar     != null ? " Reader: " + m_jar    .getName() : "") +
            (m_fileOut != null ? " Writer: " + m_fileOut.getName() : "");
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
                "ApplicationReader operation is not allowed at: " + this);
            }

        JarEntry je = entry instanceof Entry ?
            ((Entry) entry).getJarEntry() : m_jar.getJarEntry(entry.getName());

        return je == null ? null : m_jar.getInputStream(je);
        }

    /**
    * Return an ApplicationReader that represents an application containted
    * within this application as a specified application entry
    *
    * Note: caller is responsible for removing the extracted jar file
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
                "ApplicationReader operation is not allowed at: " + this);
            }

        JarFile jar = com.tangosol.dev.component.JarStorage.extractJar(m_jar, sName);
        
        return jar == null ? null : new JarStorage(jar);
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
                "ApplicationReader operation is not allowed at: " + this);
            }

        JarEntry je = m_jar.getJarEntry(sName);
 
        return je == null ? null : new Entry(je);
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
                "ApplicationReader operation is not allowed at: " + this);
            }

        final Enumeration enmr = m_jar.entries();
        return new Enumeration()
            {
            public boolean hasMoreElements()
                {
                return enmr.hasMoreElements();
                }
            public Object nextElement()
                {
                return new Entry((JarEntry) enmr.nextElement());
                }
            };
        }

    /**
    * Close the application reader and release all used resources
    */
    public void close()
        {
        try
            {
            if (m_fAllowRead)
                {
                JarFile jar = m_jar;
                if (jar != null)
                    {
                    jar.close();

                    m_jar = null;
                    }
                m_fAllowRead = false;
                }

            if (m_fAllowWrite)
                {
                closeEntry();

                JarOutputStream jarOut = m_jarOut;
                if (jarOut != null)
                    {
                    jarOut.close();

                    m_jarOut  = null;
                    m_fileOut = null;
                    }
                m_fAllowWrite = false;
                }
            }
        catch (IOException e) {}
        }

    // ---- ApplicationWriter interface ---------------------------------------

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
                "ApplicationWriter operation is not allowed at: " + this);
            }

        Entry entry = m_currEntry;
        if (entry != null)
            {
            closeEntry();
            }

        JarEntry je = new JarEntry(sName);
        m_currEntry = entry = new Entry(je);

        return entry;
        }

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
                "ApplicationWriter operation is not allowed at: " + this);
            }

        Entry entryCurr = m_currEntry;
        if (entryCurr != null)
            {
            closeEntry();
            }

        JarEntry je;
        if (entry instanceof Entry)
            {
            je = new JarEntry(((Entry) entry).getJarEntry());

            // the compression algorithm could be different
            je.setCompressedSize(-1);
            }
        else
            {
            je = new JarEntry(entry.getName());

            je.setTime(entry.getTime());
            je.setSize(entry.getSize());
            je.setMethod(JarOutputStream.DEFLATED);
            }
        m_currEntry = entryCurr = new Entry(je);

        return entryCurr;
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
                "ApplicationWriter operation is not allowed: " + this);
            }

        Entry entry = m_currEntry;
        if (entry == null)
            {
            throw new IllegalStateException("Request is out of sequence");
            }

        JarOutputStream jarOut = m_jarOut;
        if (!m_fAdded)
            {
            jarOut.putNextEntry(entry.getJarEntry());

            m_fAdded = true;
            }

        if (cb > 0)
            {
            jarOut.write(ab, of, cb);
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
                "ApplicationWriter operation is not allowed: " + this);
            }

        Entry entry = m_currEntry;

        if (entry != null)
            {
            m_jarOut.closeEntry();

            m_currEntry = null;
            m_fAdded    = false;
            }
        }


    // ---- data fields and constants ----------------------------------------

    /**
    * Specifies whether or not the "read" operations are allowed
    */
    private boolean         m_fAllowRead;

    /**
    * Specifies whether or not the "write" operations are allowed
    */
    private boolean         m_fAllowWrite;

    /**
    * Specifies the root directory of this storage
    */
    private JarFile          m_jar;
    
    /**
    * Specifies the currently "active" output file
    */
    private File             m_fileOut;

    /**
    * Specifies the currently "active" file output stream
    */
    private JarOutputStream  m_jarOut;

    /**
    * Specifies the currently "active" entry
    */
    private Entry            m_currEntry;

    /**
    * Specifies whether the currently entry has been added to
    * the jar output stream
    */
    private boolean          m_fAdded;

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
    private static final String CLASS = "JarStorage";


    // ---- inner class -------------------------------------------------------
    
    static public class Entry
            implements ApplicationEntry
        {
        /**
        * Construct a new entry backed up by the specified JarEntry
        */
        protected Entry(JarEntry entry)
            {
            azzert(entry != null);
            m_entry = entry;
            }

        /**
        * Get the JarEntry object backing up this entry
        */
        public JarEntry getJarEntry()
            {
            return m_entry;
            }

        // ---- ApplicationEntry interface ------------------------------------

        /**
        * Return the name of the entry.
        *
        * @return the name of the entry
        */
        public String getName()
            {
            return m_entry.getName();
            }

        /**
        * Return the modification time of the entry, or -1 if not specified.
        *
        * @return the modification time of the entry, or -1 if not specified
        */
        public long getTime()
            {
            return m_entry.getTime();
            }

        /**
        * Set the modification time of the entry.
        *
        * @param lTime the entry modification time in number of milliseconds
        *		   since the epoch
        */
        public void setTime(long lTime)
            {
            m_entry.setTime(lTime);
            }

        /**
        * Return the [uncompressed] size of the entry data, or -1 if not known.
        *
        * @return the size of the entry data, or -1 if not known
        */
        public long getSize()
            {
            return m_entry.getSize();
            }

        /**
        * Set the [uncompressed] size of the entry data.
        *
        * @param lSize the size in bytes
        */
        public void setSize(long lSize)
            {
            JarEntry je = m_entry;

            je.setMethod(JarOutputStream.DEFLATED);
            je.setSize(lSize);
            je.setCompressedSize(-1);
            }

        // ----- Object methods -------------------------------------------

        /**
        * Provide a human-readable description of the storage.
        *
        * @return a string describing the property
        */
        public String toString()
            {
            return "JarStorage$Entry: " + getName();
            }

        // ---- data fields --------------------------------------------------

        /**
        * File object representing this Entry
        */
        private JarEntry m_entry;
        }
    }
