/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.entryprocessors;

// # tag::IncrementingEntryProcessor[]
import com.oracle.coherence.guides.entryprocessors.model.Country;
import com.tangosol.util.InvocableMap;

/**
 *  @author Gunnar Hillert  2022.02.25
 */
public class IncrementingEntryProcessor implements InvocableMap.EntryProcessor<String, Country, Double> { // <1>

	@Override
	public Double process(InvocableMap.Entry<String, Country> entry) { // <2>
		Country country = entry.getValue();
		country.setPopulation(country.getPopulation() + 1); // <3>
		return country.getPopulation(); // <4>
	}
}
// # end::IncrementingEntryProcessor[]