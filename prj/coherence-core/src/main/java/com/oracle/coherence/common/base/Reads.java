/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import java.net.URL;
import java.net.URLConnection;

import static com.oracle.coherence.common.base.Assertions.azzert;

/**
 * Class for providing read functionality.
 *
 * @author cp  2000.08.02
 * @since 20.06
 */
@SuppressWarnings("DuplicatedCode")
public abstract class Reads
    {
    // ----- read support ----------------------------------------------

    /**
     * Read the contents out of the passed stream into the passed byte array
     * and return the length read.  This method will read up to the number of
     * bytes that can fit into the passed array.
     *
     * @param stream  a java.io.InputStream object to read from
     * @param ab      a byte array to read into
     *
     * @return  the number of bytes read from the InputStream and stored into
     *          the passed byte array
     *
     * @throws IOException  if an error occurs
     */
    public static int read(InputStream stream, byte[] ab)
            throws IOException
        {
        final int MAX  = ab.length;
        int       cb   = 0;
        boolean   fEOF = false;
        while (!fEOF && cb < MAX)
            {
            int cbBlock = stream.read(ab, cb, MAX - cb);
            if (cbBlock < 0)
                {
                fEOF = true;
                }
            else
                {
                cb += cbBlock;
                }

            }
        return cb;
        }

    /**
     * Read the contents out of the passed stream and return the result as a
     * byte array.
     *
     * @param stream  a java.io.InputStream object to read from
     *
     * @return  a byte array containing the contents of the passed InputStream
     *
     * @throws IOException  if an error occurs
     */
    public static byte[] read(InputStream stream)
            throws IOException
        {
        final int             BLOCK     = 1024;
        byte[]                ab        = new byte[BLOCK];
        ByteArrayOutputStream streamBuf = new ByteArrayOutputStream(BLOCK);
        while (true)
            {
            try
                {
                int cb = stream.read(ab, 0, BLOCK);
                if (cb < 0)
                    {
                    break;
                    }
                else if (cb > 0)
                    {
                    streamBuf.write(ab, 0, cb);
                    }
                }
            catch (EOFException e)
                {
                break;
                }
            }
        stream.close();
        return streamBuf.toByteArray();
        }

    /**
     * Read the contents out of the passed stream and return the result as a
     * byte array.
     *
     * @param stream  a java.io.DataInput object to read from
     *
     * @return  a byte array containing the contents of the passed stream
     *
     * @throws IOException  if an error occurs
     */
    public static byte[] read(DataInput stream)
            throws IOException
        {
        if (stream instanceof InputStream)
            {
            return read((InputStream) stream);
            }

        final int BLOCK = 1024;
        int       cb    = 0;
        byte[]    ab    = new byte[BLOCK];
        ByteArrayOutputStream streamBuf = null;
        try
            {
            //noinspection InfiniteLoopStatement
            while (true)
                {
                ab[cb++] = stream.readByte();
                if (cb == BLOCK)
                    {
                    if (streamBuf == null)
                        {
                        streamBuf = new ByteArrayOutputStream(BLOCK);
                        }
                    streamBuf.write(ab, 0, cb);
                    cb = 0;
                    }
                }
            }
        catch (EOFException ignore)
            {
            // end of input reached; eat it
            }

        if (streamBuf == null)
            {
            // contents fit in first block
            if (cb == BLOCK)
                {
                // perfect fit
                return ab;
                }
            // shrink block and return
            byte[] abNew = new byte[cb];
            System.arraycopy(ab, 0, abNew, 0, cb);
            return abNew;
            }

        // contents span multiple blocks
        if (cb != 0)
            {
            // copy remainder into streamBuf
            streamBuf.write(ab, 0, cb);
            }
        return streamBuf.toByteArray();
        }

    /**
     * Read the contents out of the passed stream and return the result as a
     * byte array.
     *
     * @param stream  a java.io.DataInputStream object to read from
     *
     * @return  a byte array containing the contents of the passed InputStream
     *
     * @throws IOException  if an error occurs
     */
    public static byte[] read(DataInputStream stream)
            throws IOException
        {
        // this method resolves the ambiguity between the read(InputStream)
        // and read(DataInput) methods for DataInputStreams and its derivatives
        return read((InputStream) stream);
        }

    /**
     * Read the contents out of the passed Reader and return the result as a
     * String.
     *
     * @param reader  a java.io.Reader object to read from
     *
     * @return  a String containing the contents of the passed Reader
     *
     * @throws IOException  if an error occurs
     */
    public static String read(Reader reader)
            throws IOException
        {
        final int       BLOCK  = 1024;
        char[]          ach  = new char[BLOCK];
        CharArrayWriter writer = new CharArrayWriter(BLOCK);
        while (true)
            {
            try
                {
                int cch = reader.read(ach, 0, BLOCK);
                if (cch < 0)
                    {
                    break;
                    }
                else if (cch > 0)
                    {
                    writer.write(ach, 0, cch);
                    }
                }
            catch (EOFException e)
                {
                break;
                }
            }
        reader.close();
        return writer.toString();
        }

    /**
     * Read the contents out of the specified file and return the result as a
     * byte array.
     *
     * @param file  the java.io.File object to read the contents of
     *
     * @return the contents of the specified File as a byte array
     *
     * @throws IOException  if an error occurs
     */
    public static byte[] read(File file)
            throws IOException
        {
        if (file == null || !file.exists() || !file.isFile())
            {
            return null;
            }

        long cbFile = file.length();
        azzert(cbFile < 0x7FFFFFFFL);

        int    cb = (int) cbFile;
        byte[] ab = new byte[cb];

        try (InputStream in = new FileInputStream(file))
            {
            int cbRead = read(in, ab);
            azzert(cb == cbRead);
            }

        return ab;
        }

    /**
     * Read the contents of the specified URL and return the result as a
     * byte array.
     *
     * @param url  the java.net.URL object to read the contents of
     *
     * @return the contents of the specified URL as a byte array
     *
     * @throws IOException  if an error occurs
     */
    @SuppressWarnings("UnusedAssignment")
    public static byte[] read(URL url)
            throws IOException
        {
        if (url == null)
            {
            return null;
            }

        URLConnection con = url.openConnection();
        int           cb  = con.getContentLength();
        byte[]        ab  = null;
        try (InputStream in = con.getInputStream())
            {
            if (cb == -1)
                {
                // unknown content length
                ab = read(in);
                }
            else
                {
                // known content length (optimize by pre-allocating fully)
                ab = new byte[cb];
                int cbRead = read(in, ab);
                azzert(cb == cbRead);
                }
            }

        return ab;
        }
    }
