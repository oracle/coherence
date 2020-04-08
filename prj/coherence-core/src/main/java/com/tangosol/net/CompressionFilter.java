/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.io.WrapperStreamFactory;

import com.tangosol.run.xml.XmlConfigurable;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;


/**
* Provides a compression wrapper for an InputStream and OutputStream.
*
* @deprecated  As of Coherence 3.7
*
* @author cp  2002.08.20
*/
@Deprecated
public class CompressionFilter
        extends Base
        implements WrapperStreamFactory, XmlConfigurable
    {
    // ----- WrapperStreamFactory interface ---------------------------------

    /**
    * Requests an InputStream that wraps the passed InputStream.
    *
    * @param  stream  the java.io.InputStream to be wrapped
    *
    * @return an InputStream that delegates to ("wraps") the passed
    *         InputStream
    */
    public InputStream getInputStream(InputStream stream)
        {
        int cbBuffer = m_cbBuffer;
        if (m_fGzip)
            {
            // create the GZIP input stream
            if (cbBuffer == 0)
                {
                try
                    {
                    return new GZIPInputStream(stream);
                    }
                catch (IOException e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }
                }
            else
                {
                try
                    {
                    return new GZIPInputStream(stream, cbBuffer);
                    }
                catch (IOException e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        else
            {
            // create the inflating input stream
            Inflater inflater = new Inflater();
            return cbBuffer == 0 ?
                new InflaterInputShell(stream, inflater) :
                new InflaterInputShell(stream, inflater, cbBuffer);
            }
        }

    /**
    * Requests an OutputStream that wraps the passed OutputStream.
    *
    * @param  stream  the java.io.OutputStream to be wrapped
    *
    * @return an OutputStream that delegates to ("wraps") the passed
    *         OutputStream
    */
    public OutputStream getOutputStream(OutputStream stream)
        {
        int cbBuffer = m_cbBuffer;
        if (m_fGzip)
            {
            // create the GZIP output stream
            if (cbBuffer == 0)
                {
                try
                    {
                    return new GZIPOutputStream(stream);
                    }
                catch (IOException e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }
                }
            else
                {
                try
                    {
                    return new GZIPOutputStream(stream, cbBuffer);
                    }
                catch (IOException e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        else
            {
            // create an configure a Deflater
            Deflater deflater = new Deflater();

            if (m_nLevel != Deflater.DEFAULT_COMPRESSION)
                {
                deflater.setLevel(m_nLevel);
                }

            if (m_nStrategy != Deflater.DEFAULT_STRATEGY)
                {
                deflater.setStrategy(m_nStrategy);
                }

            // create the deflating output stream
            return cbBuffer == 0 ?
                new DeflaterOutputShell(stream, deflater) :
                new DeflaterOutputShell(stream, deflater, cbBuffer);
            }
        }


    // ----- XmlConfigurable interface --------------------------------------

    /**
    * Determine the current configuration of the object.
    *
    * @return the XML configuration or null
    */
    public XmlElement getConfig()
        {
        return m_xmlConfig;
        }

    /**
    * Specify the configuration for the object.
    *
    * @param xml  the XML configuration for the object
    *
    * @exception IllegalStateException  if the object is not in a state that
    *            allows the configuration to be set; for example, if the
    *            object has already been configured and cannot be reconfigured
    */
    public void setConfig(XmlElement xml)
        {
        m_xmlConfig = xml;
        if (xml != null)
            {
            m_cbBuffer = xml.getSafeElement("buffer-length").getInt(m_cbBuffer);
            m_fGzip    = false;

            // determine compression strategy (& whether to use GZIP)
            String sStrategy = xml.getSafeElement("strategy").getString("gzip");
            if (sStrategy.equals("default"))
                {
                m_nStrategy = Deflater.DEFAULT_STRATEGY;
                }
            else if (sStrategy.equals("huffman-only"))
                {
                m_nStrategy = Deflater.HUFFMAN_ONLY;
                }
            else if (sStrategy.equals("filtered"))
                {
                m_nStrategy = Deflater.FILTERED;
                }
            else if (sStrategy.equals("gzip"))
                {
                m_nStrategy = Deflater.DEFAULT_STRATEGY;
                m_fGzip     = true;
                }
            else
                {
                try
                    {
                    m_nStrategy = Integer.parseInt(sStrategy);
                    }
                catch (Exception e) {}
                }

            // determine compression level
            if (m_fGzip)
                {
                m_nLevel = Deflater.DEFAULT_COMPRESSION;
                }
            else
                {
                String sLevel = xml.getSafeElement("level").getString("default");
                if (sLevel.equals("default"))
                    {
                    m_nLevel = Deflater.DEFAULT_COMPRESSION;
                    }
                else if (sLevel.endsWith("compression"))
                    {
                    m_nLevel = Deflater.BEST_COMPRESSION;
                    }
                else if (sLevel.endsWith("speed"))
                    {
                    m_nLevel = Deflater.BEST_SPEED;
                    }
                else if (sLevel.equals("none"))
                    {
                    m_nLevel = Deflater.NO_COMPRESSION;
                    }
                else
                    {
                    try
                        {
                        m_nLevel = Integer.parseInt(sLevel);
                        }
                    catch (Exception e) {}
                    }
                }
            }
        }


    // ----- inner classes --------------------------------------------------

    /**
    * InflaterInputShell enforces the "end" call for the
    * corresponding Inflater object.
    */
    public static class InflaterInputShell
            extends InflaterInputStream
        {
        /**
        * Create a new input stream with the specified decompressor.
        *
        * @param stream   the input stream
        * @param inflater the decompressor ("inflater")
        */
        public InflaterInputShell(InputStream stream, Inflater inflater)
            {
            super(stream, inflater);
            }

        /**
        * Create a new input stream with the specified decompressor and
        * buffer size.
        *
        * @param stream   the input stream
        * @param inflater the decompressor ("inflater")
        * @param cbSize   the input buffer size
        */
        public InflaterInputShell(InputStream stream, Inflater inflater, int cbSize)
            {
            super(stream, inflater, cbSize);
            }

        /**
        * Close the input stream.
        */
        public void close()
                throws IOException
            {
            super.close();
            super.inf.end();
            }

        /**
        * Overrides the underlying {@link InflaterInputStream#read()}
        * implementation making the known bug fix in JDK 1.4.1
        * (<a href="http://developer.java.sun.com/developer/bugParade/bugs/4274779.html">
        * "InflaterInputStream is very memory inefficient"</a>)
        * retroactive for prior JDKs.
        */
        public int read() throws IOException
            {
            byte[] ab = m_singleByteBuf;
	        return read(ab, 0, 1) == -1 ? -1 : (ab[0] & 0xff);
            }

        // ----- data members ----------------------------------------------

        private byte[] m_singleByteBuf = new byte[1];
        }

    /**
    * DeflaterOutputShell enforces the "end" call for the
    * corresponding Deflater object.
    */
    public static class DeflaterOutputShell
            extends DeflaterOutputStream
        {
        /**
        * Create a new input stream with the specified decompressor.
        *
        * @param stream   the output stream
        * @param deflater the compressor ("deflater")
        */
        public DeflaterOutputShell(OutputStream stream, Deflater deflater)
            {
            super(stream, deflater);
            }

        /**
        * Create a new input stream with the specified decompressor and
        * buffer size.
        *
        * @param stream   the output stream
        * @param deflater the compressor ("inflater")
        * @param cbSize   the output buffer size
        */
        public DeflaterOutputShell(OutputStream stream, Deflater deflater, int cbSize)
            {
            super(stream, deflater, cbSize);
            }

        /**
        * Close the input stream.
        */
        public void close()
                throws IOException
            {
            super.close();
            super.def.end();
            }

        /**
        * Overrides the underlying {@link DeflaterOutputStream#write(int)}
        * implementation making it more memory efficient.
        *
        * @see <a href="http://developer.java.sun.com/developer/bugParade/bugs/181398.html">
        * DeflaterOutputStream is memory inefficient</a>
        *
        */
        public void write(int b)
                throws IOException
            {
            byte[] ab = m_singleByteBuf;
            ab[0] = (byte) (b & 0xff);
	        write(ab, 0, 1);
            }

        // ----- data members ----------------------------------------------

        private byte[] m_singleByteBuf = new byte[1];
        }


    // ----- data members ---------------------------------------------------

    /**
    * XML configuration for the filter.
    */
    private XmlElement m_xmlConfig;

    /**
    * Compression level; n/a for GZIP.
    */
    private int m_nLevel    = Deflater.DEFAULT_COMPRESSION;

    /**
    * Compression strategy; n/a for GZIP.
    */
    private int m_nStrategy = Deflater.DEFAULT_STRATEGY;

    /**
    * Length of desired buffer, or zero for default.
    */
    private int m_cbBuffer;

    /**
    * True if using GZIP, false otherwise.
    */
    private boolean m_fGzip;
    }
