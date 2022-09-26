package com.oracle.coherence.guides.extend.loadbalancer;

import com.tangosol.net.proxy.DefaultProxyServiceLoadBalancer;
import com.tangosol.net.proxy.ProxyServiceLoad;

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
