/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package rwbm;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestHelper;

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.ReadWriteBackingMap;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.tangosol.net.cache.ReadWriteBackingMap.PROP_WB_REMOVE_DEFAULT;
import static org.junit.Assert.assertFalse;

/**
* A test for {@link ReadWriteBackingMap} setting
* coherence.rwbm.writebehind.remove system property to override the
* cache configure setting.
*
* @author lh  2023.04.04
*/
public class RWBMRemoveOverrideSysPropTest
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public RWBMRemoveOverrideSysPropTest()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");
        System.setProperty(PROP_WB_REMOVE_DEFAULT, "true");

        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testConfigWbRemoveSysOverride()
        {
        NamedCache cache = getNamedCache("dist-rwbm-wb-remove-false");
        try
            {
            ReadWriteBackingMap rwbm = (ReadWriteBackingMap) TestHelper.getBackingMap(cache);
            assertFalse(rwbm.isWriteBehindRemove());
            }
        finally
            {
            cache.destroy();
            }
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "rwbm-cache-config.xml";
    }
