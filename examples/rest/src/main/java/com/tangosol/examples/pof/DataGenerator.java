/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.examples.pof;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import com.tangosol.util.Base;

/**
 * DataGenerator generates random Contact information and store the result in a
 * CSV file. The data can may then be loaded with the LoaderExample.
 *
 * @author dag  2009.02.19
 */
public class DataGenerator
    {
    // ----- static methods -------------------------------------------------

    /**
     * Generate the contacts and write them to a file.
     *
     * @param out        output stream for contacts
     * @param cContacts  number of contacts to create
     *
     * @throws IOException if file cannot be written
     */
    public static void generate(OutputStream out, int cContacts)
            throws IOException
        {
        PrintWriter writer = new PrintWriter(new BufferedWriter(
                new OutputStreamWriter(out)));

        for (int i = 0; i < cContacts; ++i)
            {
            StringBuffer sb = new StringBuffer(256);

            //contact person
            sb.append("John,")
              .append(getRandomName())
              .append(',');

            // random birth date in millis before or after the epoch

            sb.append(getRandomDate().format(FORMAT))
              .append(',');

            // home and work addresses
            sb.append(Integer.toString(Base.getRandom().nextInt(999)))
              .append(" Beacon St.,,") /*street1,empty street2*/
              .append(getRandomName()) /*random city name*/
              .append(',')
              .append(getRandomState())
              .append(',')
              .append(getRandomZip())
              .append(",US,Yoyodyne Propulsion Systems,")
              .append("330 Lectroid Rd.,Grover's Mill,")
              .append(getRandomState())
              .append(',')
              .append(getRandomZip())
              .append(",US,");

            // home and work phone numbers
            sb.append("home,")
              .append(Base.toDelimitedString(getRandomPhoneDigits(), ","))
              .append(",work,")
              .append(Base.toDelimitedString(getRandomPhoneDigits(), ","))
              .append(',');

            writer.println(sb);
            }
        writer.flush();
        }

    /**
     * Generate N contacts and return them in a Map.
     *
     * @param nCount  the number of contacts to generate
     *
     * @return a Map of generated contacts
     */
    public static Map<ContactId, Contact> generateContacts(int nCount)
        {
        Map<ContactId, Contact> mapContacts = new HashMap<>();

        for (int i = 0; i < nCount ; i++)
            {
            Contact contact = generateContact();
            mapContacts.put(new ContactId(contact.getFirstName(), contact.getLastName()), contact);
            }

        return mapContacts;
        }

    /**
     * Return a random name.
     *
     * @return a random name
     */
    private static String getRandomName()
        {
        Random rand = Base.getRandom();
        int    cCh  = 4 + rand.nextInt(7);
        char[] ach  = new char[cCh];

        ach[0] = (char) ('A' + rand.nextInt(26));
        for (int of = 1; of < cCh; ++of)
            {
            ach[of] = (char) ('a' + rand.nextInt(26));
            }
        return new String(ach);
        }

    /**
     * Return a random phone number.
     * <p/>
     * The phone number contains including access, country, area code, and local
     * number.
     *
     * @return a random phone number
     */
    private static int[] getRandomPhoneDigits()
        {
        Random rand = Base.getRandom();
        return new int[] {
            11,                   // access code
            rand.nextInt(99),     // country code
            rand.nextInt(999),    // area code
            rand.nextInt(9999999) // local number
            };
        }

    /**
     * Return a random Phone.
     *
     * @return a random phone
     */
    private static PhoneNumber getRandomPhone()
        {
        int[] anPhone = getRandomPhoneDigits();

        return new PhoneNumber((short)anPhone[0], (short)anPhone[1],
                (short)anPhone[2], anPhone[3]);

        }
    /**
     * Return a random Zip code.
     *
     * @return a random Zip code
     */
    private static String getRandomZip()
        {
        return Base.toDecString(Base.getRandom().nextInt(99999), 5);
        }

    /**
     * Return a random state.
     *
     * @return a random state
     */
    private static String getRandomState()
        {
        return STATE_CODES[Base.getRandom().nextInt(STATE_CODES.length)];
        }

    /**
     * Return a random date before or after the epoch.
     *
     * @return a random date before or after the epoch
     */
    private static LocalDate getRandomDate()
        {
        return LocalDate.ofEpochDay(RANDOM.nextInt(365 * 40) - (365 * 20) * 1L);
        }

    /**
     * Generate a Contact with random information.
     *
     * @return a Contact with random information
     */
    public static Contact generateContact()
        {
        return new Contact("John",
                getRandomName(),
                generateHomeAddress(),
                generateWorkAddress(),
                Collections.singletonMap("work", getRandomPhone()),
                getRandomDate()
          );
        }

    public static Address generateHomeAddress()
        {
        return new Address("1500 Boylston St.", null, getRandomName(),
                            getRandomState(), getRandomZip(), "US");
        }

    public static Address generateWorkAddress()
        {
        return new Address("8 Yawkey Way", null, getRandomName(),
                            getRandomState(), getRandomZip(), "US");
        }

    // ----- constants ------------------------------------------------------

    /**
     * US Postal Service two letter postal codes.
     */
    private static final String[] STATE_CODES = {
            "AL", "AK", "AS", "AZ", "AR", "CA", "CO", "CT", "DE", "OF", "DC",
            "FM", "FL", "GA", "GU", "HI", "ID", "IL", "IN", "IA", "KS", "KY",
            "LA", "ME", "MH", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE",
            "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "MP", "OH", "OK", "OR",
            "PW", "PA", "PR", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VI",
            "VA", "WA", "WV", "WI", "WY"
    };

    /**
     * Used for generating random dates.
     */
    private static Random RANDOM = new Random();

    /**
     * Date formatter.
     */
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }