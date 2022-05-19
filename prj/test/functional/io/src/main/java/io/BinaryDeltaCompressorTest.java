/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;


import com.oracle.coherence.testing.util.BinaryUtils;
import com.tangosol.io.BinaryDeltaCompressor;
import com.tangosol.io.ReadBuffer;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import org.junit.Test;

import static org.junit.Assert.*;

import static com.oracle.coherence.testing.util.BinaryUtils.*;


/**
* Tests for the BinaryDeltaCompressor class.
*
* @author cp  2009.01.22
*/
public class BinaryDeltaCompressorTest
        extends Base
    {
    // ----- unit tests -----------------------------------------------------

    @Test
    public void testEmpty()
        {
        testExtract("", "");
        }

    @Test
    public void testSame()
        {
        String s = "this is the same identical thing in both cases";
        testExtract(s, s);
        }

    @Test
    public void testHeadDiff()
        {
        String s1 = "this is the same identical thing in both cases";
        String s2 = "other than this little bit on the front, " + s1;
        testExtract(s1, s2);
        }

    @Test
    public void testTailDiff()
        {
        String s1 = "this is the same identical thing in both cases";
        String s2 = s1 + " except for something tacked onto the end";
        testExtract(s1, s2);
        }

    @Test
    public void testMidDiff()
        {
        String s1 = "this is almost the same identical thing in both cases";
        String s2 = "this is almost the same NOT identical thing in both cases";
        testExtract(s1, s2);
        }

    @Test
    public void testRandom()
        {
        long ldtStop = getSafeTimeMillis() + 60000;
        do
            {
            Binary binOld = getRandomBinary(0, 1000);
            Binary binNew = alter(binOld);
            testExtract(binOld, binNew);
            }
        while (getSafeTimeMillis() < ldtStop);
        }

    @Test
    public void testRegression1()
        {
        String sOld =
            "0x4C47CDECF44E2223A2B28332F77157B7F42D33F31844B7FBB4D9AA85E8" +
            "58A88118E12FBC779BECFFD0124B2A244EC78D877C8FA965035D3D2158FF" +
            "6E5798744BD6B7F1591F9BF38B9F469BA856EA4FA28434A5AA1E90B0A1D7" +
            "92AB23943222CB5F0A6763AFBC896FF2850207DA6DB0B525366450C31B9E" +
            "B4EFC620DA3771DCEA6DCE90C403260C5132F48A8834DFB3DDC72243B127" +
            "6C289E43D1201D4CBC5C2B22293FDE8802D31FCE0C93869A56D2146EDD40" +
            "29F3917F47F1E2603CD4745C2D8277DAC3BF216DD6B71F97D27700D6A83E" +
            "9B650421EFC71EDE451E76439F7734F4E614C9BEE540F4E0DDFDF3EAEF2D" +
            "D703842585818D4B613031D79F035ECAF2C59D069EAE81EC17C4F539F944" +
            "C1D850AAE246AB37C50F51D3AD99E8F03BD3CBA47376C64E88FF34DFBFD2" +
            "6B8E0C11F3C6E52D523B3309A0F68A9A6BDB466444273A514F6ECAE7D834" +
            "FB259701A825BD0CF0AF77D7FF67A88B6206AE07FAAC844595914375852B" +
            "D4C699F4B98BBA27EFD778D0BE2D75B6BECAAD3ECFAFF8A6A8DB631A88DC" +
            "E14A661F9D0CEB519E5848A57C098CAFE5C307AEE9F4267E986F1A27B845" +
            "74B1C1E2BBCC02A2712793818F977D016BA5E295C176CBD548153C585950" +
            "602AB315DFEA27";
        String sNew =
            "0x4C47CDECF44E2223A2B28332F77157B7F42D33F31844B7FBB4D9AA85E8" +
            "58A88118E12FBC779BECFFD0124B2A244EC78D877C8FA965035D3D2158FF" +
            "6E5798744BD6B7F1591F9BF38B9F469BA856EA4FA28434A5AA1E90B0A1D7" +
            "92AB23943222CB5F0A6763AFBC896FF2850207DA3771DCEA6DCE90C40326" +
            "0C5132F48A8834DFB3DDC72243B1276C289E43D1201D4CBC5C2B22293FDE" +
            "8802D31FCE0C93869A56D2146EDD4029F3917F47F1E2603CD4745C2D8277" +
            "DAC3BF216DD6B71F97D27700D6A83E9B650421EFC71EDE451E76439F7734" +
            "F4E614C9BEE540F4E0DDFDF3EAEF2DD703842585818D4B613031D79F035E" +
            "CAF2C59D069EAE81EC17C4F539F944C1D850AAE246AB37C50F51D3AD99E8" +
            "F03BD3CBA47376C64E88FF34DFBFD26B8E0C11F3C6E52D523B3309A0F68A" +
            "9A6BDB466444273A514F6ECAE7D834FB259701A825BD0CF0AF77D7FF67A8" +
            "8B6206AE07FAAC844595914375852BD4C699F4B98BBA27EFD778D0BE2D75" +
            "B6BECAAD3ECFAFF8A6A8DB631A88DCE14A661F9D0CEB519E5848A57C098C" +
            "AFE5C307AEE9F4267E986F1A27B84574B1C1E2BBCC02A2712793818F977D" +
            "016BA5E295C176CBD548153C585950602AB315DFEA27"                ;

        testExtract(new Binary(parseHex(sOld)), new Binary(parseHex(sNew)));
        }


    // ----- internal--------------------------------------------------------

    /**
    * Test the extraction (and corresponding application) of binary deltas
    * using the passed old and new strings as the basis for the delta.
    *
    * @param sOld  the old string value to diff against
    * @param sNew  the new string value
    */
    public void testExtract(String sOld, String sNew)
        {
        Binary binOld = str2bin(sOld);
        Binary binNew = str2bin(sNew);

        try
            {
            testExtractInternal(binOld, binNew);
            testExtractInternal(binNew, binOld);
            }
        catch (AssertionError e)
            {
            err("Old value=" + str(sOld) + ", new value=" + str(sNew));
            throw e;
            }
        }


    /**
    * Test the extraction (and corresponding application) of binary deltas
    * using the passed old and new binary values as the basis for the delta.
    *
    * @param binOld  the old binary value to diff against
    * @param binNew  the new binary value
    */
    public void testExtract(Binary binOld, Binary binNew)
        {
        try
            {
            testExtractInternal(binOld, binNew);
            testExtractInternal(binNew, binOld);
            }
        catch (AssertionError e)
            {
            err("Old value=" + toHexEscape(binOld.toByteArray()) +
                ", new value=" + toHexEscape(binNew.toByteArray()));
            throw e;
            }
        }

    /**
    * Test the extraction (and corresponding application) of binary deltas
    * using the passed old and new binary values as the basis for the delta.
    * <p/>
    * This test includes several sub-tests:
    * <li>padded Binary values (see {@link BinaryUtils#invisipad})</li>
    * <li>alternative ReadBuffer implementations (not Binary, nor derived
    * from AbstractByteArrayReadBuffer)</li>
    *
    * @param binOld  the old binary value to diff against
    * @param binNew  the new binary value
    */
    protected static void testExtractInternal(Binary binOld, Binary binNew)
        {
        // extract delta
        ReadBuffer bufDelta = s_compressor.extractDelta(binOld, binNew);

        // repeat test with padded binaries
        ReadBuffer bufDelta2 = s_compressor.extractDelta(invisipad(binOld), invisipad(binNew));
        if (!buffersEqual(bufDelta, bufDelta2))
            {
            fail("binDelta=" + binToHex(bufDelta)
                    + ", binDelta2=" + binToHex(bufDelta2));
            }

        // repeat test with a different buffer impl
        ReadBuffer bufDelta3 = s_compressor.extractDelta(toNonBinary(binOld), toNonBinary(binNew));
        if (!buffersEqual(bufDelta, bufDelta3))
            {
            fail("binDelta=" + binToHex(bufDelta)
                    + ", binDelta3=" + binToHex(bufDelta3));
            }

        if (bufDelta == null)
            {
            if (!buffersEqual(binOld, binNew))
                {
                fail("binOld=" + binToHex(binOld)
                        + ", binNew=" + binToHex(binNew));
                }
            }
        else
            {
            // apply delta
            ReadBuffer bufCheck = s_compressor.applyDelta(binOld, bufDelta);
            if (!buffersEqual(binNew, bufCheck))
                {
                fail("binNew=" + binToHex(binNew)
                        + ", binCheck=" + binToHex(bufCheck));
                }

            // repeat test with padded binaries
            ReadBuffer bufCheck2 = s_compressor.applyDelta(invisipad(binOld), invisipad(bufDelta.toBinary()));
            if (!buffersEqual(bufCheck, bufCheck2))
                {
                fail("binCheck=" + binToHex(bufCheck)
                        + ", binCheck2=" + binToHex(bufCheck2));
                }

            // repeat test with a different buffer impl
            ReadBuffer bufCheck3 = s_compressor.applyDelta(toNonBinary(binOld), toNonBinary(bufDelta.toBinary()));
            if (!buffersEqual(bufCheck, bufCheck3))
                {
                fail("binCheck=" + binToHex(bufCheck)
                        + ", binCheck3=" + binToHex(bufCheck3));
                }
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The instance to test. (BinaryDeltaCompressor is stateless so only one
    * instance is required.)
    */
    public static final BinaryDeltaCompressor s_compressor = new BinaryDeltaCompressor();
    }