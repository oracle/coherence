/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2019.11.28
 * @since 20.06
 */
class ErrorsHelperTest
    {
    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldConvertException()
        {
        Exception              error     = new RuntimeException("Computer says No!");
        StatusRuntimeException exception = ErrorsHelper.ensureStatusRuntimeException(error);

        assertThat(exception,                              is(notNullValue()));
        assertThat(exception.getStatus().getCode(),        is(Status.INTERNAL.getCode()));
        assertThat(exception.getStatus().getDescription(), is(error.getMessage()));
        assertThat(exception.getStatus().getCause(),       is(sameInstance(error)));
        }

    @Test
    public void shouldConvertExceptionWithDescription()
        {
        Exception              error        = new RuntimeException("Computer says No!");
        String                 sDescription = "something went wrong!";
        StatusRuntimeException exception    = ErrorsHelper.ensureStatusRuntimeException(error, sDescription);

        assertThat(exception,                              is(notNullValue()));
        assertThat(exception.getStatus().getCode(),        is(Status.INTERNAL.getCode()));
        assertThat(exception.getStatus().getDescription(), is(sDescription));
        assertThat(exception.getStatus().getCause(),       is(sameInstance(error)));
        }

    @Test
    public void shouldConvertStatusException()
        {
        Exception              cause     = new RuntimeException("Computer says No!");
        StatusException        error     = Status.ABORTED.withDescription("Oops...").withCause(cause).asException();
        StatusRuntimeException exception = ErrorsHelper.ensureStatusRuntimeException(error);

        assertThat(exception,                              is(notNullValue()));
        assertThat(exception.getStatus().getCode(),        is(Status.ABORTED.getCode()));
        assertThat(exception.getStatus().getDescription(), is(error.getStatus().getDescription()));
        assertThat(exception.getStatus().getCause(),       is(sameInstance(cause)));
        }

    @Test
    public void shouldConvertStatusExceptionWithDescription()
        {
        String                 sDescription = "something went wrong!";
        Exception              cause        = new RuntimeException("Computer says No!");
        StatusException        error        = Status.ABORTED.withDescription("Oops...").withCause(cause).asException();
        StatusRuntimeException exception    = ErrorsHelper.ensureStatusRuntimeException(error, sDescription);

        assertThat(exception,                              is(notNullValue()));
        assertThat(exception.getStatus().getCode(),        is(Status.ABORTED.getCode()));
        assertThat(exception.getStatus().getDescription(), is(sDescription));
        assertThat(exception.getStatus().getCause(),       is(sameInstance(error)));
        }

    @Test
    public void shouldConvertStatusRuntimeException()
        {
        Exception              cause     = new RuntimeException("Computer says No!");
        StatusRuntimeException error     =
                Status.ABORTED.withDescription("Oops...").withCause(cause).asRuntimeException();
        StatusRuntimeException exception = ErrorsHelper.ensureStatusRuntimeException(error);

        assertThat(exception, is(sameInstance(error)));
        }

    @Test
    public void shouldConvertStatusRuntimeExceptionWithDescription()
        {
        String                 sDescription = "something went wrong!";
        Exception              cause        = new RuntimeException("Computer says No!");
        StatusRuntimeException error        =
                Status.ABORTED.withDescription("Oops...").withCause(cause).asRuntimeException();
        StatusRuntimeException exception    = ErrorsHelper.ensureStatusRuntimeException(error, sDescription);

        assertThat(exception,                              is(notNullValue()));
        assertThat(exception.getStatus().getCode(),        is(Status.ABORTED.getCode()));
        assertThat(exception.getStatus().getDescription(), is(sDescription));
        assertThat(exception.getStatus().getCause(),       is(sameInstance(error)));
        }
    }
