/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.DatagramTest.PacketTracker;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Kirk Lund  2025.07.11
 */
public class DatagramTestTest
    {

    /**
     * Verify that long values for packets are formatted correctly in the summary string.
     */
    @Test
    void shouldFormatPacketCountsWithLongValues()
        {
        PacketTracker packetTracker = new PacketTracker(null, null, null);
        long packetCount = 603_640_473_583_617L;

        // simulate sending and receiving packet counts
        packetTracker.m_nMin = 0;
        packetTracker.m_nMax = packetCount - 1;
        packetTracker.m_cPacketsRcvd = packetCount;

        String result = PacketTracker.toString(new PacketTracker[] {packetTracker});

        assertThat("Output should include packet count summary", result, containsString(packetCount + " of " + packetCount));
        }
    }
