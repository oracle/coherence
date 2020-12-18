/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.helidon.client;

import com.oracle.coherence.client.ChannelProvider;

import io.grpc.Channel;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import java.util.Optional;

/**
 * An implementation of a {@link ChannelProvider} that uses
 * Helidon Microprofile  gRPC integration to provide named
 * {@link Channel} instances.
 *
 * @author Jonathan Knight  2020.12.18
 * @since 20.12
 */
public class HelidonChannelProvider
        implements ChannelProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Must have a public constructor because this class will be discovered and
     * loaded by {@link com.oracle.coherence.client.ChannelProviders} using the
     * {@link java.util.ServiceLoader}.
     */
    public HelidonChannelProvider()
        {
        }

    // ----- ChannelProvider methods ----------------------------------------

    @Override
    public Optional<Channel> getChannel(String sName)
        {
        return getBeanManager().flatMap(cdi -> getChannel(cdi, sName));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the named {@link Channel} from {@link CDI}.
     *
     * @param beanManager  the CDI {@link BeanManager}
     * @param sName        the name of the {@link Channel} to obtain from CDI
     *
     * @return  the requested {@link Channel} or an empty {@link Optional}
     *          if Helidon MP gRPC could not supply a {@link Channel}
     */
    Optional<Channel> getChannel(BeanManager beanManager, String sName)
        {
        return beanManager.createInstance()
                  .select(Channel.class, GrpcChannelLiteral.of(sName))
                  .stream()
                  .findFirst();
        }

    /**
     * Returns the current {@link CDI} environment.
     *
     * @return the current {@link CDI} environment or an empty
     *         {@link Optional} if not running in CDI.
     */
    Optional<BeanManager> getBeanManager()
        {
        try
            {
            return Optional.of(CDI.current().getBeanManager());
            }
        catch (IllegalStateException e)
            {
            return Optional.empty();
            }
        }
    }