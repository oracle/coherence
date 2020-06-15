/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;


/**
* The SWITCH op implements the general multi-case switch.  It assembles to
* either a TABLESWITCH or a LOOKUPSWITCH depending on which is more
* "efficient".  Since a TABLESWITCH is always more efficient for performance,
* it is always selected unless the amount of additional code generated for
* the TABLESWITCH compared to the LOOKUPSWITCH is considered to be too
* expensive in terms of size.
* <p><code><pre>
* JASM op         :  SWITCH        (0xfc)
* JVM byte code(s):  TABLESWITCH   (0xaa)
*                    LOOKUPSWITCH  (0xab)
* Details         :
* </pre></code>
*
* @version 0.50, 06/16/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class Switch extends OpSwitch implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the op.
    *
    * @param label  the default label to branch to if no cases match
    */
    public Switch(Label label)
        {
        super(SWITCH, label);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine which op should be used to implement the switch.
    *
    * @return  one of TABLESWITCH, LOOKUPSWITCH
    */
    protected int getSwitch()
        {
        int iOp = super.getSwitch();
        if (iOp == SWITCH)
            {
            // based on the case ops, determine which is better:
            // a TABLESWITCH or a LOOKUPSWITCH
            Case[] acase = getCases();
            int cCases = acase.length;

            if (cCases <= 1)
                {
                // with zero or one case, a LOOKUPSWITCH is as or more efficient
                // space-wise and theoretically just as efficient time-wise
                iOp = LOOKUPSWITCH;
                }
            else
                {
                int iLow  = acase[0].getCase();
                int iHigh = acase[cCases - 1].getCase();

                double dblRange  = (double) iHigh - (double) iLow + 1.0;
                double dblSpread = dblRange / cCases;

                // 2.0 is break-even for space
                // 4.0 is leaning toward speed at the expense of space
                iOp = (dblSpread > 4.0 ? LOOKUPSWITCH : TABLESWITCH);
                }
            }

        return iOp;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "Switch";
    }
