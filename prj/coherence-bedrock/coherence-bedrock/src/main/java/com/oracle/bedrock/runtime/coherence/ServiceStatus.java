/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

public enum ServiceStatus
    {
        /**
         * The service is endangered.  Any loss will cause data/computation loss.
         */
        ENDANGERED,

        /**
         * The service is machine-safe.  Any machine may be safely shutdown without loss.
         */
        MACHINE_SAFE,

        /**
         * The service is node-safe.  Any node may be safely shutdown without loss.
         */
        NODE_SAFE,

        /**
         * The service has been orphaned.  Data/computational services have been lost.
         */
        ORPHANED,

        /**
         * The service is rack-safe.  Any rack may be safely shutdown without loss.
         */
        RACK_SAFE,

        /**
         * The service is site-safe.  Any site may be safely shutdown without loss.
         */
        SITE_SAFE,

        /**
         * The service is running (but no other information is available).
         */
        RUNNING,

        /**
         * The service is not running.
         */
        STOPPED,

        /**
         * The service is running, but the actual status is undefined / unknown.
         */
        UNKNOWN
    }
