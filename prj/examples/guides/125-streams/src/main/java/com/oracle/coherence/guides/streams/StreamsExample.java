/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.streams;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.stream.RemoteCollectors;
import com.tangosol.util.stream.RemoteStream;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.tangosol.util.Filters.equal;
import static com.tangosol.util.Filters.greater;

/**
 * Simple aggregation examples.
 *
 * @author Tim Middleton  2022.02.16
 */
public class StreamsExample {

    private static final int OFFICE = 0;
    private static final int POSTAL = 1;

    /**
     * Random instance with predictable seed.
     */
    protected static final Random RANDOM = new Random(12312383838L);

    private static final String[] STATES = new String[] {"AL", "AK", "CA", "MA", "HI", "NY", "TX", "WA", "TN"};

    private static final String[] STREETS = new String[] {
            "Main Street", "5th Avenue", "Lowell Street", "Lock Street",
            "Concord Street", "Fletcher Street", "North Beach Road", "Grand Blvd",
            "Franklin Street", "Pearson Ave", "Park Street", "Temple Street"
    };

    private static final String[] CITIES = new String[] {
            "Hudson", "Litchfield", "Derry", "Lowell", "Boston",
            "Belmont", "Dunstable", "Cambridge", "Waterford", "Braintree",
            "Watertown", "Dracut"
    };

    private static final String[] ZIPCODES = new String[] {
            "02127", "02127", "01844", "01841", "02703",
            "02740", "01915", "01453", "01752", "02138",
            "02184", "02472", "01826", "02324", "01880"
    };

    public static final String STORAGE_ENABLED = "coherence.distributed.localstorage";

    /**
     * Customers {@link NamedMap}.
     */
    private final NamedMap<Integer, Contact> contacts;

    /**
     * Entry point to run from IDE.
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        System.setProperty(STORAGE_ENABLED, "true");

        StreamsExample example = new StreamsExample();
        example.populate();
        example.runExample();
    }

    /**
     * Constructor.
     */
    public StreamsExample() {
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                                                           .build();
        Coherence coherence = Coherence.clusterMember(cfg);
        coherence.start().join();
        Session session = coherence.getSession();
        this.contacts = session.getMap("contacts");
    }

    /**
     * Populate the map with data.
     */
    public void populate() {
        NamedMap<Integer, Contact> contacts = getContacts();
        contacts.clear();
        contacts.addIndex(ValueExtractor.of(Contact::getWorkAddress).andThen(Address::getState), false, null);

        addContacts();
    }

    // tag::runExample[]
    /**
     * Run the example.
     */
    public void runExample() {
        NamedMap<Integer, Contact> contacts = getContacts();
        
        System.out.println("Cache size is " + contacts.size());

        // get the distinct years that the contacts were born in
        Set<Integer> setYears = contacts.stream(Contact::getDoB)
                                        .map(LocalDate::getYear)
                                        .distinct()
                                        .collect(RemoteCollectors.toSet());
        System.out.println("Distinct years the contacts were born in:\n" + setYears);

        // get a set of contact names where the age is > 40
        Set<String> setNames = contacts.stream(greater(Contact::getAge, 60))
                                       .map(entry->entry.extract(Contact::getLastName) + " " +
                                                   entry.extract(Contact::getFirstName) + " age=" +
                                                   entry.extract(Contact::getAge))
                                       .collect(RemoteCollectors.toSet());
        System.out.println("\nSet of contact names where age > 60:\n" + setNames);

        // get the distinct set of states for home addresses
        Set<String> setStates = contacts.stream(Contact::getHomeAddress)
                                        .map(Address::getState)
                                        .distinct()
                                        .collect(RemoteCollectors.toSet());
        System.out.println("\nDistinct set of states for home addresses:\n" + setStates);

        // get the average ages of all contacts
        double avgAge = contacts.stream(Contact::getAge)
                                .mapToInt(Number::intValue)
                                .average()
                                .orElse(0);  // in-case of no values
        System.out.println("\nThe average age of all contacts is: " + avgAge);

        // get average age using collectors
        avgAge = contacts.stream()
                         .collect(RemoteCollectors.averagingInt(Contact::getAge));
        System.out.println("\nThe average age of all contacts using collect() is: " + avgAge);

        // get the maximum age of all contacts
        int maxAge = contacts.stream(Contact::getAge)
                             .mapToInt(Number::intValue)
                             .max()
                             .orElse(0);  // in-case of no values
        System.out.println("\nThe maximum age of all contacts is: " + maxAge);

        // get average age of contacts who live in MA
        // Note: The filter should be applied as early as possible, e.g as an argument
        // to the stream() call in order to take advantage of indexes
        avgAge = RemoteStream.toIntStream(contacts.stream(equal(homeState(), "MA"), Contact::getAge))
                             .average()
                             .orElse(0);
        System.out.println("\nThe average age of contacts who work in MA is: " + avgAge);

        // get a map of birth months and the contact names for that month
        Map<String, List<Contact>> mapContacts =
                contacts.stream()
                        .map(Map.Entry::getValue)
                        .collect(RemoteCollectors.groupingBy(birthMonth()));
        System.out.println("\nContacts born in each month:");
        mapContacts.forEach(
                (key, value)->System.out.println("\nMonth: " + key + ", Contacts:\n" +
                                                 displayNames(value)));

        // get a map of states and the contacts living in each state
        Map<String, List<Contact>> mapStateContacts =
                contacts.stream()
                        .map(Map.Entry::getValue)
                        .collect(RemoteCollectors.groupingBy(homeState()));
        System.out.println("\nContacts with home addresses in each state:");
        mapStateContacts.forEach(
                (key, value)->System.out.println("State " + key + " has " + value.size() +
                                                 " Contacts\n" + displayNames(value)));
    }
    // end::runExample[]

