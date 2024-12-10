/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

import com.tangosol.util.Base;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the FileHelper class.
 *
 * @author tam  2013.05.08
 */
public class FileHelperTest
        extends Base
    {
    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setupTest()
            throws IOException
        {
        m_file = FileHelper.createTempDir();
        }

    @After
    public void teardownTest()
        {
        try
            {
            FileHelper.deleteDir(m_file);
            }
        catch (IOException e)
            {
            // ignore
            }
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test the copyFile() method and exception handling.
     */
    @Test
    public void testCopyFile()
            throws IOException
        {
        // test copying a non-existent file
        File file    = new File(m_file, "non_existent_file");
        File fileNew = new File(new File(m_file, "dir"), "new_file");

        try
            {
            FileHelper.copyFile(file, fileNew);
            fail("Should not get to this point and should have thrown exception");
            }
        catch (Exception e)
            {
            // expected
            }

        assertFalse(fileNew.exists());

        // test copying a zero-length file
        file.createNewFile();
        FileHelper.copyFile(file, fileNew);
        assertTrue(fileNew.exists());
        assertEquals(0L, fileNew.length());
        assertTrue(file.isFile() == fileNew.isFile());

        fileNew.delete();
        assertFalse(fileNew.exists());

        // test copying a non-empty file
        final int FILE_SIZE = 1024;
        FileOutputStream out = new FileOutputStream(file);
        try
            {
            for (int i = 0; i < FILE_SIZE; ++i)
                {
                out.write((byte)(i % 8));
                }
            }
        finally
            {
            out.close();
            }

        FileHelper.copyFile(file, fileNew);
        assertTrue(fileNew.exists());
        assertEquals(FILE_SIZE, fileNew.length());
        assertTrue(file.isFile() == fileNew.isFile());

        FileInputStream in = new FileInputStream(fileNew);
        try
            {
            for (int i = 0; i < FILE_SIZE; ++i)
                {
                assertEquals(i % 8, in.read());
                }
            }
        finally
            {
            in.close();
            }
        }

    /**
     * Test the copyDir() method.
     */
    @Test
    public void testCopyDir()
            throws IOException
        {
        final File fileSrcDir         = new File(m_file, "src");
        final File fileSrc1           = new File(fileSrcDir, "test.txt");
        final File fileSrcDirChild    = new File(fileSrcDir, "child");
        final File fileSrc2           = new File(fileSrcDirChild, "test2.txt");
        final File fileTargetDir      = new File(m_file, "target");
        final File fileTargetDirChild = new File(fileTargetDir, "child");
        final File fileTarget1        = new File(fileTargetDir, "test.txt");
        final File fileTarget2        = new File(fileTargetDirChild, "test2.txt");

        try
            {
            FileHelper.copyDir(fileSrcDir, fileTargetDir);
            fail();
            }
        catch (IOException e)
            {
            // expected
            }

        fileSrcDir.mkdir();
        fileSrc1.createNewFile();
        fileSrcDirChild.mkdir();
        fileSrc2.createNewFile();

        FileHelper.copyDir(fileSrcDir, fileTargetDir);

        assertTrue(fileTargetDir.isDirectory());
        assertEquals(2, fileTargetDir.list().length);
        assertTrue(fileTargetDirChild.isDirectory());
        assertEquals(1, fileTargetDirChild.list().length);
        assertTrue(fileTarget1.isFile());
        assertTrue(fileTarget2.isFile());
        }

    /**
     * Test the moveFile() method and exception handling.
     */
    @Test
    public void testMoveFile()
            throws IOException
        {
        // test moving a non-existent file
        File file    = new File(m_file, "non_existent_file");
        File fileNew = new File(new File(m_file, "dir"), "new_file");

        try
            {
            FileHelper.moveFile(file, fileNew);
            fail("Should not get to this point and should have thrown exception");
            }
        catch (Exception e)
            {
            // expected
            }

        assertFalse(fileNew.exists());

        // test moving a zero-length file
        file.createNewFile();
        FileHelper.moveFile(file, fileNew);
        assertTrue(fileNew.exists());
        assertFalse(file.exists());
        assertEquals(0L, fileNew.length());
        fileNew.delete();

        // test moving a non-empty file
        final int FILE_SIZE = 1024;
        FileOutputStream out = new FileOutputStream(file);
        try
            {
            for (int i = 0; i < FILE_SIZE; ++i)
                {
                out.write((byte)(i % 8));
                }
            }
        finally
            {
            out.close();
            }

        FileHelper.moveFile(file, fileNew);
        assertTrue(fileNew.exists());
        assertFalse(file.exists());
        assertEquals(FILE_SIZE, fileNew.length());

        FileInputStream in = new FileInputStream(fileNew);
        try
            {
            for (int i = 0; i < FILE_SIZE; ++i)
                {
                assertEquals(i % 8, in.read());
                }
            }
        finally
            {
            in.close();
            }
        }

    /**
     * Test the moveDir() method.
     */
    @Test
    public void testMoveDir()
            throws IOException
        {
        File fileSrcDir         = new File(m_file, "src");
        File fileSrc1           = new File(fileSrcDir, "test.txt");
        File fileSrcDirChild    = new File(fileSrcDir, "child");
        File fileSrc2           = new File(fileSrcDirChild, "test2.txt");
        File fileTargetDir      = new File(m_file, "target");
        File fileTargetDirChild = new File(fileTargetDir, "child");
        File fileTarget1        = new File(fileTargetDir, "test.txt");
        File fileTarget2        = new File(fileTargetDirChild, "test2.txt");

        try
            {
            FileHelper.moveDir(fileSrcDir, fileTargetDir);
            fail();
            }
        catch (IOException e)
            {
            // expected
            }

        fileSrcDir.mkdir();
        fileSrc1.createNewFile();
        fileSrcDirChild.mkdir();
        fileSrc2.createNewFile();

        FileHelper.moveDir(fileSrcDir, fileTargetDir);

        assertFalse(fileSrcDir.exists());
        assertTrue(fileTargetDir.isDirectory());
        assertEquals(2, fileTargetDir.list().length);
        assertTrue(fileTargetDirChild.isDirectory());
        assertEquals(1, fileTargetDirChild.list().length);
        assertTrue(fileTarget1.isFile());
        assertTrue(fileTarget2.isFile());
        }

    /**
     * Test the sizeDir() method.
     */
    @Test
    public void testSizeDir()
            throws IOException
        {
    	File fileTestDir = new File(m_file, "testdir");
    	assertTrue(fileTestDir.mkdir());

    	// validate that we should see 0 size as nothing in directory yet
    	assertEquals(0L, FileHelper.sizeDir(fileTestDir));

    	// create a new file in this directory and write 1000 bytes of data to it
    	createAndWriteToFile(new File(fileTestDir, "filetest1.txt"), 1000);

    	assertEquals(1000L, FileHelper.sizeDir(fileTestDir));

    	// add another file of 500
    	createAndWriteToFile(new File(fileTestDir, "filetest2.txt"), 500);

    	// total number of bytes should now be 1500
    	assertEquals(1500L, FileHelper.sizeDir(fileTestDir));

    	// create a new sub-directory
    	File fileNewSubDir = new File(fileTestDir, "newsubdir");
    	assertTrue(fileNewSubDir.mkdir());

    	// add a new file to this directory of 500 bytes
    	createAndWriteToFile(new File(fileNewSubDir, "filetest3.txt"), 500);

    	// total number of bytes should now be 2000L and include sub-dirs
    	assertEquals(2000L, FileHelper.sizeDir(fileTestDir));

    	// try second level of sub-dirs
    	File fileThirdSubDir = new File(fileNewSubDir, "lastsubdir");
    	assertTrue(fileThirdSubDir.mkdir());

    	// add a new file to this directory of 250 bytes
    	createAndWriteToFile(new File(fileThirdSubDir, "filetest5.txt"), 250);

    	// total number of bytes should now be 2250L and include sub-dirs
    	assertEquals(2250L, FileHelper.sizeDir(fileTestDir));

    	// perform some validation against illegal arguments
    	long    cUsage = -1L;
    	boolean fError = false;
    	try
    	    {
            File fileTest = new File(fileTestDir, "thisisafile");
            fileTest.createNewFile();

            cUsage = FileHelper.sizeDir(fileTest);
    	    }
    	catch (Exception e)
    	    {
            if (e instanceof IllegalArgumentException)
                {
            	// caught exception - should now not happen due to COH-10194
                fError = true;
                }
            }

        // we should not throw an error now - refer: COH-10194
        assertFalse(fError);

        // usage should return 0L and not be initial value of -1L
        assertEquals(0L, cUsage );
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Create new file of the specified size.
     *
     * @param file  the file to create
     * @param cb    the number of bytes to write to the file
     *
     * @throws IOException on exception populating the file
     */
    private void createAndWriteToFile(File file, int cb)
            throws IOException
        {
        if (file.createNewFile())
            {
            WriteBuffer.BufferOutput out = new WrapperBufferOutput(
                    new DataOutputStream(new FileOutputStream(file)));
            try
                {
                out.writeBuffer(Base.getRandomBinary(cb, cb));
                }
            finally
                {
                out.close();
                }
            }
        }

    // ----- data members ---------------------------------------------------

    protected File m_file;
    }
