/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import java.util.Arrays;

/**
 * A utility class for working with Coherence versions.
 */
public class VersionHelper
    {

    /**
     * Convert the version elements into a single integer value.
     *
     * @param nYear   the version year
     * @param nMonth  the version month
     * @param nPatch  the version patch
     *
     * @return  the yy-mm-patch version encoded as an int
     */
    public static int encodeVersion(int nYear, int nMonth, int nPatch)
        {
        // 64(0x3F)-based - 5 elements 6 bits each

        // the version prefix (e.g. 14.1.1 or 14.1.2) consumes the first 3 elements
        // with the remainder used for year, month and patch

        nPatch = (nMonth > 6 ? 0x1 << 5 : 0x0) | (nPatch & 0x1F);

        return getVersionPrefix(nYear, nMonth)
            | ((nYear & 0x3F) << 6)
            | (nPatch & 0x3F);
        }

    /**
     * Convert the version elements into a single integer value.
     *
     * @param nMajor     the major version number
     * @param nMinor     the minor version number
     * @param nMicro     the micro version number
     * @param nPatchSet  the patch set version number
     * @param nPatch     the patch version number
     *
     * @return  the version encoded as an int
     */
    public static int encodeVersion(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        // 64(0x3F)-based - 5 elements 6 bits each

        int nPatchSetActual = resolveFusionAppsPatchSet(nMajor, nMinor, nMicro, nPatchSet);

        if (nPatchSetActual > 2000)
            {
            // Assume this is a feature pack version
            int nPrefix = encodeVersion(nMajor, nMinor, nMicro, 0, 0);
            int nYear   = nPatchSetActual / 100;
            return nPrefix
                | ((nYear & 0x3F) << 6)
                | (nPatch & 0x3F);
            }

        return ((nMajor         & 0x3F) << 6*4)
            | ((nMinor          & 0x3F) << 6*3)
            | ((nMicro          & 0x3F) << 6*2)
            | ((nPatchSetActual & 0x3F) << 6)
            | (nPatch           & 0x3F);
        }

    /**
     * Return the version prefix for a given yy-mm version.
     *
     * @param nYear   the version year
     * @param nMonth  the version month
     *
     * @return  the version prefix
     */
    public static int getVersionPrefix(int nYear, int nMonth)
        {
        if (nYear > 23)
            {
            return encodeVersion(15, 0, 0, 0, 0);
            }
        else if (nYear == 23)
            {
            return encodeVersion(14, 1, 2, 0, 0);
            }
        return encodeVersion(14, 1, 1, 0, 0);
        }

    /**
     * Return {@code true} if the provided part of the version could represent a year.
     *
     * @param nVersion  the version part to check
     *
     * @return {@code true} if the provided part of the version could represent a year
     */
    public static boolean isCalendarVersion(int nVersion)
        {
        return nVersion >= 20; // 2020 was the first use of calendar versions
        }

    /**
     * Parse the specified version string and generate an internal integer
     * representation.
     *
     * @param sVersion  the version string to encode
     *
     * @return the version string encoded to an {@code int}
     */
    public static int parseVersion(String sVersion)
        {
        // import java.util.Arrays;

        if (sVersion == null || sVersion.isEmpty())
            {
            return 0;
            }

        // the format of the version string is
        //   major.minor.micro.patch-set.patch [<space><suffix>]
        //          or
        //   major.minor.micro.year.month.patch

        // for example:
        //   "12.2.1.1.0" or "12.2.3.0.0 internal build"
        //          or
        //   "14.1.1.20.06.0" or "14.1.1.20.06.0 internal build"
        //          or feature pack version
        //   "14.1.1.2006.0" or "14.1.1.2006.0 internal build"

        // (optional suffix could come from the "Implementation-Description"
        // element of the manifest; see Coherence._initStatic)

        // remove an optional suffix first
        int ofSuffix = sVersion.indexOf(" ");
        if (ofSuffix > 0)
            {
            sVersion = sVersion.substring(0, ofSuffix);
            }

        final int INDEX_YEAR  = 3; // 14.1.1.20.06.01
        final int INDEX_MONTH = 4; // 14.1.1.20.06.01

        String[] asVersions = sVersion.split("\\.");

        if (asVersions.length == 2 || asVersions.length == 3)
            {
            // this is a CE version...
            int nYear  = Integer.parseInt(asVersions[0]);
            int nMonth = Integer.parseInt(asVersions[1]);
            int nPatch =  asVersions.length == 3 ? Integer.parseInt(asVersions[2]) : 0;
            return encodeVersion(nYear, nMonth, nPatch);
            }

        // handle feature pack which condenses YY && MM into a single string
        String sYear = asVersions.length > INDEX_YEAR ? asVersions[INDEX_YEAR] : "";
        if (sYear.length() >= 4) // YYMM
            {
            asVersions = Arrays.copyOf(asVersions, asVersions.length + 1);

            // right shift
            for (int i = asVersions.length - 2; i > INDEX_YEAR; --i)
                {
                asVersions[i + 1] = asVersions[i];
                asVersions[i]     = null;
                }

            asVersions[INDEX_YEAR]  = sYear.substring(0, 2);
            asVersions[INDEX_MONTH] = sYear.substring(2);
            }

        int[] an = new int[5];
        
        // process the version converting to 5 base 64 encoded integers
        for (int i = 0, c = Math.min(an.length, asVersions.length); i < c; i++)
            {
            try
                {
                // the range of the version part is 0 .. 63
                int nVersion = Integer.parseInt(asVersions[i]);

                if (i == INDEX_MONTH && isCalendarVersion(an[i - 1]))
                    {
                    nVersion = nVersion > 6
                            ? 0x1 << 5
                            : 0x0;

                    nVersion |= i + 1 < asVersions.length
                                ? Integer.parseInt(asVersions[i + 1])
                                : 0;
                    }

                an[i] = Math.min(63, nVersion);

                if (i == INDEX_YEAR)
                    {
                    // if we have done the "year" we can map a possible FA version
                    // to the Coherence version
                    resolveFusionAppsPatchSet(an);
                    }
                }
            catch (NumberFormatException e)
                {
                // un-parsable part; leave as zero
                }
            }

        return encodeVersion(an[0], an[1], an[2], an[3], an[4]);
        }


    /**
     * Create an array of version elements, where index 0 is "major", 1 is
     * "minor", ... 4 is "patch".
     */
    public static int[] toVersionArray(String sVersion)
        {
        // 64-based - 5 elements 6 bits each

        int nVersion = parseVersion(sVersion);
        return new int[]
            {
            (nVersion & 0x3F000000) >> 6*4,
            (nVersion & 0x00FC0000) >> 6*3,
            (nVersion & 0x0003F000) >> 6*2,
            (nVersion & 0x00000FC0) >> 6,
            (nVersion & 0x0000003F),
            };
        }

    /**
     * Create a string representation of the specified version in internal
     * encoding.
     */
    public static String toVersionString(int nVersion, boolean fIncludePrefix)
        {
        // nVersion is 64-based: 5 elements with 6 bits each

        int nYear  = (nVersion & 0x00000FC0) >> 6;
        int nPatch = nVersion & 0x0000003F;

        String sVersion = (fIncludePrefix || !isCalendarVersion(nYear)
                ? ((nVersion & 0x3F000000) >> 6*4) + "." +
                  ((nVersion & 0x00FC0000) >> 6*3) + "." +
                  ((nVersion & 0x0003F000) >> 6*2) + "."
                : "") + nYear;

        if (isCalendarVersion(nYear))
            {
            int nPatchActual = nPatch & ~0x20;

            sVersion += (fIncludePrefix ? "" : ".");
            if ((nPatch & 0x20) == 0)
                {
                // display 6 for feature packs in years before 22 and 03 for years beyond
                sVersion += (nYear <= 22 ? "06" : "03");
                }
            else
                {
                // display 12 for feature packs in years before 21 and 09 for years beyond
                sVersion += (nYear <= 21 ? "12" : "09");
                }

            sVersion += "." + nPatchActual;
            }
        else
            {
            sVersion += "." + nPatch;
            }

        return sVersion;
        }

    /**
     * Return {@code true} iff the actual version provided is greater than or equal
     * to the required version.
     *
     * @param nRequired  the required version in its encoded form
     * @param nActual    the actual version in its encoded form
     *
     * @return {@code true} iff the actual version provided is greater than or equal
     *         to the required version
     */
    public static boolean isVersionCompatible(int nRequired, int nActual)
        {
        return nRequired <= nActual;
        }

    /**
     * Return {@code true} iff the required and the actual versions provided have
     * the identical base version, and the actual patch version is greater or equal
     * to the required patch version.
     *
     * @param nRequired  the required version in its encoded form
     * @param nActual    the actual version in its encoded form
     *
     * @return {@code true} iff the required and the actual versions provided have
     *         the identical base version, and the actual patch version is greater or equal
     *         to the required patch version
     */
    public static boolean isPatchCompatible(int nRequired, int nActual)
        {
        return (nRequired & ~0x3F) == (nActual & ~0x3F) && (nRequired & 0x3F) <= (nActual & 0x3F);
        }

    /**
     * This method will map Fusion Apps versions to the correct Coherence version.
     *
     * @param anVersion  the possible FA version to resolve to a Coherence version
     */
    public static void resolveFusionAppsPatchSet(int[] anVersion)
        {
        if (anVersion != null && anVersion.length == 5)
            {
            anVersion[3] = resolveFusionAppsPatchSet(anVersion[0], anVersion[1], anVersion[2], anVersion[3]);
            }
        }

    /**
     * Return the actual patch set value to use for a given version.
     * <p>
     * This method will map Fusion Apps versions to the correct Coherence version.
     *
     * @param nMajor     the major version number
     * @param nMinor     the minor version number
     * @param nMicro     the micro version number
     * @param nPatchSet  the patch set version number
     *
     * @return the actual patch set value to use
     */
    public static int resolveFusionAppsPatchSet(int nMajor, int nMinor, int nMicro, int nPatchSet)
        {
        if (nMajor == 14 && nMinor == 1 && nMicro == 2 && nPatchSet == 24)
            {
            // FA 14.1.2.24.x maps to Coherence 14.1.2.0.x
            return 0;
            }
        return nPatchSet;
        }

    /**
     * Compare two version strings for compatability.
     * <p>
     * The first string is the version to test, the second is the version required.
     * If the second version is compatible with the first, then "pass" is displayed
     * otherwise "fail" is displayed.
     *
     * @param args the two version strings to compare
     */
    public static void main(String[] args)
        {
        if (args.length == 2)
            {
            String sVersionOne = args[0];
            String sVersionTwo = args[1];
            int    nEncodedOne = VersionHelper.parseVersion(sVersionOne.replace("-", "."));
            int    nEncodedTwo = VersionHelper.parseVersion(sVersionTwo.replace("-", "."));
            int    nExitCode;
            if (VersionHelper.isVersionCompatible(nEncodedTwo, nEncodedOne))
                {
                System.out.println("pass");
                nExitCode = 0;
                }
            else
                {
                System.out.println("fail");
                nExitCode = 1;
                }

            if (Boolean.getBoolean("no.exit.code"))
                {
                nExitCode = 0;
                }
            System.exit(nExitCode);
            }

        System.err.println("Usage:");
        System.err.println("VersionHelper <versionCheck> <versionRequired>");
        System.err.println();
        System.err.println("Displays \"pass\" if <versionCheck> is compatible with <versionRequired>");
        System.err.println("Displays \"fail\" if <versionCheck> is not compatible with <versionRequired>");
        System.err.println();
        System.err.println("Exit code zero, versionCheck is compatible with versionRequired");
        System.err.println("Exit code one, versionCheck is not compatible with versionRequired");
        System.err.println("Exit code two, incorrect version arguments were specified");
        System.err.println();
        System.err.println("If the no.exit.code system property is set to true, the exit code will be zero regardless of a pass or fail.");
        System.err.println();
        System.exit(2);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The encoded CE 24.03.0 and 1 versions.
     */
    public static final int VERSION_24_09 = encodeVersion(24, 9, 0);

    /**
     * The encoded CE 24.03.0 and 1 versions.
     */
    public static final int VERSION_24_03 = encodeVersion(24, 3, 0);
    public static final int VERSION_24_03_1 = encodeVersion(24, 3, 1);

    /**
     * The encoded CE 23.09.0 and 1 versions.
     */
    public static final int VERSION_23_09 = encodeVersion(23, 9, 0);
    public static final int VERSION_23_09_1 = encodeVersion(23, 9, 1);

    /**
     * The encoded 14.1.1.2206.0, 6, 7 and 9 versions.
     */
    public static final int VERSION_14_1_1_2206   = encodeVersion(14, 1, 1, 2206, 0);
    public static final int VERSION_14_1_1_2206_6 = encodeVersion(14, 1, 1, 2206, 6);
    public static final int VERSION_14_1_1_2206_7 = encodeVersion(14, 1, 1, 2206, 7);
    public static final int VERSION_14_1_1_2206_9 = encodeVersion(14, 1, 1, 2206, 9);

    /**
     * The encoded 14.1.2.0.0 version.
     */
    public static final int VERSION_14_1_2_0 = encodeVersion(14, 1, 2, 0, 0);

    /**
     * The encoded 14.1.1.0.0, 16 and 17 versions.
     */
    public static final int VERSION_14_1_1_0    = encodeVersion(14, 1, 1, 0, 0);
    public static final int VERSION_14_1_1_0_16 = encodeVersion(14, 1, 1, 0, 16);
    public static final int VERSION_14_1_1_0_17 = encodeVersion(14, 1, 1, 0, 17);

    /**
     * The encoded 12.2.1.4.0, 20 and 21 versions.
     */
    public static final int VERSION_12_2_1_4    = encodeVersion(12, 2, 1, 4, 0);
    public static final int VERSION_12_2_1_4_20 = encodeVersion(12, 2, 1, 4, 20);
    public static final int VERSION_12_2_1_4_21 = encodeVersion(12, 2, 1, 4, 21);

    /**
     * The encoded 12.2.1.6.0, 6 and 7 versions.
     */
    public static final int VERSION_12_2_1_6   = encodeVersion(12, 2, 1, 6, 0);
    public static final int VERSION_12_2_1_6_6 = encodeVersion(12, 2, 1, 6, 6);
    public static final int VERSION_12_2_1_6_7 = encodeVersion(12, 2, 1, 6, 7);
    }
