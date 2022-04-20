/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.unit.Bytes;
import com.tangosol.coherence.config.unit.Megabytes;
import com.tangosol.coherence.config.unit.Millis;
import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;

import com.tangosol.internal.util.ObjectFormatter;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.Dependencies;
import com.tangosol.net.ExtensibleConfigurableCacheFactory.DependenciesHelper;

import java.util.ArrayList;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ObjectFormatter}.
 *
 * @author pfm  2012.04.01
 */
public class ObjectFormatterTest
{

    /**
     * Test formatting the sample cache config.
     */
    @Test
    public void testSampleCacheConfig()
    {
        Dependencies dependencies = DependenciesHelper.newInstance();
        ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(dependencies);
        CacheConfig config = eccf.getCacheConfig();
        System.out.println(new ObjectFormatter().format("CacheConfig ", config, null));
        }

    /**
     * Test all of the variations of the formatter.
     */
    @Test
    public void testFormatAll()
        {
        ObjectFormatter formatter = new ObjectFormatter();

        String s1 = formatter.format("Type0 contents", new Type0());
        String s2 = formatter.format("Level1 contents", new Level1());

        StringBuffer buf1 = new StringBuffer();
        buf1.append(s1);
        buf1.append(s2);

        String sResult = buf1.toString();

        System.out.println("ObjectFormatter output matches\n\n" + sResult);

        assertTrue(m_sOutput.equals(sResult));
        }

    // test private
    private static class Type0
        {
        // test various protection levels

        public    Millis     Types0_Millis_5       = new Millis("5");
        private   Seconds    Types0_Seconds_10     = new Seconds(10);
        private   Bytes      Types0_Bytes_15       = new Bytes(15);
        protected Megabytes  Types0_Megabytes_20   = new Megabytes(20);
                  Expression Types0_Expression_foo = new LiteralExpression("Types0_Expression");

        private Millis    Types0_Empty_Millis_5     = new Millis("0");
        private Seconds   Types0_Empty_Seconds_10   = new Seconds(0);
        private Bytes     Types0_Empty_Bytes        = new Bytes(0);
        private Megabytes Types0_Empty_Megabytes_10 = new Megabytes(0);
        }

    // test protected
    protected static class Level0
        {
        private String  Level0_Name    = "val-Level0Member";
        private Nested0 Level0_Nested0 = new Nested0();
        public String toString()
            {
            return "toString " + Level0_Name;
            }
        }

    public static class Level1 extends Level0
        {
        private String  Level1_Name    = "val-Level1Member";
        private String  Level1_str_1   = "val-Level1-str-1";
        private int     Level1_int_1   = 100;
        private Nested1 Level1_Nested1 = new Nested1();
        }

    public static class Level2 extends Level1
        {
        private String  Level2_Name    = "val-Level2Member";
        private String  Level2_str_2   = "val-Level2-str-2";
        private int     Level2_int_2   = 200;
        private Nested2 Level2_Nested2 = new Nested2();
        }

    public static class Nested0
        {
        private String   Nested0_Member        = "Nested0Member-Val";
        private String[] Nested0_emptyStrArray = {"", "", ""};
        private String[] Nested0_strArray1     = { "Nested0-StrArray1-Val0", "Nested0-StrArray1-Val1", ""};
        private String[] Nested0_strArray2     = { "Nested0-StrArray2-Val0", "", "Nested0-StrArray2-Val2"};
        }

    public static class Nested1
        {
        Nested1()
            {
            Nested0_ArrayListOfNested0.add(new Nested0());
            Nested0_ArrayListOfNested0.add(new Nested0());
            }
        private String Nested1_Member = "Nested1Member-Val";
        //private String[] Nested1_strArray = { "Nested1-StrArray-Val1", "Nested1-StrArray-Val1" };
        private Nested0[] Nested1_ArrayOfNested0 = { new Nested0(), new Nested0() };
        private ArrayList<Nested0> Nested0_ArrayListOfNested0 = new ArrayList<Nested0>();
        }

