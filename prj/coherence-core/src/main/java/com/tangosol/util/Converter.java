/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* Provide for "pluggable" object conversions.
*
* @param <F> the "from" type
* @param <T> the "to" type
*
* @version 1.00 04/25/00
* @author Pat McNerthney
*/
public interface Converter<F, T>
        extends com.oracle.common.base.Converter<F, T>
    {
    }
