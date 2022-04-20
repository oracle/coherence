/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.08.19
 */
public class NotificationKeyTest
    {
    @Test
    public void shouldSerializeUsingPof()
            throws Exception
        {
        ConfigurablePofContext serializer = new ConfigurablePofContext("coherence-pof-config.xml");
        NotificationKey        key        = new NotificationKey(5, 6);
        Binary                 binary     = ExternalizableHelper.toBinary(key, serializer);
        NotificationKey        result     = (NotificationKey) ExternalizableHelper.fromBinary(binary, serializer);

        assertThat(result.getPartitionId(), is(key.getPartitionId()));
        assertThat(result.m_nId, is(key.m_nId));
        assertNotNull(result.toString());
        assertNotNull(key.toString());
        assertNotEquals(result, null);
        }

    @Test
    public void shouldTestForEquality() throws Exception
        {
        NotificationKey key = new NotificationKey(1, 0);

        assertThat(key.equals(key), is(true));
        assertThat(key.equals(null), is(false));
        assertThat(key.equals(new NotificationKey(3, 0)), is(false));
        assertThat(key.equals(new NotificationKey(1, 1)), is(false));
        assertEquals(key.hashCode(), new NotificationKey(1,0).hashCode());
        }
    }
