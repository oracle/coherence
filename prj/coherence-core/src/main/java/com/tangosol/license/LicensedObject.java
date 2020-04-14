/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.license;


import com.tangosol.io.Base64OutputStream;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.UID;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;


/**
* This class is a base class for classes that may be license-limited.
*
* WARNING:  DO NOT MODIFY OR RECOMPILE THIS CLASS!
*
* @version 1.00, 01/25/01
* @author cp
*/
public class LicensedObject
        extends Base
    {
    // ----- command line ---------------------------------------------------

    /**
    * To see registered licenses:
    *
    *   java com.tangosol.license.LicensedObject
    */
    public static void main(String[] asArg)
        {
        out();
        out("Registered Licenses:");

        LicenseData[] aLicense = getLicenseData();
        for (int i = 0, c = aLicense.length; i < c; ++i)
            {
            out();
            out(aLicense[i]);
            }

        out();
        }


    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public LicensedObject()
        {
        if (!m_mapChecked.containsKey(getClass()))
            {
            LicenseData[] aLicense = getClassLicenseData();
            if (aLicense == null || aLicense.length == 0)
                {
                throw new LicenseException("The necessary edition to perform"
                        + " the operation is not available; \""
                        + toString() + "\" is required.");
                }

            String sFailure = null;
            for (int i = 0, c = aLicense.length; i < c; ++i)
                {
                sFailure = getLicenseFailure(aLicense[i]);
                if (sFailure == null)
                    {
                    break;
                    }
                }
            if (sFailure != null)
                {
                throw new LicenseException(sFailure);
                }

            m_mapChecked.put(getClass(), null);
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the set of known licenses for this object.
    *
    * @return an array of LicenseData objects for this object
    */
    public LicenseData[] getClassLicenseData()
        {
        return getClassLicenseData(getClass());
        }

    /**
    * Determine the set of known licenses for this object.
    *
    * @return an array of LicenseData objects for this object
    */
    public static LicenseData[] getClassLicenseData(Class clz)
        {
        m_fConfigured = true;
        while (clz != null)
            {
            List list = (List) m_mapLicenses.get(clz.getName());
            if (list != null)
                {
                return (LicenseData[]) list.toArray(new LicenseData[list.size()]);
                }
            clz = clz.getSuperclass();
            }
        return null;
        }

    /**
    * Determine the set of known licenses.
    *
    * @return an array of LicenseData objects
    */
    public static LicenseData[] getLicenseData()
        {
        m_fConfigured = true;
        Map  map  = m_mapLicenses;
        List list = new ArrayList();
        for (Iterator iter = map.values().iterator(); iter.hasNext(); )
            {
            list.addAll((List) iter.next());
            }

        return (LicenseData[]) list.toArray(new LicenseData[list.size()]);
        }

    /**
    * Provide the set of known licenses.  This method is called during system
    * initialization, and must not be invoked subsequently.
    *
    * @param aLicense  an array of LicenseData objects
    */
    public static void setLicenseData(LicenseData[] aLicense)
        {
        Map map = m_mapLicenses;
        if (map == null)
            {
            m_mapLicenses = map = new SafeHashMap();
            }

        synchronized (map)
            {
            if (m_fConfigured)
                {
                throw new LicenseException(
                        "The license information has already been configured.");
                }

            for (int i = 0, c = aLicense.length; i < c; ++i)
                {
                LicenseData license = aLicense[i];
                List list = (List) map.get(license.sClass);
                if (list == null)
                    {
                    list = new LinkedList();
                    map.put(license.sClass, list);
                    }
                list.add(license);
                }

            m_fConfigured = true;
            }
        }

    /**
    * Specify one particular license that this node will use. All other
    * licenses are discarded. Note that multiple
    *
    * @param sEdition      the product edition name (such as "DGE")
    * @param nLicenseType  the license type (0, 1, 2 for eval, dev, prod)
    * @param uid           the license key (UID)
    */
    public static void retain(String sEdition, int nLicenseType, UID uid)
        {
        retain(sEdition, nLicenseType);

        LicenseData[] aLicense = getLicenseData();
        for (int i = 0, c = aLicense.length; i < c; ++i)
            {
            LicenseData license = aLicense[i];
            if (Base.equals(license.uid, uid))
                {
                List list = new LinkedList();
                list.add(license);
                Map map = new SafeHashMap();
                map.put(license.sClass, list);

                m_mapLicenses = map;
                m_mapChecked  = new SafeHashMap();
                return;
                }
            }

        throw new IllegalArgumentException("Unknown license key: " + uid);
        }

    /**
    * Specify one particular product edition / license type that this node
    * will use. All other licenses are discarded.
    *
    * @param sEdition      the product edition name (such as "DGE")
    * @param nLicenseType  the license type (0, 1, 2 for eval, dev, prod)
    */
    public static void retain(String sEdition, int nLicenseType)
        {
        LicenseData[] aLicense = getLicenseData();

        for (Iterator iterByClass = m_mapLicenses.values().iterator();
                iterByClass.hasNext(); )
            {
            List list = (List) iterByClass.next();
            for (Iterator iterLicenses = list.iterator(); iterLicenses.hasNext(); )
                {
                LicenseData license = (LicenseData) iterLicenses.next();
                if (!(Base.equals(license.sEdition, sEdition)
                        && license.nLicenseType == nLicenseType))
                    {
                    iterLicenses.remove();
                    }
                }
            if (list.isEmpty())
                {
                iterByClass.remove();
                }
            }

        // reset cache of "checked" licenses
        m_mapChecked.clear();
        }

    /**
    * Determine if any licenses are not expired.
    *
    * @return true if any licenses are still valid
    */
    public static boolean isExpired()
        {
        LicenseData[] aLicense = getLicenseData();
        for (int i = 0, c = aLicense.length; i < c; ++i)
            {
            if (getLicenseFailure(aLicense[i]) == null)
                {
                return false;
                }
            }
        return true;
        }

    /**
    * Determine if the loaded licenses contain the specified edition.
    *
    * @param clzEdition  the class corresponding to the license edition
    *
    * @return true iff the specified edition is loaded
    */
    public static boolean containsEdition(Class clzEdition)
        {
        return m_mapLicenses.containsKey(clzEdition.getName());
        }

    /**
    * Ensure that the specified license has been loaded.
    *
    * @param clzEdition  the class corresponding to the license edition
    */
    public static void ensureEdition(Class clzEdition)
        {
        Object o;
        try
            {
            o = clzEdition.newInstance();
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e.getCause());
            }

        if (!containsEdition(clzEdition) &&
            LicensedObject.class.isAssignableFrom(clzEdition))
            {
            throw new LicenseException("The necessary edition to perform"
                    + " the operation is not available; \""
                    + o + "\" is required.");
            }
        }

    // ----- license listings -----------------------------------------------

    /**
    * Print the generic license message.
    *
    * @see #printLicense(Class clz, String sName)
    */
    public static void printLicense()
        {
        printLicense(getLicenseData());
        }

    /**
    * Print the product license information.
    *
    * @see #printLicense()
    */
    static public void printLicense(Class clz, String sName)
        {
        printLicense(getClassLicenseData(clz));
        }

    /**
    * Print out the specified licenses.
    *
    * @param aLicense  an array of LicenseData objects
    */
    private static void printLicense(LicenseData[] aLicense)
        {
/*
License templates:

0         1         2         3         4         5         6         7
01234567890123456789012345678901234567890123456789012345678901234567890123456789
******************************************************************************
*
* Oracle Coherence is licensed by Oracle Corp.
*
* Licensed for evaluation use from 28 Aug 2006 until 28 Oct 2006:
*   Oracle Coherence: DataGrid Edition
*   Oracle Coherence: Application Edition
*   Oracle Coherence: Caching Edition
*   Oracle Coherence: compute Edition
*   Oracle Coherence: Real-Time Client
*   Oracle Coherence: Application Client
*
* A production license is required for production use. For more information,
* see http://www.tangosol.com/license.jsp.
*
* Copyright (c) 2000-2009 Oracle Corp.
*
******************************************************************************

******************************************************************************
*
* Oracle Coherence is licensed by Oracle Corp.
*
* Licensed to Big Bank Incorporated for development use from 25 Aug 2005
* until 25 Aug 2006 (expired):
*   Oracle Coherence: DataGrid Edition
*   Oracle Coherence: Application Edition
*   Oracle Coherence: Caching Edition
*   Oracle Coherence: compute Edition
*   Oracle Coherence: Real-Time Client
*   Oracle Coherence: Application Client
*
* A production license is required for production use. For more information,
* see http://www.tangosol.com/license.jsp.
*
* Copyright (c) 2000-2009 Oracle Corp.
*
******************************************************************************

******************************************************************************
*
* Oracle Coherence is licensed by Oracle Corp.
*
* Licensed to Big Bank Incorporated for production use in accordance with the
* terms of the Oracle production license (see http://www.tangosol.com/license.jsp):
*
*   Oracle Coherence: DataGrid Edition
*     6D59E86A11A19B18CF2E98A4B6A19658  16 CPU-sockets
*     581A1A1F2E98A4B6A1969B18C6D59E86  12 CPU-sockets
*
*   Oracle Coherence: Real-Time Client
*     1A19B18C6D59E86A1F2E98A4B6A19658  1000 users
*     A1F2E98A4B6A196581A19B18C6D59E86  1000 users
*
* Licensed for OEM production use to AppHouse Software in accordance with the
* terms of the Oracle Master License Agreement 174628:
*
*   Oracle Coherence: DataGrid Edition
*     B6A196586D59E86A11A19B18CF2E98A4  Application "OnlineBanking"
*
* Copyright (c) 2000-2009 Oracle Corp.
*
******************************************************************************

******************************************************************************
*
* Oracle Coherence is licensed by Oracle Corp.
*
* Licensed for evaluation use from 28 Aug 2006 until 28 Oct 2006 (55 days
* remaining):
*   Oracle Coherence: DataGrid Edition
*   Oracle Coherence: Application Edition
*   Oracle Coherence: Caching Edition
*   Oracle Coherence: compute Edition
*   Oracle Coherence: Real-Time Client
*   Oracle Coherence: Application Client
*
* Licensed to Big Bank Incorporated for development use from 25 Aug 2005
* until 25 Aug 2006 (expired):
*   Oracle Coherence: DataGrid Edition
*   Oracle Coherence: Application Edition
*   Oracle Coherence: Caching Edition
*   Oracle Coherence: compute Edition
*   Oracle Coherence: Real-Time Client
*   Oracle Coherence: Application Client
*
* Licensed to Big Bank Incorporated for development use from 28 Aug 2006
* until 28 Aug 2007:
*   Oracle Coherence: DataGrid Edition
*   Oracle Coherence: Application Edition
*   Oracle Coherence: Caching Edition
*   Oracle Coherence: compute Edition
*   Oracle Coherence: Real-Time Client
*   Oracle Coherence: Application Client
*
* Licensed to Big Bank Incorporated for production use in accordance with the
* terms of the Oracle production license (see http://www.tangosol.com/license.jsp):
*
*   Oracle Coherence: DataGrid Edition
*     6D59E86A11A19B18CF2E98A4B6A19658  16 CPU-sockets
*     581A1A1F2E98A4B6A1969B18C6D59E86  12 CPU-sockets
*
*   Oracle Coherence: Real-Time Client
*     1A19B18C6D59E86A1F2E98A4B6A19658  1000 users
*     A1F2E98A4B6A196581A19B18C6D59E86  1000 users
*
* Licensed for OEM production use to AppHouse Software in accordance with the
* terms of the Oracle Master License Agreement 174628:
*
*   Oracle Coherence: DataGrid Edition
*     B6A196586D59E86A11A19B18CF2E98A4  Application "OnlineBanking"
*
* Copyright (c) 2000-2009 Oracle Corp.
*
******************************************************************************
*/

        // sort licenses, determine the product mix and the license type
        Map     mapPublicEval  = new HashMap();
        Map     mapByAgreement = new HashMap();
        int     nTypeMax       = LicenseData.TYPE_EVAL;
        for (int i = 0, c = aLicense.length; i < c; ++i)
            {
            LicenseData license = aLicense[i];

            if (license.nLicenseType == LicenseData.TYPE_EVAL)
                {
                // generic public evaluation
                String sTerm = license.lDateFrom + "|" + license.lDateTo;
                List   list  = (List) mapPublicEval.get(sTerm);
                if (list == null)
                    {
                    list = new ArrayList();
                    mapPublicEval.put(sTerm, list);
                    }
                String sProduct = license.sSoftware;
                if (!list.contains(sProduct))
                    {
                    list.add(sProduct);
                    }
                }
            else
                {
                // first split out by agreement
                String sAgreement    = license.sAgreement;
                Map    mapByLicensee = (Map) mapByAgreement.get(sAgreement);
                if (mapByLicensee == null)
                    {
                    mapByLicensee = new HashMap();
                    mapByAgreement.put(sAgreement, mapByLicensee);
                    }

                // next, split out by licensee / oem
                String sLicensee = license.sLicensee + "|" + license.fOem;
                Map mapByType = (Map) mapByLicensee.get(sLicensee);
                if (mapByType == null)
                    {
                    mapByType = new HashMap();
                    mapByLicensee.put(sLicensee, mapByType);
                    }

                // next, split out by license type
                int nType = license.nLicenseType;
                Integer IType = Integer.valueOf(nType);
                Map mapByProduct = (Map) mapByType.get(IType);
                if (mapByProduct == null)
                    {
                    mapByProduct = new HashMap();
                    mapByType.put(IType, mapByProduct);
                    }

                // next, split out by product
                String sProduct = license.sSoftware;
                Map mapByRestrict = (Map) mapByProduct.get(sProduct);
                if (mapByRestrict == null)
                    {
                    mapByRestrict = new HashMap();
                    mapByProduct.put(sProduct, mapByRestrict);
                    }

                // next, sort by restrictions
                String sRestrict = license.sSite + "|" + license.sApplication
                        + "|" + license.lDateFrom + "|" + license.lDateTo;
                Map mapByUid = (Map) mapByRestrict.get(sRestrict);
                if (mapByUid == null)
                    {
                    mapByUid = new HashMap();
                    mapByRestrict.put(sRestrict, mapByUid);
                    }

                mapByUid.put(license.uid, license);

                // check if a "higher" license type
                if (nType > nTypeMax)
                    {
                    nTypeMax = nType;
                    }
                }
            }

        final String sBlock = dup('*', 78);
        StringBuffer sb     = new StringBuffer();
        sb.append('\n')
          .append(sBlock)
          .append("\n*")
          .append("\n* " + CacheFactory.PRODUCT + " is licensed by Oracle.")
          .append("\n*\n");

        // print the generic evaluation licenses
        for (Iterator iterDateRanges = mapPublicEval.entrySet().iterator();
                iterDateRanges.hasNext(); )
            {
            Map.Entry entry     = (Map.Entry) iterDateRanges.next();
            String    sTerm     = (String) entry.getKey();
            List      list      = (List)   entry.getValue();
            String[]  asDate    = parseDelimitedString(sTerm, '|');
            long      lDateFrom = Long.parseLong(asDate[0]);
            long      lDateTo   = Long.parseLong(asDate[1]);
            int       cDaysLeft = (int) ((lDateTo - System.currentTimeMillis()) / UNIT_D);
            String    sMessage  = "Licensed for evaluation use from "
                    + formatDate(lDateFrom) + " until " + formatDate(lDateTo)
                    + " (" + (cDaysLeft < 0 ? "expired"
                    : (cDaysLeft + " days remaining")) + ")";
            sb.append(breakLines(sMessage, 76, "* "));

            for (Iterator iterProductNames = list.iterator();
                    iterProductNames.hasNext(); )
                {
                sb.append("\n*   ")
                  .append(iterProductNames.next());
                }
            sb.append("\n*\n");
            }

        // mapByAgreement keyed by sAgreement
        // mapByLicensee  keyed by sLicensee | fOem
        // mapByType      keyed by nLicenseType
        // mapByProduct   keyed by sSoftware
        // mapByRestrict  keyed by sSite | sApplication | lDateFrom | lDateTo
        // mapByUid       keyed by UID (value is the license)
        for (Iterator iterAgreements = mapByAgreement.entrySet().iterator();
                iterAgreements.hasNext(); )
            {
            Map.Entry entryAgreement = (Map.Entry) iterAgreements.next();
            String    sAgreement     = (String) entryAgreement.getKey();
            Map       mapByLicensee  = (Map) entryAgreement.getValue();
            for (Iterator iterLicensees = mapByLicensee.entrySet().iterator();
                    iterLicensees.hasNext(); )
                {
                Map.Entry entryLicensee  = (Map.Entry) iterLicensees.next();
                String    sLicenseeInfo  = (String) entryLicensee.getKey();
                String[]  asLicenseeInfo = parseDelimitedString(sLicenseeInfo, '|');
                String    sLicensee      = asLicenseeInfo[0];
                boolean   fOem           = Boolean.valueOf(asLicenseeInfo[1]).booleanValue();
                Map       mapByType      = (Map) entryLicensee.getValue();
                for (Iterator iterTypes = mapByType.entrySet().iterator();
                        iterTypes.hasNext(); )
                    {
                    Map.Entry entryType     = (Map.Entry) iterTypes.next();
                    int       nLicenseType  = ((Integer) entryType.getKey()).intValue();
                    Map       mapByProduct  = (Map) entryType.getValue();

                    String sType;
                    switch (nLicenseType)
                        {
                        case LicenseData.TYPE_EVAL:
                            sType = LicenseData.NAME_EVAL;
                            break;
                        case LicenseData.TYPE_DEV:
                            sType = LicenseData.NAME_DEV;
                            break;
                        case LicenseData.TYPE_PROD:
                            sType = LicenseData.NAME_PROD;
                            break;
                        default:
                            sType = "<unknown>";
                            break;
                        }

                    // print out "according to" terms
                    StringBuffer sbTerms = new StringBuffer();
                    if (fOem || sAgreement != null)
                        {
                        sbTerms.append("Licensed for ");

                        if (fOem)
                            {
                            sbTerms.append("OEM ");
                            }

                        sbTerms.append(sType)
                               .append(" use to ")
                               .append(formatString(sLicensee))
                               .append(" in accordance with the terms of the ");

                        if (sAgreement == null)
                            {
                            sbTerms.append("Oracle ")
                                   .append(sType)
                                   .append(" license (see http://www.tangosol.com/license.jsp):");
                            }
                        else
                            {
                            sbTerms.append(sAgreement)
                                   .append(':');
                            }
                        }
                    else
                        {
                        sbTerms.append("Licensed to ")
                               .append(formatString(sLicensee))
                               .append(" for ")
                               .append(sType)
                               .append(" use in accordance with the")
                               .append(" terms of the Oracle ")
                               .append(sType)
                               .append(" license (see http://www.tangosol.com/license.jsp):");
                        }

                    sb.append(breakLines(sbTerms.toString(), 76, "* "))
                      .append("\n*\n");

                    for (Iterator iterProducts = mapByProduct.entrySet().iterator();
                            iterProducts.hasNext(); )
                        {
                        Map.Entry entryProduct  = (Map.Entry) iterProducts.next();
                        String    sSoftware     = (String) entryProduct.getKey();
                        Map       mapByRestrict = (Map) entryProduct.getValue();

                        // print out product name
                        sb.append("*   ")
                          .append(sSoftware)
                          .append('\n');

                        for (Iterator iterRestricts = mapByRestrict.entrySet().iterator();
                                iterRestricts.hasNext(); )
                            {
                            Map.Entry entryRestrict = (Map.Entry) iterRestricts.next();
                            Map       mapByUid      = (Map) entryRestrict.getValue();
                            for (Iterator iterUids = mapByUid.entrySet().iterator();
                                    iterUids.hasNext(); )
                                {
                                Map.Entry   entryLicense = (Map.Entry) iterUids.next();
                                UID         uid          = (UID) entryLicense.getKey();
                                LicenseData license      = (LicenseData) entryLicense.getValue();

                                String sDesc;
                                if (license.cSeats > 0)
                                    {
                                    sDesc = license.cSeats + " seats";
                                    }
                                else if (license.cUsers > 0)
                                    {
                                    sDesc = license.cUsers + " users";
                                    }
                                else if (license.cServers > 0)
                                    {
                                    sDesc = license.cServers + " servers";
                                    }
                                else if (license.cSockets > 0)
                                    {
                                    sDesc = license.cSockets + " CPU-sockets";
                                    }
                                else if (license.cCpus > 0)
                                    {
                                    sDesc = license.cCpus + " CPU-cores";
                                    }
                                else
                                    {
                                    sDesc = "Unlimited use";
                                    }

                                if (license.sSite != null)
                                    {
                                    sDesc += " at site \"" + formatString(license.sSite) + "\"";
                                    }

                                if (license.sApplication != null)
                                    {
                                    sDesc += " in application \"" + formatString(license.sApplication) + "\"";
                                    }

                                if (license.lDateTo > 0L)
                                    {
                                    int    cDaysLeft = (int) ((license.lDateTo - System.currentTimeMillis()) / UNIT_D);
                                    String sDate     = formatDate(license.lDateTo);
                                    if (cDaysLeft < 0)
                                        {
                                        sDesc += " (expired " + sDate + ")";
                                        }
                                    else if (cDaysLeft <= 30)
                                        {
                                        sDesc += " (expires " + sDate + ", " + cDaysLeft + " days remaining)";
                                        }
                                    else
                                        {
                                        sDesc += " (expires " + sDate + ")";
                                        }
                                    }

                                sb.append("*     ")
                                  .append(toHex(uid.toByteArray()))
                                  .append("  ")
                                  .append(breakLines(sDesc, 77, "*" + dup(' ', 39), false))
                                  .append('\n');
                                }
                            }

                        sb.append("*\n");
                        }
                    }
                }
            }

        if (nTypeMax != LicenseData.TYPE_PROD)
            {
            sb.append("* A production license is required for production use.")
              .append(" For more information,\n")
              .append("* see http://www.tangosol.com/license.jsp.")
              .append("\n*\n");
            }

        sb.append("* Copyright (c) Oracle Corp.")
          .append("\n*\n")
          .append(sBlock)
          .append('\n');

        out(sb.toString());
        }

    /**
    * Format a date in the form YYYY-MM-DD.
    *
    * @param ldt  the date
    */
    static String formatDate(long ldt)
        {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(ldt);

        int nYear  = calendar.get(Calendar.YEAR);
        int nMonth = calendar.get(Calendar.MONTH) + 1;
        int nDay   = calendar.get(Calendar.DATE);

        return toDecString(nYear,  Math.max(4, getMaxDecDigits(nYear))) + "-"
               + toDecString(nMonth, Math.max(2, getMaxDecDigits(nMonth))) + "-"
               + toDecString(nDay,   Math.max(2, getMaxDecDigits(nDay)));
        }

    /**
    * Replace all asterisk and whitespace characters that span a line
    * terminator with a single space.
    *
    * @param s  the string to format
    * @return a version of the given string with all asterisk and whitespace
    *         characters that span a line terminator replaced with a single
    *         space (including the line terminator)
    */
    static String formatString(String s)
        {
        return s.replaceAll("[*\\s]*[\n\r\u0085\u2028\u2029][*\\s]*", " ");
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Determines the reason that the passed license is invalid.
    *
    * @param data  the license data to check
    *
    * @return  a String describing the problem with the license, otherwise
    *          null
    */
    public static String getLicenseFailure(LicenseData data)
        {
        if (data.lDateFrom > 0 || data.lDateTo > 0)
            {
            long lDateFrom = data.lDateFrom;
            long lDateTo   = data.lDateTo;

            if (lDateFrom <= 0)
                {
                lDateFrom = 0;
                }
            if (lDateTo <= 0)
                {
                lDateTo = Long.MAX_VALUE;
                }
            else
                {
                // the day of the expiry plus a 3-day grace period
                lDateTo += UNIT_D << 2;
                }

            long lDate = System.currentTimeMillis();
            if (lDate < lDateFrom || lDate > lDateTo)
                {
                return "The necessary edition to perform the operation has expired.";
                }
            }

        return null;
        }


    // ----- inner class:  LicenseData --------------------------------------

    /**
    * This class holds the license data for one license.
    */
    public static class LicenseData
            implements Serializable
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a LicenseData object from raw license data.
        */
        public LicenseData(String sSoftware, String sEdition, String sLicensee,
                boolean fOem, String sAgreement, int nLicenseType, String sClass,
                String sSite, String sApplication, long lDateFrom, long lDateTo,
                long lDateRenew, int cSeats, int cUsers, int cServers,
                int cSockets, int cCpus, UID uid)
            {
            this.sSoftware    = sSoftware;
            this.sEdition     = sEdition;
            this.sLicensee    = sLicensee;
            this.fOem         = fOem;
            this.sAgreement   = sAgreement;
            this.nLicenseType = nLicenseType;
            this.sClass       = sClass;
            this.sSite        = sSite;
            this.sApplication = sApplication;
            this.lDateFrom    = lDateFrom;
            this.lDateTo      = lDateTo;
            this.lDateRenew   = lDateRenew;
            this.cSeats       = cSeats;
            this.cUsers       = cUsers;
            this.cServers     = cServers;
            this.cSockets     = cSockets;
            this.cCpus        = cCpus;
            this.uid          = uid;
            }

        // ----- internal -----------------------------------------------

        /**
        * Serialize the license data into a URI.
        *
        * @return a String containing a URI-serialized form of the license
        */
        public String toUri()
            {
            return new String(Base64OutputStream.encode(toString().getBytes()));
            }

        // ----- Object methods -----------------------------------------

        /**
        * Provide a human-readable and complete description of the information
        * held by this LicenseData object.
        *
        * @return a String description of this LicenseData object
        */
        public String toString()
            {
            StringBuffer sb = new StringBuffer();

            sb.append("Software            :  ")
              .append(sSoftware)
              .append('\n');

            sb.append("Licensee            :  ")
              .append(formatString(sLicensee));
            if (fOem)
                {
                sb.append(" (OEM use only)");
                }
            sb.append('\n');

            if (sAgreement != null)
                {
                sb.append("License Agreement   :  ")
                  .append(sAgreement)
                  .append('\n');
                }

            if (uid != null)
                {
                sb.append("License Key         :  ")
                  .append(uid)
                  .append('\n');
                }

            String sType;
            switch (nLicenseType)
                {
                case TYPE_EVAL:
                    sType = NAME_EVAL;
                    break;
                case TYPE_DEV:
                    sType = NAME_DEV;
                    break;
                case TYPE_PROD:
                    sType = NAME_PROD;
                    break;
                default:
                    sType = "<unknown>";
                    break;
                }
            sb.append("License Type        :  ")
              .append(sType)
              .append('\n');

            if (sSite != null)
                {
                sb.append("Site Restriction    :  ")
                  .append(formatString(sSite))
                  .append('\n');
                }

            if (sApplication != null)
                {
                sb.append("Only for Application:  ")
                  .append(formatString(sApplication))
                  .append('\n');
                }

            if (lDateFrom > 0L || lDateTo > 0L)
                {
                if (lDateFrom <= 0L)
                    {
                    sb.append("Termination Date    :  ")
                      .append(formatDate(lDateTo));
                    }
                else if (lDateTo <= 0L)
                    {
                    sb.append("Effective Date      :  ")
                      .append(formatDate(lDateFrom));
                    }
                else
                    {
                    sb.append("License Term        :  ")
                      .append(formatDate(lDateFrom))
                      .append(" to ")
                      .append(formatDate(lDateTo));
                    }

                long lDateNow = System.currentTimeMillis();
                if (lDateNow < (lDateFrom <= 0L ? 0L             : lDateFrom) ||
                    lDateNow > (lDateTo   <= 0L ? Long.MAX_VALUE : lDateTo  ))
                    {
                    sb.append(" (expired)");
                    }

                sb.append('\n');
                }

            if (cSeats > 0)
                {
                sb.append("Maximum Seats       :  ")
                  .append(cSeats)
                  .append('\n');
                }

            if (cUsers > 0)
                {
                sb.append("Maximum Users       :  ")
                  .append(cUsers)
                  .append('\n');
                }

            if (cServers > 0)
                {
                sb.append("Maximum Servers     :  ")
                  .append(cServers)
                  .append('\n');
                }

            if (cSockets > 0)
                {
                sb.append("Maximum CPU Sockets :  ")
                  .append(cSockets)
                  .append('\n');
                }

            if (cCpus > 0)
                {
                sb.append("Maximum CPU Cores   :  ")
                  .append(cCpus)
                  .append('\n');
                }

            return sb.toString();
            }

        // ----- constants ----------------------------------------------

        public static final int TYPE_EVAL = 0;
        public static final int TYPE_DEV  = 1;
        public static final int TYPE_PROD = 2;

        public static final String NAME_EVAL = "evaluation";
        public static final String NAME_DEV  = "development";
        public static final String NAME_PROD = "production";

        public static final String URL_EVAL = "http://www.tangosol.com/license-eval.htm";
        public static final String URL_DEV  = "http://www.tangosol.com/license-develop.htm";
        public static final String URL_PROD = "http://www.tangosol.com/license-prod.htm";

        // ----- data members -------------------------------------------

        public final String  sSoftware;
        public final String  sEdition;
        public final String  sLicensee;
        public final boolean fOem;
        public final String  sAgreement;
        public final int     nLicenseType;
        public final String  sClass;
        public final String  sSite;
        public final String  sApplication;
        public final long    lDateFrom;
        public final long    lDateTo;
        public final long    lDateRenew;
        public final int     cSeats;
        public final int     cUsers;
        public final int     cServers;
        public final int     cSockets;
        public final int     cCpus;                     // a.k.a. "cores"
        public final UID     uid;
        }


    // ----- data members ---------------------------------------------------

    /**
    * Tracks whether the license data has been configured.
    */
    private static boolean m_fConfigured;

    /**
    * Map from class names to list of license data.
    */
    private static Map m_mapLicenses;
    static
        {
        // it is possible that <clinit> is called after setLicenseData
        if (!m_fConfigured)
            {
            m_mapLicenses = new SafeHashMap();
            }
        }

    /**
    * Set of classes that have been checked.
    */
    private static Map m_mapChecked = new SafeHashMap();
    }