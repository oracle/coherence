/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.oracle.coherence.io.json.genson.ext.GensonBundle;

/**
 * Service interface for libraries that need to register a custom {@link GensonBundle} with
 * our <code>Genson</code> runtime.
 * <p>
 * Note to implementors: this process makes no guarantees on bundle registration ordering.
 *
 * @since 20.06
 */
public interface GensonBundleProvider
    {
    /**
     * Obtain the {@link com.oracle.coherence.io.json.genson.ext.GensonBundle}.
     *
     * @return the {@link com.oracle.coherence.io.json.genson.ext.GensonBundle}
     */
    GensonBundle provide();
    }
