/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.examples.rest.application;

import com.tangosol.examples.rest.model.Country;
import com.tangosol.examples.rest.model.State;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.application.LifecycleEvent;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link EventInterceptor} for bootstrapping the Coherence REST Application.
 * <p>
 * Copyright (c) 2015. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author bko/tam  2015.07.22
 * @since 12.2.1
 */
public class ApplicationBootstrapInterceptor
        implements EventInterceptor<LifecycleEvent>
    {
    @Override
    public void onEvent(LifecycleEvent event)
        {
        if (event.getType() == LifecycleEvent.Type.ACTIVATED)
            {
            if (CacheFactory.getCluster().getLocalMember().getId() == 1)
                {
                // acquire the ConfigurableCacheFactory for the application and populate
                // caches on startup of member 1
                ConfigurableCacheFactory factory = event.getConfigurableCacheFactory();

                NamedCache<String, State> ncStates = factory.ensureTypedCache(
                        "states", null, TypeAssertion.withTypes(
                                String.class, State.class));
                NamedCache<String, Country> ncCountries = CacheFactory.getTypedCache(
                        "countries", null,
                        TypeAssertion.withTypes(String.class, Country.class));

                populateReferenceData(ncStates, ncCountries);
                }

            String sHost = System.getProperty("coherence.examples.rest.address");
            String sURL = sHost == null ? null :  "http://" +
                          (sHost.equals("0.0.0.0") ? "127.0.0.1" : sHost) +
                          ":" +
                          System.getProperty("coherence.examples.rest.port") +
                          "/application/index.html";

            // open the default web browser to start the front-end
            try
                {
                if (sHost != null)
                    {
                    if (Desktop.isDesktopSupported())
                        {
                        Desktop.getDesktop().browse(new URI(sURL));
                        }
                    else
                        {
                        System.out.println("Open: " + sURL + " to start the demo");
                        }
                    }
                }
            catch (Exception e)
                {
                System.out.println("Open: " + sURL + " to start the demo");
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Populate reference data.
     *
     * @param ncStates     NamedCache for states
     * @param ncCountries  NamedCache for countries
     */
    private void populateReferenceData(NamedCache<String, State> ncStates,
                                       NamedCache<String, Country> ncCountries)
        {
        CacheFactory.log("Populating Reference Data", CacheFactory.LOG_INFO);

        Map<String, State> mapStates = new HashMap<>();

        mapStates.put("",   new State("",   "Other"));
        mapStates.put("AL", new State("AL", "Alabama"));
        mapStates.put("AK", new State("AK", "Alaska"));
        mapStates.put("AS", new State("AS", "American Samoa"));
        mapStates.put("AZ", new State("AZ", "Arizona"));
        mapStates.put("AR", new State("AR", "Arkansas"));
        mapStates.put("CA", new State("CA", "California"));
        mapStates.put("CO", new State("CO", "Colorado"));
        mapStates.put("CT", new State("CT", "Connecticut"));
        mapStates.put("DE", new State("DE", "Delaware"));
        mapStates.put("DC", new State("DC", "District Of Columbia"));
        mapStates.put("FM", new State("FM", "Federated States Of Micronesia"));
        mapStates.put("FL", new State("FL", "Florida"));
        mapStates.put("GA", new State("GA", "Georgia"));
        mapStates.put("GU", new State("GU", "Guam Gu"));
        mapStates.put("HI", new State("HI", "Hawaii"));
        mapStates.put("ID", new State("ID", "Idaho"));
        mapStates.put("IL", new State("IL", "Illinois"));
        mapStates.put("IN", new State("IN", "Indiana"));
        mapStates.put("IA", new State("IA", "Iowa"));
        mapStates.put("KS", new State("KS", "Kansas"));
        mapStates.put("KY", new State("KY", "Kentucky"));
        mapStates.put("LA", new State("LA", "Louisiana"));
        mapStates.put("ME", new State("ME", "Maine"));
        mapStates.put("MH", new State("MH", "Marshall Islands"));
        mapStates.put("MD", new State("MD", "Maryland"));
        mapStates.put("MA", new State("MA", "Massachusetts"));
        mapStates.put("MI", new State("MI", "Michigan"));
        mapStates.put("MN", new State("MN", "Minnesota"));
        mapStates.put("MS", new State("MS", "Mississippi"));
        mapStates.put("MO", new State("MO", "Missouri"));
        mapStates.put("MT", new State("MT", "Montana"));
        mapStates.put("NE", new State("NE", "Nebraska"));
        mapStates.put("NV", new State("NV", "Nevada"));
        mapStates.put("NH", new State("NH", "New Hampshire"));
        mapStates.put("NJ", new State("NJ", "New Jersey"));
        mapStates.put("NM", new State("NM", "New Mexico"));
        mapStates.put("NY", new State("NY", "New York"));
        mapStates.put("NC", new State("NC", "North Carolina"));
        mapStates.put("ND", new State("ND", "North Dakota"));
        mapStates.put("MP", new State("MP", "Northern Mariana Islands"));
        mapStates.put("OH", new State("OH", "Ohio"));
        mapStates.put("OK", new State("OK", "Oklahoma"));
        mapStates.put("OR", new State("OR", "Oregon"));
        mapStates.put("PW", new State("PW", "Palau"));
        mapStates.put("PA", new State("PA", "Pennsylvania"));
        mapStates.put("PR", new State("PR", "Puerto rico"));
        mapStates.put("RI", new State("RI", "Rhode Island"));
        mapStates.put("SC", new State("SC", "South Carolina"));
        mapStates.put("SD", new State("SD", "South Dakota"));
        mapStates.put("TN", new State("TN", "Tennessee"));
        mapStates.put("TX", new State("TX", "Texas"));
        mapStates.put("UT", new State("UT", "Utah"));
        mapStates.put("VT", new State("VT", "Vermont"));
        mapStates.put("VI", new State("VI", "Virgin Islands"));
        mapStates.put("VA", new State("VA", "Virginia"));
        mapStates.put("WA", new State("WA", "Washington"));
        mapStates.put("WV", new State("WV", "West Virginia"));
        mapStates.put("WI", new State("WI", "Wisconsin"));
        mapStates.put("WY", new State("WY", "Wyoming"));

        ncStates.putAll(mapStates);

        Map<String, Country> mapCountries = new HashMap<>();

        // Refer: https://en.wikipedia.org/wiki/List_of_ISO_3166_country_codes

        mapCountries.clear();
        mapCountries.put("", new Country("", "Other"));
        mapCountries.put("AF", new Country("AF", "Afghanistan"));
        mapCountries.put("AX", new Country("AX", "Aland Islands"));
        mapCountries.put("AL", new Country("AL", "Albania"));
        mapCountries.put("DZ", new Country("DZ", "Algeria"));
        mapCountries.put("AS", new Country("AS", "American Samoa"));
        mapCountries.put("AD", new Country("AD", "Andorra"));
        mapCountries.put("AO", new Country("AO", "Angola"));
        mapCountries.put("AI", new Country("AI", "Anguilla"));
        mapCountries.put("AQ", new Country("AQ", "Antarctica"));
        mapCountries.put("AG", new Country("AG", "Antigua and Barbuda"));
        mapCountries.put("AR", new Country("AR", "Argentina"));
        mapCountries.put("AM", new Country("AM", "Armenia"));
        mapCountries.put("AW", new Country("AW", "Aruba"));
        mapCountries.put("AU", new Country("AU", "Australia"));
        mapCountries.put("AT", new Country("AT", "Austria"));
        mapCountries.put("AZ", new Country("AZ", "Azerbaijan"));
        mapCountries.put("BS", new Country("BS", "Bahamas"));
        mapCountries.put("BH", new Country("BH", "Bahrain"));
        mapCountries.put("BD", new Country("BD", "Bangladesh"));
        mapCountries.put("BB", new Country("BB", "Barbados"));
        mapCountries.put("BY", new Country("BY", "Belarus"));
        mapCountries.put("BE", new Country("BE", "Belgium"));
        mapCountries.put("BZ", new Country("BZ", "Belize"));
        mapCountries.put("BJ", new Country("BJ", "Benin"));
        mapCountries.put("BM", new Country("BM", "Bermuda"));
        mapCountries.put("BT", new Country("BT", "Bhutan"));
        mapCountries.put("BO", new Country("BO", "Bolivia"));
        mapCountries.put("BA", new Country("BA", "Bosnia and Herzegovina"));
        mapCountries.put("BW", new Country("BW", "Botswana"));
        mapCountries.put("BV", new Country("BV", "Bouvet Island"));
        mapCountries.put("BR", new Country("BR", "Brazil"));
        mapCountries.put("IO", new Country("IO", "British Indian Ocean Territory"));
        mapCountries.put("BN", new Country("BN", "Brunei Darussalam"));
        mapCountries.put("BG", new Country("BG", "Bulgaria"));
        mapCountries.put("BF", new Country("BF", "Burkina Faso"));
        mapCountries.put("BI", new Country("BI", "Burundi"));
        mapCountries.put("KH", new Country("KH", "Cambodia"));
        mapCountries.put("CM", new Country("CM", "Cameroon"));
        mapCountries.put("CA", new Country("CA", "Canada"));
        mapCountries.put("CV", new Country("CV", "Cape Verde"));
        mapCountries.put("KY", new Country("KY", "Cayman Islands"));
        mapCountries.put("CF", new Country("CF", "Central African Republic"));
        mapCountries.put("TD", new Country("TD", "Chad"));
        mapCountries.put("CL", new Country("CL", "Chile"));
        mapCountries.put("CN", new Country("CN", "China"));
        mapCountries.put("CX", new Country("CX", "Christmas Island"));
        mapCountries.put("CC", new Country("CC", "Cocos (Keeling) Islands"));
        mapCountries.put("CO", new Country("CO", "Colombia"));
        mapCountries.put("KM", new Country("KM", "Comoros"));
        mapCountries.put("CG", new Country("CG", "Congo"));
        mapCountries.put("CD", new Country("CD", "Congo, The Democratic Republic of The"));
        mapCountries.put("CK", new Country("CK", "Cook Islands"));
        mapCountries.put("CR", new Country("CR", "Costa Rica"));
        mapCountries.put("CI", new Country("CI", "Cote D'ivoire"));
        mapCountries.put("HR", new Country("HR", "Croatia"));
        mapCountries.put("CU", new Country("CU", "Cuba"));
        mapCountries.put("CY", new Country("CY", "Cyprus"));
        mapCountries.put("CZ", new Country("CZ", "Czech Republic"));
        mapCountries.put("DK", new Country("DK", "Denmark"));
        mapCountries.put("DJ", new Country("DJ", "Djibouti"));
        mapCountries.put("DM", new Country("DM", "Dominica"));
        mapCountries.put("DO", new Country("DO", "Dominican Republic"));
        mapCountries.put("EC", new Country("EC", "Ecuador"));
        mapCountries.put("EG", new Country("EG", "Egypt"));
        mapCountries.put("SV", new Country("SV", "El Salvador"));
        mapCountries.put("GQ", new Country("GQ", "Equatorial Guinea"));
        mapCountries.put("ER", new Country("ER", "Eritrea"));
        mapCountries.put("EE", new Country("EE", "Estonia"));
        mapCountries.put("ET", new Country("ET", "Ethiopia"));
        mapCountries.put("FK", new Country("FK", "Falkland Islands (Malvinas)"));
        mapCountries.put("FO", new Country("FO", "Faroe Islands"));
        mapCountries.put("FJ", new Country("FJ", "Fiji"));
        mapCountries.put("FI", new Country("FI", "Finland"));
        mapCountries.put("FR", new Country("FR", "France"));
        mapCountries.put("GF", new Country("GF", "French Guiana"));
        mapCountries.put("PF", new Country("PF", "French Polynesia"));
        mapCountries.put("TF", new Country("TF", "French Southern Territories"));
        mapCountries.put("GA", new Country("GA", "Gabon"));
        mapCountries.put("GM", new Country("GM", "Gambia"));
        mapCountries.put("GE", new Country("GE", "Georgia"));
        mapCountries.put("DE", new Country("DE", "Germany"));
        mapCountries.put("GH", new Country("GH", "Ghana"));
        mapCountries.put("GI", new Country("GI", "Gibraltar"));
        mapCountries.put("GR", new Country("GR", "Greece"));
        mapCountries.put("GL", new Country("GL", "Greenland"));
        mapCountries.put("GD", new Country("GD", "Grenada"));
        mapCountries.put("GP", new Country("GP", "Guadeloupe"));
        mapCountries.put("GU", new Country("GU", "Guam"));
        mapCountries.put("GT", new Country("GT", "Guatemala"));
        mapCountries.put("GG", new Country("GG", "Guernsey"));
        mapCountries.put("GN", new Country("GN", "Guinea"));
        mapCountries.put("GW", new Country("GW", "Guinea-bissau"));
        mapCountries.put("GY", new Country("GY", "Guyana"));
        mapCountries.put("HT", new Country("HT", "Haiti"));
        mapCountries.put("HM", new Country("HM", "Heard Island and Mcdonald Islands"));
        mapCountries.put("VA", new Country("VA", "Holy See (Vatican City State)"));
        mapCountries.put("HN", new Country("HN", "Honduras"));
        mapCountries.put("HK", new Country("HK", "Hong Kong"));
        mapCountries.put("HU", new Country("HU", "Hungary"));
        mapCountries.put("IS", new Country("IS", "Iceland"));
        mapCountries.put("IN", new Country("IN", "India"));
        mapCountries.put("ID", new Country("ID", "Indonesia"));
        mapCountries.put("IR", new Country("IR", "Iran, Islamic Republic of"));
        mapCountries.put("IQ", new Country("IQ", "Iraq"));
        mapCountries.put("IE", new Country("IE", "Ireland"));
        mapCountries.put("IM", new Country("IM", "Isle of Man"));
        mapCountries.put("IL", new Country("IL", "Israel"));
        mapCountries.put("IT", new Country("IT", "Italy"));
        mapCountries.put("JM", new Country("JM", "Jamaica"));
        mapCountries.put("JP", new Country("JP", "Japan"));
        mapCountries.put("JE", new Country("JE", "Jersey"));
        mapCountries.put("JO", new Country("JO", "Jordan"));
        mapCountries.put("KZ", new Country("KZ", "Kazakhstan"));
        mapCountries.put("KE", new Country("KE", "Kenya"));
        mapCountries.put("KI", new Country("KI", "Kiribati"));
        mapCountries.put("KP", new Country("KP", "Korea, Democratic People's Republic of"));
        mapCountries.put("KR", new Country("KR", "Korea, Republic of"));
        mapCountries.put("KW", new Country("KW", "Kuwait"));
        mapCountries.put("KG", new Country("KG", "Kyrgyzstan"));
        mapCountries.put("LA", new Country("LA", "Lao People's Democratic Republic"));
        mapCountries.put("LV", new Country("LV", "Latvia"));
        mapCountries.put("LB", new Country("LB", "Lebanon"));
        mapCountries.put("LS", new Country("LS", "Lesotho"));
        mapCountries.put("LR", new Country("LR", "Liberia"));
        mapCountries.put("LY", new Country("LY", "Libyan Arab Jamahiriya"));
        mapCountries.put("LI", new Country("LI", "Liechtenstein"));
        mapCountries.put("LT", new Country("LT", "Lithuania"));
        mapCountries.put("LU", new Country("LU", "Luxembourg"));
        mapCountries.put("MO", new Country("MO", "Macao"));
        mapCountries.put("MK", new Country("MK", "Macedonia, The Former Yugoslav Republic of"));
        mapCountries.put("MG", new Country("MG", "Madagascar"));
        mapCountries.put("MW", new Country("MW", "Malawi"));
        mapCountries.put("MY", new Country("MY", "Malaysia"));
        mapCountries.put("MV", new Country("MV", "Maldives"));
        mapCountries.put("ML", new Country("ML", "Mali"));
        mapCountries.put("MT", new Country("MT", "Malta"));
        mapCountries.put("MH", new Country("MH", "Marshall Islands"));
        mapCountries.put("MQ", new Country("MQ", "Martinique"));
        mapCountries.put("MR", new Country("MR", "Mauritania"));
        mapCountries.put("MU", new Country("MU", "Mauritius"));
        mapCountries.put("YT", new Country("YT", "Mayotte"));
        mapCountries.put("MX", new Country("MX", "Mexico"));
        mapCountries.put("FM", new Country("FM", "Micronesia, Federated States of"));
        mapCountries.put("MD", new Country("MD", "Moldova, Republic of"));
        mapCountries.put("MC", new Country("MC", "Monaco"));
        mapCountries.put("MN", new Country("MN", "Mongolia"));
        mapCountries.put("ME", new Country("ME", "Montenegro"));
        mapCountries.put("MS", new Country("MS", "Montserrat"));
        mapCountries.put("MA", new Country("MA", "Morocco"));
        mapCountries.put("MZ", new Country("MZ", "Mozambique"));
        mapCountries.put("MM", new Country("MM", "Myanmar"));
        mapCountries.put("NA", new Country("NA", "Namibia"));
        mapCountries.put("NR", new Country("NR", "Nauru"));
        mapCountries.put("NP", new Country("NP", "Nepal"));
        mapCountries.put("NL", new Country("NL", "Netherlands"));
        mapCountries.put("AN", new Country("AN", "Netherlands Antilles"));
        mapCountries.put("NC", new Country("NC", "New Caledonia"));
        mapCountries.put("NZ", new Country("NZ", "New Zealand"));
        mapCountries.put("NI", new Country("NI", "Nicaragua"));
        mapCountries.put("NE", new Country("NE", "Niger"));
        mapCountries.put("NG", new Country("NG", "Nigeria"));
        mapCountries.put("NU", new Country("NU", "Niue"));
        mapCountries.put("NF", new Country("NF", "Norfolk Island"));
        mapCountries.put("MP", new Country("MP", "Northern Mariana Islands"));
        mapCountries.put("NO", new Country("NO", "Norway"));
        mapCountries.put("OM", new Country("OM", "Oman"));
        mapCountries.put("PK", new Country("PK", "Pakistan"));
        mapCountries.put("PW", new Country("PW", "Palau"));
        mapCountries.put("PS", new Country("PS", "Palestinian, State of"));
        mapCountries.put("PA", new Country("PA", "Panama"));
        mapCountries.put("PG", new Country("PG", "Papua New Guinea"));
        mapCountries.put("PY", new Country("PY", "Paraguay"));
        mapCountries.put("PE", new Country("PE", "Peru"));
        mapCountries.put("PH", new Country("PH", "Philippines"));
        mapCountries.put("PN", new Country("PN", "Pitcairn"));
        mapCountries.put("PL", new Country("PL", "Poland"));
        mapCountries.put("PT", new Country("PT", "Portugal"));
        mapCountries.put("PR", new Country("PR", "Puerto Rico"));
        mapCountries.put("QA", new Country("QA", "Qatar"));
        mapCountries.put("RE", new Country("RE", "Reunion"));
        mapCountries.put("RO", new Country("RO", "Romania"));
        mapCountries.put("RU", new Country("RU", "Russian Federation"));
        mapCountries.put("RW", new Country("RW", "Rwanda"));
        mapCountries.put("SH", new Country("SH", "Saint Helena"));
        mapCountries.put("KN", new Country("KN", "Saint Kitts and Nevis"));
        mapCountries.put("LC", new Country("LC", "Saint Lucia"));
        mapCountries.put("PM", new Country("PM", "Saint Pierre and Miquelon"));
        mapCountries.put("VC", new Country("VC", "Saint Vincent and The Grenadines"));
        mapCountries.put("WS", new Country("WS", "Samoa"));
        mapCountries.put("SM", new Country("SM", "San Marino"));
        mapCountries.put("ST", new Country("ST", "Sao Tome and Principe"));
        mapCountries.put("SA", new Country("SA", "Saudi Arabia"));
        mapCountries.put("SN", new Country("SN", "Senegal"));
        mapCountries.put("RS", new Country("RS", "Serbia"));
        mapCountries.put("SC", new Country("SC", "Seychelles"));
        mapCountries.put("SL", new Country("SL", "Sierra Leone"));
        mapCountries.put("SG", new Country("SG", "Singapore"));
        mapCountries.put("SK", new Country("SK", "Slovakia"));
        mapCountries.put("SI", new Country("SI", "Slovenia"));
        mapCountries.put("SB", new Country("SB", "Solomon Islands"));
        mapCountries.put("SO", new Country("SO", "Somalia"));
        mapCountries.put("ZA", new Country("ZA", "South Africa"));
        mapCountries.put("GS", new Country("GS", "South Georgia and The South Sandwich Islands"));
        mapCountries.put("ES", new Country("ES", "Spain"));
        mapCountries.put("LK", new Country("LK", "Sri Lanka"));
        mapCountries.put("SD", new Country("SD", "Sudan"));
        mapCountries.put("SR", new Country("SR", "Suriname"));
        mapCountries.put("SJ", new Country("SJ", "Svalbard and Jan Mayen"));
        mapCountries.put("SZ", new Country("SZ", "Swaziland"));
        mapCountries.put("SE", new Country("SE", "Sweden"));
        mapCountries.put("CH", new Country("CH", "Switzerland"));
        mapCountries.put("SY", new Country("SY", "Syrian Arab Republic"));
        mapCountries.put("TW", new Country("TW", "Taiwan, Province of China"));
        mapCountries.put("TJ", new Country("TJ", "Tajikistan"));
        mapCountries.put("TZ", new Country("TZ", "Tanzania, United Republic of"));
        mapCountries.put("TH", new Country("TH", "Thailand"));
        mapCountries.put("TL", new Country("TL", "Timor-leste"));
        mapCountries.put("TG", new Country("TG", "Togo"));
        mapCountries.put("TK", new Country("TK", "Tokelau"));
        mapCountries.put("TO", new Country("TO", "Tonga"));
        mapCountries.put("TT", new Country("TT", "Trinidad and Tobago"));
        mapCountries.put("TN", new Country("TN", "Tunisia"));
        mapCountries.put("TR", new Country("TR", "Turkey"));
        mapCountries.put("TM", new Country("TM", "Turkmenistan"));
        mapCountries.put("TC", new Country("TC", "Turks and Caicos Islands"));
        mapCountries.put("TV", new Country("TV", "Tuvalu"));
        mapCountries.put("UG", new Country("UG", "Uganda"));
        mapCountries.put("UA", new Country("UA", "Ukraine"));
        mapCountries.put("AE", new Country("AE", "United Arab Emirates"));
        mapCountries.put("GB", new Country("GB", "United Kingdom"));
        mapCountries.put("US", new Country("US", "United States"));
        mapCountries.put("UM", new Country("UM", "United States Minor Outlying Islands"));
        mapCountries.put("UY", new Country("UY", "Uruguay"));
        mapCountries.put("UZ", new Country("UZ", "Uzbekistan"));
        mapCountries.put("VU", new Country("VU", "Vanuatu"));
        mapCountries.put("VE", new Country("VE", "Venezuela"));
        mapCountries.put("VN", new Country("VN", "Viet Nam"));
        mapCountries.put("VG", new Country("VG", "Virgin Islands, British"));
        mapCountries.put("VI", new Country("VI", "Virgin Islands, U.S."));
        mapCountries.put("WF", new Country("WF", "Wallis and Futuna"));
        mapCountries.put("EH", new Country("EH", "Western Sahara"));
        mapCountries.put("YE", new Country("YE", "Yemen"));
        mapCountries.put("ZM", new Country("ZM", "Zambia"));
        mapCountries.put("ZW", new Country("ZW", "Zimbabwe"));

        ncCountries.putAll(mapCountries);

        CacheFactory.log("Completed", CacheFactory.LOG_INFO);
        }
    }
