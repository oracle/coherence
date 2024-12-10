/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.util.Base;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.lang.reflect.Method;

import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;


/**
* Manages a ByteBuffer on a file.
*
* @see ByteBuffer
*
* @author cp  2002.09.18
*
* @since Coherence 2.2
*/
public class MappedBufferManager
        extends AbstractBufferManager
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a MappedBufferManager using defaults.
    */
    public MappedBufferManager()
        {
        this(DEFAULT_MIN_SIZE, DEFAULT_MAX_SIZE, null);
        }

    /**
    * Construct a MappedBufferManager that supports a buffer of a certain
    * initial and maximum size.
    *
    * @param cbInitial  the initial size
    * @param cbMaximum  the maximum size
    * @param file       the file to use, or the directory to use, or null
    *                   to generate a temporary file in the default location
    */
    public MappedBufferManager(int cbInitial, int cbMaximum, File file)
        {
        super(cbInitial, cbMaximum);
        configureFile(file);
        allocateBuffer();
        }


    // ----- internal -------------------------------------------------------

    /**
    * Configure the buffer manager based on the passed file, which may be
    * null to imply a default temp file, a directory to imply that a file
    * be created in that directory, or it may be the temp file itself.
    *
    * @param file  the file to use, or the directory to use, or null
    *              to generate a temporary file in the default location
    */
    protected void configureFile(File file)
        {
        try
            {
            boolean fTemp = false;

            // ensure the temp file, and if it is created here, make sure
            // it is registered for removal
            if (file == null || file.isDirectory())
                {
                file  = File.createTempFile("coherence", null, file);
                fTemp = true;
                }
            else if (!file.exists())
                {
                file.createNewFile();
                fTemp = true;
                }

            // configure random access to the file
            RandomAccessFile hfile = new RandomAccessFile(file, "rw");
            setFile(hfile);

            if (fTemp)
                {
                file.deleteOnExit();
                m_fileTemp = file;
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Obtain the RandomAccessFile that the buffer is constructed on.
    *
    * @return the RandomAccessFile object
    */
    protected RandomAccessFile getFile()
        {
        return m_hfile;
        }

    /**
    * Specify the RandomAccessFile that the buffer will be constructed on.
    *
    * @param file  the RandomAccessFile object
    */
    protected void setFile(RandomAccessFile file)
        {
        m_hfile = file;
        }

    /**
    * Allocate a new buffer, copying old data if there is any.
    *
    * @see FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long) FileChannel.map()
    */
    protected void allocateBuffer()
        {
        try
            {
            RandomAccessFile hfile = getFile();

            int cbOld = (int) hfile.length();
            int cbNew = getCapacity();

            if (cbNew != cbOld)
                {
                setBuffer(null);

                // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4857305
                System.gc();
                Thread.yield();

                try
                    {
                    hfile.setLength(cbNew);
                    }
                catch (IOException e)
                    {
                    // only "worry" if the file cannot grow
                    if (cbNew > cbOld)
                        {
                        throw e;
                        }
                    }
                setBuffer(hfile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0L, cbNew));
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- life-cycle support ---------------------------------------------

    /**
    * Close the underlying resources.
    */
    public void close()
        {
        try
            {
            RandomAccessFile hfile = m_hfile;
            if (hfile != null)
                {
                // need to close the file to have it deleted on exit:
                //   http://developer.java.sun.com/developer/bugParade/bugs/4171239.html
                // however, this may not work on Windows:
                //   http://developer.java.sun.com/developer/bugParade/bugs/4715154.html
                // attempt to unmap before close as suggested in the bug link above
                try
                    {
                    ByteBuffer buffer = getBuffer();
                    Method getCleanerMethod = buffer.getClass().getMethod("cleaner");
                    getCleanerMethod.setAccessible(true);
                    Object cleaner = getCleanerMethod.invoke(buffer);
                    Method cleanMethod = cleaner.getClass().getMethod("clean");
                    cleanMethod.invoke(cleaner);
                    }
                catch (Throwable t) {}

                hfile.close();
                }

            File fileTemp = m_fileTemp;
            if (fileTemp != null)
                {
                if (fileTemp.delete())
                    {
                    m_fileTemp = null;
                    }
                else
                    {
                    // Java BugID: 4715154; Closed, will not be fixed on windows
                    log("MappedBufferManager: " + "failed to remove " + fileTemp);
                    }
                }
            }
        catch (Exception e) {}

        m_hfile = null;
        }

    /**
    * Perform cleanup during garbage collection.
    */
    protected void finalize()
        {
        close();
        }


    // ----- constants ------------------------------------------------------

    /**
    * Default minimum size for a memory mapped file.
    */
    public static final int DEFAULT_MIN_SIZE  = 0x4000;

    /**
    * Default maximum size for a memory mapped file.
    */
    public static final int DEFAULT_MAX_SIZE  = Integer.MAX_VALUE;


    // ----- data members ---------------------------------------------------

    /**
    * The underlying temporary File.
    */
    private File m_fileTemp;

    /**
    * The underlying RandomAccessFile.
    */
    private RandomAccessFile m_hfile;
    }
