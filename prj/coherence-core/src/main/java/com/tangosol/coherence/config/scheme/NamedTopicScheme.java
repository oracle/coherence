/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.net.TopicService;

import com.tangosol.net.topic.NamedTopic;

/**
 * The {@link TopicScheme} class is responsible for building a fully
 * configured instance of a {@link NamedTopic}.
 *
 * @author jk 2015.05.21
 * @since Coherence 14.1.1
 */
public interface NamedTopicScheme
        extends TopicScheme<NamedTopic,TopicService>
    {
    }
