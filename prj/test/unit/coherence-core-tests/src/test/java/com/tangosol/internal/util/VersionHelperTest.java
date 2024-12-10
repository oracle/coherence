/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
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
        int nFAPatchZero  = VersionHelper.encodeVersion(14, 1, 2, 24, 0);
        int nFAPatchOne   = VersionHelper.encodeVersion(14, 1, 2, 24, 1);
        int nFAPatchTwo   = VersionHelper.encodeVersion(14, 1, 2, 24, 2);
        int nCohPatchZero = VersionHelper.encodeVersion(14, 1, 2, 0, 0);
        int nCohPatchOne  = VersionHelper.encodeVersion(14, 1, 2, 0, 1);
        int nCohPatchTwo  = VersionHelper.encodeVersion(14, 1, 2, 0, 2);

        assertThat(nFAPatchZero, is(nCohPatchZero));
        assertThat(nFAPatchOne, is(nCohPatchOne));
        assertThat(nFAPatchTwo, is(nCohPatchTwo));
        assertThat(nFAPatchOne, is(greaterThan(nFAPatchZero)));
        assertThat(nFAPatchTwo, is(greaterThan(nFAPatchOne)));
        }

    @Test
    public void shouldParseFusionAppsVersions()
        {
        int nFAPatchZero  = VersionHelper.parseVersion("14.1.2.24.0");
        int nFAPatchOne   = VersionHelper.parseVersion("14.1.2.24.1");
        int nFAPatchTwo   = VersionHelper.parseVersion("14.1.2.24.2");
        int nCohPatchZero = VersionHelper.parseVersion("14.1.2.0.0");
        int nCohPatchOne  = VersionHelper.parseVersion("14.1.2.0.1");
        int nCohPatchTwo  = VersionHelper.parseVersion("14.1.2.0.2");

        assertThat(nFAPatchZero, is(nCohPatchZero));
        assertThat(nFAPatchOne, is(nCohPatchOne));
        assertThat(nFAPatchTwo, is(nCohPatchTwo));
        assertThat(nFAPatchOne, is(greaterThan(nFAPatchZero)));
        assertThat(nFAPatchTwo, is(greaterThan(nFAPatchOne)));
        }

    }
