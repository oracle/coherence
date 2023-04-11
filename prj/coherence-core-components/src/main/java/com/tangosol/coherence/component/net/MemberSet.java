
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.MemberSet

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.memberSet.ActualMemberSet;
import com.tangosol.coherence.component.net.memberSet.EmptyMemberSet;
import com.tangosol.coherence.component.net.memberSet.LiteSingleMemberSet;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.util.Base;
import java.util.Set;

/**
 * Set of Member objects; must be thread safe.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class MemberSet
        extends    com.tangosol.coherence.component.Net
        implements com.tangosol.io.ExternalizableLite,
                   java.util.Set
    {
    // ---- Fields declarations ----
    
    /**
     * Property BIT_COUNT
     *
     * Array that maps a byte value to a number of bits set in that byte.
     */
    protected static final int[] BIT_COUNT;
    
    /**
     * Property BIT_ID
     *
     * Array that maps a byte with one and only one bit set to an Id that this
     * bit represents. Note that Ids are 1-based so the bit 0 (byte 0x01)
     * represents an Id equal to 1.
     */
    protected static final int[] BIT_ID;
    
    /**
     * Property BIT_LEFTMOST
     *
     * Array that maps a byte value to the bit position (0..7) of its most
     * significant bit.
     */
    protected static final int[] BIT_LEFTMOST;
    
    /**
     * Property BIT_RIGHTMOST
     *
     * Array that maps a byte value to the bit position (0..7) of its least
     * significant bit.
     */
    protected static final int[] BIT_RIGHTMOST;
    
    /**
     * Property BitSet
     *
     * The array of 32-bit integer values that hold the bit-set information.
     */
    private int[] __m_BitSet;
    
    /**
     * Property Member
     *
     * Members, indexed by Member mini-id. May not be supported if the
     * MemberSet does not hold on the Members that are in the MemberSet.
     */
    private Member[] __m_Member;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        try
            {
            int[] a0 = new int[256];
                {
                a0[1] = 1;
                a0[2] = 1;
                a0[3] = 2;
                a0[4] = 1;
                a0[5] = 2;
                a0[6] = 2;
                a0[7] = 3;
                a0[8] = 1;
                a0[9] = 2;
                a0[10] = 2;
                a0[11] = 3;
                a0[12] = 2;
                a0[13] = 3;
                a0[14] = 3;
                a0[15] = 4;
                a0[16] = 1;
                a0[17] = 2;
                a0[18] = 2;
                a0[19] = 3;
                a0[20] = 2;
                a0[21] = 3;
                a0[22] = 3;
                a0[23] = 4;
                a0[24] = 2;
                a0[25] = 3;
                a0[26] = 3;
                a0[27] = 4;
                a0[28] = 3;
                a0[29] = 4;
                a0[30] = 4;
                a0[31] = 5;
                a0[32] = 1;
                a0[33] = 2;
                a0[34] = 2;
                a0[35] = 3;
                a0[36] = 2;
                a0[37] = 3;
                a0[38] = 3;
                a0[39] = 4;
                a0[40] = 2;
                a0[41] = 3;
                a0[42] = 3;
                a0[43] = 4;
                a0[44] = 3;
                a0[45] = 4;
                a0[46] = 4;
                a0[47] = 5;
                a0[48] = 2;
                a0[49] = 3;
                a0[50] = 3;
                a0[51] = 4;
                a0[52] = 3;
                a0[53] = 4;
                a0[54] = 4;
                a0[55] = 5;
                a0[56] = 3;
                a0[57] = 4;
                a0[58] = 4;
                a0[59] = 5;
                a0[60] = 4;
                a0[61] = 5;
                a0[62] = 5;
                a0[63] = 6;
                a0[64] = 1;
                a0[65] = 2;
                a0[66] = 2;
                a0[67] = 3;
                a0[68] = 2;
                a0[69] = 3;
                a0[70] = 3;
                a0[71] = 4;
                a0[72] = 2;
                a0[73] = 3;
                a0[74] = 3;
                a0[75] = 4;
                a0[76] = 3;
                a0[77] = 4;
                a0[78] = 4;
                a0[79] = 5;
                a0[80] = 2;
                a0[81] = 3;
                a0[82] = 3;
                a0[83] = 4;
                a0[84] = 3;
                a0[85] = 4;
                a0[86] = 4;
                a0[87] = 5;
                a0[88] = 3;
                a0[89] = 4;
                a0[90] = 4;
                a0[91] = 5;
                a0[92] = 4;
                a0[93] = 5;
                a0[94] = 5;
                a0[95] = 6;
                a0[96] = 2;
                a0[97] = 3;
                a0[98] = 3;
                a0[99] = 4;
                a0[100] = 3;
                a0[101] = 4;
                a0[102] = 4;
                a0[103] = 5;
                a0[104] = 3;
                a0[105] = 4;
                a0[106] = 4;
                a0[107] = 5;
                a0[108] = 4;
                a0[109] = 5;
                a0[110] = 5;
                a0[111] = 6;
                a0[112] = 3;
                a0[113] = 4;
                a0[114] = 4;
                a0[115] = 5;
                a0[116] = 4;
                a0[117] = 5;
                a0[118] = 5;
                a0[119] = 6;
                a0[120] = 4;
                a0[121] = 5;
                a0[122] = 5;
                a0[123] = 6;
                a0[124] = 5;
                a0[125] = 6;
                a0[126] = 6;
                a0[127] = 7;
                a0[128] = 1;
                a0[129] = 2;
                a0[130] = 2;
                a0[131] = 3;
                a0[132] = 2;
                a0[133] = 3;
                a0[134] = 3;
                a0[135] = 4;
                a0[136] = 2;
                a0[137] = 3;
                a0[138] = 3;
                a0[139] = 4;
                a0[140] = 3;
                a0[141] = 4;
                a0[142] = 4;
                a0[143] = 5;
                a0[144] = 2;
                a0[145] = 3;
                a0[146] = 3;
                a0[147] = 4;
                a0[148] = 3;
                a0[149] = 4;
                a0[150] = 4;
                a0[151] = 5;
                a0[152] = 3;
                a0[153] = 4;
                a0[154] = 4;
                a0[155] = 5;
                a0[156] = 4;
                a0[157] = 5;
                a0[158] = 5;
                a0[159] = 6;
                a0[160] = 2;
                a0[161] = 3;
                a0[162] = 3;
                a0[163] = 4;
                a0[164] = 3;
                a0[165] = 4;
                a0[166] = 4;
                a0[167] = 5;
                a0[168] = 3;
                a0[169] = 4;
                a0[170] = 4;
                a0[171] = 5;
                a0[172] = 4;
                a0[173] = 5;
                a0[174] = 5;
                a0[175] = 6;
                a0[176] = 3;
                a0[177] = 4;
                a0[178] = 4;
                a0[179] = 5;
                a0[180] = 4;
                a0[181] = 5;
                a0[182] = 5;
                a0[183] = 6;
                a0[184] = 4;
                a0[185] = 5;
                a0[186] = 5;
                a0[187] = 6;
                a0[188] = 5;
                a0[189] = 6;
                a0[190] = 6;
                a0[191] = 7;
                a0[192] = 2;
                a0[193] = 3;
                a0[194] = 3;
                a0[195] = 4;
                a0[196] = 3;
                a0[197] = 4;
                a0[198] = 4;
                a0[199] = 5;
                a0[200] = 3;
                a0[201] = 4;
                a0[202] = 4;
                a0[203] = 5;
                a0[204] = 4;
                a0[205] = 5;
                a0[206] = 5;
                a0[207] = 6;
                a0[208] = 3;
                a0[209] = 4;
                a0[210] = 4;
                a0[211] = 5;
                a0[212] = 4;
                a0[213] = 5;
                a0[214] = 5;
                a0[215] = 6;
                a0[216] = 4;
                a0[217] = 5;
                a0[218] = 5;
                a0[219] = 6;
                a0[220] = 5;
                a0[221] = 6;
                a0[222] = 6;
                a0[223] = 7;
                a0[224] = 3;
                a0[225] = 4;
                a0[226] = 4;
                a0[227] = 5;
                a0[228] = 4;
                a0[229] = 5;
                a0[230] = 5;
                a0[231] = 6;
                a0[232] = 4;
                a0[233] = 5;
                a0[234] = 5;
                a0[235] = 6;
                a0[236] = 5;
                a0[237] = 6;
                a0[238] = 6;
                a0[239] = 7;
                a0[240] = 4;
                a0[241] = 5;
                a0[242] = 5;
                a0[243] = 6;
                a0[244] = 5;
                a0[245] = 6;
                a0[246] = 6;
                a0[247] = 7;
                a0[248] = 5;
                a0[249] = 6;
                a0[250] = 6;
                a0[251] = 7;
                a0[252] = 6;
                a0[253] = 7;
                a0[254] = 7;
                a0[255] = 8;
                }
            BIT_COUNT = a0;
            int[] a1 = new int[256];
                {
                a1[1] = 1;
                a1[2] = 2;
                a1[4] = 3;
                a1[8] = 4;
                a1[16] = 5;
                a1[32] = 6;
                a1[64] = 7;
                a1[128] = 8;
                }
            BIT_ID = a1;
            int[] a2 = new int[256];
                {
                a2[0] = -1;
                a2[2] = 1;
                a2[3] = 1;
                a2[4] = 2;
                a2[5] = 2;
                a2[6] = 2;
                a2[7] = 2;
                a2[8] = 3;
                a2[9] = 3;
                a2[10] = 3;
                a2[11] = 3;
                a2[12] = 3;
                a2[13] = 3;
                a2[14] = 3;
                a2[15] = 3;
                a2[16] = 4;
                a2[17] = 4;
                a2[18] = 4;
                a2[19] = 4;
                a2[20] = 4;
                a2[21] = 4;
                a2[22] = 4;
                a2[23] = 4;
                a2[24] = 4;
                a2[25] = 4;
                a2[26] = 4;
                a2[27] = 4;
                a2[28] = 4;
                a2[29] = 4;
                a2[30] = 4;
                a2[31] = 4;
                a2[32] = 5;
                a2[33] = 5;
                a2[34] = 5;
                a2[35] = 5;
                a2[36] = 5;
                a2[37] = 5;
                a2[38] = 5;
                a2[39] = 5;
                a2[40] = 5;
                a2[41] = 5;
                a2[42] = 5;
                a2[43] = 5;
                a2[44] = 5;
                a2[45] = 5;
                a2[46] = 5;
                a2[47] = 5;
                a2[48] = 5;
                a2[49] = 5;
                a2[50] = 5;
                a2[51] = 5;
                a2[52] = 5;
                a2[53] = 5;
                a2[54] = 5;
                a2[55] = 5;
                a2[56] = 5;
                a2[57] = 5;
                a2[58] = 5;
                a2[59] = 5;
                a2[60] = 5;
                a2[61] = 5;
                a2[62] = 5;
                a2[63] = 5;
                a2[64] = 6;
                a2[65] = 6;
                a2[66] = 6;
                a2[67] = 6;
                a2[68] = 6;
                a2[69] = 6;
                a2[70] = 6;
                a2[71] = 6;
                a2[72] = 6;
                a2[73] = 6;
                a2[74] = 6;
                a2[75] = 6;
                a2[76] = 6;
                a2[77] = 6;
                a2[78] = 6;
                a2[79] = 6;
                a2[80] = 6;
                a2[81] = 6;
                a2[82] = 6;
                a2[83] = 6;
                a2[84] = 6;
                a2[85] = 6;
                a2[86] = 6;
                a2[87] = 6;
                a2[88] = 6;
                a2[89] = 6;
                a2[90] = 6;
                a2[91] = 6;
                a2[92] = 6;
                a2[93] = 6;
                a2[94] = 6;
                a2[95] = 6;
                a2[96] = 6;
                a2[97] = 6;
                a2[98] = 6;
                a2[99] = 6;
                a2[100] = 6;
                a2[101] = 6;
                a2[102] = 6;
                a2[103] = 6;
                a2[104] = 6;
                a2[105] = 6;
                a2[106] = 6;
                a2[107] = 6;
                a2[108] = 6;
                a2[109] = 6;
                a2[110] = 6;
                a2[111] = 6;
                a2[112] = 6;
                a2[113] = 6;
                a2[114] = 6;
                a2[115] = 6;
                a2[116] = 6;
                a2[117] = 6;
                a2[118] = 6;
                a2[119] = 6;
                a2[120] = 6;
                a2[121] = 6;
                a2[122] = 6;
                a2[123] = 6;
                a2[124] = 6;
                a2[125] = 6;
                a2[126] = 6;
                a2[127] = 6;
                a2[128] = 7;
                a2[129] = 7;
                a2[130] = 7;
                a2[131] = 7;
                a2[132] = 7;
                a2[133] = 7;
                a2[134] = 7;
                a2[135] = 7;
                a2[136] = 7;
                a2[137] = 7;
                a2[138] = 7;
                a2[139] = 7;
                a2[140] = 7;
                a2[141] = 7;
                a2[142] = 7;
                a2[143] = 7;
                a2[144] = 7;
                a2[145] = 7;
                a2[146] = 7;
                a2[147] = 7;
                a2[148] = 7;
                a2[149] = 7;
                a2[150] = 7;
                a2[151] = 7;
                a2[152] = 7;
                a2[153] = 7;
                a2[154] = 7;
                a2[155] = 7;
                a2[156] = 7;
                a2[157] = 7;
                a2[158] = 7;
                a2[159] = 7;
                a2[160] = 7;
                a2[161] = 7;
                a2[162] = 7;
                a2[163] = 7;
                a2[164] = 7;
                a2[165] = 7;
                a2[166] = 7;
                a2[167] = 7;
                a2[168] = 7;
                a2[169] = 7;
                a2[170] = 7;
                a2[171] = 7;
                a2[172] = 7;
                a2[173] = 7;
                a2[174] = 7;
                a2[175] = 7;
                a2[176] = 7;
                a2[177] = 7;
                a2[178] = 7;
                a2[179] = 7;
                a2[180] = 7;
                a2[181] = 7;
                a2[182] = 7;
                a2[183] = 7;
                a2[184] = 7;
                a2[185] = 7;
                a2[186] = 7;
                a2[187] = 7;
                a2[188] = 7;
                a2[189] = 7;
                a2[190] = 7;
                a2[191] = 7;
                a2[192] = 7;
                a2[193] = 7;
                a2[194] = 7;
                a2[195] = 7;
                a2[196] = 7;
                a2[197] = 7;
                a2[198] = 7;
                a2[199] = 7;
                a2[200] = 7;
                a2[201] = 7;
                a2[202] = 7;
                a2[203] = 7;
                a2[204] = 7;
                a2[205] = 7;
                a2[206] = 7;
                a2[207] = 7;
                a2[208] = 7;
                a2[209] = 7;
                a2[210] = 7;
                a2[211] = 7;
                a2[212] = 7;
                a2[213] = 7;
                a2[214] = 7;
                a2[215] = 7;
                a2[216] = 7;
                a2[217] = 7;
                a2[218] = 7;
                a2[219] = 7;
                a2[220] = 7;
                a2[221] = 7;
                a2[222] = 7;
                a2[223] = 7;
                a2[224] = 7;
                a2[225] = 7;
                a2[226] = 7;
                a2[227] = 7;
                a2[228] = 7;
                a2[229] = 7;
                a2[230] = 7;
                a2[231] = 7;
                a2[232] = 7;
                a2[233] = 7;
                a2[234] = 7;
                a2[235] = 7;
                a2[236] = 7;
                a2[237] = 7;
                a2[238] = 7;
                a2[239] = 7;
                a2[240] = 7;
                a2[241] = 7;
                a2[242] = 7;
                a2[243] = 7;
                a2[244] = 7;
                a2[245] = 7;
                a2[246] = 7;
                a2[247] = 7;
                a2[248] = 7;
                a2[249] = 7;
                a2[250] = 7;
                a2[251] = 7;
                a2[252] = 7;
                a2[253] = 7;
                a2[254] = 7;
                a2[255] = 7;
                }
            BIT_LEFTMOST = a2;
            int[] a3 = new int[256];
                {
                a3[0] = -1;
                a3[2] = 1;
                a3[4] = 2;
                a3[6] = 1;
                a3[8] = 3;
                a3[10] = 1;
                a3[12] = 2;
                a3[14] = 1;
                a3[16] = 4;
                a3[18] = 1;
                a3[20] = 2;
                a3[22] = 1;
                a3[24] = 3;
                a3[26] = 1;
                a3[28] = 2;
                a3[30] = 1;
                a3[32] = 5;
                a3[34] = 1;
                a3[36] = 2;
                a3[38] = 1;
                a3[40] = 3;
                a3[42] = 1;
                a3[44] = 2;
                a3[46] = 1;
                a3[48] = 4;
                a3[50] = 1;
                a3[52] = 2;
                a3[54] = 1;
                a3[56] = 3;
                a3[58] = 1;
                a3[60] = 2;
                a3[62] = 1;
                a3[64] = 6;
                a3[66] = 1;
                a3[68] = 2;
                a3[70] = 1;
                a3[72] = 3;
                a3[74] = 1;
                a3[76] = 2;
                a3[78] = 1;
                a3[80] = 4;
                a3[82] = 1;
                a3[84] = 2;
                a3[86] = 1;
                a3[88] = 3;
                a3[90] = 1;
                a3[92] = 2;
                a3[94] = 1;
                a3[96] = 5;
                a3[98] = 1;
                a3[100] = 2;
                a3[102] = 1;
                a3[104] = 3;
                a3[106] = 1;
                a3[108] = 2;
                a3[110] = 1;
                a3[112] = 4;
                a3[114] = 1;
                a3[116] = 2;
                a3[118] = 1;
                a3[120] = 3;
                a3[122] = 1;
                a3[124] = 2;
                a3[126] = 1;
                a3[128] = 7;
                a3[130] = 1;
                a3[132] = 2;
                a3[134] = 1;
                a3[136] = 3;
                a3[138] = 1;
                a3[140] = 2;
                a3[142] = 1;
                a3[144] = 4;
                a3[146] = 1;
                a3[148] = 2;
                a3[150] = 1;
                a3[152] = 3;
                a3[154] = 1;
                a3[156] = 2;
                a3[158] = 1;
                a3[160] = 5;
                a3[162] = 1;
                a3[164] = 2;
                a3[166] = 1;
                a3[168] = 3;
                a3[170] = 1;
                a3[172] = 2;
                a3[174] = 1;
                a3[176] = 4;
                a3[178] = 1;
                a3[180] = 2;
                a3[182] = 1;
                a3[184] = 3;
                a3[186] = 1;
                a3[188] = 2;
                a3[190] = 1;
                a3[192] = 6;
                a3[194] = 1;
                a3[196] = 2;
                a3[198] = 1;
                a3[200] = 3;
                a3[202] = 1;
                a3[204] = 2;
                a3[206] = 1;
                a3[208] = 4;
                a3[210] = 1;
                a3[212] = 2;
                a3[214] = 1;
                a3[216] = 3;
                a3[218] = 1;
                a3[220] = 2;
                a3[222] = 1;
                a3[224] = 5;
                a3[226] = 1;
                a3[228] = 2;
                a3[230] = 1;
                a3[232] = 3;
                a3[234] = 1;
                a3[236] = 2;
                a3[238] = 1;
                a3[240] = 4;
                a3[242] = 1;
                a3[244] = 2;
                a3[246] = 1;
                a3[248] = 3;
                a3[250] = 1;
                a3[252] = 2;
                a3[254] = 1;
                }
            BIT_RIGHTMOST = a3;
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Iterator", MemberSet.Iterator.get_CLASS());
        }
    
    // Default constructor
    public MemberSet()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public MemberSet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        
        if (fInit)
            {
            __init();
            }
        }
    
    // Main initializer
    public void __init()
        {
        // private initialization
        __initPrivate();
        
        
        // containment initialization: children
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.net.MemberSet();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/net/MemberSet".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    // From interface: java.util.Set
    public synchronized boolean add(Object o)
        {
        Member member = (Member) o;
        
        int iSet  = member.getByteOffset();
        int nSet  = getBitSet(iSet);
        int nMask = member.getByteMask();
        
        if ((nSet & nMask) == 0)
            {
            nSet |= nMask;
            setBitSet(iSet, nSet);
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // From interface: java.util.Set
    public synchronized boolean addAll(java.util.Collection collection)
        {
        // import java.util.Iterator as java.util.Iterator;
        
        boolean fMod = false;
        
        if (collection instanceof MemberSet)
            {
            MemberSet that   = (MemberSet) collection;
            int[]     anThat = that.getBitSet();
            int       cThat  = (anThat == null ? 0 : anThat.length);
            for (int i = cThat - 1; i >= 0; --i)
                {
                int nThis;
                int nThat = anThat[i];
                if (nThat != 0)
                    {
                    nThis = getBitSet(i);
                    if (nThis != nThat)
                        {
                        setBitSet(i, nThis | nThat);
                        fMod = true;
                        }
                    }
                }
            }
        else
            {
            for (java.util.Iterator iter = collection.iterator(); iter.hasNext(); )
                {
                fMod = fMod | add(iter.next());
                }
            }
        
        return fMod;
        }
    
    // From interface: java.util.Set
    public synchronized void clear()
        {
        setBitSet(null);
        }
    
    public synchronized boolean contains(int nId)
        {
        if (nId > 0)
            {
            int nSet  = Member.calcByteOffset(nId);
            int nMask = Member.calcByteMask(nId);
        
            return (getBitSet(nSet) & nMask) != 0;
            }
        return false;
        }
    
    // From interface: java.util.Set
    public synchronized boolean contains(Object o)
        {
        Member member = (Member) o;
        
        int iSet  = member.getByteOffset();
        int nMask = member.getByteMask();
        
        return (getBitSet(iSet) & nMask) != 0;
        }
    
    // From interface: java.util.Set
    public synchronized boolean containsAll(java.util.Collection collection)
        {
        // import java.util.Iterator as java.util.Iterator;
        
        if (collection instanceof MemberSet)
            {
            MemberSet that   = (MemberSet) collection;
            int[]     anThat = that.getBitSet();
            int       cThat  = (anThat == null ? 0 : anThat.length);
            for (int i = 0; i < cThat; ++i)
                {
                int nThis;
                int nThat = anThat[i];
                if (nThat != 0)
                    {
                    nThis = getBitSet(i);
                    if (nThis != (nThis | nThat))
                        {
                        return false;
                        }
                    }
                }
            }
        else
            {
            for (java.util.Iterator iter = collection.iterator(); iter.hasNext(); )
                {
                if (!contains(iter.next()))
                    {
                    return false;
                    }
                }
            }
        
        return true;
        }
    
    public static int countBits(int n)
        {
        final int[] BIT_COUNT = MemberSet.BIT_COUNT;
        return BIT_COUNT[(n & 0x000000FF)       ]
             + BIT_COUNT[(n & 0x0000FF00) >>>  8]
             + BIT_COUNT[(n & 0x00FF0000) >>> 16]
             + BIT_COUNT[(n & 0xFF000000) >>> 24];
        }
    
    // From interface: java.util.Set
    // Declared at the super level
    public boolean equals(Object obj)
        {
        // import java.util.Set;
        
        if (obj == this)
            {
            return true;
            }
        
        if (obj instanceof Set)
            {
            Set that = (Set) obj;
            return this.size() == that.size()
                && this.containsAll(that);
            }
        
        return false;
        }
    
    // Accessor for the property "BitSet"
    /**
     * Getter for property BitSet.<p>
    * The array of 32-bit integer values that hold the bit-set information.
     */
    protected int[] getBitSet()
        {
        return __m_BitSet;
        }
    
    // Accessor for the property "BitSet"
    /**
     * Getter for property BitSet.<p>
    * The array of 32-bit integer values that hold the bit-set information.
     */
    public int getBitSet(int i)
        {
        int[] an = getBitSet();
        return (an == null || i >= an.length) ? 0 : an[i];
        }
    
    // Accessor for the property "BitSetCount"
    /**
     * Getter for property BitSetCount.<p>
    * The number of 32-bit integer values that hold the bit-set information.
     */
    public int getBitSetCount()
        {
        int[] an = getBitSet();
        return an == null ? 0 : an.length;
        }
    
    /**
     * Create a new MemberSet that only contains Members from this MemberSet
    * whose machine id(s) are different from the specified Member's machine id.
     */
    public MemberSet getDistantMembers(Member member)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet;
        // import java.util.Iterator as java.util.Iterator;
        
        MemberSet setOthers  = new ActualMemberSet();
        int       nMachineId = member.getMachineId();
        
        for (java.util.Iterator iter = iterator(); iter.hasNext(); )
            {
            Member memberOther = (Member) iter.next();
            if (memberOther.getMachineId() != nMachineId)
                {
                setOthers.add(memberOther);
                }
            }
        
        return setOthers;
        }
    
    // Accessor for the property "FirstId"
    /**
     * Return the lowest member id contained in this member set, or 0 if empty.
     */
    public int getFirstId()
        {
        int[] an = getBitSet();
        
        if (an != null)
            {
            for (int i = 0, c = an.length; i < c; ++i)
                {
                int n = an[i];
                if (n != 0)
                    {
                    return (i << 5) + getRightmostBit(n) + 1;
                    }
                }
            }
        
        return 0;
        }
    
    // Accessor for the property "IdList"
    /**
     * Getter for property IdList.<p>
     */
    public String getIdList()
        {
        switch (size())
            {
            case 0:
                return "";
        
            case 1:
                return String.valueOf(getFirstId());
        
            default:
                {
                StringBuffer sb     = new StringBuffer();
                boolean      fFirst = true;
                for (int i = 1, c = 32 * getBitSetCount(); i < c; ++i)
                    {
                    if (contains(i))
                        {
                        if (fFirst)
                            {
                            fFirst = false;
                            }
                        else
                            {
                            sb.append(",");
                            }
        
                        sb.append(i);
                        }
                    }
                return sb.toString();
                }
            }
        }
    
    // Accessor for the property "LastId"
    /**
     * Return the highest member id contained in this member set, or 0 if empty.
     */
    public int getLastId()
        {
        int[] an = getBitSet();
        
        if (an != null)
            {
            for (int i = an.length - 1; i >= 0; --i)
                {
                int n = an[i];
                if (n != 0)
                    {
                    return (i << 5) + getLeftmostBit(n) + 1;
                    }
                }
            }
        
        return 0;
        }
    
    /**
     * Determine the most significant bit of an integer value.
    * 
    * @param n  an integer value
    * 
    * @return -1 if no bits set, otherwise the bit position <i>p</i> of the
    * most significant bit such that 1 << p is the most significant bit
     */
    public static int getLeftmostBit(int n)
        {
        if (n == 0)
            {
            return -1;
            }
        
        for (int of = 24; of >= 0; of -= 8)
            {
            int b = (n >>> of) & 0xFF;
            if (b != 0)
                {
                return of + BIT_LEFTMOST[b];
                }
            }
        
        throw new IllegalStateException();
        }
    
    // Accessor for the property "Member"
    /**
     * Getter for property Member.<p>
    * Members, indexed by Member mini-id. May not be supported if the MemberSet
    * does not hold on the Members that are in the MemberSet.
     */
    protected Member[] getMember()
        {
        return __m_Member;
        }
    
    // Accessor for the property "Member"
    /**
     * Getter for property Member.<p>
    * Members, indexed by Member mini-id. May not be supported if the MemberSet
    * does not hold on the Members that are in the MemberSet.
     */
    public Member getMember(int i)
        {
        throw new UnsupportedOperationException();
        }
    
    /**
     * Return the lowest member id contained in this member set that is larger
    * than the specified id ,
    * or 0 if there is none.
     */
    public int getNextId(int nId)
        {
        // import Component.Net.Member;
        
        if (nId <= 0)
            {
            // 0 is not a valid Member id
            return getFirstId();
            }
        
        int[] an = getBitSet();
        if (an != null)
            {
            // mask for all members > nId
            int iMask = ~((Member.calcByteMask(nId) << 1) - 1);
            int iWord = Member.calcByteOffset(nId);
            
            for (int i = iWord, c = an.length; i < c; ++i)
                {
                int n = an[i] & iMask;
                if (n != 0)
                    {            
                    return (i << 5) + getRightmostBit(n) + 1;
                    }
        
                // each subsequent word should be fully evaluated
                iMask = 0xFFFFFFFF;
                }
            }
        
        return 0;
        }
    
    /**
     * Create a new MemberSet by removing the specified Member object from this
    * MemberSet.
     */
    public MemberSet getOtherMembers(Member member)
        {
        // import Component.Net.MemberSet.ActualMemberSet;
        
        MemberSet setOthers = getClass() == MemberSet.class ?
            new MemberSet() : new ActualMemberSet();
        
        setOthers.addAll(this);
        setOthers.remove(member);
        
        return setOthers;
        }
    
    /**
     * Return the lowest member id contained in this member set that is larger
    * than the specified id ,
    * or 0 if there is none.
     */
    public int getPrevId(int nId)
        {
        // import Component.Net.Member;
        
        if (nId <= 0)
            {
            // 0 is not a valid Member id
            return 0;
            }
        
        int[] an = getBitSet();
        if (an != null)
            {
            // mask for all members < nId
            int iMask = Member.calcByteMask(nId) - 1;
            int iWord = Member.calcByteOffset(nId);
            
            for (int i = iWord; i >= 0; --i)
                {
                int n = an[i] & iMask;
                if (n != 0)
                    {            
                    return (i << 5) + getLeftmostBit(n) + 1;
                    }
        
                // each subsequent word should be fully evaluated
                iMask = 0xFFFFFFFF;
                }
            }
        
        return 0;
        }
    
    /**
     * Determine the most significant bit of an integer value.
    * 
    * @param n  an integer value
    * 
    * @return -1 if no bits set, otherwise the bit position <i>p</i> of the
    * most significant bit such that 1 << p is the most significant bit
     */
    public static int getRightmostBit(int n)
        {
        if (n == 0)
            {
            return -1;
            }
        
        for (int of = 0; of < 32; of += 8)
            {
            int b = (n >>> of) & 0xFF;
            if (b != 0)
                {
                return of + BIT_RIGHTMOST[b];
                }
            }
        
        throw new IllegalStateException();
        }
    
    /**
     * Instantiate a MemberSet containing the specified member.  The resulting
    * MemberSet may not be iteratable.
     */
    public static MemberSet instantiate(Member member)
        {
        // import Component.Net.MemberSet.EmptyMemberSet;
        // import Component.Net.MemberSet.SingleMemberSet;
        
        return member == null
            ? (EmptyMemberSet) EmptyMemberSet.get_Instance()
            : SingleMemberSet.instantiate(member);
        }
    
    /**
     * Instantiate a MemberSet containing the same members as the supplied
    * MemberSet.  The resulting MemberSet may not be iteratable.
     */
    public static MemberSet instantiate(MemberSet setMembers)
        {
        // import Component.Net.MemberSet.LiteSingleMemberSet;
        // import Component.Net.MemberSet.EmptyMemberSet;
        
        switch (setMembers.size())
            {
            case 0:
                return (EmptyMemberSet) EmptyMemberSet.get_Instance();
        
            case 1:
                try
                    {
                    return LiteSingleMemberSet.copyFrom(setMembers);
                    }
                catch (UnsupportedOperationException e)
                    {
                    // concurrent grow; fall through
                    }
        
            default:
                MemberSet setCopy = new MemberSet();
                setCopy.addAll(setMembers);
                return setCopy;
            }
        }
    
    // From interface: java.util.Set
    public synchronized boolean isEmpty()
        {
        int[] an = getBitSet();
        if (an == null)
            {
            return true;
            }
        
        for (int i = 0, c = an.length; i < c; ++i)
            {
            if (an[i] != 0)
                {
                return false;
                }
            }
        
        return true;
        }
    
    // From interface: java.util.Set
    public java.util.Iterator iterator()
        {
        return (MemberSet.Iterator) _newChild("Iterator");
        }
    
    /**
     * Randomly select a Member id from the MemberSet
    * 
    * @return a Member id or 0 if no Members are available
     */
    public synchronized int random()
        {
        if (isEmpty())
            {
            return 0;
            }
        
        int[] an = getBitSet();
        int   cn = an.length;
        int   iIndex;
        int   nBits;
        do
            {
            iIndex = (int) (Math.random() * cn);
            nBits  = an[iIndex];
            }
        while (nBits == 0);
        
        int cBits  = countBits(nBits);
        int cIters = (int) (Math.random() * cBits) + 1;
        int nMask  = 1;
        while (nMask != 0)
            {
            if ((nBits & nMask) != 0 && --cIters == 0)
                {
                return translateBit(iIndex, nMask);
                }
        
            nMask <<= 1;
            }
        
        throw new RuntimeException("cIters=" + cIters
                 + ", nBits=" + nBits + ", iIndex=" + iIndex
                 + ", cBits=" + cBits);
        }
    
    /**
     * Ensure all reads made after this call will have visibility to any writes
    * made prior to a corresponding call to writeBarrier for any member in this
    * set on another thread.
     */
    public void readBarrier()
        {
        int[] anWord = getBitSet();
        if (anWord != null)
            {
            for (int iWord = 0, cWords = anWord.length; iWord < cWords; ++iWord)
                {
                int nWord = anWord[iWord];
                if (nWord != 0)
                    {
                    for (int of = 0, nMask = 1; of < 32; ++of, nMask <<= 1)
                        {
                        if ((nWord & nMask) != 0)
                            {
                            int nId = translateBit(iWord, nMask);
                            readBarrier(nId);
                            }
                        }
                    }
                }
            }
        }
    
    /**
     * Ensure all reads made after this call will have visibility to any writes
    * made prior to a corresponding call to writeBarrier for the same member id.
     */
    public static void readBarrier(int nId)
        {
        // import com.tangosol.util.Base;
        
        Base.getCommonMonitor(nId).readBarrier();
        }
    
    /**
     * Ensure all reads made after this call will have visibility to any writes
    * made prior to a corresponding call to writeBarrier for the same member.
     */
    public static void readBarrier(Member member)
        {
        // import com.tangosol.util.Base;
        
        Base.getCommonMonitor(member == null ? 0 : member.getId()).readBarrier();
        }
    
    // From interface: com.tangosol.io.ExternalizableLite
    /**
     * Read a MemberSet from the specified stream.
     */
    public void readExternal(java.io.DataInput stream)
            throws java.io.IOException
        {
        int cMembers = stream.readUnsignedShort();
        if (cMembers > 0)
            {
            if (cMembers == 1)
                {
                readOne(stream);
                }
            else if (cMembers < 255)
                {
                readFew(stream);
                }
            else
                {
                readMany(stream);
                }
            }
        }
    
    /**
     * Read a trivial (containing a single member) MemberSet from the specified
    * stream.
     */
    public void readFew(java.io.DataInput stream)
            throws java.io.IOException
        {
        _assert(getBitSet() == null);
        
        int c = stream.readUnsignedByte();
        for (int i = 0; i < c; ++i)
            {
            int   nId   = stream.readUnsignedShort();
            int   iSet  = Member.calcByteOffset(nId);
            int   nMask = Member.calcByteMask(nId);
            int   nSet  = getBitSet(iSet);
            setBitSet(iSet, nSet | nMask);
            }
        }
    
    /**
     * Read the MemberSet serialized as a bit-set from the specified stream.
     */
    public void readMany(java.io.DataInput stream)
            throws java.io.IOException
        {
        _assert(getBitSet() == null);
        
        int   c  = stream.readUnsignedByte();
        int[] an = new int[c];
        for (int i = 0; i < c; ++i)
            {
            an[i] = stream.readInt();
            }
        setBitSet(an);
        }
    
    /**
     * Read a length-encoded array of Member mini-ids from the specified stream.
     */
    public void readOne(java.io.DataInput stream)
            throws java.io.IOException
        {
        _assert(getBitSet() == null);
        
        int nId = stream.readUnsignedShort();
        if (nId > 0)
            {
            int   nSet  = Member.calcByteOffset(nId);
            int   nMask = Member.calcByteMask(nId);
            int[] an    = new int[nSet + 1];
        
            // store bits
            an[nSet] = nMask;
            setBitSet(an);
            }
        }
    
    public synchronized boolean remove(int nId)
        {
        int iSet  = Member.calcByteOffset(nId);
        int nMask = Member.calcByteMask(nId);
        int nSet  = getBitSet(iSet);
        
        if ((nSet & nMask) != 0)
            {
            nSet &= ~nMask;
            setBitSet(iSet, nSet);
            return true;
            }
        else
            {
            return false;
            }
        }
    
    // From interface: java.util.Set
    public synchronized boolean remove(Object o)
        {
        Member member = (Member) o;
        
        return remove(member.getId());
        }
    
    // From interface: java.util.Set
    public synchronized boolean removeAll(java.util.Collection collection)
        {
        // import java.util.Iterator as java.util.Iterator;
        
        boolean fMod = false;
        
        if (collection instanceof MemberSet)
            {
            MemberSet that   = (MemberSet) collection;
            int[]     anThat = that.getBitSet();
            int       cThat  = (anThat == null ? 0 : anThat.length);
            for (int i = cThat - 1; i >= 0; --i)
                {
                int nThis;
                int nThat = anThat[i];
                if (nThat != 0)
                    {
                    nThis = getBitSet(i);
                    if ((nThis & nThat) != 0)
                        {
                        setBitSet(i, nThis & ~nThat);
                        fMod = true;
                        }
                    }
                }
            }
        else
            {
            for (java.util.Iterator iter = collection.iterator(); iter.hasNext(); )
                {
                fMod = fMod | remove(iter.next());
                }
            }
        
        return fMod;
        }
    
    // From interface: java.util.Set
    public synchronized boolean retainAll(java.util.Collection collection)
        {
        // import java.util.Iterator as java.util.Iterator;
        
        boolean fMod = false;
        
        if (collection instanceof MemberSet)
            {
            MemberSet that   = (MemberSet) collection;
            int[]     anThis = this.getBitSet();
            int[]     anThat = that.getBitSet();
            int       cThis  = (anThis == null ? 0 : anThis.length);
            int       cThat  = (anThat == null ? 0 : anThat.length);
        
            // remove all from "this" that extend beyond the end of "that"
            for (int i = cThat; i < cThis; ++i)
                {
                if (anThis[i] != 0)
                    {
                    anThis[i] = 0;
                    fMod = true;
                    }
                }
        
            // retain all in "this" that overlap with "that"
            for (int i = 0, c = Math.min(cThis, cThat); i < c; ++i)
                {
                int nThis = anThis[i];
                int nThat = anThat[i];
                int nBoth = nThis & nThat;
                if (nThis != nBoth)
                    {
                    anThis[i] = nBoth;
                    fMod = true;
                    }
                }
            }
        else
            {
            for (java.util.Iterator iter = iterator(); iter.hasNext(); )
                {
                if (!collection.contains(iter.next()))
                    {
                    iter.remove();
                    fMod = true;
                    }
                }
            }
        
        return fMod;
        }
    
    // Accessor for the property "BitSet"
    /**
     * Setter for property BitSet.<p>
    * The array of 32-bit integer values that hold the bit-set information.
     */
    protected void setBitSet(int[] an)
        {
        __m_BitSet = an;
        }
    
    // Accessor for the property "BitSet"
    /**
     * Setter for property BitSet.<p>
    * The array of 32-bit integer values that hold the bit-set information.
     */
    protected void setBitSet(int i, int n)
        {
        int[] an = getBitSet();
        
        boolean fBeyondBounds = (an == null || i >= an.length);
        if (n != 0 && fBeyondBounds)
            {
            // resize, making the bit set at least 32 more positions
            // than is necessary
            int[] anNew = new int[i + 2];
        
            // copy original bits
            if (an != null)
                {
                System.arraycopy(an, 0, anNew, 0, an.length);
                }
        
            // store bits
            an = anNew;
            setBitSet(an);
        
            fBeyondBounds = false;
            }
        
        if (!fBeyondBounds)
            {
            an[i] = n;
            }
        }
    
    // Accessor for the property "Member"
    /**
     * Setter for property Member.<p>
    * Members, indexed by Member mini-id. May not be supported if the MemberSet
    * does not hold on the Members that are in the MemberSet.
     */
    protected void setMember(Member[] aMember)
        {
        __m_Member = aMember;
        }
    
    // Accessor for the property "Member"
    /**
     * Setter for property Member.<p>
    * Members, indexed by Member mini-id. May not be supported if the MemberSet
    * does not hold on the Members that are in the MemberSet.
     */
    protected void setMember(int i, Member member)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: java.util.Set
    public synchronized int size()
        {
        int[] an = getBitSet();
        if (an == null)
            {
            return 0;
            }
        
        int cMembers = 0;
        for (int i = 0, c = an.length; i < c; ++i)
            {
            int n = an[i];
            if (n != 0)
                {
                cMembers += countBits(n);
                }
            }
        return cMembers;
        }
    
    // From interface: java.util.Set
    public synchronized Object[] toArray()
        {
        return toArray((Object[]) null);
        }
    
    // From interface: java.util.Set
    public synchronized Object[] toArray(Object[] ao)
        {
        throw new UnsupportedOperationException();
        }
    
    /**
     * Return an int[] containing the mini ids of the members in this set.
    * 
    * Note: it's possible that some trailing array elements contain zeros.
     */
    public int[] toIdArray()
        {
        int   cIds = size();
        int[] anId = new int[cIds];
        
        if (cIds == 1)
            {
            anId[0] = getFirstId();
            }
        else if (cIds > 1)
            {
            int[] anWord = getBitSet();
            int   iId    = 0;
            if (anWord != null)
                {
                for (int iWord = 0, cWords = anWord.length; iWord < cWords; ++iWord)
                    {
                    int nWord = anWord[iWord];
                    if (nWord != 0)
                        {
                        for (int of = 0, nMask = 1; of < 32; ++of, nMask <<= 1)
                            {
                            if ((nWord & nMask) != 0)
                                {
                                int nId = translateBit(iWord, nMask);
                                anId[iId++] = nId;
                                if (iId == cIds)
                                    {
                                    // protect against concurrent growth of the MemberSet
                                    return anId;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        
        return anId;
        }
    
    // Declared at the super level
    public String toString()
        {
        return toString(Member.SHOW_STD);
        }
    
    /**
     * Obtain a human-readable Member descrition with specified level of
    * details, see Member.toString(int nShow) for details.
     */
    public String toString(int nShow)
        {
        // import Component.Net.Member;
        // import java.util.Iterator as java.util.Iterator;
        
        StringBuffer sb = new StringBuffer("MemberSet(Size=");
        
        sb.append(size());
        
        try
            {
            for (java.util.Iterator iter = iterator(); iter.hasNext(); )
                {
                Member member = (Member) iter.next();
        
                if (member != null)
                    {
                    sb.append("\n  ")
                       .append(member.toString(nShow));
                    }
                }
            sb.append("\n  ");
            }
        catch (Exception e)
            {
            sb.append(", ids=[");
        
            boolean fFirst = true;
            for (int i = 1, c = 32 * getBitSetCount(); i < c; ++i)
                {
                if (contains(i))
                    {
                    if (fFirst)
                        {
                        fFirst = false;
                        }
                    else
                        {
                        sb.append(", ");
                        }
        
                    sb.append(i);
                    }
                }
            sb.append(']');
            }
        
        sb.append(')');
        
        return sb.toString();
        }
    
    /**
     * Return an Id represented by the integer with the value of
    * <code>nMask</code> located in the <code>nIndex</code> position of the
    * integer array carrying the bitset. Note that the bit 0 (mask 0x0001)
    * represents the Id equals to 1 (not 0).
     */
    public static int translateBit(int iIndex, int nMask)
        {
        int     nId    = 0;
        int     nBase  = iIndex << 5;
        boolean fFound = false;
        
        for (int i = 0; i < 4; ++i)
            {
            int nByte = nMask & 0xFF;
            if (nByte != 0)
                {
                int n = BIT_ID[nByte];
                if (fFound || n == 0)
                    {
                    throw new IllegalArgumentException("more than one bit was set: " + nMask);
                    }
        
                nId    = nBase + n;
                fFound = true;
                }
        
            nBase += 8;
            nMask >>>= 8;
            }
        
        return nId;
        }
    
    /**
     * Ensure all writes made prior to this call to be visible to any thread
    * which calls the corresponding readBarrier method for any of the members
    * in this MemberSet.
     */
    public void writeBarrier()
        {
        int[] anWord = getBitSet();
        if (anWord != null)
            {
            for (int iWord = 0, cWords = anWord.length; iWord < cWords; ++iWord)
                {
                int nWord = anWord[iWord];
                if (nWord != 0)
                    {
                    for (int of = 0, nMask = 1; of < 32; ++of, nMask <<= 1)
                        {
                        if ((nWord & nMask) != 0)
                            {
                            int nId = translateBit(iWord, nMask);
                            writeBarrier(nId);
                            }
                        }
                    }
                }
            }
        }
    
    /**
     * Ensure all writes made prior to this call to be visible to any thread
    * which calls the corresponding readBarrier method for the same member id.
     */
    public static void writeBarrier(int nId)
        {
        // import com.tangosol.util.Base;
        
        Base.getCommonMonitor(nId).writeBarrier();
        }
    
    /**
     * Ensure all writes made prior to this call to be visible to any thread
    * which calls the corresponding readBarrier method for the same member.
     */
    public static void writeBarrier(Member member)
        {
        // import com.tangosol.util.Base;
        
        Base.getCommonMonitor(member == null ? 0 : member.getId()).writeBarrier();
        }
    
    // From interface: com.tangosol.io.ExternalizableLite
    /**
     * Write this MemberSet to the specified stream.
     */
    public void writeExternal(java.io.DataOutput stream)
            throws java.io.IOException
        {
        int cMembers = size();
        
        stream.writeShort(cMembers);
        if (cMembers > 0)
            {
            if (cMembers == 1)
                {
                writeOne(stream);
                }
            else if (cMembers < 255)
                {
                writeFew(stream);
                }
            else
                {
                writeMany(stream);
                }
            }
        }
    
    /**
     * Write the MemberSet into the specified stream as a length-encoded array
    * of Member mini-ids.
     */
    public void writeFew(java.io.DataOutput stream)
            throws java.io.IOException
        {
        int cMembers = size();
        _assert(cMembers <= 255);
        stream.writeByte(cMembers);
        
        if (cMembers > 0)
            {
            int[] an = getBitSet();
            for (int i = 0, c = an.length; i < c && cMembers > 0; ++i)
                {
                int n = an[i];
                if (n != 0)
                    {
                    int nBase = i << 5;
                    for (int of = 1, nMask = 1; of <= 32; ++of, nMask <<= 1)
                        {
                        if ((n & nMask) != 0)
                            {
                            int nId = nBase + of;
                            stream.writeShort(nId);
                            --cMembers;
                            }
                        }
                    }
                }
            }
        }
    
    /**
     * Write the MemberSet into the specified stream as a bit-set.
     */
    public void writeMany(java.io.DataOutput stream)
            throws java.io.IOException
        {
        int[] an = getBitSet();
        int   c  = an == null ? 0 : an.length;
        
        // discard trailing empty bits
        while (c > 0 && an[c-1] == 0)
            {
            --c;
            }
        
        _assert(c <= 0xFF);
        stream.writeByte(c);
        for (int i = 0; i < c; ++i)
            {
            stream.writeInt(an[i]);
            }
        }
    
    /**
     * Write a trivial (containing a single member) MemberSet to the specified
    * stream.
     */
    public void writeOne(java.io.DataOutput stream)
            throws java.io.IOException
        {
        stream.writeShort(getFirstId());
        }

    // ---- class: com.tangosol.coherence.component.net.MemberSet$Iterator
    
    /**
     * Iterator over an array of Objects.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Iterator
            extends    com.tangosol.coherence.component.util.Iterator
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public Iterator()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.net.MemberSet.Iterator();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/net/MemberSet$Iterator".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            setItem(((MemberSet) get_Module()).toArray());
            
            super.onInit();
            }
        
        // Declared at the super level
        /**
         * Removes from the underlying collection the last element returned by
        * the iterator (optional operation).  This method can be called only
        * once per call to <tt>next</tt>.  The behavior of an iterator is
        * unspecified if the underlying collection is modified while the
        * iteration is in progress in any way other than by calling this
        * method.
        * 
        * @exception UnsupportedOperationException if the <tt>remove</tt>
        * operation is not supported by this Iterator
        * @exception IllegalStateException if the <tt>next</tt> method has not
        * yet been called, or the <tt>remove</tt> method has already been
        * called after the last call to the <tt>next</tt> method
         */
        public void remove()
            {
            if (isCanRemove())
                {
                setCanRemove(false);
                ((MemberSet) get_Module()).remove((Member) getItem(getNextIndex()-1));
                }
            else
                {
                throw new IllegalStateException();
                }
            }
        }
    }
