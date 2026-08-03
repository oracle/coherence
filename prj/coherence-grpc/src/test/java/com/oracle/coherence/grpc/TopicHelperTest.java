/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.oracle.coherence.grpc.messages.topic.v1.PublishedValueStatus;

import com.tangosol.internal.net.topic.PublishResult;
import com.tangosol.internal.net.topic.SimplePublishResult;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.net.grpc.GrpcDiagnosticsPolicy;
import com.tangosol.util.LongArray;
import com.tangosol.util.SimpleLongArray;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link TopicHelper}.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
class TopicHelperTest
    {
    @Test
    void shouldCreateSafePublishErrorWithoutSerializedThrowable()
        {
        LongArray<Throwable> aErrors = new SimpleLongArray();
        aErrors.set(0, new IllegalStateException("secret token"));

        SimplePublishResult result = new SimplePublishResult(1, 1, new SimpleLongArray(), aErrors, 0, null,
                PublishResult.Status.Success);

        com.oracle.coherence.grpc.messages.topic.v1.PublishResult message =
                TopicHelper.toProtobufPublishResult(result, new DefaultSerializer(),
                        GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);
        PublishedValueStatus status = message.getValueStatus(0);

        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, status.getError().getMessage());
        assertFalse(status.getError().hasError());
        }
    }
