/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.tangosol.util.Filter;

/**
 * Filter class for CQC persistence tests.
 *
 * @author tam 2025.08.07
 */
@SuppressWarnings("rawtypes")
class CQCFilter implements Filter
    {
    @Override
    public boolean evaluate(Object o)
        {
        return (o instanceof String)
                || (o instanceof Integer
                && ((Integer) o) >= 9500
                && ((Integer) o) < 10500);
        }
    }
