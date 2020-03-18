/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;

import java.io.IOException;

import java.lang.reflect.Field;

import java.nio.channels.spi.AbstractInterruptibleChannel;


/**
 * Set of InterruptibleChannels related helper methods.
 *
 * @author mf  2013.11.04
 */
public final class InterruptibleChannels
    {
    /**
     * Attempt to change a channel's interruptibility.
     *
     * Note: this feature is deprecated as it no longer with Java 9.
     *
     * @param chan            the channel
     * @param fInterruptible  true if interrupts are to be supported; false otherwise
     *
     * @return true iff the channel was successfully updated
     *
     * @deprecated
     */
    public static boolean setInterruptible(AbstractInterruptibleChannel chan, boolean fInterruptible)
        {
        // This is a "work-around for"
        // JDK-6908931 : (so) Thread.interrupt impact on thread doing non-blocking I/O operation is not clear
        if (s_fieldInterruptor == null || (!fInterruptible && s_interruptibleNoOp == null))
            {
            return false;
            }

        try
            {
            while (chan instanceof WrapperSocketChannel) // special awareness of wrapper channels
                {
                chan = ((WrapperSocketChannel) chan).f_delegate;
                }

            s_fieldInterruptor.set(chan, fInterruptible
                    ? null
                    : s_interruptibleNoOp);

            return true;
            }
        catch (Throwable e)
            {
            return false;
            }
        }

    // ----- inner class: InterruptibleFetcher ------------------------------

    /**
     * Helper hack to fetch an implementation of sun.nio.ch.Interruptible
     */
    private static class InterruptibleFetcher
        extends AbstractInterruptibleChannel
        {
        /**
         * Retrieve the internal interruptible and close the channel.
         *
         * @return the interruptible
         */
        Object fetch()
            {
            try
                {
                begin(); // initializes the interruptor field
                return s_fieldInterruptor.get(this);
                }
            catch (Throwable e) {}
            finally
                {
                try
                    {
                    end(true);
                    close();
                    }
                catch (Throwable e) {}
                }

            return null;
            }

        @Override
        protected void implCloseChannel()
                throws IOException
            {
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Field for accessing an AbstractInterruptibleChannel's private interruptor.
     */
    private static final Field s_fieldInterruptor;

    /**
     * An InterruptibleNoOp, obtained from a pre-closed channel.
     */
    private static final Object s_interruptibleNoOp;

    static
        {
        Field field = null;
        try
            {
            if (System.getProperty("java.vm.specification.version").startsWith("1."))
                {
                Field fieldTmp = AbstractInterruptibleChannel.class.getDeclaredField("interruptor");
                fieldTmp.setAccessible(true);
                field = fieldTmp;
                }
            // else; we're on at least 9, it is inaccessable unless --permit-illegal-access is specified and
            // that generates warnings.  The primary usecase for this functionality is TMB, and thankfully TMB will do
            // reconnects on unexpected closes, so we simply drop this feature
            }
        catch (Throwable e) {}

        s_fieldInterruptor  = field;
        s_interruptibleNoOp = field == null ? null : new InterruptibleFetcher().fetch();
        }
    }
