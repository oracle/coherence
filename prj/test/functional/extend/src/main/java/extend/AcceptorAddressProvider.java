/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.tangosol.net.CompositeAddressProvider;

import java.net.InetSocketAddress;


/**
* The AcceptorAddressProvider is used by the AddressProviderTest to validate
* that the TcpAcceptor can start with an address provider.
*
* @author nsa  20009.09.25
*/
public class AcceptorAddressProvider
        extends CompositeAddressProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new AcceptorAddressProvider.
    */
    public AcceptorAddressProvider()
        {
        super();
        addAddress(new InetSocketAddress("non.existant.host.name.neverever", 32000));
        addAddress(new InetSocketAddress("127.0.0.1", 32000));
        }
    }
