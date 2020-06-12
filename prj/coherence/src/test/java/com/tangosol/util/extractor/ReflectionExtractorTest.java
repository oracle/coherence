/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.extractor;


import data.extractor.InvokeTestClass;

import com.tangosol.io.WriteBuffer;

import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.WrapperException;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
* Unit test of the {@link ReflectionExtractor}.
*
* @author oew 01/22/2007
*/
public class ReflectionExtractorTest
        extends Base
    {
    /**
    * Test the {@link ReflectionExtractor} independent of Coherence.
    */
    @Test
    public void test()
        {
        Object              oTestClass     = new InvokeTestClass();
        ReflectionExtractor extractorTest1 = new ReflectionExtractor("retVal");
        String              sRet           = (String)extractorTest1.extract(oTestClass);
        assertTrue("ReflectionExtractor : Error invoking on No Parameters",
                sRet.equals("Return Value"));

        Integer             iTest2Value    = Integer.valueOf(100);
        Object[]            aoTest2Parm    = {iTest2Value};
        ReflectionExtractor extractorTest2 = new ReflectionExtractor("retVal", aoTest2Parm);
        Integer             iRet           = (Integer) extractorTest2.extract(oTestClass);
        assertTrue("ReflectionExtractor : Error invoking on int Parameter",
                iRet.equals(iTest2Value));

        Object[]            aoTest3Parm    = {Boolean.TRUE};
        ReflectionExtractor extractorTest3 = new ReflectionExtractor("retVal", aoTest3Parm);
        Boolean             bRet           = (Boolean) extractorTest3.extract(oTestClass);
        assertTrue("ReflectionExtractor : Error invoking on boolean Parameter",
                bRet.equals(Boolean.TRUE));

        Object[]            aoTest4Value   = new Object[10];
        Object[]            aoTest4Parm    = {aoTest4Value};
        ReflectionExtractor ExtractorTest4 = new ReflectionExtractor("sumIntTest", aoTest4Parm);
        int                 nExp = 0;

        for (int j = 0, c = aoTest4Value.length; j < c; j++)
            {
            nExp += j + 1;
            aoTest4Value[j] = Integer.valueOf(j + 1);
            }
        iRet = (Integer) ExtractorTest4.extract(oTestClass);
        assertTrue("ReflectionExtractor : Error invoking Array Parameters",
                iRet.intValue() == nExp);

        ReflectionExtractor test4ExtractorDup;
        Binary binTest4Extractor =
                ExternalizableHelper.toBinary(ExtractorTest4);
        test4ExtractorDup = (ReflectionExtractor) ExternalizableHelper
                .fromBinary(binTest4Extractor);
        iRet = (Integer) test4ExtractorDup.extract(oTestClass);
        assertTrue("ReflectionExtractor : Error invoking on ExternalizableLite Duplication",
                iRet.intValue() == nExp);

        WriteBuffer buf = new BinaryWriteBuffer(0);
        WriteBuffer.BufferOutput out = buf.getBufferOutput();

        SimplePofContext ctx            = new SimplePofContext();
        PofWriter writer         = new PofBufferWriter(out, ctx);
        PofSerializer pofSerializer1 = new PortableObjectSerializer(1);
        ctx.registerUserType(1, ExtractorTest4.getClass(), pofSerializer1);
        try
            {
            writer.writeObject(0, ExtractorTest4);

            PofReader reader = new PofBufferReader(
                    buf.getReadBuffer().getBufferInput(), ctx);
            ReflectionExtractor test4ExtractorPOFDup = (ReflectionExtractor) reader.readObject(0);
            iRet = (Integer) test4ExtractorPOFDup.extract(oTestClass);
            assertTrue("ReflectionExtractor : Error invoking on PortableObject Duplication",
                    iRet.intValue() == nExp);
            }
        catch (IOException e)
            {
            fail(e.toString());
            }

        Object[]            aoTest5Parm    = {Integer.valueOf(100), Integer.valueOf(200), Integer.valueOf(300)};
        ReflectionExtractor extractorTest5 = new ReflectionExtractor("sumIntTest", aoTest5Parm);
        iRet  = (Integer) extractorTest5.extract(oTestClass);
        assertTrue("ReflectionExtractor : Error Invoking sum of 3 Integer Parameters.",
                iRet.intValue() == 600);
        }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testDefaultReflectionBlacklist()
        throws Throwable
        {
        ReflectionExtractor extractor = new ReflectionExtractor("exec", new Object[] {});
        try
            {
            extractor.extract(Runtime.getRuntime());
            }
        catch (WrapperException e)
            {
            throw e.getOriginalException();
            }
        }

    @SuppressWarnings("unchecked")
    @Test(expected = IllegalArgumentException.class)
    public void testDefaultReflectionBlacklistWithClass()
        throws Throwable
        {
        ReflectionExtractor extractor = new ReflectionExtractor("getName", new Object[] {});
        try
            {
            extractor.extract(String.class);
            }
        catch (WrapperException e)
            {
            throw e.getOriginalException();
            }
        }
    }
