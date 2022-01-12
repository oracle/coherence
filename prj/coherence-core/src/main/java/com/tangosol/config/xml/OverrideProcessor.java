/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.run.xml.XmlElement;

/**
 * A {@link OverrideProcessor} is responsible for processing override config
 * elements and merging the elements with the Document root elements.
 * 
 * @since Coherence 14.1.2/22.06
 */
public interface OverrideProcessor
    {

    /**
     * Process {@link XmlElement} override and merge with the
     * {@link XmlElement} base element from the root configuration xml.
     *
     * @param xmlBase      base cache configuration xml element
     * @param xmlOverride  override cache configuration xml element
     */
    public void process(XmlElement xmlBase, XmlElement xmlOverride);
    }
