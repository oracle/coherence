/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.JavaApplication;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * @author jk 2016.01.07
 */
public class JFRGetRecording
        implements RemoteCallable<byte[]>
    {
    public JFRGetRecording(String sFileName)
        {
        m_sFileName = sFileName;
        }

    @Override
    public byte[] call()
        {
        Logger.log("Getting JFR recording at " + m_sFileName, Logger.ALWAYS);

        byte[] bytes = null;

        try
            {
            File file = new File(m_sFileName);
            if (file.exists() && !file.isDirectory())
                {
                BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file));

                bytes = new byte[stream.available()];
                stream.read(bytes, 0, bytes.length);
                }
            }
        catch (Exception e)
            {
            CacheFactory.err("Error getting JFR recording at " + m_sFileName);
            CacheFactory.err(e);
            }

        return bytes;
        }


    public static void getRecording(JavaApplication application, String recodingFile, File localFile)
        {
        try
            {
            byte[] bytes = application.submit(new JFRGetRecording(recodingFile)).get();

            try (FileOutputStream stream = new FileOutputStream(localFile))
                {
                stream.write(bytes);
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    private String m_sFileName;
    }
