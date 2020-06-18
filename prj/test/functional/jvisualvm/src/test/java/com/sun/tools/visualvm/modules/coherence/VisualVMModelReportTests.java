/*
 * Copyright (c) 2000, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.sun.tools.visualvm.modules.coherence;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Run tests on VisualVMModel to test loading of reports.
 *
 * @author tam  2013.12.09
 * @since  12.1.3
 */
public class VisualVMModelReportTests
    {

    @Test
    public void testVisualVMMode()
        {
        VisualVMModel model = VisualVMModel.getInstance();

        Map<Class,String> mapReportXML = model.getReportXMLMap();
        Assert.assertTrue("mapReportXML must not be null", mapReportXML != null);

        Assert.assertTrue("mapReportXML size should be 18 but is " + mapReportXML.size(), mapReportXML.size() == 18);
        }
    }
