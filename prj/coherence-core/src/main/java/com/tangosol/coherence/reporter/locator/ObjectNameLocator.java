/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter.locator;


import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.IdentityExtractor;

/**
* Locator to include the ObjectName in a Report.  To remove Group By Logic.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/

public class ObjectNameLocator
    extends BaseLocator
    {
    public ValueExtractor getExtractor()
        {
        return new IdentityExtractor();
        }

    }
