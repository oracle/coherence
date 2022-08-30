/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.events.EventInterceptor;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link SessionConfiguration} that delegates to another {@link SessionConfiguration}.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class WrapperSessionConfiguration
        implements SessionConfiguration
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link WrapperSessionConfiguration}.
     *
     * @param delegate  the {@link SessionConfiguration} to delegate to
     *
     * @throws NullPointerException if the delegate is {@code null}
     */
    public WrapperSessionConfiguration(SessionConfiguration delegate)
        {
        this.f_delegate = Objects.requireNonNull(delegate);
        }

    // ----- SessionConfiguration methods -----------------------------------

    @Override
    public String getName()
        {
        return f_delegate.getName();
        }

    @Override
    public String getScopeName()
        {
        return f_delegate.getScopeName();
        }

    @Override
    public Iterable<EventInterceptor<?>> getInterceptors()
        {
        return f_delegate.getInterceptors();
        }

    @Override
    public boolean isEnabled()
        {
        return f_delegate.isEnabled();
        }

    @Override
    public Optional<String> getConfigUri()
        {
        return f_delegate.getConfigUri();
        }

    @Override
    public Optional<ClassLoader> getClassLoader()
        {
        return f_delegate.getClassLoader();
        }

    @Override
    public int getPriority()
        {
        return f_delegate.getPriority();
        }

    @Override
    public int compareTo(SessionConfiguration other)
        {
        return f_delegate.compareTo(other);
        }

    @Override
    public Optional<ParameterResolver> getParameterResolver()
        {
        return f_delegate.getParameterResolver();
        }

    @Override
    public Optional<Coherence.Mode> getMode()
        {
        return f_delegate.getMode();
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "WrapperSessionConfiguration(" +
                "delegate=" + f_delegate +
                ')';
        }


    // ----- data members ---------------------------------------------------

    /**
     * The {@link SessionConfiguration} to delegate to.
     */
    private final SessionConfiguration f_delegate;
    }
