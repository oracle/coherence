/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;

import com.tangosol.io.WriteBuffer;
import com.tangosol.io.WriteBuffer.BufferOutput;

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HashEncoded;
import com.tangosol.util.NullImplementation;

import com.tangosol.util.extractor.KeyExtractor;

import com.tangosol.util.processor.ExtractorProcessor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

/**
 * HashEncodedTests tests the functionality of Binary caching hashes via the
 * HashEncoded interface, and ensuring PartitionedService obeys the contract.
 *
 * @author hr  2015.12.17
 */
public class HashEncodedTests
        extends AbstractFunctionalTest
    {
    // ----- junit lifecycle methods ----------------------------------------

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * A test to ensure a key that is decorated with the reserved value is
     * correctly handled (avoided).
     *
     * @see PartitionedService$ConverterKeyToBinary
     */
    @Test
    public void testReservedHash()
        {
        NamedCache cache = getNamedCache("dist-reservedHashTest", NullImplementation.getClassLoader(), TypeAssertion.WITH_RAW_TYPES);

        Binary binKey   = ExternalizableHelper.toBinary("foo", cache.getCacheService().getSerializer());
               binKey   = decorateBinary(binKey);
        Binary binValue = ExternalizableHelper.toBinary("bar", cache.getCacheService().getSerializer());

        cache.put(binKey, binValue);

        assertNotNull("Unable to deserialize a key with a hashCode of: " + HashEncoded.UNENCODED,
            cache.invoke(binKey, new ExtractorProcessor<>(new KeyExtractor())));
        }

    /**
     * Decorate the specified Binary with a HashEncoded.UNENCODED value.
     *
     * @param binKey  the Binary to be decorated
     *
     * @return the decorated Binary
     */
    public static Binary decorateBinary(Binary binKey)
        {
        try
            {
            WriteBuffer  buf = new BinaryWriteBuffer(6 + binKey.length());
            BufferOutput out = buf.getBufferOutput();

            out.writeByte(/*EH.FMT_IDO*/ 13);
            out.writePackedInt(HashEncoded.UNENCODED);
            out.writeBuffer(binKey);

            return buf.toBinary();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }
    }
