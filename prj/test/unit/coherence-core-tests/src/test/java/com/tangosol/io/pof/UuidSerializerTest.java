/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UuidSerializerTest
    {
    @Test
    public void shouldSerializeUUID()
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext();
        UUID                   uuid       = UUID.randomUUID();
        Binary                 binary     = ExternalizableHelper.toBinary(uuid, serializer);
        UUID                   result     = ExternalizableHelper.fromBinary(binary, serializer);
        assertThat(result, is(uuid));
        }

    }
