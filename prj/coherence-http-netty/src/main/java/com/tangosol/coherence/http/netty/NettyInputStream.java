package com.tangosol.coherence.http.netty;

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

import java.io.IOException;
import java.io.InputStream;

import java.util.concurrent.LinkedBlockingDeque;


/**
 * Input stream which serves as Request entity input.
 * <p>
 * Converts Netty NIO buffers to an input streams and stores them in a queue,
 * waiting for Jersey to process it.
 *
 * <pre>
 * NOTE: This class is copied from Jersey and will be removed as soon
 *       as it is possible to migrate to Jersey 2.24 or later.
 * </pre>
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class NettyInputStream
        extends InputStream
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Constructs a new {@link NettyInputStream} instance.
     *
     * @param isList the queue for pending reads
     */
    public NettyInputStream(LinkedBlockingDeque<InputStream> isList)
        {
        this.isList = isList;
        }


    private interface ISReader
        {
        int readFrom(InputStream take) throws IOException;
        }

    private int readInternal(ISReader isReader) throws IOException
        {

        if (m_fEnd)
            {
            return -1;
            }

        InputStream take;
        try
            {
            take = isList.take();

            if (checkEndOfInput(take))
                {
                return -1;
                }

            int read = isReader.readFrom(take);

            if (take.available() > 0)
                {
                isList.addFirst(take);
                }
            else
                {
                take.close();
                }

            return read;
            }
        catch (InterruptedException e)
            {
            throw new IOException("Interrupted.", e);
            }
        }

    // ---- methods from InputStream ----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] abDest, int cbOff, int cbLen) throws IOException
        {
        return readInternal(take -> take.read(abDest, cbOff, cbLen));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException
        {
        return readInternal(InputStream::read);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException
        {
        InputStream peek = isList.peek();
        if (peek != null)
            {
            return peek.available();
            }

        return 0;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
        {
        isList.forEach(is ->
                       {
                       try
                           {
                           is.close();
                           }
                       catch (IOException e)
                           {
                           e.printStackTrace();
                           }
                       });
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Check if the specified {@link InputStream} represents the EOS.
     *
     * @param inToCheck the {@link InputStream} to check
     *
     * @return <code>true</code> if there is no more input, otherwise <code>false</code>
     *
     * @throws IOException if an error occurred at some point of the input
     *                     queuing process
     */
    private boolean checkEndOfInput(InputStream inToCheck) throws IOException
        {
        if (inToCheck == END_OF_INPUT)
            {
            m_fEnd = true;
            return true;
            }
        else if (inToCheck == END_OF_INPUT_ERROR)
            {
            m_fEnd = true;
            throw new IOException("Connection was closed prematurely.");
            }
        return false;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Flag indicating EOS status.
     */
    private volatile boolean m_fEnd = false;

    /**
     * End of input.
     */
    public static final InputStream END_OF_INPUT = new InputStream()
        {
        @Override
        public int read() throws IOException
            {
            return 0;
            }

        @Override
        public String toString()
            {
            return "END_OF_INPUT " + super.toString();
            }
        };

    /**
     * Unexpected m_fEnd of input.
     */
    public static final InputStream END_OF_INPUT_ERROR = new InputStream()
        {
        @Override
        public int read() throws IOException
            {
            return 0;
            }

        @Override
        public String toString()
            {
            return "END_OF_INPUT_ERROR " + super.toString();
            }
        };

    private final LinkedBlockingDeque<InputStream> isList;
    }
