/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.examples.rest.processor;

import com.tangosol.coherence.rest.util.processor.ProcessorFactory;
import com.tangosol.examples.pof.DataGenerator;
import com.tangosol.examples.pof.Contact;
import com.tangosol.examples.pof.ContactId;
import com.tangosol.util.InvocableMap;

/**
 * Example {@link ProcessorFactory} implementation that creates a
 * contact with the given first and last name but with random
 * attributes using RandomContactCreator EP.
 * <p>
 * Called via rest by using POST
 * <pre>
 * http://localhost:8080/cache/product/FirstName___LastName/random-contact-creator()
 * </pre>
 *
 * @author tam 2015.07.14
 * @since 12.2.1
 */
public class RandomContactCreatorFactory
        implements ProcessorFactory<ContactId, Contact, Void>
    {
    // ----- ProcessorFactory methods ---------------------------------------

    @Override
    public InvocableMap.EntryProcessor<ContactId, Contact, Void> getProcessor(String[] asArgs)
        {
        return (entry) ->
            {
            ContactId key = entry.getKey();
            Contact contact = DataGenerator.generateContact();
            contact.setFirstName(key.getFirstName());
            contact.setLastName(key.getLastName());
            entry.setValue(contact);
            return null;
            };
        }
    }
