/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.oracle.coherence.common.base.Objects;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class PofConfigLogger
    {
    @SuppressWarnings("unchecked")
    public static void log(String... asConfig)
        {
        XmlElement xmlRoot = new SimpleElement("pof-config");
        XmlElement xmlUserTypes = xmlRoot.addElement("user-type-list");

        xmlUserTypes.addElement("include").setString("coherence-pof-config.xml");

        for (String sConfig : asConfig)
            {
            if (sConfig != null && !sConfig.isEmpty())
                {
                xmlUserTypes.addElement("include").setString(sConfig);
                }
            }

        xmlRoot.addElement("enable-type-discovery").setBoolean(true);

        ConfigurablePofContext           context   = new ConfigurablePofContext(xmlRoot);
        context.ensureInitialized();
        ConfigurablePofContext.PofConfig pofConfig = context.getPofConfig();
        SortedSet<Integer>               setId     = new TreeSet<>((Set<Integer>)pofConfig.m_mapClassNameByTypeId.keySet());


        int last  = -1;
        Package pkgLast = context.getClass(0).getPackage();
        int pkgId = 0;
        for (Integer id : setId)
            {
            Class clz = context.getClass(id);
            Package pkg = clz.getPackage();
            if (!pkg.equals(pkgLast))
                {
                System.out.println(pkgId + " - " + last + " " + pkgLast.getName());
                pkgLast = pkg;
                pkgId = id;
                }

            if (id == last + 1)
                {
                last = id;
                }
            else
                {
                if (id != pkgId)
                    {
                    System.out.println(pkgId + " - " + (id - 1) + " " + pkgLast.getName());
                    pkgId = id;
                    }
                System.out.println((last + 1) + " - " + (id - 1) + " AVAILABLE");
                last = id;
                }
            }
        }

    public static void main(String[] args)
        {
        PofConfigLogger.log(args);
        }
/*
<pof-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-pof-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-pof-config coherence-pof-config.xsd">
  <user-type-list>
    <!-- by default just include coherence POF user types -->
    <include>coherence-pof-config.xml</include>
  </user-type-list>
  <enable-type-discovery>true</enable-type-discovery>
</pof-config>
 */
    }
