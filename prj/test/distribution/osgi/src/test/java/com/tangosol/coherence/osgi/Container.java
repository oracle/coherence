/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import java.util.Map;
import java.util.Properties;

/**
 * Container provides abstraction around the core OSGi classes to provide a
 * more convenient means of packaging and deploying bundles to the OSGi
 * container. OSGi already provides a comprehensive API in interacting
 * with a container {@link org.osgi.framework.launch.Framework}, however the
 * one area it does and does not intend to address is the packaging of
 * bundles to subsequently be deployed to the container.
 *
 * @author hr  2012.01.27
 * @since Coherence 12.1.2
 */
public interface Container
    {
    /**
     * Configure the container with the provided additional properties.
     *
     * @param mapConfigProperties  configuration hints to initialize the
     *                             container with
     */
    public void configure(Map<String,String> mapConfigProperties);

    /**
     * Start the container.
     */
    public void start();

    /**
     * Stop the container.
     */
    public void stop();

    /**
     * Deploy the bundle located at <tt>sBundleLocation</tt> to the
     * container.
     *
     * @param sBundleLocation  location of the bundle to deploy to the
     *                         container
     *
     * @return a reference to the deployed bundle
     *
     * @throws BundleException  iff an error occurred in deploying to the
     *                          container
     */
    public Bundle deploy(String sBundleLocation) throws BundleException;

    /**
     * Based on the location provided by <tt>sBndFileLocation</tt> create a
     * Jar file / bundle using the BND file location as the input. The BND
     * file will contain a list of exported packages which are in turn
     * analyzed for class references. The classes referenced are packaged
     * within the bundle unless specified to be ignored. Once the bundle is
     * created it will be deployed to the container via {@link #deploy(String)}.
     *
     * @param sBndFileLocation  the location of the BND file
     *
     * @return the deployed bundle
     *
     * @throws BundleException  iff unsuccessful in deployment
     */
    public Bundle packageAndDeploy(String sBndFileLocation) throws BundleException;

    /**
     * Based on the location provided by <tt>sBndFileLocation</tt> create a
     * Jar file / bundle using the BND file location as the input. The BND
     * file will contain a list of exported packages which are in turn
     * analyzed for class references. The classes referenced are packaged
     * within the bundle unless specified to be ignored. Once the bundle is
     * created it will be deployed to the container via {@link #deploy(String)}.
     * <p>
     * The provided {@link Properties} allow replacement of expressions
     * present within the BND file referenced by {@literal sBndFileLocation}.
     *
     * @param sBndFileLocation  the location of the BND file
     *
     * @return the deployed bundle
     *
     * @throws BundleException  iff unsuccessful in deployment
     */
    public Bundle packageAndDeploy(String sBndFileLocation, Properties props) throws BundleException;

    /**
     * Convenience method to return a {@link Bundle} known by the container
     * as the name specified.
     *
     * @param sBundleName  the name of the bundle
     *
     * @return a bundle if a name is matched or null
     */
    public Bundle getBundle(String sBundleName);

    /**
     * Returns the {@link BundleContext} of the system bundle.
     *
     * @return BundleContext of the system bundle
     */
    public BundleContext getSystemBundleContext();
    }
