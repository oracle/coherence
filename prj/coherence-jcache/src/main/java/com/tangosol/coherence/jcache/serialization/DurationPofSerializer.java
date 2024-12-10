/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.serialization;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

import java.util.concurrent.TimeUnit;

import javax.cache.expiry.Duration;

/**
 * The {@link PofSerializer} for {@link Duration}s.
 *
 * @author jf  2013.09.09
 * @since Coherence 12.1.3
 */
public class DurationPofSerializer
        implements PofSerializer
    {
    @Override
    public void serialize(PofWriter pofWriter, Object o)
            throws IOException
        {
        Duration d = (Duration) o;

        pofWriter.writeLong(0, d.getDurationAmount());
        pofWriter.writeObject(1, d.getTimeUnit());
        pofWriter.writeRemainder(null);
        }

    @Override
    public Object deserialize(PofReader pofReader)
            throws IOException
        {
        Duration result = null;

        try
            {
            long     amount = pofReader.readLong(0);
            TimeUnit unit   = (TimeUnit) pofReader.readObject(1);

            if (amount == 0L)
                {
                result = (unit == null) ? Duration.ETERNAL : Duration.ZERO;
                }
            else
                {
                result = new Duration(unit, amount);
                }
            }
        catch (Throwable t)
            {
            throw new IOException("Malformed Duration", t);
            }

        finally
            {
            pofReader.readRemainder();
            }

        return result;
        }
    }
