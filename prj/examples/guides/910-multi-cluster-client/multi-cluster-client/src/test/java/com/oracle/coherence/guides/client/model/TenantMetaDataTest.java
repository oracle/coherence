/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client.model;

import com.oracle.coherence.guides.client.Application;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.WriteBuffer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TenantMetaDataTest {
    @Test
    public void shouldSerializeAsJson() throws Exception {
        TenantMetaData metaData = new TenantMetaData("marvel", "extend", "127.0.0.1", 20000, "pof");
        WriteBuffer    buffer   = Application.SERIALIZER.serialize(metaData);

        byte[]              bytes  = buffer.toByteArray();
        ByteArrayReadBuffer in     = new ByteArrayReadBuffer(bytes);
        TenantMetaData      result = Application.SERIALIZER.deserialize(in.getBufferInput(), TenantMetaData.class);

        assertThat(result, is(notNullValue()));
        assertThat(result.getTenant(), is(metaData.getTenant()));
        assertThat(result.getType(), is(metaData.getType()));
        assertThat(result.getHostName(), is(metaData.getHostName()));
        assertThat(result.getPort(), is(metaData.getPort()));
        assertThat(result.getSerializer(), is(metaData.getSerializer()));
    }

    @Test
    public void shouldDeserializeFromJson() throws Exception {
        String json = "{" +
                      "  \"hostName\":\"127.0.0.1\"," +
                      "  \"port\":20000," +
                      "  \"tenant\":\"marvel\"," +
                      "  \"type\":\"extend\"," +
                      "  \"serializer\":\"java\"" +
                      "}";

        ByteArrayReadBuffer in     = new ByteArrayReadBuffer(json.getBytes(StandardCharsets.UTF_8));
        TenantMetaData      result = Application.SERIALIZER.deserialize(in.getBufferInput(), TenantMetaData.class);

        assertThat(result, is(notNullValue()));
        assertThat(result.getTenant(), is("marvel"));
        assertThat(result.getType(), is("extend"));
        assertThat(result.getHostName(), is("127.0.0.1"));
        assertThat(result.getPort(), is(20000));
        assertThat(result.getSerializer(), is("java"));
    }

}