    // tag::extractors[]
    /**
     * A {@link ValueExtractor} to extract the birth month from a {@link Contact}.
     *
     * @return the birth month
     */
    protected static ValueExtractor<Contact, String> birthMonth() {
        return contact->contact.getDoB().getMonth().toString();
    }

     /**
     * A {@link ValueExtractor} to extract the home state from a {@link Contact}.
     *
     * @return the home state
     */
    protected static ValueExtractor<Contact, String> homeState() {
        return contact->contact.getHomeAddress().getState();
    }
    // end::extractors[]

    /**
     * Display a list of names from the supplied contacts.
     *
     * @param listContacts the List of contacts to display names from
     */
    private String displayNames(List<Contact> listContacts) {
        StringBuilder sb = new StringBuilder();
        for (Contact contact : listContacts) {
            sb.append("    ")
              .append(contact.getFirstName())
              .append(" ")
              .append(contact.getLastName())
              .append("\n");
        }

        return sb.toString();
    }

    /**
     * Add the specified number of contacts.
     */
    protected void addContacts() {
        final int             COUNT = 100;
        Map<Integer, Contact> map   = new HashMap<>();
        System.out.println("Creating " + COUNT + " contacts");

        for (int i = 1; i <= COUNT; i++) {
            map.put(i, new Contact(i, "Firstname" + i, "Lastname" + i, getRandomDate(),
                    getRandomAddress(OFFICE), getRandomAddress(POSTAL)));
        }

        contacts.putAll(map);
    }

    /**
     * Return a random date before or after the epoch.
     *
     * @return a random date before or after the epoch
     */
    private static LocalDate getRandomDate() {
        return LocalDate.ofEpochDay(RANDOM.nextInt(365 * 40) - (long) (365 * 20));
    }

    /**
     * Return a random address in USA.
     *
     * @param type either POSTAL or OFFICE
     *
     * @return a new {@link Address}
     */
    protected Address getRandomAddress(int type) {
        String addressLine1;
        String addressLine2 = null;
        if (type == POSTAL) {
            addressLine1 = ("PO Box " + (RANDOM.nextInt(200) + 1));
        }
        else {
            addressLine1 = (RANDOM.nextInt(400) + 1) + " " + getRandomValue(STREETS);
            if (RANDOM.nextInt(100) >= 90) {
                // 10% of the time add an address line 1
                addressLine2 = addressLine1;
                addressLine1 = "Level " + RANDOM.nextInt(10) + 1;
            }
        }

        return new Address(addressLine1, addressLine2, getRandomValue(CITIES),
                getRandomValue(STATES), getRandomValue(ZIPCODES));
    }

    /**
     * Return a random value from an array.
     *
     * @param array array to return from
     *
     * @return a random value
     */
    protected String getRandomValue(String[] array) {
        return array[RANDOM.nextInt(array.length)];
    }

    /**
     * Returns the contacts {@link NamedMap}.
     *
     * @return the contacts {@link NamedMap}
     */
    private NamedMap<Integer, Contact> getContacts() {
        return contacts;
    }
}
