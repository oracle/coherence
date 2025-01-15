/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

/**
 * A listener that can be registered with a topic instance
 * to be notified of disconnection events.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface NamedTopicDeactivationListener
    {
    /**
     * The topic has been disconnected.
     */
    void onDisconnect();

    /**
     * The topic has been connected.
     */
    void onConnect();

    /**
     * The topic has been destroyed.
     */
    void onDestroy();

    /**
     * The topic has been released.
     */
    void onRelease();
    }
