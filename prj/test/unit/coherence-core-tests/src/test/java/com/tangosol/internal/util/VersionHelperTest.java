/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;

public class VersionHelperTest
    {
    @Test
    public void shouldPrefixCalendarInternalVersions()
        {
        for (String sVersion : new String[] {"26.10", "26.10.1", "27.01", "27.07.2", "28.01"})
            {
            assertThat(VersionHelper.toInternalVersion(sVersion, "15.0.0"), is("15.0.0." + sVersion));
            }

        assertThat(VersionHelper.toInternalVersion("22.06.8", "14.1.1"), is("14.1.1.22.06.8"));
        }

    @Test
    public void shouldLeaveCompleteInternalVersionsUnprefixed()
        {
        for (String sVersion : new String[] {"12.2.1.4.31", "14.1.1.2206.18", "26.1.0.0.0", "26.1.0.0.1",
                "27.1.0.0.0", "28.1.0.0.0", "15.0.0.26.10", "15.0.0.26.10.1"})
            {
            assertThat(VersionHelper.toInternalVersion(sVersion, "15.0.0"), is(sVersion));
            }

        assertThat(VersionHelper.parseVersion(VersionHelper.toInternalVersion("26.1.0.0.0", "15.0.0")),
                is(VersionHelper.encodeVersion(26, 1, 0, 0, 0)));
        }

    @Test
    public void shouldPreserveInternalVersionBuildDescriptions()
        {
        String sDescription = " (dev-phf build.1.2.3)";

        assertThat(VersionHelper.toInternalVersion("27.01" + sDescription, "15.0.0"),
                is("15.0.0.27.01" + sDescription));
        assertThat(VersionHelper.toInternalVersion("26.10.1" + sDescription, "15.0.0"),
                is("15.0.0.26.10.1" + sDescription));
        assertThat(VersionHelper.toInternalVersion("27.1.0.0.0" + sDescription, "15.0.0"),
                is("27.1.0.0.0" + sDescription));
        }

    @Test
    public void shouldAllowInternalVersionsWithoutConfiguredPrefix()
        {
        for (String sVersion : new String[] {"26.10", "26.10.1", "27.01", "26.1.0.0.0", "27.1.0.0.0"})
            {
            assertThat(VersionHelper.toInternalVersion(sVersion, null), is(sVersion));
            assertThat(VersionHelper.toInternalVersion(sVersion, ""), is(sVersion));
            }
        }

    @Test
    public void shouldLeaveUnavailableInternalVersionsUnprefixed()
        {
        assertThat(VersionHelper.toInternalVersion(null, "15.0.0"), is((String) null));
        assertThat(VersionHelper.toInternalVersion("", "15.0.0"), is(""));
        assertThat(VersionHelper.toInternalVersion("n/a", "15.0.0"), is("n/a"));
        }

    @Test
    public void shouldCompareCalendarVersionsAcrossYearsWithinTheirPrefix()
        {
        int nRequired = VersionHelper.parseVersion("15.0.0.26.07");
        for (String sVersion : new String[] {"15.0.0.26.07", "15.0.0.26.10", "15.0.0.27.01",
                "15.0.0.27.07", "15.0.0.28.01"})
            {
            assertThat(sVersion, VersionHelper.isCalendarVersionCompatible(nRequired,
                    VersionHelper.parseVersion(sVersion)), is(true));
            }

        assertThat(VersionHelper.isCalendarVersionCompatible(VersionHelper.parseVersion("15.0.0.26.10.1"),
                VersionHelper.parseVersion("15.0.0.26.10.0")), is(false));
        assertThat(VersionHelper.isCalendarVersionCompatible(VersionHelper.parseVersion("15.0.0.26.10.1"),
                VersionHelper.parseVersion("15.0.0.26.10.2")), is(true));
        }

    @Test
    public void shouldRejectEarlierCalendarVersionsAndDifferentPrefixes()
        {
        int nRequired = VersionHelper.parseVersion("15.0.0.26.07");
        for (String sVersion : new String[] {"15.0.0.26.04", "15.0.0.25.10.31", "15.0.0.24.03", "14.1.2.27.01",
                "15.0.1.27.01", "15.1.1.0.3", "15.1.1.0.4", "26.1.0.0.0"})
            {
            assertThat(sVersion, VersionHelper.isCalendarVersionCompatible(nRequired,
                    VersionHelper.parseVersion(sVersion)), is(false));
            }
        }

    @Test
    public void shouldEncodeVersionPatch()
        {
        int nOne   = VersionHelper.encodeVersion(14, 1, 1, 0, 0);
        int nTwo   = VersionHelper.encodeVersion(14, 1, 1, 0, 1);
        int nThree = VersionHelper.encodeVersion(14, 1, 1, 0, 3);

        assertThat(nOne, is(lessThan(nTwo)));
        assertThat(nTwo, is(lessThan(nThree)));
        }

    @Test
    public void shouldEncodeVersionPatchSet()
        {
        int nOne   = VersionHelper.encodeVersion(14, 1, 1, 0, 0);
        int nTwo   = VersionHelper.encodeVersion(14, 1, 1, 1, 0);
        int nThree = VersionHelper.encodeVersion(14, 1, 1, 2, 1);

        assertThat(nOne, is(lessThan(nTwo)));
        assertThat(nTwo, is(lessThan(nThree)));
        }

    @Test
    public void shouldEncodeVersionMicro()
        {
        int nOne   = VersionHelper.encodeVersion(14, 1, 1, 0, 0);
        int nTwo   = VersionHelper.encodeVersion(14, 1, 2, 0, 0);
        int nThree = VersionHelper.encodeVersion(14, 1, 3, 0, 1);

        assertThat(nOne, is(lessThan(nTwo)));
        assertThat(nTwo, is(lessThan(nThree)));
        }

    @Test
    public void shouldEncodeVersionMinor()
        {
        int nOne   = VersionHelper.encodeVersion(14, 1, 1, 0, 0);
        int nTwo   = VersionHelper.encodeVersion(14, 2, 1, 0, 0);
        int nThree = VersionHelper.encodeVersion(14, 3, 1, 0, 1);

        assertThat(nOne, is(lessThan(nTwo)));
        assertThat(nTwo, is(lessThan(nThree)));
        }

    @Test
    public void shouldEncodeVersionMajor()
        {
        int nOne   = VersionHelper.encodeVersion(14, 1, 1, 0, 0);
        int nTwo   = VersionHelper.encodeVersion(15, 1, 1, 0, 0);
        int nThree = VersionHelper.encodeVersion(16, 1, 1, 0, 1);

        assertThat(nOne, is(lessThan(nTwo)));
        assertThat(nTwo, is(lessThan(nThree)));
        }

    @Test
    public void shouldEncodeCEVersionPatch()
        {
        int nOne   = VersionHelper.encodeVersion(22, 6, 0);
        int nTwo   = VersionHelper.encodeVersion(22, 6, 1);
        int nThree = VersionHelper.encodeVersion(22, 6, 3);

        assertThat(nOne, is(lessThan(nTwo)));
        assertThat(nTwo, is(lessThan(nThree)));
        }

    @Test
    public void shouldEncodeCEVersionMonth()
        {
        int nOne   = VersionHelper.encodeVersion(22, 3, 0);
        int nTwo   = VersionHelper.encodeVersion(22, 9, 0);

        assertThat(nOne, is(lessThan(nTwo)));
        }

    @Test
    public void shouldEncodeCEVersionYear()
        {
        int nOne   = VersionHelper.encodeVersion(22, 6, 0);
        int nTwo   = VersionHelper.encodeVersion(23, 6, 0);
        int nThree = VersionHelper.encodeVersion(24, 6, 0);

        assertThat(nOne, is(lessThan(nTwo)));
        assertThat(nTwo, is(lessThan(nThree)));
        }

    @Test
    public void shouldParseCommercialVersion()
        {
        int nOne = VersionHelper.encodeVersion(14, 1, 1, 0, 0);
        int nTwo = VersionHelper.parseVersion("14.1.1.0.0");
        assertThat(nOne, is(nTwo));
        }

    @Test
    public void shouldParseFiveDigitCEVersion()
        {
        int nOne = VersionHelper.encodeVersion(22, 6, 8);
        int nTwo = VersionHelper.parseVersion("14.1.1.2206.8");
        assertThat(nOne, is(nTwo));
        }

    @Test
    public void shouldParseSixDigitCEVersion()
        {
        int nOne = VersionHelper.encodeVersion(22, 6, 8);
        int nTwo = VersionHelper.parseVersion("14.1.1.22.06.8");
        assertThat(nOne, is(nTwo));
        }

    @Test
    public void shouldParseThreeDigitCEVersion()
        {
        int nOne = VersionHelper.encodeVersion(22, 6, 8);
        int nTwo = VersionHelper.parseVersion("22.06.8");
        assertThat(nOne, is(nTwo));
        }

    @Test
    public void shouldEncodeFusionAppsVersions()
        {
        int nFAPatchZero    = VersionHelper.encodeVersion(14, 1, 2, 24, 0);
        int nFAPatchOne     = VersionHelper.encodeVersion(14, 1, 2, 24, 1);
        int nFAPatchTwo     = VersionHelper.encodeVersion(14, 1, 2, 24, 2);
        int nCohPatchZero   = VersionHelper.encodeVersion(14, 1, 2, 0, 0);
        int nCohPatchOne    = VersionHelper.encodeVersion(14, 1, 2, 0, 1);
        int nCohPatchTwo    = VersionHelper.encodeVersion(14, 1, 2, 0, 2);
        int nFA12PatchZero  = VersionHelper.encodeVersion(12, 2, 1, 6, 0);
        int nFA12PatchOne   = VersionHelper.encodeVersion(12, 2, 1, 6, 1);
        int nCoh12PatchZero = VersionHelper.encodeVersion(12, 2, 1, 4, 0);

        assertThat(nFAPatchZero, is(nCohPatchZero));
        assertThat(nFAPatchOne, is(nCohPatchOne));
        assertThat(nFAPatchTwo, is(nCohPatchTwo));
        assertThat(nFAPatchOne, is(greaterThan(nFAPatchZero)));
        assertThat(nFAPatchTwo, is(greaterThan(nFAPatchOne)));
        assertThat(nFA12PatchZero, is(nCoh12PatchZero));
        assertThat(nFA12PatchOne, is(greaterThan(nFA12PatchZero)));
        }

    @Test
    public void shouldParseFusionAppsVersions()
        {
        int nFAPatchZero    = VersionHelper.parseVersion("14.1.2.24.0");
        int nFAPatchOne     = VersionHelper.parseVersion("14.1.2.24.1");
        int nFAPatchTwo     = VersionHelper.parseVersion("14.1.2.24.2");
        int nCohPatchZero   = VersionHelper.parseVersion("14.1.2.0.0");
        int nCohPatchOne    = VersionHelper.parseVersion("14.1.2.0.1");
        int nCohPatchTwo    = VersionHelper.parseVersion("14.1.2.0.2");
        int nFA12PatchZero  = VersionHelper.parseVersion("12.2.1.6.0");
        int nFA12PatchOne   = VersionHelper.parseVersion("12.2.1.6.1");
        int nCoh12PatchZero = VersionHelper.parseVersion("12.2.1.4.0");

        assertThat(nFAPatchZero, is(nCohPatchZero));
        assertThat(nFAPatchOne, is(nCohPatchOne));
        assertThat(nFAPatchTwo, is(nCohPatchTwo));
        assertThat(nFAPatchOne, is(greaterThan(nFAPatchZero)));
        assertThat(nFAPatchTwo, is(greaterThan(nFAPatchOne)));
        assertThat(nFA12PatchZero, is(nCoh12PatchZero));
        assertThat(nFA12PatchOne, is(greaterThan(nFA12PatchZero)));
        }

    @Test
    public void shouldCompareTo_15_1_1()
        {
        int v1 = VersionHelper.encodeVersion(15, 1, 1, 0, 0);

        int v2 = VersionHelper.encodeVersion(15, 1, 1, 0, 1);
        assertThat(v2, is(greaterThan(v1)));

        int v3 = VersionHelper.encodeVersion(26, 3, 0);
        assertThat(v3, is(greaterThan(v1)));
        assertThat(v3, is(greaterThan(v2)));

        int v4 = VersionHelper.encodeVersion(25, 3, 0);
        assertThat(v4, is(lessThan(v1)));
        }
    }
