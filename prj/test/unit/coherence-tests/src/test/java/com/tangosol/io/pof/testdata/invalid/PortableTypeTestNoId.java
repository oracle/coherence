/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.testdata.invalid;

import com.tangosol.io.pof.schema.annotation.PortableType;
import com.tangosol.io.pof.testdata.PortableTypeTestBase;

@PortableType
public class PortableTypeTestNoId
        extends PortableTypeTestBase
{
    public PortableTypeTestNoId()
    {
        super();
    }

    public PortableTypeTestNoId(int nId, String sString)
    {
        super(nId, sString);
    }
}
