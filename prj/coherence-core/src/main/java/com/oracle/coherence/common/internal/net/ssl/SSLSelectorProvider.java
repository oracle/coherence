/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.ssl;

import java.net.ProtocolFamily;
import java.nio.channels.spi.SelectorProvider;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.IOException;


/**
 * SSLSelectorProvider is a provider of Selectors for SSL based channels.
 *
 * @author mf 2011.02.07
 */
public class SSLSelectorProvider
        extends SelectorProvider
    {
    // ----- constructors -------------------------------------------

    public SSLSelectorProvider(SelectorProvider delegate)
        {
        m_delegate = delegate;
        }

    // ----- SelectorProvider interface -----------------------------

    @Override
    public DatagramChannel openDatagramChannel()
            throws IOException
        {
        throw new UnsupportedOperationException();
        }

    public DatagramChannel openDatagramChannel(ProtocolFamily family)
            throws IOException
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public Pipe openPipe()
            throws IOException
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public AbstractSelector openSelector()
            throws IOException
        {
        return new SSLSelector(m_delegate.openSelector(), this);
        }

    @Override
    public ServerSocketChannel openServerSocketChannel()
            throws IOException
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public SocketChannel openSocketChannel()
            throws IOException
        {
        throw new UnsupportedOperationException();
        }


    // ----- Object interface ---------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (o == this)
            {
            return true;
            }
        else if (o instanceof SSLSelectorProvider)
            {
            return m_delegate.equals(((SSLSelectorProvider) o).m_delegate);
            }
        else
            {
            return false;
            }
        }

    @Override
    public int hashCode()
        {
        return m_delegate.hashCode();
        }

    // ----- data members -------------------------------------------

    /**
     * The delegate SelectorProvider.
     */
    protected SelectorProvider m_delegate;
    }
