/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import java.net.InetAddress;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

/**
 * A NameService is a service that accepts connections from external clients
 * (e.g. Coherence*Extend) and provides a name lookup service.
 *
 * @author phf 2012.03.05
 *
 * @since 12.1.2
 */
public interface NameService
        extends Service
    {
    /**
     * Register a {@link LookupCallback} to be used to perform lookups on
     * names that are not bound to the {@link NameService}'s directory.
     * <p>
     * If more than one {@link LookupCallback} is registered, they are
     * called in the order in which they are registered with the
     * {@link NameService}.
     *
     * @param callback  the {@link LookupCallback} to register
     *
     * @since 12.2.1
     */
    public void addLookupCallback(LookupCallback callback);

    /**
     * Binds a name to an object.
     *
     * @param sName  the name to bind; may not be empty
     * @param o      the object to bind; possibly null
     *
     * @throws NameAlreadyBoundException if name is already bound
     * @throws NamingException if a naming exception is encountered
     */
    public void bind(String sName, Object o)
            throws NamingException;

    /**
     * Retrieve the running or configured listening address.
     *
     * @return the running or configured listening address
     *
     * @since 12.2.1
     */
    public InetAddress getLocalAddress();

    /**
     * Retrieves the named object. If the retrieved object is {@link Resolvable},
     * then the result of the {@link Resolvable#resolve resolve} call is returned.
     *
     * @param sName  the name of the object to look up
     *
     * @return the object bound to the specified name or the result of the
     *         {@link Resolvable#resolve resolve} call if the bound
     *         object is {@link Resolvable}
     *
     * @throws NamingException if a naming exception is encountered
     */
    public Object lookup(String sName) throws NamingException;

    /**
     * Unbinds the named object.
     *
     * @param sName  the name to bind; may not be empty
     *
     * @throws NamingException if a naming exception is encountered
     */
    public void unbind(String sName)
            throws NamingException;

    // ----- inner interfaces -----------------------------------------------

    /**
     * During the {@link NameService#lookup(String) lookup} call, if the retrieved
     * object is {@link Resolvable}, then the result of the {@link #resolve resolve}
     * call is returned.
     */
    public interface Resolvable
        {
        /**
         * Resolve the object to be returned by the {@link NameService#lookup lookup} call
         *
         * @param ctx  the lookup request context
         *
         * @return the resolved object
         */
        public Object resolve(RequestContext ctx);
        }

    /**
     * An object which implements {@link LookupCallback} can be registered
     * with a {@link NameService} via {@link NameService#addLookupCallback} to
     * perform a lookup on names that were not found in the NameService's directory.
     *
     * @since 12.2.1
     */
     public interface LookupCallback
        {
        /**
         * Retrieve the named object.
         *
         * @param sName    the name of the object to look up
         * @param cluster  the {@link Cluster} to which this NameService belongs
         * @param ctx      the lookup request context
         *
         * @return the object bound to sName, or <tt>null</tt> if not found
         *
         * @throws NamingException if a naming exception is encountered
         */
        public Object lookup(String sName, Cluster cluster, RequestContext ctx)
                throws NamingException;
        }

    /**
     * An object which implements {@link RequestContext} stores information about the NameService request.
     *
     * @since 12.2.1
     */
    public interface RequestContext
        {
        /**
         * Get the {@link InetAddress} that the NameService received the request on.
         * May return <tt>null</tt> if the request is local.
         *
         * @return the local address that received the request
         */
        public InetAddress getAcceptAddress();

        /**
         * Get the {@link InetAddress} that the request originated from.
         * May return <tt>null</tt> if the request is local.
         *
         * @return the address that originated the request
         */
        public InetAddress getSourceAddress();

        /**
         * Get the client {@link Member} that sent the request. May
         * return <tt>null</tt> unknown or if the request is local.
         *
         * @return the Member that sent the request
         */
        public Member getMember();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Default service name for the NameService.
     */
    public static final String NAME_DEFAULT = "NameService";

    /**
     * Remote name service type constant.
     */
    public static final String TYPE_REMOTE = "RemoteNameService";
    }
