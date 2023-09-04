/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;

import com.oracle.coherence.common.base.Disposable;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.io.SerializerFactory;
import com.tangosol.io.WrapperStreamFactory;

import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.IdentityTransformer;

import com.tangosol.persistence.SnapshotArchiverFactory;


import java.net.InetAddress;

import java.util.Map;

/**
* OperationalContext is an interface for providing Oracle Coherence
* operational configuration.
*
* @author lh  2010.10.29
*
* @since Coherence 3.7
*/
public interface OperationalContext
    {
    /**
    * Get the product edition.
    *
    * @return the product edition
    */
    public int getEdition();

    /**
    * Get the product edition in a formatted string.
    *
    * @return the product edition in a formatted string
    */
    public String getEditionName();

    /**
    * Return a Member object representing this process.
    *
    * @return the local Member
    */
    public Member getLocalMember();

    /**
    * Return a Map of network filter factories.
    *
    * @return a Map of WrapperStreamFactory instances keyed by filter name
    */
    public Map<String, WrapperStreamFactory> getFilterMap();

    /**
    * Return a Map of serializer factories.
    *
    * @return a Map of SerializerFactory instances keyed by serializer name
    */
    public Map<String, SerializerFactory> getSerializerMap();

    /**
     * Return a Map of snapshot archiver factories.
     *
     * @return a Map of {@link SnapshotArchiverFactory} keyed by name
     *
     * @since Coherence 12.1.3
     */
    public Map<String, SnapshotArchiverFactory> getSnapshotArchiverMap();

    /**
    * Return a Map of address provider factories.
    *
    * @return a Map of {@link AddressProviderFactory} keyed by name
    *
    * @since Coherence 12.1.2
    */
    public Map<String, AddressProviderFactory> getAddressProviderMap();

    /**
     * The registry for all builders associated with the cluster.
     * A builder can be looked up via the class it produces and a name for the
     * builder using {@link ParameterizedBuilderRegistry#getBuilder(Class, String)} or
     * just by the class it builds if there are no named builders.
     * <p>
     * Currently, only {@link SerializerFactory}, {@link
     * com.tangosol.coherence.config.builder.ParameterizedBuilder ParameterizedBuilder}&lt;
     * {@link com.oracle.coherence.persistence.PersistenceEnvironment PersistenceEnvironment}&gt;
     * and {@link
     * com.tangosol.coherence.config.builder.ParameterizedBuilder ParameterizedBuilder}&lt;
     * {@link com.tangosol.net.security.StorageAccessAuthorizer StorageAccessAuthorizer}&gt;
     * are registered by the implementation.
     * <p>
     * All registered builders implementing {@link Disposable} will be disposed.
     *
     * @return  the {@link ParameterizedBuilderRegistry}
     *
     * @since Coherence 12.2.1
     */
    public ParameterizedBuilderRegistry getBuilderRegistry();

    /**
    * Return a SocketProviderFactory.
    *
    * @return a SocketProviderFactory
    */
    public SocketProviderFactory getSocketProviderFactory();

    /**
     * Return the TCP port on which this member is bound.  While this is often the same
     * value as getLocalMember().getPort(), it is not always so.
     *
     * @return the local TCP port or zero if unbound.
     *
     * @since 12.2.1
     */
    public int getLocalTcpPort();

    /**
    * Return an IdentityAsserter that can be used to establish a user's
    * identity.
    *
    * @return the IdentityAsserter
    */
    public IdentityAsserter getIdentityAsserter();

    /**
    * Return an IdentityTransformer that can be used to transform a Subject
    * into an identity assertion.
    *
    * @return the IdentityTransformer
    */
    public IdentityTransformer getIdentityTransformer();

    /**
    * Indicates if Subject scoping is enabled.
    *
    * @return true if subject scoping is enabled
    */
    public boolean isSubjectScopingEnabled();

    /**
     * Return the TTL for multicast based discovery.
     *
     * @return the TTL
     *
     * @since 12.2.1
     */
    public int getDiscoveryTimeToLive();

    /**
     * Return the IP associated with the network interface to use for multicast based discovery.
     *
     * @return the associated InetAddress or null
     *
     * @since 12.2.1
     */
    public InetAddress getDiscoveryInterface();

    /**
     * Return the common {@link DaemonPool}.
     * <p/>
     * The common pool is used by Coherence for common tasks such
     * as async handling of completable futures in place of the
     * fork-join pool.
     *
     * @return the common {@link DaemonPool}
     */
    public DaemonPool getCommonDaemonPool();
    }
