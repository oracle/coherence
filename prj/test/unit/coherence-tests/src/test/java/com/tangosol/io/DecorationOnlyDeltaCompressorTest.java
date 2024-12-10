/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import static com.oracle.coherence.testing.util.BinaryUtils.*;

import com.oracle.coherence.testing.util.BinaryUtils;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Random;


/**
 * Unit tests for DecorationOnlyDeltaCompresor.
 */
public class DecorationOnlyDeltaCompressorTest
        extends ExternalizableHelper
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
    public void testDecoOnly()
        {
        Binary[] abinDeco1 = new Binary[]
                {
                null,                     // DECO_VALUE
                str2bin("expiry-millis"), // DECO_EXPIRY
                };
        Binary[] abinDeco2 = new Binary[]
                {
                null,                     // DECO_VALUE
                str2bin("expiry-millis"), // DECO_EXPIRY
                str2bin("store"),         // DECO_STORE
                };
        Binary[] abinDeco3 = new Binary[] // test "extended" format
                {
                null,                     // DECO_VALUE
                str2bin("expiry-millis"), // DECO_EXPIRY
                str2bin("store"),         // DECO_STORE
                null,                     // DECO_TX
                null,                     // DECO_PUSHREP
                null,
                null,
                null,                     // DECO_CUSTOM
                str2bin("toplink"),       // DECO_TOPLINK
                };
        Binary[] abinDeco4 = new Binary[] // test "extended" format
                {
                null,                     // DECO_VALUE
                null,                     // DECO_EXPIRY
                null,                     // DECO_STORE
                null,                     // DECO_TX
                null,                     // DECO_PUSHREP
                null,
                null,
                null,                     // DECO_CUSTOM
                str2bin("toplink"),       // DECO_TOPLINK
                };

        Binary bin1, bin2;

        // test decoration-only updates to empty binary
        bin1 = Binary.NO_BINARY;
        bin2 = decorate(bin1, abinDeco1);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        bin2 = decorate(bin1, abinDeco2);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        bin2 = decorate(bin1, abinDeco3);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        bin2 = decorate(bin1, abinDeco4);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        // test decoration-only updates to a binary of empty-string
        bin1 = str2bin("");
        bin2 = decorate(bin1, abinDeco1);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        bin2 = decorate(bin1, abinDeco2);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        bin2 = decorate(bin1, abinDeco3);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        bin2 = decorate(bin1, abinDeco4);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        // test decoration-only updates to an arbitrary binary
        bin1 = str2bin("some arbitrary binary");
        bin2 = decorate(bin1, abinDeco1);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        bin2 = decorate(bin1, abinDeco2);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        bin2 = decorate(bin1, abinDeco3);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);

        bin2 = decorate(bin1, abinDeco4);
        testExtract(bin1, bin2);
        testExtract(bin2, bin1);
        }

    //@Test
    public void testRandom()
        {
        int  cIters        = 0;
        long cbTotalOld    = 0L;
        long cbTotalNew    = 0L;
        long cbTotalDelta  = 0L;
        int  cDeltasLonger = 0;
        int  cbLongest     = 0;

        final Random rnd      = getRandom();
        final int    MAX_DECO = DECO_ID_MAX + 1;

        long ldtStop = getSafeTimeMillis() + 60000;
        do
            {
            Binary binOld = getRandomBinary(0, 1000);
            Binary binNew = rnd.nextBoolean() ? alter(binOld) : binOld;
            if (isDecorated(binOld) || isDecorated(binNew) ||
                binOld.byteAt(0) == FMT_UNKNOWN || binNew.byteAt(0) == FMT_UNKNOWN)
                {
                continue;
                }

            Binary[] abinOld = new Binary[MAX_DECO];
            Binary[] abinNew = new Binary[MAX_DECO];

            if (rnd.nextInt(5) > 0) // 20% with no old decorations
                {
                for (int i = 0, c = rnd.nextInt(MAX_DECO); i < c; ++i)
                    {
                    int    iDeco = rnd.nextInt(DECO_ID_MAX) + 1; // note: value is 0
                    Binary bin   = getRandomBinary(0,  100);
                    abinOld[iDeco] = bin;
                    abinNew[iDeco] = bin;
                    }
                }

            if (rnd.nextInt(5) == 0) // 20% wth no new decorations
                {
                // delete all
                for (int i = 0, c = abinNew.length; i < c; ++i)
                    {
                    abinNew[i] = null;
                    }
                }
            else
                {
                for (int i = 0, c = rnd.nextInt(MAX_DECO); i < c; ++i)
                    {
                    int iDeco = rnd.nextInt(DECO_ID_MAX) + 1; // note: value is 0
                    if (rnd.nextInt(3) == 0)
                        {
                        // 33% deletes
                        abinNew[iDeco] = null;
                        }
                    else
                        {
                        // 66% inserts or updates
                        Binary bin = abinOld[iDeco];
                        abinNew[iDeco] = (bin == null || rnd.nextInt(4) == 0)
                                ? getRandomBinary(0,  100) : alter(bin);
                        }
                    }
                }

            // decorate
            Binary binOldDecorated = decorate(binOld, abinOld);
            Binary binNewDecorated = decorate(binNew, abinNew);
            int cbDelta = testExtract(binOldDecorated, binNewDecorated);

            ++cIters;
            cbTotalOld   += binOldDecorated.length();
            cbTotalNew   += binNewDecorated.length();
            cbTotalDelta += cbDelta;
            if (cbDelta > binNewDecorated.length())
                {
                ++cDeltasLonger;
                int cbLonger = cbDelta - binNewDecorated.length();
                if (cbLonger > cbLongest)
                    {
                    cbLongest = cbLonger;
                    }
                }
            }
        while (getSafeTimeMillis() < ldtStop);


        out("Results of DecorationOnlyCompressorTest.testRandom():");
        out(" iters=" + cIters
                + ", avg old=" + (cbTotalOld / cIters)
                + ", avg new=" + (cbTotalNew / cIters)
                + ", avg delta=" + (cbTotalDelta / cIters)
                + ", avg savings=" + toString(100.0 * (cbTotalNew - cbTotalDelta) / cbTotalNew, 2) + "%"
                + ", deltas longer=" + toString(100.0 * cDeltasLonger / cIters, 2) + "%"
                + ", max extra=" + cbLongest + " bytes");
        }

    @Test
    public void testRegression1()
        {
        String sOld =
            "0x12D9A4054A44015517CE81EDE94316B995A4850E4FA66A712A8F475087" +
            "A90E1FC93A9554B224B7B61747D8EAE0F5872F3845BF6FE6E381F5DA666E" +
            "3CB124E247F78419191E0D6C781A8520FE6E7DBC3137B53516C8F559F256" +
            "C91B6A64F015DF968BC77B59BBDC285D563D3D21A233A9B1E939CF4F57B3" +
            "570368B3F81349DA7CFD136DF0736F7AFF1118A6CE6E410D68D701212E5F" +
            "6652457DFE7240537B5053D91A2C243F70B71FDA2006762EA798706EF5CF" +
            "63DBAF3F2E11E58D1F5F44E7D6A8EA82A1D49FAF7251FB6E9C1C76468150" +
            "939E812D4C15E2447338B795C2B2443E82D0BB0D209F608270A7855820BF" +
            "78EBCF2B5DC24BEBAA4A32CD98F6F5A1D4444B8F770907D9AF6F62614BE7" +
            "EF1F1CC063FDA38C33B52E8FDF757EAAD0B37FB420C0D1AB2330138B7660" +
            "C6309FA047E5B904EF7E89220B323F02EE7AD78D645EA53FDE1AD2037E3F" +
            "90DBC480812398842E63CF1471543385BED68F545216FF67EAFFB142E062" +
            "289101383F7B41F3BC7207D15F549819545D0A7F33FC4B78BE837EDFD80A" +
            "F88D6E646ADEAFDC458441A186A38509A8A30AC744D9BC48FC5420F42816" +
            "9AC4F8F08A660478D54CF3AE15BDD7B14DB0D26ABCCC1D1680014FF2CFDD" +
            "78A43C4A91E3FBA7D66A453F457C02D020F535E2F273DC0DB8E57067C2DA" +
            "20F3E351DA6753CC9CC99FD46A8AAD550497B9A50A6F1EF7576EE1CF00AB" +
            "37E6264C8D2992099BF9E7F80A9AB58E72E89E2EA5989C61D9F2D26CB7CF" +
            "3504A9282144B95D08840D94DB70961B1EFEDE70A349873CE73C9D01D726" +
            "D62ED90B29233940FFD89EB1A7FA8A9E9F5395A6159BC4381E383D57A7F6" +
            "D04E30626FA3925086DD939E99C574B1653EFDE80B74A28E84C6960D397D" +
            "2E348B773A70238EF65DBB4691D7438A3C5842711932ACEEF61D789A56A542";

        String sNew =
            "0x127BA4054A44015517CE81EDE94316B995A4850E4FA66A712A8F475087" +
            "A90E1FC93A9554B224B7B61747D8EAE0F5872F3845BF6FE6E381F5DA666E" +
            "3CB124E247F78419191E0D6C781A8520FE6E7DBC3137B53516C8F559F256" +
            "C91B6A64F015DF968BC77B59BBDC285D563D3D21A233A9B1E939CF4F57B3" +
            "570368B3F81349DA7CFD136DF0736F7AFF1118A6CE6E410D68D701212E5F" +
            "6652457DFE7240537B5053D91A2C243F70B71FDA2006762EA798706EF5CF" +
            "63DBAF3F2E11E58D1F5F44E7D6A8EA82A1D49FAF7251FB6E9C1C76468150" +
            "939E812D4C15E2447338B795C2B2443E82D0BB0D209F608270A7855820BF" +
            "78EBCF2B5DC24BEBAA4A32CD98F6F5A1D4444B8F770907D9AF6F62614BE7" +
            "EF1F1CC063FDA38C33B52E8FDF757EAAD0B37FB420C0D1AB2330138B7660" +
            "C6309FA047E5B904EF7E89220B323F02EE7AD78D645EA53FDE1AD2037E3F" +
            "90DBC480812398842E63CF1471543385BED68F545216FF67EAFFB142E062" +
            "2810D9EF8BEF7C8721E8B8800B3A5076A5B88F01383F7B41F3BC7207D15F" +
            "549819545D0A7F33FC4B78BE837EDFD80AF88D6E646ADEAFDC4584418485" +
            "09A8A30AC744D9BC48FC5420F428169AC4F8F08A660478D54CF3AE15BDD7" +
            "B14DB0D26ABCCC1D169E014FDACFCDDCDD78A43C86693EFBA7DE09D094D7" +
            "5925870CECD8511F676AAB94453F457C02D020F535E2F273DC0DB8E57067" +
            "C2DA20EFDA6753CC9CC99FD46A8AAD55041370EDE1BDA289C3BD3A7E9D5A" +
            "E0188486D7F2539F1C6929F5D900AB0503841AEE1A37E6264C8D2992099B" +
            "F9E7F80A9AB58E72E89E2EA5989C61D9F2D26CB7CF3504A9282144B95D08" +
            "840D94DB70961B1EFEDE70A349873CE73C";

        testExtract(new Binary(parseHex(sOld)), new Binary(parseHex(sNew)));
        }

    @Test
    public void testRegression2()
        {
        String sOld =
            "0xFCB8FA41B6A0743BBC912B4D2F28E65694BB20F30CE223DD17D1875599" +
            "817FFBA77B131D2C2CB05792EACAC1A9CE5C39890AC553781BFCB84BC8F8" +
            "37C7854B8AB5277F1D1AD078FB9440316467072BECF15451185AD94CA5AF" +
            "C01F7D7A6B94FCAF8809823402AE49D95F74F527322D50B6221BAF25D332" +
            "68899EAC53BACBA7F90107EEEC45DDD95CF9551281DC2B307DE7024A7F9A" +
            "22D238E59B3B2A04DC31BE7BE0306DAA2FF180929EBEDE2D41486235CCBB" +
            "D951BA78D811BD7897C9080134D1F0B6C7D5D99686C45FF8CAF5F023FE77" +
            "6D009D372C29D821558DA2ED5FB2531B0A5E23CB7CFC7EB5B67BA0C17E04" +
            "A5264ECF610AC047F16C9A09C548CB90B655DD97A1A778BA380F14E02C55" +
            "554615E13E988FDFD963B50D4BBC33281B31FEE072495B153B39C77DA8F5" +
            "522739A38EEBF9114B9DB0579E63612A78CEE0941EFF32F1AFD7DD853731" +
            "E1B32DE24A98BBA833E8CA805438B591D89828EC18DEA65B7E55D34415AC" +
            "5661F4CA2EE5D999FF233BEB409605D09BFF80DA982DA6DDDB80D63D0CC4" +
            "6EEF16C57D2A44C206FFA78B2A633045DA9CC1D7E453B2AD9B06D75E96F8" +
            "2455DF7DB21B2B0F68BF35CF90B2A768BCC3B58DF2883CE7BD7D5790E3C8" +
            "3AAF2409A27523A478691F5851DB098D3465339D8EF73A4773BACE216DD1" +
            "02F83ACB86866658EAA47DAF52BBDCCC2BB70F1DDF31057D51893B09A911" +
            "601DD80971EDCB84CF4565E5C86B9D4AD61458EDA4E4EA334AE2548B3375" +
            "743B2EAF3B7C123A6E5341972B354D1BA91D6AE451938DE3032C5507B1CA" +
            "1986457FAA6A5BB438FBF85200B7FAE35D2C687D7499392D218731F66A82" +
            "6B8B114C8E32A77E9F7AF9DB41799F4CC246BB300E6AD448B7E795864641" +
            "F494ED54F755D29CF03180790F3E88D0BE184736AF9472168E50CC0EB11A" +
            "3512A4749E71C56D8E7A176519C3126C8DF5191AA591396D2368BD1B1795" +
            "074B833FEE22467008CD207C0B938F4A41AA8E9BE6D5B2B84C2FCE7B5882" +
            "CF91CE7216F4CD4C3EA563BBA2C8855A4968D767680583DDE80F1A62CC1C" +
            "C26E65BD3B5F8F30DCECEBA725C9107543DA9758F812108F74EBF9806E9E" +
            "924F2CB376F775A97A49B6A9D9D1EC17D311106AA7BA26510026B631B9F8" +
            "1E6D78CACA38C4C1152EAB64B42A10C25B63B31CA07A79134D9E29CA56B3" +
            "45E984D6CF78D5F2E9C135C42EFEEFDCF6DDDDBB928B3BA6374A56947713" +
            "68C3B2C70AB58BF75A3FE04113966F669AE21CCA7B25BD46383C672D565D" +
            "01A93C038F8BC81CC41BE39FB8AF3EBD3E40A326C7F3BF8C8DA944D163EA" +
            "41128ACBF679BC33306A4069E1F39A1E5C04B703AFCE0508ABC33007829E" +
            "0E9638080CC9669F0540CAC9CE9CF79EC3C473F0207C";

        String sNew =
            "0x12C19A0C83658082A523DC47B9C8EE2FE09CD6F344D0C012C0B618D235" +
            "86ADDD8FE9FE524BA5AC8A199C429FFCB8FA41B6A0743BBC912B4D2F28E6" +
            "5694BB20F30CE223DD17D1875599817FFBA77B131D2C2CB05792EACAC1A9" +
            "CE5C39890AC553781BFCB84BC8F837C7854B8AB5277F1D1AD078FB944031" +
            "6467072BECF15451185AD94CA5AFC01F7D7A6B94FCAF8809823402AE49D9" +
            "5F74F527322D50D593E8F5A7A2E7B8C2D950A19E14219E0E218C509CE725" +
            "AAF14B284583A21E508B411ED2F6AC2593AD6CBE08A28A357774C6400422" +
            "319C1851103F69FC221BAF25D33268899EF8CAF5F023FE776D009D372C29" +
            "D821558DA2ED5FB2531B0A5E23CB7CFC7EB5B67BA0C17E04A5264ECF610A" +
            "C047F16C9A09C548CB90B6DE02D131B400748F207A2C9A6DE4457FAA6A5B" +
            "B438FBF85200B7FAE35D518384F70A445711702EC187FD830F2B5431DECA" +
            "59EC21B814C845732D14D3F9BBECA746E37D9055D29CF03180790F3E88D0" +
            "BE184736AF941F03D0D8A21806643F7AECD65FE1194B1F19D6FBEA55493D" +
            "DABB407ACE50D9C509A46C3E6E0464F7B522502285B99E218AD12DFBE4BE" +
            "91195CC4A9DB804072168E50CC0EB11A2479475DCBE67D3571D33C0F4333" +
            "A759A0C8F3E96B4A03C0DA8EE85090977D7404E836F20E678B19AFBB193F" +
            "6E9E924F2CB376F775A97A49B6A9D9D1EC17D311106AA7BA26510026B631" +
            "B9F81E6D78CACA38C4C107D5EB6A1D1E41538B080874A5A22BAC4900158B" +
            "B0845213CF8F3DB16265A54A1844DC95CF32FB29B25C80EBF2B14BC55232" +
            "C896BD7FA951561634C3BEFCAE4B6F2C0266670E0C496C186DFAF7050B03" +
            "4369A343DCAAF61367AE203822A188D32EBA71C19BF86AD97F2AB78DAB6B" +
            "8585236F114A601B7E91324DF298E3D5BC2D2EC35088F84CE5839B1ADCF1" +
            "33E3B9C1F0C595DFF902A85A270877D9D524ABCB148D207597A9162C253F" +
            "97AEE10B7F07158296090A0CF9AF1A5C0E4DE6C01A3A9EB32D565D01A93C" +
            "038F8BC81CC41BE39FB8AF3EBD3E40A326C7F3BF8C8DA944D163EA41128A" +
            "CBF679BC33306A4069E1F39A1E5C04B703AFCE0508ABC33007829E0E9638" +
            "080CC9669F0540CAC9CE9CF79EC3C473F0207C27B3BE93B8E467E2043D04" +
            "CEFD418F58A5146F9112CA59966E0399779863A18163C436437A0F9A0E8B" +
            "0176D98704D305CB2AC3C112B1F2A51C784A25AA00A1FE3429AC52B4C4A9" +
            "6C38E4EABB71D41CFD21245D9D16C1897EB9BC8825326E8D64582BE7EF47" +
            "3AD3E89AE62D95AB17CE9F3C248D1DAD";

        testExtract(new Binary(parseHex(sOld)), new Binary(parseHex(sNew)));
        }

    @Test
    public void testRegression3()
        {
        String sOld =
            "0x12718C0ED32A9F55553C5DBB5E5F492EA25928627A488E825C672A4AD2" +
            "39F6952F7EDB9C88EA43A5FF75763AF0ECEE5E9B32B8F0C938D8ABA61D31" +
            "78C52C6977EA66AFA24F2DE6A0696CF23CEFC9F1F7E94D4D94CAE718F1D2" +
            "BB744F0727257F9397B9E91D62EAD9E78B74515B1175D8B56D96AA2D5AC0" +
            "5E668A847EE0F0A665D516C0D8FB840D07524A84E26683F09D5063A90BF4" +
            "5EEAE3AE6F4CEC0D974881C2482F8C872DFA7E88530BB1F777E42DA94F80" +
            "E5F805A6C43864F9D23E0532E04CB75A7A74ED0032E9C5BCEA28A129C6D9" +
            "73AEA46466455858F747729970D2E4CD6ADE5605FF0D86DB7E776AC89D86" +
            "3D9891AEE338B56F4EA5E36E7636922CBF7FD2A222964336014877F5A0DA" +
            "5B75E9C4DB6C64C7ABEF927AB317E5A0B17F5C053D45C4139275201B305A" +
            "5C144B4291E176EE29393E782D7675175E5A7F174EE1E0E67A7F557811C7" +
            "2E7225615FEFF1948938E20AA2D927FC80D9C158BF4CB7FB74A4FB6F7B8A" +
            "47BF89B53160E3849927DDD8F289A4609AB44AAEDB82C3008A205938108C" +
            "B2050DFF845FF9E21ABA4B090EB2A45A515A7B0849708235B61D38814E89" +
            "1B15F9D843823E2D5970B51FB54B4CED1DF5E8F5B3A8E830BEC1B4E58730" +
            "C027FF5A7B0381C96A95C7C21036ED23AC33132427FF490791DFE5ECB91A" +
            "68A69BFB7A42C21C905B218B8D494B192B6D06915B89D92A23DBB81C546D" +
            "58497840B9A446C63144353FB91D23A626AD1B2569A2DC16C6EF03A10AAB" +
            "467245C078BDD44FD274D711B583B587C830BA727BEE17F29692E125F116" +
            "9930B81AB3FEFCC21502D3ED886243B9C17E3473F571F18E1CA3F41BD6E5" +
            "08BF233A88FC7AA38A56289D7C675219FB75BED6B8ACF2E0AB6F67CEDE9F" +
            "D9623C626BB2119F1827C666E51D922B35306AD0DDC98E4E1973EA208033" +
            "8861D4B8118B963F64FF73EA6D428BD7421836B20EE8CF77147F107A75D2" +
            "C51E4E7BD7E8B908A638B10F7C64AB1E86AD211EBC785A5DFF6DA59B8142" +
            "FCDC3FA78A56E37D546DF92421319178B192C88D61C43EC6F4D6DD4A60B0" +
            "AA567838FEA5B533FE932F91E3F732BFBF3FB61BB4ADE8A34418FA7A40CE" +
            "9B01C190FE030A09BCA59A3B300F4F7AFA86CACE1598914BBAE9B1CAEB78" +
            "CCE805842327DA01E66307154E7354C672940F4605C3D2CD99F6ACD76666" +
            "DA7F739C67645D1C69169FDF493DD7316D5A9669C433FF9FB8E38D9F6143" +
            "2EB5164600C0D07FF8BCF4CE6CECC4AD743AC4B1E6774477FD471BE6DFF3" +
            "F517FCF45E05B3F77EE81F15AC3EB038575D0D0B17593E310000E2C17D54" +
            "469635397465B9ED19EEC7BD828DA27FF53068B8DFF609804ABF80F3E0AC" +
            "9B7490365D8EDFA24584F20F3D836A9B83013A8B07DA2B6CC309A006C346" +
            "B66C550206A367611E56B9B1D4FF4EE6B4583BFD387B37114EB031AD5AF1" +
            "CEBF01EF2FF2C72CEF071DF3A7A0434108BC7FCE16465AD2E738E6F14E8B" +
            "F464F3A743E7CC99398EA073CC42916FAA3BDEFCD0EDF391CC5B1DB98DAA" +
            "86B63F1508AF39F9D749763EBD4C087E31636D758D6E";

        String sNew =
            "0x12791B4C10F35FF16CC7CA84766C628BA94EDDBC037620CD73C7693846" +
            "C535DF39004BC9BE3ADE35AB19C3DBC0317DC8075136CB31E55A46A4BFB9" +
            "ED3933ED1474A5165C56A61C8D4222DCD715AF3475612626493EB038575D" +
            "0D0B17593E310000E2C17D54469635397465B9ED19EEC7BD828DA27FF530" +
            "68B8DFF609804ABF80F3E0AC9B7490365D8EDFA24584F20F3D836A9B8301" +
            "3A8B07DA2B6CC309A006C346B66C550206A367611E56B9B1D4FF4EE6B458" +
            "3BFD387B37114EB031AD5AF1CEBF01EF2FF2C72CEF071DF3A7A0434108BC" +
            "7FCE16465AD2E72FE6F14E8BF464F3A743E7CC99398EA073CC42916FAA3B" +
            "DEFCD0ED2E16D81659EBF391BD4C087EBE57120465A9144328";

        testExtract(new Binary(parseHex(sOld)), new Binary(parseHex(sNew)));
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Test the extraction (and corresponding application) of binary deltas
    * using the passed old and new strings as the basis for the delta.
    *
    * @param sOld  the old string value to diff against
    * @param sNew  the new string value
    */
    public static void testExtract(String sOld, String sNew)
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
    public static int testExtract(Binary binOld, Binary binNew)
        {
        try
            {
            int cbDelta = testExtractInternal(binOld, binNew);
            testExtractInternal(binNew, binOld);
            return cbDelta;
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
    * <p/>
    * Identical to {@link BinaryDeltaCompressorTest#testExtractInternal}.
    *
    * @param binOld  the old binary value to diff against
    * @param binNew  the new binary value
    */
    private static int testExtractInternal(Binary binOld, Binary binNew)
        {
        // extract delta
        Binary binDelta = asBinary(s_compressor.extractDelta(binOld, binNew));

        // repeat test with padded binaries
        Binary binDelta2 = asBinary(s_compressor.extractDelta(invisipad(binOld), invisipad(binNew)));
        if (!equals(binDelta, binDelta2))
            {
            fail("binDelta=" + binToHex(binDelta)
                    + ", binDelta2=" + binToHex(binDelta2));
            }

        // repeat test with a different buffer impl
        Binary binDelta3 = asBinary(s_compressor.extractDelta(toNonBinary(binOld), toNonBinary(binNew)));
        if (!equals(binDelta, binDelta3))
            {
            fail("binDelta=" + binToHex(binDelta)
                    + ", binDelta3=" + binToHex(binDelta3));
            }

        if (binDelta == null)
            {
            if (!equals(binOld, binNew))
                {
                fail("binOld=" + binToHex(binOld)
                        + ", binNew=" + binToHex(binNew));
                }
            }
        else
            {
            // apply delta
            Binary binCheck = asBinary(s_compressor.applyDelta(binOld, binDelta));
            if (!equals(binNew, binCheck))
                {
                fail("binNew=" + binToHex(binNew)
                        + ", binCheck=" + binToHex(binCheck));
                }

            // repeat test with padded binaries
            Binary binCheck2 = asBinary(s_compressor.applyDelta(invisipad(binOld), invisipad(binDelta)));
            if (!equals(binCheck, binCheck2))
                {
                fail("binCheck=" + binToHex(binCheck)
                        + ", binCheck2=" + binToHex(binCheck2));
                }

            // repeat test with a different buffer impl
            Binary binCheck3 = asBinary(s_compressor.applyDelta(toNonBinary(binOld), toNonBinary(binDelta)));
            if (!equals(binCheck, binCheck3))
                {
                fail("binCheck=" + binToHex(binCheck)
                        + ", binCheck3=" + binToHex(binCheck3));
                }

            // check that the generated delta is of the minimal format
            Binary binValueOld = getUndecorated(binOld);
            Binary binValueNew = getUndecorated(binNew);
            if (equals(binValueOld, binValueNew))
                {
                // decoration-only update; the "delta" should be an optionally
                // decorated decoration token
                ReadBuffer[] abufDecoDelta = getDecorations(binDelta);
                ReadBuffer[] abufDecoNew   = getDecorations(binNew);

                assertEquals(DecorationOnlyDeltaCompressor.BIN_DECO_ONLY, getUndecorated(binDelta));
                abufDecoDelta[DECO_VALUE] = null;
                abufDecoNew  [DECO_VALUE] = null;
                assertArrayEquals(abufDecoDelta, abufDecoNew);
                }
            else
                {
                // the "delta" should be a whole-value replacement
                assertEquals(binNew, binDelta);
                }
            }

        return binDelta == null ? 0 : binDelta.length();
        }


    // ----- constants ------------------------------------------------------

    /**
    * The instance to test.
    */
    public static final DecorationOnlyDeltaCompressor s_compressor
            = new DecorationOnlyDeltaCompressor();
    }
