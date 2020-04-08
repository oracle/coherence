/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.ClassLoaderAware;

import com.tangosol.run.xml.XmlElement;


/**
* The Controllable interface represents a configurable daemon-like object,
* quite often referred to as a <i>service</i>, that usually operates on its
* own thread and has a controllable life cycle.
*
* @author gg  2002.02.08, 2003.02.11
*/
public interface Controllable
        extends ClassLoaderAware
    {
    /**
    * Configure the controllable service.
    * <p>
    * This method can only be called before the controllable
    * service is started.
    *
    * @param xml an XmlElement carrying configuration information
    *            specific to the Controllable object
    *
    * @exception IllegalStateException thrown if the service is
    *            already running
    * @exception IllegalArgumentException thrown if the configuration
    *            information is invalid
    */
    @Deprecated
    public void configure(XmlElement xml);

    /**
    * Start the controllable service.
    * <p>
    * This method should only be called once per the life cycle
    * of the Controllable service. This method has no affect if the
    * service is already running.
    *
    * @exception IllegalStateException thrown if a service does not support
    *            being re-started, and the service was already started and
    *            subsequently stopped and then an attempt is made to start
    *            the service again; also thrown if the Controllable service
    *            has not been configured
    */
    public void start();

    /**
    * Determine whether or not the controllable service is running.
    * This method returns false before a service is started, while
    * the service is starting, while a service is shutting down and
    * after the service has stopped. It only returns true after
    * completing its start processing and before beginning its
    * shutdown processing.
    *
    * @return true if the service is running; false otherwise
    */
    public boolean isRunning();

    /**
    * Stop the controllable service. This is a controlled shut-down,
    * and is preferred to the {@link #stop()} method.
    * <p>
    * This method should only be called once per the life cycle
    * of the controllable service. Calling this method for a service
    * that has already stopped has no effect.
    */
    public void shutdown();

    /**
    * Hard-stop the controllable service. Use {@link #shutdown()}
    * for normal service termination. Calling this method for a service
    * that has already stopped has no effect.
    */
    public void stop();
    }