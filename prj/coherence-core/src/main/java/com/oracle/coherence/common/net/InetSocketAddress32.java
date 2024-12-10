/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;

import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * InetSocketAddress32 is equivalent to the standard {@link java.net.InetSocketAddress}
 * but supports 32 bit port numbers.  As such it is not compatible with
 * standard TCP based sockets.
 *
 * @see	java.net.InetSocketAddress
 * @see MultiplexedSocketProvider
 */
public class InetSocketAddress32 extends SocketAddress
    {
    // Note: this is a literal copy-n-past of java.net.InetAddress with the
    // port restrictions removed

    /* The hostname of the Socket Address
     * @serial
     */
    private String hostname = null;
    /* The IP address of the Socket Address
     * @serial
     */
    private InetAddress addr = null;
    /* The port number of the Socket Address
     * @serial
     */
    private int port;

    private InetSocketAddress32() {
    }

    /**
     * Creates a socket address where the IP address is the wildcard address
     * and the port number a specified value.
     * <p>
     * A port number of <code>zero</code> will let the system pick up an
     * ephemeral port in a <code>bind</code> operation.
     * <p>
     * @param	port	The port number
     * @throws IllegalArgumentException if the port parameter is outside the specified
     * range of valid port values.
     */
    public InetSocketAddress32(int port) {
	this((InetAddress) null, port);
    }

    /**
     *
     * Creates a socket address from an IP address and a port number.
     * <p>
     * A port number of <code>zero</code> will let the system pick up an
     * ephemeral port in a <code>bind</code> operation.
     * <P>
     * A <code>null</code> address will assign the <i>wildcard</i> address.
     * <p>
     * @param	addr	The IP address
     * @param	port	The port number
     * @throws IllegalArgumentException if the port parameter is outside the specified
     * range of valid port values.
     */
    public InetSocketAddress32(InetAddress addr, int port) {
	this.port = port;
	if (addr == null)
        {
        try
            {
	        this.addr = InetAddress.getByAddress(new byte[]{0,0,0,0});
            }
        catch (UnknownHostException e)
            {
            throw new RuntimeException(e);
            }
        }
	else
        {
	    this.addr = addr;
        }
    }

	/**
	 * Construct an InetSocketAddress32 from an InetSocketAddress.
	 *
	 * @param addr the source InetSocketAddress.
     */
	public InetSocketAddress32(InetSocketAddress addr)
		{
		this (addr.getAddress(), addr.getPort());
		}

    /**
     *
     * Creates a socket address from a hostname and a port number.
     * <p>
     * An attempt will be made to resolve the hostname into an InetAddress.
     * If that attempt fails, the address will be flagged as <I>unresolved</I>.
     * <p>
     * If there is a security manager, its <code>checkConnect</code> method
     * is called with the host name as its argument to check the permissiom
     * to resolve it. This could result in a SecurityException.
     * <P>
     * A port number of <code>zero</code> will let the system pick up an
     * ephemeral port in a <code>bind</code> operation.
     * <P>
     * @param	hostname the Host name
     * @param	port	The port number
     * @throws IllegalArgumentException if the port parameter is outside the range
     * of valid port values, or if the hostname parameter is <TT>null</TT>.
     * @throws SecurityException if a security manager is present and
     *				 permission to resolve the host name is
     *				 denied.
     * @see	#isUnresolved()
     */
    public InetSocketAddress32(String hostname, int port) {
	if (hostname == null) {
	    throw new IllegalArgumentException("hostname can't be null");
	}
	try {
	    addr = InetAddress.getByName(hostname);
	} catch(UnknownHostException e) {
	    this.hostname = hostname;
	    addr = null;
	}
	this.port = port;
    }

    /**
     *
     * Creates an unresolved socket address from a hostname and a port number.
     * <p>
     * No attempt will be made to resolve the hostname into an InetAddress.
     * The address will be flagged as <I>unresolved</I>.
     * <p>
     * A port number of <code>zero</code> will let the system pick up an
     * ephemeral port in a <code>bind</code> operation.
     * <P>
     * @param	host    the Host name
     * @param	port	The port number
     * @throws IllegalArgumentException if the port parameter is outside
     *                  the range of valid port values, or if the hostname
     *                  parameter is <TT>null</TT>.
     * @see	#isUnresolved()
     * @return  a <code>InetSocketAddress32</code> representing the unresolved
     *          socket address
     * @since 1.5
     */
    public static InetSocketAddress32 createUnresolved(String host, int port) {
	if (host == null) {
	    throw new IllegalArgumentException("hostname can't be null");
	}
	InetSocketAddress32 s = new InetSocketAddress32();
	s.port = port;
	s.hostname = host;
	s.addr = null;
	return s;
    }

    private void readObject(ObjectInputStream s)
 	throws IOException, ClassNotFoundException {
 	s.defaultReadObject();

 	// Check that our invariants are satisfied
 	if (hostname == null && addr == null) {
 	    throw new InvalidObjectException("hostname and addr " +
 					     "can't both be null");
 	}
    }

    /**
     * Gets the port number.
     *
     * @return the port number.
     */
    public final int getPort() {
	return port;
    }

    /**
     *
     * Gets the <code>InetAddress</code>.
     *
     * @return the InetAdress or <code>null</code> if it is unresolved.
     */
    public final InetAddress getAddress() {
	return addr;
    }

    /**
     * Gets the <code>hostname</code>.
     *
     * @return	the hostname part of the address.
     */
    public final String getHostName() {
	if (hostname != null)
	    return hostname;
	if (addr != null)
	    return addr.getHostName();
	return null;
    }

    /**
     * Returns the hostname, or the String form of the address if it
     * doesn't have a hostname (it was created using a litteral).
     * This has the benefit of <b>not</b> attemptimg a reverse lookup.
     *
     * @return the hostname, or String representation of the address.
     * @since 1.6
     */
    final String getHostString() {
	if (hostname != null)
	    return hostname;
	if (addr != null) {
	    if (addr.getHostName() != null)
		return addr.getHostName();
	    else
		return addr.getHostAddress();
	}
	return null;
    }

    /**
     * Checks whether the address has been resolved or not.
     *
     * @return <code>true</code> if the hostname couldn't be resolved into
     *		an <code>InetAddress</code>.
     */
    public final boolean isUnresolved() {
	return addr == null;
    }

    /**
     * Constructs a string representation of this InetSocketAddress32.
     * This String is constructed by calling toString() on the InetAddress
     * and concatenating the port number (with a colon). If the address
     * is unresolved then the part before the colon will only contain the hostname.
     *
     * @return  a string representation of this object.
     */
    public String toString() {
	if (isUnresolved()) {
	    return hostname + ":" + port;
	} else {
	    return addr.toString() + ":" + port;
	}
    }

    /**
     * Compares this object against the specified object.
     * The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and it represents the same address as
     * this object.
     * <p>
     * Two instances of <code>InetSocketAddress32</code> represent the same
     * address if both the InetAddresses (or hostnames if it is unresolved) and port
     * numbers are equal.
     * If both addresses are unresolved, then the hostname and the port number
     * are compared.
     *
     * @param   obj   the object to compare against.
     * @return  <code>true</code> if the objects are the same;
     *          <code>false</code> otherwise.
     * @see java.net.InetAddress#equals(java.lang.Object)
     */
    public final boolean equals(Object obj) {
	if (obj == null || !(obj instanceof InetSocketAddress32))
	    return false;
	InetSocketAddress32 sockAddr = (InetSocketAddress32) obj;
	boolean sameIP = false;
	if (this.addr != null)
	    sameIP = this.addr.equals(sockAddr.addr);
	else if (this.hostname != null)
	    sameIP = (sockAddr.addr == null) &&
		this.hostname.equals(sockAddr.hostname);
	else
	    sameIP = (sockAddr.addr == null) && (sockAddr.hostname == null);
	return sameIP && (this.port == sockAddr.port);
    }

    /**
     * Returns a hashcode for this socket address.
     *
     * @return  a hash code value for this socket address.
     */
    public final int hashCode() {
	if (addr != null)
	    return addr.hashCode() + port;
	if (hostname != null)
	    return hostname.hashCode() + port;
	return port;
    }
}
