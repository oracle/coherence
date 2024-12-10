/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.serialization;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.jcache.Constants;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.EternalExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;

/**
 * The {@link PofSerializer} for {@link javax.cache.expiry.ExpiryPolicy}s.
 *
 * @author jf  2013.05.16
 * @since Coherence 12.1.3
 */
public class ExpiryPolicyPofSerializer
        implements PofSerializer
    {
    @Override
    public void serialize(PofWriter pofWriter, Object o)
            throws IOException
        {
        Duration d = null;

        if (o instanceof AccessedExpiryPolicy)
            {
            pofWriter.writeString(0, "AccessedExpiryPolicy");

            try
                {
                d = ((AccessedExpiryPolicy) o).getExpiryForAccess();
                }
            catch (Throwable e)
                {
                Logger.warn("Unexpected exception in user-provided ExpiryPolicy:", e);
                }

            pofWriter.writeObject(1, d);
            }
        else if (o instanceof ModifiedExpiryPolicy)
            {
            pofWriter.writeString(0, "ModifiedExpiryPolicy");

            try
                {
                d = ((ModifiedExpiryPolicy) o).getExpiryForUpdate();
                }
            catch (Throwable e)
                {
                Logger.warn("Unexpected exception in user-provided ExpiryPolicy:", e);
                }

            pofWriter.writeObject(1, d);
            }
        else if (o instanceof EternalExpiryPolicy)
            {
            pofWriter.writeString(0, "EternalExpiryPolicy");
            }
        else if (o instanceof TouchedExpiryPolicy)
            {
            pofWriter.writeString(0, "TouchedExpiryPolicy");

            try
                {
                d = ((TouchedExpiryPolicy) o).getExpiryForAccess();
                }
            catch (Throwable e)
                {
                Logger.warn("Unexpected exception in user-provided ExpiryPolicy:", e);
                }

            pofWriter.writeObject(1, d);
            }
        else if (o instanceof CreatedExpiryPolicy)
            {
            pofWriter.writeString(0, "CreatedExpiryPolicy");

            try
                {
                d = ((CreatedExpiryPolicy) o).getExpiryForCreation();
                }
            catch (Throwable e)
                {
                // default if JCache client provided expiry policy throws an exception.
                d = Constants.DEFAULT_EXPIRY_DURATION;
                Logger.warn("Defaulting to implemention-specifc ExpiryForCreation default "
                            + "due to handling unexpected exception in user-provided ExpiryPolicy:", e);
                }

            pofWriter.writeObject(1, d);
            }
        else
            {
            pofWriter.writeString(0, "CustomExpiryPolicy");
            pofWriter.writeObject(1, o);
            }

        pofWriter.writeRemainder(null);
        }

    @Override
    public Object deserialize(PofReader pofReader)
            throws IOException
        {
        String sExpiryPolicyClassName = pofReader.readString(0);

        if ("AccessedExpiryPolicy".equals(sExpiryPolicyClassName))
            {
            Duration d = (Duration) pofReader.readObject(1);

            pofReader.readRemainder();

            return new AccessedExpiryPolicy(d);
            }
        else if ("ModifiedExpiryPolicy".equals(sExpiryPolicyClassName))
            {
            Duration d = (Duration) pofReader.readObject(1);

            pofReader.readRemainder();

            return new ModifiedExpiryPolicy(d);
            }
        else if ("EternalExpiryPolicy".equals(sExpiryPolicyClassName))
            {
            pofReader.readRemainder();

            return new EternalExpiryPolicy();
            }
        else if ("TouchedExpiryPolicy".equals(sExpiryPolicyClassName))
            {
            Duration d = (Duration) pofReader.readObject(1);

            pofReader.readRemainder();

            return new TouchedExpiryPolicy(d);
            }
        else if ("CreatedExpiryPolicy".equals(sExpiryPolicyClassName))
            {
            Duration d = (Duration) pofReader.readObject(1);

            pofReader.readRemainder();

            return new CreatedExpiryPolicy(d);
            }
        else if ("CustomExpiryPolicy".equals(sExpiryPolicyClassName))
            {
            Object o = pofReader.readObject(1);

            pofReader.readRemainder();

            return o;
            }
        else
            {
            throw new IOException("Unknown type of expiry policy [" + sExpiryPolicyClassName + "]");
            }
        }
    }
