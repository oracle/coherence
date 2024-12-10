/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.license;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.util.Base;
import com.tangosol.util.UID;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import java.util.regex.Pattern;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.LineNumberReader;

import java.security.Signature;

import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;


/**
* Provides information regarding host platform's CPUs.
* <p>
* The following terms are used to describe CPUs:
* <ul>
* <li/>Execution-Unit - a logical unit on which tasks may be scheduled by the OS
* <li/>Socket         - a physical piece of hardware which houses one or more
*                       execution units
* <li>CPU-Descriptor  - a Map containing information about a single execution unit
* </ul>
* Multi-core and hyper-threaded CPUs are examples of CPUs with multiple
* Execution-Units per Socket. The Operating System and Java will typically
* report each Execution-Unit as a unique CPU.  For instance a quad-core box
* will appear to the OS and Java as having four CPUs.
*
* @author mf 2006.09.25
*/
public class ProcessorInfo
       extends Base
    {
    // ---- constructor(s) --------------------------------------------------

    /**
    * Construct a ProcessorInfo object, and compute information about this
    * machine.
    */
    public ProcessorInfo()
        {
        inspectMachine();
        }

    /**
    * Construct a ProcessorInfo object, and compute information about this
    * machine.
    *
    * @param uid Coherence UID to include in each descriptor
    */
    public ProcessorInfo(UID uid)
        {
        m_sUid = uid.toString();
        inspectMachine();
        }

    /**
    * Construct a ProcessorInfo object, and compute information about the
    * machine specified in the supplied descriptor array
    *
    * @param amapCpu the CPU descriptors
    */
    public ProcessorInfo(Map[] amapCpu)
        {
        loadDictionary();
        m_amapCpu = amapCpu;
        m_cSocket = getSocketCount(amapCpu);
        m_cCpu    = computeCpuCount(amapCpu.length);
        }

    // ---- accessors -------------------------------------------------------

    /**
    * Return an array of Maps describing this machines processors.
    *
    * @return array of processor maps
    */
    public Map[] getDescriptors()
        {
        return m_amapCpu.clone();
        }

    /**
    * Return the total number of execution units for this machine.
    *
    * @return unit count
    */
    public int getExecutionUnitCount()
        {
        return getExecutionUnitCount(m_amapCpu);
        }

    /**
    * Return the total number of sockets for this machine.
    *
    * @return socket count
    */
    public int getSocketCount()
        {
        return m_cSocket;
        }


    /**
    * Return the total number of CPUs for this machine.
    *
    * @return cpu count
    */
    public int getCpuCount()
        {
        return m_cCpu;
        }


    // ---- Object methods -------------------------------------------------

    /**
    * Return a string representation of this machines CPU information.
    *
    * @return CPU information as a string
    */
    public String toString()
        {
        int cSockets = getSocketCount();
        int cUnits   = getExecutionUnitCount();
        int cCpus    = getCpuCount();

        StringBuffer sb = new StringBuffer()
          .append(toXml())
          .append("\n\n<!-- Machine has ")
          .append(getExecutionUnitCount())
          .append(" total cores");

        if (cUnits > cCpus)
            {
            sb.append(" (")
              .append(cUnits - cCpus)
              .append(" virtual)");
            }

        sb.append(" on ")
          .append(cSockets)
          .append(" sockets -->\n");

        return sb.toString();
        }


    // ---- helper methods --------------------------------------------------

    /**
    * Load the processor dictionary.
    */
    protected void loadDictionary()
        {
        SimpleParser sp = new SimpleParser();
        ClassLoader  cl = Base.class.getClassLoader();
        if (cl == null)
            {
            cl = getContextClassLoader();
            }

        InputStream in = null;
        XmlElement  xmlDoc;
        try
            {
            in = cl.getResourceAsStream(DICTIONARY_XML);
            if (in == null)
                {
                throw new Exception(DICTIONARY_XML + " not found");
                }

            xmlDoc = sp.parseXml(in);

            if (!xmlDoc.getName().equals(DICTIONARY))
                {
                throw new Exception(DICTIONARY_XML + " does not contain "
                        + DICTIONARY + " element");
                }
            }
        catch (Exception e)
            {
            log("Error loading " + DICTIONARY_XML +": " + e);
            return;
            }
        finally
            {
            if (in != null)
                {
                try
                    {
                    in.close();
                    }
                catch (IOException e) {}
                }
            }

        // load signature
        Signature   signature;
        try
            {
            in = cl.getResourceAsStream("tangosol.cer");
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate        cert    = factory.generateCertificate(in);

            signature = Signature.getInstance("SHA1withDSA");
            signature.initVerify(cert.getPublicKey());
            }
        catch (Exception e)
            {
            log("Error during signature preparation: " + e);
            return;
            }
        finally
            {
            if (in != null)
                {
                try
                    {
                    in.close();
                    }
                catch (IOException e) {}
                }
            }

        List listTemplates = new ArrayList();
        for (Iterator i = xmlDoc.getElements(TEMPLATE); i.hasNext(); )
            {
            XmlElement   xml        = (XmlElement) i.next();
            HashMap      map        = new HashMap();
            List         list       = xml.getElementList();
            StringBuffer sbConcat   = new StringBuffer();
            XmlElement   xmlExUnits = null;
            String       sSig       = null;

            for (ListIterator j = list.listIterator(); j.hasNext(); )
                {
                XmlElement xmlSub = (XmlElement) j.next();
                String     sName  = xmlSub.getName();
                String     sValue = xmlSub.getString();
                if (sName.equals(SIGNATURE))
                    {
                    sSig = sValue;
                    continue;
                    }
                sbConcat.append(xmlSub);

                if (sName.equals(EXECUTION_UNITS))
                    {
                    xmlExUnits = xmlSub;
                    }

                if (xmlSub.getSafeAttribute("regex").getBoolean())
                    {
                    Pattern pat = Pattern.compile(sValue);
                    map.put(sName, pat);
                    }
                else
                    {
                    map.put(sName, sValue);
                    }
                }

            if (sSig != null && xmlExUnits != null)
                {
                try
                    {
                    ByteArrayOutputStream streamRaw = new ByteArrayOutputStream();
                    DataOutputStream      streamSig = new DataOutputStream(streamRaw);
                    streamSig.writeUTF(sbConcat.toString());

                    signature.update(streamRaw.toByteArray());

                    if (signature.verify(Base.parseHex(sSig)))
                        {
                        // valid signature, add the template
                        listTemplates.add(map);

                        // check for threads attribute
                        int cThreads = xmlExUnits.getSafeAttribute("thread-count").getInt(0);
                        int cExUnits = xmlExUnits.getInt(0);
                        if (cThreads > 0)
                            {
                            // add additional definition for when threading is disabled
                            // this will only be used if we detect that there aren't
                            // enough logical cores during matching
                            Map mapClone = (Map) map.clone();
                            mapClone.put(EXECUTION_UNITS, Integer.toString(cExUnits - cThreads));
                            listTemplates.add(mapClone);
                            }
                        }
                    else
                        {
                        log("Skipping processor template with invalid signature:\n" + xml);
                        }
                    }
                catch (Exception e)
                    {
                    // error validating, skip it
                    if (m_fVerbose)
                        {
                        log("Error while validating template:\n" + xml);
                        log(e);
                        }
                    }
                }
            }

        m_amapCpuTemplate = (Map[]) listTemplates.toArray(
                                new Map[listTemplates.size()]);
        }

    /**
    * Retrieve the value for a environment variable.
    *
    * @param sEnvName  the variable name
    *
    * @return the env variable name or null if it is unset
    */
    protected String getEnv(String sEnvName)
        {
        if (m_mapEnv == null)
            {
            try
                {
                return System.getenv(sEnvName);
                }
            catch (Throwable e)
                {
                // System.getenv isn't supported, avoid future use
                if (m_fVerbose)
                    {
                    log("getEnv unavailable: " + e);
                    log(e);
                    }
                }

            // try to read environment by launching a shell
            try
                {
                Runtime  rt = Runtime.getRuntime();
                String[] asCmd;

                if (System.getProperty("os.name").indexOf("Windows") == -1)
                    {
                    // Unix
                    asCmd = new String[] {"sh", "-c", "set"};
                    }
                else
                    {
                    // Windows
                    asCmd = new String[] {"cmd", "/c", "set"};
                    }

                Process          proc   = rt.exec(asCmd);
                InputStream      in     = proc.getInputStream();
                LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));
                Map              mapEnv = new HashMap();

                for (String sLine = reader.readLine(); sLine != null; sLine = reader.readLine())
                    {
                    // split at = sign
                    int ofEq = sLine.indexOf('=');
                    if (ofEq > 0 && ofEq + 1 < sLine.length())
                        {
                        String sName  = sLine.substring(0, ofEq).trim();
                        String sValue = sLine.substring(ofEq + 1).trim();
                        mapEnv.put(sName.toUpperCase(), sValue);
                        }
                    }

                // save for future use
                m_mapEnv = mapEnv;
                }
            catch (Exception e)
                {
                // we have no way to get env variables, try system properties
                if (m_fVerbose)
                    {
                    log("Environment variables unavailable: " + e);
                    log(e);
                    }

                try
                    {
                    m_mapEnv = System.getProperties();
                    }
                catch (Exception e2)
                    {
                    if (m_fVerbose)
                        {
                        log("System property map unavailable: " + e);
                        log(e);
                        }
                    return System.getProperty(sEnvName.toUpperCase());
                    }
                }
            }

        return (String) m_mapEnv.get(sEnvName.toUpperCase());
        }

    /**
    * Return an array of processor descriptions for the current machine,
    * based on information Windows specific information.
    *
    * @return array of processor descriptions
    */
    protected Map[] inspectWindows()
        {
        // Windows XP includes a number of useful environment variables;
        // for example:

        // Results for single CPU Intel
        //  PROCESSOR_ARCHITECTURE=x86
        //  PROCESSOR_IDENTIFIER=x86 Family 6 Model 13 Stepping 8, GenuineIntel
        //  PROCESSOR_LEVEL=6
        //  PROCESSOR_REVISION=0d08
        //  NUMBER_OF_PROCESSORS=1

        // Results for dual CPU AMD
        //  PROCESSOR_ARCHITECTURE=x86
        //  PROCESSOR_IDENTIFIER=x86 Family 15 Model 5 Stepping 1, AuthenticAMD
        //  PROCESSOR_LEVEL=15
        //  PROCESSOR_REVISION=0501
        //  NUMBER_OF_PROCESSORS=2

        // NOTE: LEVEL == FAMILY, REVISION ~ Stepping
        // likely that NUMBER_OF_PROCESSORS == cores

        // TODO: check Win2K, Win2003, Vista

        Map mapInfo = new HashMap();
        mapInfo.put(ENVIRONMENT, "Windows");

        mapInfo.put(ARCHITECTURE, getEnv("PROCESSOR_ARCHITECTURE"));
        if (m_sUid != null)
            {
            mapInfo.put(COHERENCE_UID, m_sUid);
            }

        // parse proc id to get additional info
        String sId = getEnv("PROCESSOR_IDENTIFIER");
        mapInfo.put(DESCRIPTION, sId);
        // get Family, Model, Stepping, and Vendor
        String[] sTok = sId.split(" |,");
        int cTokens = sTok.length;

        mapInfo.put(VENDOR, sTok[cTokens - 1]);

        for (int i = 0; i < cTokens; ++i)
            {
            if (sTok[i].equals("Family") && i + 1 < cTokens)
                {
                mapInfo.put(FAMILY, sTok[++i]);
                }
            else if (sTok[i].equals("Model") && i + 1 < cTokens)
                {
                mapInfo.put(MODEL, sTok[++i]);
                }
            else if (sTok[i].equals("Stepping") && i + 1 < cTokens)
                {
                mapInfo.put(STEPPING, sTok[++i]);
                }
            }

        // treat each processor the same
        mapInfo = Collections.unmodifiableMap(mapInfo);
        Map[] amapCpu = new Map[Runtime.getRuntime().availableProcessors()];
        Arrays.fill(amapCpu, mapInfo);
        return amapCpu;
        }

    /**
    * Return an array of processor descriptions for the current machine,
    * based on information Linux specific information.
    *
    * @return array of processor descriptions
    */
    protected Map[] inspectLinux()
        throws Exception
        {
        // look at contents of /proc/cpuinfo
        FileInputStream  in     = new FileInputStream("/proc/cpuinfo");
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(in));

        // file format is id : value
        // there is one info block per processor, each has a "processor"
        // element at the start
        String sArch    = System.getProperty("os.arch");
        List   listInfo = new ArrayList();
        Map    mapInfo  = null;
        for (String sLine = reader.readLine(); sLine != null; sLine = reader.readLine())
            {
            int iColon = sLine.indexOf(':');
            if (iColon > 0 && iColon + 1 < sLine.length())
                {
                String sName  = sLine.substring(0, iColon).trim();
                String sValue = sLine.substring(iColon + 1).trim();
                if (sName.equals("processor"))
                    {
                    // new processor
                    if (mapInfo != null)
                        {
                        listInfo.add(Collections.unmodifiableMap(mapInfo));
                        }
                    mapInfo = new HashMap();
                    mapInfo.put(ENVIRONMENT, "Linux");
                    mapInfo.put(ARCHITECTURE, sArch);
                    if (m_sUid != null)
                        {
                        mapInfo.put(COHERENCE_UID, m_sUid);
                        }
                    }
                else if (sName.equals("vendor_id"))
                    {
                    mapInfo.put(VENDOR, sValue);
                    }
                else if (sName.equals("cpu family"))
                    {
                    mapInfo.put(FAMILY, sValue);
                    }
                else if (sName.equals("model"))
                    {
                    mapInfo.put(MODEL, sValue);
                    }
                else if (sName.equals("stepping"))
                    {
                    mapInfo.put(STEPPING, sValue);
                    }
                else if (sName.equals("cpu cores"))
                    {
                    // Sometimes Linux identifies cores/cpu
                    mapInfo.put(EXECUTION_UNITS, sValue);
                    }
                else
                    {
                    // include other info, this will be useful
                    // for generating new templates especially
                    // for non x86 processors
                    sName = sName.replace(' ', '-');
                    mapInfo.put(sName, sValue);
                    }
                }
            }
        in.close();

        if (mapInfo != null)
            {
            listInfo.add(Collections.unmodifiableMap(mapInfo));
            }

        return (Map[]) listInfo.toArray(new Map[listInfo.size()]);
        }


    /**
    * Return an array of processor descriptions for the current machine
    * based solely on information available from the JVM.
    *
    * @return array of processor descriptions
    */
    protected Map[] inspectJava()
        {
        // Use System properties
        Map mapInfo = new HashMap();
        mapInfo.put(ENVIRONMENT, System.getProperty("os.name"));
        mapInfo.put(ARCHITECTURE, System.getProperty("os.arch"));
        if (m_sUid != null)
            {
            mapInfo.put(COHERENCE_UID, m_sUid);
            }

        // treat each processor the same
        mapInfo = Collections.unmodifiableMap(mapInfo);
        Map[] amapCpu = new Map[Runtime.getRuntime().availableProcessors()];
        Arrays.fill(amapCpu, mapInfo);
        return amapCpu;
        }

    /**
    * Inspect this machine and determine its CPU descriptors.
    */
    public void inspectMachine()
        {
        try
            {
            loadDictionary();

            // start by identifying the OS we are on
            Map[] amapCpu = null;
            try
                {
                String sOs = System.getProperty("os.name");
                if (sOs.indexOf("Windows") != -1)
                    {
                    amapCpu = inspectWindows();
                    }
                else if (sOs.indexOf("Linux") != -1)
                    {
                    amapCpu = inspectLinux();
                    }
                // TODO: Solaris, SunOs "/usr/sbin/psrinfo -vp" or "/usr/sbin/psrinfo -v"
                // see http://solaris.reys.net/english/2006/07/t2000_first_impressions for
                // info on psrinfo example formatting

                // TODO: AIX "lsdev -Cc processor"
                // See http://www.unixguide.net/unixguide.shtml for details on
                // calculating CPU info for various UNIXs
                }
            catch (Throwable e)
                {
                // error reading OS specific CPU information
                // eat the exception and use pure Java info
                if (m_fVerbose)
                    {
                    log("Error in OS specific analysis falling back on pure Java approach: " + e);
                    log(e);
                    }
                }

            if (amapCpu == null || amapCpu.length == 0)
                {
                amapCpu = inspectJava();
                }

            // allow for socket count to be increased for license testing
            int cCfgSocket = Config.getInteger("coherence.socket", 1);
            int cCfgCpu    = Config.getInteger("coherence.cpu", 1);

            m_amapCpu = amapCpu;
            m_cSocket = Math.max(cCfgSocket, getSocketCount(amapCpu));
            m_cCpu    = Math.max(cCfgCpu, computeCpuCount(Runtime.getRuntime().
                            availableProcessors()));
            }
        catch (Throwable t)
            {
            // ensure that we always succeed
            log("unable to identify processor info");
            log(t);
            m_cSocket = m_cCpu = Runtime.getRuntime().availableProcessors();
            }
        }

    /**
    * Return the CPU descriptors as an XmlElement
    *
    * @return XML representation
    */
    public XmlElement toXml()
        {
        Map[]      amapCpu  = m_amapCpu;
        XmlElement xmlRoot  = new SimpleElement(MACHINE);
        List       listXml  = xmlRoot.getElementList();

        for (int i = 0, c = amapCpu.length; i < c; ++i)
            {
            listXml.add(toXml(DESCRIPTOR, amapCpu[i]));
            }

        return xmlRoot;
        }


    /**
    * Return a Map as an XmlElement
    *
    * @param sParent the parent element name
    * @param map     the map to translate
    *
    * @return XML representation
    */
    public static XmlElement toXml(String sParent, Map map)
        {
            XmlElement xml = new SimpleElement(sParent);
            for (Iterator j = map.entrySet().iterator(); j.hasNext(); )
                {
                Map.Entry entry  = (Map.Entry) j.next();
                Object    oKey   = entry.getKey();
                Object    oValue = entry.getValue();
                if (oKey != null && oValue != null)
                    {
                    xml.addElement(oKey.toString()).
                            setString(oValue.toString());
                    }
                }

        return xml;
        }

    /**
    * Return a set of CPU descriptors from an XmlElement
    *
    * @return the descriptor represented as a Map
    */
    public static Map[] fromXml(XmlElement xmlDoc)
        {
        if (xmlDoc == null)
            {
            return null;
            }

        List listMaps = new ArrayList();
        for (Iterator i = xmlDoc.getElements(DESCRIPTOR); i.hasNext(); )
            {
            XmlElement xml = (XmlElement) i.next();
            Map map = new HashMap();
            List list = xml.getElementList();
            for (ListIterator j = list.listIterator(); j.hasNext(); )
                {
                XmlElement xmlSub = (XmlElement) j.next();
                map.put(xmlSub.getName(), xmlSub.getValue());
                }
            listMaps.add(map);
            }
        return (Map[]) listMaps.toArray(new Map[listMaps.size()]);
        }


    /**
    * Count the number of sockets based on the supplied processor maps.
    *
    * @param amapCpu  the array of processor descriptors
    *
    * @return socket count
    */
    public int getSocketCount(Map[] amapCpu)
        {
        if (amapCpu == null)
            {
            return 1;
            }

        retry:
        while (true)
            {
            try
                {
                int   cCpus   = amapCpu.length;
                int[] acUnits = new int[cCpus];

                // lookup core count for each CPU
                for (int i = 0; i < cCpus; ++i)
                    {
                    acUnits[i] = lookupExecutionUnits(amapCpu[i]);
                    }

                // account for clones; a clone is a "duplicate" CPU descriptor;
                // an n execution-unit processor will have n-1 clones
                int cSocket = 0;
                for (int i = 0; i < cCpus; ++i)
                    {
                    int cUnits = acUnits[i];
                    if (cUnits > 0)
                        {
                        ++cSocket;

                        // search for clones, to keep things simple a clone is any other
                        // cpu with the same core count.  Note: we could look for a more
                        // exact clone, i.e. same vendor, family, model, but this would
                        // be architecture specific, and really wouldn't help much
                        int cClones = cUnits - 1; // there should be n-1 clones
                        for (int j = i + 1; j < cCpus && cClones > 0; ++j)
                            {
                            if (acUnits[j] == cUnits)
                                {
                                // a clone found
                                acUnits[j] = 0;
                                --cClones;
                                }
                            }

                        if (cClones > 0)
                            {
                            // not all clones were found; this is most often
                            // caused by multi-core chips which support
                            // multi-threading but have it disabled.  Thus
                            // the definition may be incorrect for this machine
                            // remove it and retry, note this assumes that
                            // there will be a fall-back less specific definition
                            // which will still properly detect the multiple
                            // cores
                            if (m_fVerbose)
                                {
                                log("Missing " + cClones + " siblings for "
                                        + toXml(DESCRIPTOR, amapCpu[i]));
                                }

                            if (removeDefinition(amapCpu[i]))
                                {
                                if (m_fVerbose)
                                    {
                                    log("Definition dropped, rechecking");
                                    }
                                // definition was removed, recheck
                                continue retry;
                                }
                            else
                                {
                                // fall back on "socket == cpu"
                                log("Unable to resolve " + cClones + " siblings for "
                                        + toXml(DESCRIPTOR, amapCpu[i]));
                                return Math.max(cCpus, 1);
                                }
                            }
                        }
                    }
                return Math.max(cSocket, 1);
                }
            catch (Throwable e)
                {
                if (m_fVerbose)
                    {
                    log("Error in matching CPUs: " + e);
                    log(e);
                    }

                return Math.max(amapCpu.length, 1);
                }
            }
        }

    /**
    * compute the total number of cores for this machine.
    *
    * @param cCores the core count
    *
    * @return core count
    */
    protected int computeCpuCount(int cCores)
        {
        try
            {
            // compensate for hyper-threading (aka Symmetrical Multi Threading)
            // which doubles the number of CPUs for Intel processors
            // Note: this system property is not intended for use in socket counting
            if (cCores % 2 == 0 && Config.getBoolean("coherence.smt.enabled"))
                {
                return Math.max((cCores >>> 1), 1);
                }
            else
                {
                // Note: this system property is not intended for use in socket counting
                String sFactor = Config.getProperty("coherence.smt.factor");
                if (sFactor != null)
                    {
                    int nFactor = Integer.parseInt(sFactor);
                    if (cCores % nFactor == 0)
                        {
                        return Math.max((cCores / nFactor), 1);
                        }
                    }
                }
            }
        catch (Throwable e)
            {
            if (m_fVerbose)
                {
                log("Error in counting physical cores: " + e);
                log(e);
                }
            }

        // fall back on CPU == core
        return Math.max(cCores, 1);
        }

    /**
    * Lookup a CPU from the dictionary based on the supplied descriptor.
    *
    * @param mapCpu the CPU to lookup
    *
    * @return the best match from the dictionary, or null if none is found
    */
    public Map lookupCpu(Map mapCpu)
        {
        Map[] amapTemplate = m_amapCpuTemplate;
        if (amapTemplate == null)
            {
            return null;
            }

        if (m_fVerbose)
            {
            log("Searching for matching template for CPU:\n"
                    + toXml(DESCRIPTOR, mapCpu));
            }

        // scan through templates and find the closest match
        Map mapMatch = null;
    nextTemplate:
        for (int i = 0, c = amapTemplate.length, cMatchSize = 0; i < c; ++i)
            {
            Map mapTemplate = amapTemplate[i];
            // each piece of the template (other then EXECUTION_UNITS) must be
            // in this descriptor
            if (mapTemplate != null &&
                    mapTemplate.size() > cMatchSize)
                {
                for (Iterator iter = mapTemplate.entrySet().iterator();
                     iter.hasNext(); )
                    {
                    Map.Entry entry  = (Map.Entry) iter.next();
                    Object    oKey   = entry.getKey();
                    Object    oValue = entry.getValue();

                    if (oKey.equals(EXECUTION_UNITS))
                        {
                        // match of this element is not required
                        continue;
                        }

                    String sDescriptorValue = (String) mapCpu.get(oKey);

                    boolean fMatch =
                     sDescriptorValue != null &&
                        (oValue instanceof String &&
                                oValue.equals(sDescriptorValue)
                        || oValue instanceof Pattern &&
                                ((Pattern) oValue).matcher(sDescriptorValue).matches());
                    if (!fMatch)
                        {
                        // no match, move to the next template
                        continue nextTemplate;
                        }
                    }

                // this template is the best match so far
                if (m_fVerbose)
                    {
                    if (mapMatch == null)
                        {
                        log("Found matching template\n"
                                + toXml(TEMPLATE, mapTemplate));
                        }
                    else
                        {
                        // better match
                        log("Found better match\n"
                                + toXml(TEMPLATE, mapTemplate));
                        }
                    }

                cMatchSize = mapTemplate.size();
                mapMatch   = mapTemplate;
                }
            }

        if (mapMatch == null && m_fVerbose)
            {
            log("No matching template found");
            }
        return mapMatch;
        }

    /**
    * Count the number of execution units based on the supplied processor map.
    *
    * @param mapCpu  the processor specs to use in computing core count
    *
    * @return unit count
    */
    public int lookupExecutionUnits(Map mapCpu)
        {
        if (mapCpu == null)
            {
            return 0;
            }

        // check if descriptor was able to compute the core count
        String sUnits = m_fForceLookup ? null : (String) mapCpu.get(EXECUTION_UNITS);

        if (sUnits == null)
            {
            Map mapMatch = lookupCpu(mapCpu);
            if (mapMatch != null)
                {
                sUnits = (String) mapMatch.get(EXECUTION_UNITS);
                }
            }
        else if (m_fVerbose)
            {
            log("Using descriptor supplied execution units of " + sUnits);
            }

        // if we didn't find a matching print, fall back on 1:1 assumption
        return sUnits == null ? 1 : Integer.parseInt(sUnits);
        }


    /**
    * Remove the definition for this CPU from the dictionary.
    *
    * This is used in the event that the dictionary match and the inspected
    * CPUs do not align.  For instance when inspecting processors which
    * support both multi-core and hyper-threading.
    *
    * @param mapCpu  the CPU for which to remove the associated definition
    *
    * @return true iff a definition was removed
    */
    public boolean removeDefinition(Map mapCpu)
        {
        Map[] amapTemplate = m_amapCpuTemplate;
        Map   mapMatch     = lookupCpu(mapCpu);
        if (mapMatch != null)
            {
            // null out this ref from dictionary
            for (int i = 0, c = amapTemplate.length; i < c; ++i)
                {
                if (amapTemplate[i] == mapMatch)
                    {
                    amapTemplate[i] = null;
                    return true;
                    }
                }
            }
        return false;
        }

    /**
    * Count the number of execution units based on the supplied processor maps.
    *
    * @param amapCpu  the array of processor descriptors
    *
    * @return unit count
    */
    public int getExecutionUnitCount(Map[] amapCpu)
        {
        // assume that reported CPU count is really core count
        return amapCpu == null ? 0 : amapCpu.length;
        }


    /**
    * Log an issue related to processor identification.
    *
    * @param sMsg  the message to log
    */
    public static void log(String sMsg)
        {
        // all issues are logged at a rather high debug level as we'll always
        // fallback on assuming that the number of sockets is equal to the
        // JVMs CPU count.
        Logger.finest(sMsg);
        }

    /**
    * Log an exception related to processor identification.
    *
    * @param t  the exception
    */
    public static void log(Throwable t)
        {
        log(printStackTrace(t));
        }

    /**
    * Report info for the available processors.
    *
    * @param asArg [fingerprint]
    */
    public static void main(String[] asArg)
        {
        if (asArg.length > 0)
            {
            // output the fingerprint (and analyse) the CPUs from the
            // fingerprint in the supplied file
            try
                {
                FileInputStream in = new FileInputStream(asArg[0]);
                SimpleParser    sp = new SimpleParser();
                System.out.println(new ProcessorInfo(fromXml(sp.parseXml(in))));
                }
            catch (IOException e)
                {
                log("Error reading " + asArg[0] + ": " + e);
                }
            }
        else
            {
            // output fingerprint for this machine
            System.out.println(new ProcessorInfo());
            }
        }


    // ---- data members ----------------------------------------------------

    /**
    * Optional Customer UID.
    */
    protected String m_sUid;

    /**
    * Map containing environment variables, keys are stored in upper case.
    * The presence of this Map indicates that System.getenv is unusable.
    */
    protected Map m_mapEnv;

    /**
    * Array of Maps describing the machine's processors.
    */
    protected Map[] m_amapCpu;

    /**
    * The computed socket count.
    */
    protected int m_cSocket;

    /**
    * The computed CPU count.
    */
    protected int m_cCpu;

    /**
    * Array of known CPU fingerprints.
    */
    protected Map[] m_amapCpuTemplate;

    /**
    * If true a template lookup is forced even when the fingerprint appears
    * to contain sufficient information to avoid it.
    */
    protected boolean m_fForceLookup = Config.getBoolean("coherence.cpu.forcelookup");

    /**
    * If true include verbose logging relating to CPU analysis.
    */
    protected boolean m_fVerbose = Config.getBoolean("coherence.cpu.verbose");

    // ----- constants ------------------------------------------------------

    // general identifiers
    public static final String ENVIRONMENT     = "environment";
    public static final String DESCRIPTION     = "description";
    public static final String ARCHITECTURE    = "architecture";
    public static final String VENDOR          = "vendor";
    public static final String COHERENCE_UID   = "coherence-uid";

    // x86 identifiers
    public static final String FAMILY          = "family";
    public static final String MODEL           = "model";
    public static final String STEPPING        = "stepping";

    public static final String EXECUTION_UNITS = "execution-units";

    // Xml
    public static final String DICTIONARY_XML  = "processor-dictionary.xml";
    public static final String MACHINE         = "machine";
    public static final String DICTIONARY      = "processor-dictionary";
    public static final String DESCRIPTOR      = "processor-descriptor";
    public static final String TEMPLATE        = "processor-template";
    public static final String SIGNATURE       = "signature";
    }
