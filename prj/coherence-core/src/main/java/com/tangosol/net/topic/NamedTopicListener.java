/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.topic;

import java.util.EventListener;

/**
 * A listener that receives events related to a {@link NamedTopic}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface NamedTopicListener
        extends EventListener
    {
    /**
     * Receives {@link NamedTopicEvent events}.
     *
     * @param evt the {@link NamedTopicEvent events} from the topic
     */
    void onEvent(NamedTopicEvent evt);
    }

