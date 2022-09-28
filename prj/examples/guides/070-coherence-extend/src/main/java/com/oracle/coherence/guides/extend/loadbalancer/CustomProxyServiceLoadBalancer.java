/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.extend.loadbalancer;

import com.tangosol.net.proxy.DefaultProxyServiceLoadBalancer;
import com.tangosol.net.proxy.ProxyServiceLoad;

/**
 * Custom implementation of a ProxyService load-balancer.
 *
 * @author Gunnar Hillert  2022.09.26
 */
// # tag::loadbalancer[]
public class CustomProxyServiceLoadBalancer extends DefaultProxyServiceLoadBalancer {
	@Override
	public int compare(ProxyServiceLoad load1, ProxyServiceLoad load2) {

		int result = super.compare(load1, load2);
		System.out.println(String.format("Local Member Id: %s (Total # of Members: %s) - Connection Count: %s",
				super.getLocalMember().getId(),
				super.getMemberList(null).size(),
				load1.getConnectionCount()));
		return result;
	}
}
// # end::loadbalancer[]