    public static class Nested2
        {
        private String Nested2__Member = "Nested2Member-Val";
        private Object[] Nested2_ArrayOfNested1 = { new Nested1(), new Nested1() };
        }

    public static class Nested3
        {
        private String Nested2Member = "Nested2Member-Val";
        private Object[] Nested4_testArray = { new Nested1(), new Nested2() };
        private String[] Nested4_strArray = { "S1-Val", "S2-Val" };
        }

    private String m_sOutput = "\n"
        + "Type0 contents\n"
        + "  Types0_Millis_5: 5ms\n"
        + "  Types0_Seconds_10: 10s\n"
        + "  Types0_Bytes_15: 15B\n"
        + "  Types0_Megabytes_20: 20MB\n"
        + "  Types0_Expression_foo: Types0_Expression\n"
        + "Level1 contents\n"
        + "  Level0_Name: val-Level0Member\n"
        + "  Level0_Nested0\n"
        + "    Nested0_Member: Nested0Member-Val\n"
        + "    Nested0_emptyStrArray\n"
        + "    Nested0_strArray1 (String[])\n"
        + "      [0] : Nested0-StrArray1-Val0\n"
        + "      [1] : Nested0-StrArray1-Val1\n"
        + "    Nested0_strArray2 (String[])\n"
        + "      [0] : Nested0-StrArray2-Val0\n"
        + "      [1] : Nested0-StrArray2-Val2\n"
        + "  Level1_Name: val-Level1Member\n"
        + "  Level1_str_1: val-Level1-str-1\n"
        + "  Level1_int_1: 100\n"
        + "  Level1_Nested1\n"
        + "    Nested1_Member: Nested1Member-Val\n"
        + "    Nested1_ArrayOfNested0 (Nested0[])\n"
        + "      [0] \n"
        + "        Nested0_Member: Nested0Member-Val\n"
        + "        Nested0_emptyStrArray\n"
        + "        Nested0_strArray1 (String[])\n"
        + "          [0] : Nested0-StrArray1-Val0\n"
        + "          [1] : Nested0-StrArray1-Val1\n"
        + "        Nested0_strArray2 (String[])\n"
        + "          [0] : Nested0-StrArray2-Val0\n"
        + "          [1] : Nested0-StrArray2-Val2\n"
        + "      [1] \n"
        + "        Nested0_Member: Nested0Member-Val\n"
        + "        Nested0_emptyStrArray\n"
        + "        Nested0_strArray1 (String[])\n"
        + "          [0] : Nested0-StrArray1-Val0\n"
        + "          [1] : Nested0-StrArray1-Val1\n"
        + "        Nested0_strArray2 (String[])\n"
        + "          [0] : Nested0-StrArray2-Val0\n"
        + "          [1] : Nested0-StrArray2-Val2\n"
        + "    Nested0_ArrayListOfNested0 (ArrayList of Nested0)\n"
        + "      [0] \n"
        + "        Nested0_Member: Nested0Member-Val\n"
        + "        Nested0_emptyStrArray\n"
        + "        Nested0_strArray1 (String[])\n"
        + "          [0] : Nested0-StrArray1-Val0\n"
        + "          [1] : Nested0-StrArray1-Val1\n"
        + "        Nested0_strArray2 (String[])\n"
        + "          [0] : Nested0-StrArray2-Val0\n"
        + "          [1] : Nested0-StrArray2-Val2\n"
        + "      [1] \n"
        + "        Nested0_Member: Nested0Member-Val\n"
        + "        Nested0_emptyStrArray\n"
        + "        Nested0_strArray1 (String[])\n"
        + "          [0] : Nested0-StrArray1-Val0\n"
        + "          [1] : Nested0-StrArray1-Val1\n"
        + "        Nested0_strArray2 (String[])\n"
        + "          [0] : Nested0-StrArray2-Val0\n"
        + "          [1] : Nested0-StrArray2-Val2";
}
