/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor;

import com.tangosol.coherence.rest.providers.JacksonMapperProvider;

import com.tangosol.coherence.rest.util.JsonMap;
import com.tangosol.coherence.rest.util.StaticContent;

import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.AbstractTestInfrastructure;

import com.oracle.coherence.testing.util.SSLSocketProviderBuilderHelper;

import data.pof.Address;
import data.pof.Person;
import data.pof.PortablePerson;
import data.pof.VersionablePortablePerson;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.conn.ssl.NoopHostnameVerifier;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import org.glassfish.jersey.jackson.JacksonFeature;

import org.glassfish.jersey.logging.LoggingFeature;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rest.data.Persona;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A collection of functional tests for Coherence*Extend-REST.
 *
 * @author vp 2011.06.30
 * @author jh 2011.07.08
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractRestTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     *
     * @param sPath  the configuration resource name or file path
     */
    public AbstractRestTests(String sPath)
        {
        super(sPath);
        }

    /**
     * Initialize the test class.
     * <p>
     * This method starts the Coherence cluster, if it isn't already running.
     */
    @BeforeClass
    public static void _startup()
        {
        AbstractTestInfrastructure.setupProps();
        System.setProperty("test.extend.port", "0");

        startCluster();
        }

    @BeforeClass
    public static void setupSSL()
        {
        SSLSocketProviderDefaultDependencies deps = SSLSocketProviderBuilderHelper.loadDependencies("ssl-config-client.xml");
        s_sslProviderClient = new SSLSocketProvider(deps);
        }

    @BeforeClass
    public static void setupJsonData()
            throws Exception
        {
        ObjectMapper mapper   = new ObjectMapper();
        s_person1 = mapper.writeValueAsString(new Persona("Ivan", 33));
        String       sPerson2 = mapper.writeValueAsString(new Persona("Aleks", 37));
        String       sPerson3 = mapper.writeValueAsString(new Persona("Vaso", 37));
        s_jsonMap1 = mapper.readValue(s_person1, JsonMap.class);
        s_jsonMap2 = mapper.readValue(sPerson2, JsonMap.class);
        s_jsonMap3 = mapper.readValue(sPerson3, JsonMap.class);
        }

    @Before
    public void setupCache()
        {
        NamedCache cache = getNamedCache("dist-test1");
        cache.clear();
        cache.put(1, m_person    = PortablePerson.create());
        cache.put(2, m_verPerson = VersionablePortablePerson.create());

        cache = getNamedCache("dist-test-map");
        cache.clear();
        m_map = new HashMap<>();
        m_map.put(1, "one");
        m_map.put(2, "two");
        m_map.put(3, "three");
        cache.put(1, m_map);

        cache = getNamedCache("dist-test-collection");
        cache.clear();
        cache.put(1, m_collection = new ArrayList(m_map.values()));

        cache = getNamedCache("dist-test-proc");
        cache.clear();
        cache.put(1, new Persona("Peter", 25));
        cache.put(2, new Persona("Mary", 23));

        cache = getNamedCache("dist-test-named-query");
        cache.clear();
        cache.put(1, new Persona("Ivan", 33));
        cache.put(2, new Persona("Aleks", 37));
        cache.put(3, new Persona("Vaso", 37));

        cache = getNamedCache("dist-test2");
        cache.clear();
        cache.put(1.0, m_person    = PortablePerson.create());
        cache.put(2.0, m_verPerson = VersionablePortablePerson.create());

        cache = getNamedCache("dist-binary-named-query");
        cache.clear();
        cache.put("1", s_jsonMap1);
        cache.put("2", s_jsonMap2);
        cache.put("3", s_jsonMap3);
        }


    // ----- test methods ---------------------------------------------------

    @Test
    public void testGet()
            throws JSONException
        {
        WebTarget webTarget = getWebTarget("dist-test1/1");
        testGetInternal(webTarget);

        webTarget = getWebTarget("dist-test2/1.0");
        testGetInternal(webTarget);
        }

    @Test
    public void testGetEntriesJson()
        {
        WebTarget webTarget = getWebTarget("dist-test1/entries.json");
        Response  response  = webTarget.request().get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        try
            {
            Map<Integer, JSONObject> people = new HashMap<>();
            JSONArray jsonArray   = new JSONArray(response.readEntity(String.class));
            for (int i = 0; i < jsonArray.length(); i++)
                {
                JSONObject o = jsonArray.getJSONObject(i);
                people.put(o.getInt("key"), o.getJSONObject("value"));
                }
            Assert.assertEquals(m_person.getName(), people.get(1).getString("name"));
            Assert.assertEquals(m_verPerson.getName(), people.get(2).getString("name"));
            assertTrue(people.get(1).has("spouse"));
            }
        catch (JSONException e)
            {
            fail("Unable to parse response");
            }
        }

    @Test
    public void testGetEntriesPartialJson()
        {
        WebTarget webTarget = getWebTarget("dist-test1/entries.json;p=name,age");
        Response  response  = webTarget.request().get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        try
            {
            Map<Integer, JSONObject> people = new HashMap<>();
            JSONArray jsonArray   = new JSONArray(response.readEntity(String.class));
            for (int i = 0; i < jsonArray.length(); i++)
                {
                JSONObject o = jsonArray.getJSONObject(i);
                people.put(o.getInt("key"), o.getJSONObject("value"));
                }
            Assert.assertEquals(m_person.getName(), people.get(1).getString("name"));
            Assert.assertEquals(m_verPerson.getName(), people.get(2).getString("name"));
            assertFalse(people.get(1).has("spouse"));
            }
        catch (JSONException e)
            {
            fail("Unable to parse response");
            }
        }

    @Test
    public void testGetInternalContext()
            throws JSONException
        {
        Client client = getClient();
        WebTarget webTarget = client.target(getResourceUrl("/", "_internal/dist-test1/1"));
        testGetInternal(webTarget);
        }

    protected WebTarget getWebTarget(String url)
        {
        Client client = getClient();
        return client.target(getResourceUrl(url));
        }

    protected WebTarget getWebTarget(String url, String attr)
        {
        Client client = getClient();
        return client.target(getResourceUrl(url) + attr);
        }

    protected void testGetInternal(WebTarget webTarget)
            throws JSONException
        {
        Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
        int      status   = response.getStatus();
        assertEquals(200 /* OK */, status);

        JSONObject jsonPerson = new JSONObject(response.readEntity(String.class));
        Assert.assertEquals(m_person.getName(), jsonPerson.getString("name"));

        Address    address     = m_person.getAddress();
        JSONObject jsonAddress = jsonPerson.getJSONObject("address");
        Assert.assertEquals(address.getStreet(), jsonAddress.getString("street"));
        Assert.assertEquals(address.getCity(), jsonAddress.getString("city"));
        Assert.assertEquals(address.getState(), jsonAddress.getString("state"));
        Assert.assertEquals(address.getZip(), jsonAddress.getString("zip"));

        Person     spouse     = m_person.getSpouse();
        JSONObject jsonSpouse = jsonPerson.getJSONObject("spouse");
        Assert.assertEquals(spouse.getName(), jsonSpouse.getString("name"));

        Person[]  children     = m_person.getChildren();
        JSONArray jsonChildren = jsonPerson.getJSONArray("children");
        Assert.assertEquals(children.length, jsonChildren.length());
        for (int i = 0; i < children.length; i++)
            {
            JSONObject child =  jsonChildren.getJSONObject(i);
            Assert.assertEquals(children[i].getName(), child.getString("name"));
            assertNotNull(child.getString("dateOfBirth"));
            }
        }

    /**
     * An EntryProcessor implementation to get the Http port.
     */
    public static class GetPortProcessor
            implements InvocableMap.EntryProcessor<String, String, Integer>, PortableObject
        {
        public GetPortProcessor()
            {
            }

        // ----- InvocableMap.EntryProcessor --------------------------------------

        @Override
        public Integer process(InvocableMap.Entry<String, String> entry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;

            Cluster      cluster  = binEntry.getBackingMapContext().getManagerContext().getCacheService().getCluster();
            ProxyService service  = (ProxyService) cluster.getService("ExtendHttpProxyService");
            HttpAcceptor acceptor = (HttpAcceptor) service.getAcceptor();

            return acceptor == null ? 0 : acceptor.getListenPort();
            }

        // ----- PortableObject interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
            {
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
            {
            }
        }

    @Test
    public void testGetNonExistResource()
        {
        WebTarget webTarget = getWebTarget("bad-name-test");

        // perform a GET of a server-side resource that does not exist
        Response response = webTarget
                .request(MediaType.APPLICATION_JSON).get();

        assertEquals(404, response.getStatusInfo().getStatusCode());
        assertEquals("Not Found", response.getStatusInfo().getReasonPhrase());
        }

    @Test
    public void testConditionalGet()
            throws JSONException
        {
        WebTarget webTarget = getWebTarget("dist-test1/2");

        // perform a GET of a server-side resource that implements Versionable
        Response  response  = webTarget
                .request(MediaType.APPLICATION_JSON).get();
        assertEquals(200 /* OK */, response.getStatus());

        // verify that the current version of the resource is 1
        JSONObject json     = new JSONObject(response.readEntity(String.class));
        String     sVersion = json.getString("versionIndicator");
        assertEquals("1", sVersion);
        assertEquals(new EntityTag("1"), response.getEntityTag());

        // perform a conditional GET of the same resource and verify that we
        // get a response status of 304: Not Modified
        response = webTarget
                .request(MediaType.APPLICATION_JSON)
                .header("If-None-Match", '"' + "1" + '"').get();
        assertEquals(304 /* Not Modified */, response.getStatus());
        response.close();
        }

    @Test
    public void testConditionalPut()
            throws JSONException
        {
        WebTarget webTarget = getWebTarget("dist-test1/2");

        // perform a PUT of a server-side resource that implements Versionable
        Response  response  = webTarget
                .request(MediaType.APPLICATION_JSON).get();
        assertEquals(200 /* OK */, response.getStatus());

        // verify that the current version of the resource is 1
        JSONObject json     = new JSONObject(response.readEntity(String.class));
        String     sVersion = json.getString("versionIndicator");
        assertEquals("1", sVersion);
        assertEquals(new EntityTag("1"), response.getEntityTag());

        // simulate a version change on the server-side by rolling back the
        // version indicator on our representation of the resource
        m_verPerson.setVersionIndicator(0);

        Entity<VersionablePortablePerson> vp = Entity.entity(m_verPerson, MediaType.APPLICATION_JSON);

        // perform a conditional PUT of the same resource and verify that we
        // get a response status of 409: Conflict
        response = webTarget
                .request(MediaType.APPLICATION_JSON)
                .put(vp);
        assertEquals(409 /* Conflict */, response.getStatus());

        m_verPerson.setVersionIndicator(1);     // restore back to 1
        vp = Entity.entity(m_verPerson, MediaType.APPLICATION_JSON);
        response = webTarget
                .request(MediaType.APPLICATION_JSON)
                .put(vp);
        assertEquals(200 /* OK */, response.getStatus());
        }

    @Test
    public void testGetXml()
        {
        WebTarget webTarget = getWebTarget("dist-test1/1");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        String actual = response.readEntity(String.class);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<person age=\"36\"><dateOfBirth>1974-08-24</dateOfBirth><name>Aleksandar Seovic</name>"
            + "<address><city>Tampa</city><state>FL</state><street>123 Main St</street><zip>12345</zip></address>"
            + "<children><child xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"6\">"
            + "<dateOfBirth>2004-08-14</dateOfBirth><name>Ana Maria Seovic</name><children/></child>"
            + "<child xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"3\">"
            + "<dateOfBirth>2008-12-28</dateOfBirth><name>Novak Seovic</name><children/></child></children>"
            + "<spouse xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"portablePerson\" age=\"33\">"
            + "<dateOfBirth>1978-02-20</dateOfBirth><name>Marija Seovic</name><children/></spouse></person>",
                     actual);
        }

    @Test
    public void testGetEntriesXml()
        {
        WebTarget webTarget = getWebTarget("dist-test1/entries");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        XmlDocument xmlDoc = null;
        try
            {
            xmlDoc = new SimpleParser().parseXml(response.readEntity(String.class));
            }
        catch (IOException ioe)
            {
            fail("Unable to parse response");
            }
        Map      mapResult = new HashMap();
        Iterator iter      = xmlDoc.getElements("entry");
        while (iter.hasNext())
            {
            XmlElement elem = (XmlElement) iter.next();
            mapResult.put(elem.getElement("key").getInt(), elem.getElement("value"));
            }
        assertEquals(m_person.getName(),
                ((XmlElement) mapResult.get(1)).getElement("person").getElement("name").getString());
        assertEquals(m_verPerson.getName(),
                ((XmlElement) mapResult.get(2)).getElement("person").getElement("name").getString());
        assertNotNull(((XmlElement) mapResult.get(1)).getElement("person").getElement("spouse"));
        }

    @Test
    public void testGetEntriesPartialXml()
        {
        WebTarget webTarget = getWebTarget("dist-test1/entries;p=name,age,dateOfBirth");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        XmlDocument xmlDoc = null;
        try
            {
            xmlDoc = new SimpleParser().parseXml(response.readEntity(String.class));
            }
        catch (IOException ioe)
            {
            fail("Unable to parse response");
            }
        Map      mapResult = new HashMap();
        Iterator iter      = xmlDoc.getElements("entry");
        while (iter.hasNext())
            {
            XmlElement elem = (XmlElement) iter.next();
            mapResult.put(elem.getElement("key").getInt(), elem.getElement("value"));
            }
        assertEquals(m_person.getName(),
                ((XmlElement) mapResult.get(1)).getElement("person").getElement("name").getString());
        assertEquals(m_verPerson.getName(),
                ((XmlElement) mapResult.get(2)).getElement("person").getElement("name").getString());
        assertNull(((XmlElement) mapResult.get(1)).getElement("person").getElement("spouse"));
        }

    @Test
    public void testPartialGetJson()
            throws JSONException
        {
        WebTarget webTarget = getWebTarget("dist-test1/1;p=name,address:(city,state),children:(name)");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        JSONObject jsonPerson = new JSONObject(response.readEntity(String.class));
        Assert.assertEquals(m_person.getName(), jsonPerson.getString("name"));

        Address    address     = m_person.getAddress();
        JSONObject jsonAddress = jsonPerson.getJSONObject("address");
        assertFalse(jsonAddress.has("street"));
        Assert.assertEquals(address.getCity(), jsonAddress.getString("city"));
        Assert.assertEquals(address.getState(), jsonAddress.getString("state"));
        assertFalse(jsonAddress.has("zip"));

        assertFalse(jsonPerson.has("spouse"));

        Person[]  children     = m_person.getChildren();
        JSONArray jsonChildren = jsonPerson.getJSONArray("children");
        Assert.assertEquals(children.length, jsonChildren.length());
        for (int i = 0; i < children.length; i++)
            {
            JSONObject child =  jsonChildren.getJSONObject(i);
            Assert.assertEquals(children[i].getName(), child.getString("name"));
            assertFalse(child.has("dateOfBirth"));
            }
        }

    @Test
    public void testPartialGetXml()
        {
        WebTarget webTarget = getWebTarget("dist-test1/1;p=name,address:(city,state),children:(name)");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<person><address><city>Tampa</city><state>FL</state></address>"
            + "<children><child><name>Ana Maria Seovic</name></child><child><name>Novak Seovic</name></child></children>"
            + "<name>Aleksandar Seovic</name></person>", response.readEntity(String.class));
        }

    @Test
    public void testPartialCollectionGetJson()
        {
        WebTarget webTarget = getWebTarget("dist-test-proc/(1,2);p=age");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String  entity = response.readEntity(String.class);
        boolean orig   = entity.equals("[{\"age\":25},{\"age\":23}]");
        boolean swap   = entity.equals("[{\"age\":23},{\"age\":25}]");

        assertTrue(orig || swap);
        }

    @Test
    public void testPartialCollectionGetXml()
        {
        WebTarget webTarget = getWebTarget("dist-test-proc/(1,2);p=age");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String  entity = response.readEntity(String.class);
        boolean orig   = entity.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                + "<collection><Persona><age>25</age></Persona>"
                + "<Persona><age>23</age></Persona></collection>");
        boolean swap   = entity.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                + "<collection><Persona><age>23</age></Persona>"
                + "<Persona><age>25</age></Persona></collection>");

        assertTrue(orig || swap);
        }

    @Test
    public void testDeleteExisting()
        {
        NamedCache cache = getNamedCache("dist-test1");
        assertNotNull(cache.get(1));

        WebTarget webTarget = getWebTarget("dist-test1/1");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).delete();
        assertEquals(200 /* OK */, response.getStatus());

        assertNull(cache.get(1));
        }

    @Test
    public void testJsonPTDeleteExisting()
        {
        NamedCache cache = getNamedCache("dist-binary-named-query");
        assertNotNull(cache.get("1"));

        WebTarget webTarget = getWebTarget("dist-binary-named-query/1");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).delete();
        assertEquals(200 /* OK */, response.getStatus());

        assertNull(cache.get("1"));
        }

    @Test
    public void testPut()
        {
        NamedCache cache = getNamedCache("dist-test1");
        assertNotNull(cache.get(1));

        WebTarget webTarget = getWebTarget("dist-test1/1");

        Response  response  = webTarget.request(MediaType.TEXT_PLAIN).put(Entity.text("PutTest"));
        assertEquals(200 /* OK */, response.getStatus());

        assertEquals("PutTest", cache.get(1));
        }

    @Test
    public void testJSonPTPut()
        {
        NamedCache cache = getNamedCache("dist-binary");
        cache.clear();

        WebTarget    webTarget = getWebTarget("dist-binary/1");

        Response     response = webTarget.request(MediaType.APPLICATION_JSON).put(Entity.json(s_person1));
        assertEquals(200 /* OK */, response.getStatus());

        assertEquals(s_jsonMap1, cache.get("1"));
        }

    @Test
    public void testDeleteMissing()
        {
        NamedCache cache = getNamedCache("dist-test1");
        assertNull(cache.get(-1));

        WebTarget webTarget = getWebTarget("dist-test1/-1");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).delete();
        assertEquals(404 /* Not Found */, response.getStatus());
        assertNull(cache.get(-1));
        }

    @Test
    public void testMapJsonSerialization()
            throws JSONException
        {
        WebTarget webTarget = getWebTarget("dist-test-map/1");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        JSONObject jsonMap = new JSONObject(response.readEntity(String.class));
        for (Map.Entry<Integer, String> entry : m_map.entrySet())
            {
            assertEquals(entry.getValue(), jsonMap.getString(entry.getKey().toString()));
            }
        }

    @Test
    public void testMapXmlSerialization()
        {
        WebTarget webTarget = getWebTarget("dist-test-map/1");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        XmlDocument xmlDoc = null;

        try
            {
            xmlDoc = new SimpleParser().parseXml(response.readEntity(String.class));
            }
        catch (IOException ioe)
            {
            fail("Unable to parse response");
            }
        Map mapResult = new HashMap();
        Iterator iter = xmlDoc.getElements("entry");
        while (iter.hasNext())
            {
            XmlElement elem = (XmlElement) iter.next();
            mapResult.put(elem.getElement("key").getInt(), elem.getElement("value").getString());
            }
        for (Map.Entry<Integer, String> entry : m_map.entrySet())
            {
            assertEquals(entry.getValue(), mapResult.get(entry.getKey()));
            }
        }

    @Test
    public void testCollectionJsonSerialization()
            throws JSONException
        {
        WebTarget webTarget = getWebTarget("dist-test-collection/1");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        JSONArray collection = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(m_collection.size(), collection.length());

        for (String s : m_collection)
            {
            boolean fFound = false;
            for (int i = 0; i < collection.length(); i++)
                {
                String json = collection.getString(i);
                if (s.equals(json))
                    {
                    fFound = true;
                    break;
                    }
                }
            assertTrue("Element '" + s + "' not found.", fFound);
            }
        }

    @Test
    public void testCollectionXmlSerialization()
        {
        WebTarget webTarget = getWebTarget("dist-test-collection/1");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection>onetwothree</collection>",
            response.readEntity(String.class));
        }

    @Test
    public void testAggregationJson()
        {
        WebTarget webTarget = getWebTarget("dist-test1/long-sum(age)");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        assertEquals("72", response.readEntity(String.class));
        }

    @Test
    public void testAggregationXml()
        {
        WebTarget webTarget = getWebTarget("dist-test1/long-sum(age)");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        String    entity    = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);
        assertEquals("72", entity);
        }

    @Test
    public void testAggregationPlain()
        {
        WebTarget webTarget = getWebTarget("dist-test1/long-sum(age)");

        Response  response  = webTarget.request(MediaType.TEXT_PLAIN).get();
        int       status    = response.getStatus();
        String    entity    = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);
        assertEquals("72", entity);
        }

    @Test
    public void testSetAggregationPlain()
        {
        WebTarget webTarget = getWebTarget("dist-test1/(1,2)/long-sum(age)");

        Response  response  = webTarget.request(MediaType.TEXT_PLAIN).get();
        int       status    = response.getStatus();
        String    entity    = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);
        assertEquals("72", entity);
        }

    @Test
    public void testCustomAggregationJson()
        {
        WebTarget webTarget = getWebTarget("dist-test1/custom-long-sum(age)");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        assertEquals("72", response.readEntity(String.class));
        }

    @Test
    public void testCustomAggregationFactoryJson()
        {
        WebTarget webTarget = getWebTarget("dist-test1/custom-long-sum-factory(getAge)");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        assertEquals("72", response.readEntity(String.class));
        }

    @Test
    public void testDistinctValuesJson()
        {
        WebTarget webTarget = getWebTarget("dist-test1/distinct-values(name)");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        assertEquals("[\"Aleksandar Seovic\"]", response.readEntity(String.class));
        }

    @Test
    public void testDistinctValuesXml()
        {
        WebTarget webTarget = getWebTarget("dist-test1/distinct-values(name)");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        String    entity    = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection>Aleksandar Seovic</collection>", entity);
        }

    @Test
    public void testProcessingJson()
        {
        WebTarget webTarget = getWebTarget("dist-test-proc/1/increment(age,1)");

        Response  response  =  webTarget.request(MediaType.APPLICATION_JSON).post(Entity.text(""));
        int       status    =  response.getStatus();
        String    entity    =  response.readEntity(String.class);
        assertEquals(200 /* OK */, status);
        assertEquals("26", entity);

        NamedCache cache = getNamedCache("dist-test-proc");
        assertEquals(26, ((Persona)cache.get(1)).getAge());
        assertEquals(23, ((Persona)cache.get(2)).getAge());
        }

    @Test
    public void testJsonPTProcessing()
        {
        WebTarget webTarget = getWebTarget("dist-binary-named-query/1/increment(age,1)");

        Response  response  =  webTarget.request(MediaType.APPLICATION_JSON).post(Entity.text(""));
        int       status    =  response.getStatus();
        String    entity    =  response.readEntity(String.class);
        assertEquals(200 /* OK */, status);
        assertEquals("34", entity);

        NamedCache cache = getNamedCache("dist-binary-named-query");
        assertEquals(34, ((JsonMap) cache.get("1")).get("age"));
        assertEquals(37, ((JsonMap) cache.get("2")).get("age"));
        }

    @Test
    public void testProcessingXml()
        {
        WebTarget webTarget = getWebTarget("dist-test-proc/1/increment(age,1)");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).post(Entity.text(""));
        int       status    = response.getStatus();
        String    entity    = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);

        System.out.println("testProcessingXml, entity: " + entity);

        NamedCache cache = getNamedCache("dist-test-proc");

        System.out.println("testProcessingXml, cache entries: "
                + "1:" + ((Persona) cache.get(1)).getAge() + ", " + "2:" + ((Persona) cache.get(2)).getAge());

        assertEquals("26", entity);

        assertEquals(26, ((Persona)cache.get(1)).getAge());
        assertEquals(23, ((Persona)cache.get(2)).getAge());
        }

    @Test
    public void testMultiplierProcessingJson()
        {
        WebTarget webTarget = getWebTarget("dist-test-proc/(1,2)/multiply(age,2)");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.text(""));
        int       status    = response.getStatus();
        String    entity    = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);

        boolean orig = entity.equals("{\"1\":50,\"2\":46}");
        boolean swap = entity.equals("{\"2\":46,\"1\":50}");
        assertTrue(orig || swap);

        NamedCache cache = getNamedCache("dist-test-proc");
        assertEquals(50, ((Persona)cache.get(1)).getAge());
        assertEquals(46, ((Persona)cache.get(2)).getAge());
        }

    @Test
    public void testJsonPTMultiplierProcessingJson()
        {
        WebTarget webTarget = getWebTarget("dist-binary-named-query/(1,2)/multiply(age,2)");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.text(""));
        int       status    = response.getStatus();
        String    entity    = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);

        boolean orig = entity.equals("{\"1\":66,\"2\":74}");
        boolean swap = entity.equals("{\"2\":74,\"1\":66}");
        assertTrue(orig || swap);

        NamedCache cache = getNamedCache("dist-binary-named-query");
        assertEquals(66, ((JsonMap) cache.get("1")).get("age"));
        assertEquals(74, ((JsonMap) cache.get("2")).get("age"));
        }

    @Test
    public void testMultiplierProcessingXml()
        {
        WebTarget webTarget = getWebTarget("dist-test-proc/(1,2)/multiply(age,2)");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).post(Entity.text(""));
        int       status    = response.getStatus();
        String    entity    = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);

        boolean orig = entity.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                + "<map><entry><key>2</key><value>46</value></entry><entry><key>1</key><value>50</value></entry></map>");

        boolean swap = entity.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                + "<map><entry><key>1</key><value>50</value></entry><entry><key>2</key><value>46</value></entry></map>");

        assertTrue(orig || swap);

        NamedCache cache = getNamedCache("dist-test-proc");
        assertEquals(50, ((Persona)cache.get(1)).getAge());
        assertEquals(46, ((Persona)cache.get(2)).getAge());
        }

    @Test
    public void testCustomProcessingJson()
        {
        WebTarget webTarget = getWebTarget("dist-test-proc/(1,2)/custom-number-doubler(Age)");

        NamedCache cache = getNamedCache("dist-test-proc");

        // --- TODO: DEBUG code -- remove once all REST tests are passing consistently
        System.out.println("testCustomProcessingJson, cache entries before test: "
                + "1:" + ((Persona) cache.get(1)).getAge() + ", " + "2:" + ((Persona) cache.get(2)).getAge());

        MapListener listener = new MultiplexingMapListener()
            {
            public void onMapEvent(MapEvent evt)
                {
                System.out.println("testCustomProcessingJson, event has occurred: " + evt);
                System.out.println("Id: " + evt.getId() + ", source: " + evt.getSource() + " (old value: "
                        + ((Persona) evt.getOldValue()).getAge() + ", new value: " + ((Persona) evt.getNewValue()).getAge()
                        + ")");
                }
            };
        cache.addMapListener(listener);

        try
            {
        // --- TODO: end of DEBUG code
            Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.text(""));
            int      status   = response.getStatus();
            String   entity   = response.readEntity(String.class);
            assertEquals(200 /* OK */, status);

            System.out.println("testCustomProcessingJson, entity: " + entity);

            boolean orig = entity.equals("{\"1\":50,\"2\":46}");
            boolean swap = entity.equals("{\"2\":46,\"1\":50}");

            System.out.println("testCustomProcessingJson, cache entries after test: "
                    + "1:" + ((Persona) cache.get(1)).getAge() + ", " + "2:" + ((Persona) cache.get(2)).getAge());

            assertTrue(orig || swap);

            assertEquals(50, ((Persona)cache.get(1)).getAge());
            assertEquals(46, ((Persona)cache.get(2)).getAge());
        // --- TODO: DEBUG code -- remove once all REST tests are passing consistently
            }
        finally
            {
            cache.removeMapListener(listener);
            }
        // --- TODO: end of DEBUG code
        }

    @Test
    public void testCustomProcessingFactoryJson()
        {
        WebTarget webTarget = getWebTarget("dist-test-proc/(1,2)/custom-number-doubler-factory(Age)");

        Response response = webTarget.request(MediaType.APPLICATION_JSON).post(Entity.text(""));
        int      status   = response.getStatus();
        String   entity   = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);

        boolean orig = entity.equals("{\"1\":50,\"2\":46}");
        boolean swap = entity.equals("{\"2\":46,\"1\":50}");
        assertTrue(orig || swap);

        NamedCache cache = getNamedCache("dist-test-proc");
        assertEquals(50, ((Persona)cache.get(1)).getAge());
        assertEquals(46, ((Persona)cache.get(2)).getAge());
        }

    @Test
    public void testBasic()
        {
        WebTarget webTarget = getWebTarget("dist-test-basic/high");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).put(Entity.json("5"));
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        NamedCache cache = getNamedCache("dist-test-basic");
        assertEquals(5, cache.get("high"));
        }

    @Test
    public void testGetKeys()
        {
        WebTarget webTarget = getWebTarget("dist-test1/keys");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        String entity = response.readEntity(String.class);
        assertTrue(entity.equals("[1,2]") || entity.equals("[2,1]"));

        Response xmlResponse = webTarget.request(MediaType.APPLICATION_XML).get();
        assertEquals(200 /* OK */, xmlResponse.getStatus());
        String xmlEntity = xmlResponse.readEntity(String.class);
        assertTrue(xmlEntity, xmlEntity.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><keys><key>1</key><key>2</key></keys>")
               || xmlEntity.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><keys><key>2</key><key>1</key></keys>"));
        }

    @Test
    public void testDirectQuery()
        {
        WebTarget webTarget = getWebTarget("dist-test-named-query")
                                      .queryParam("q", "name is \"Vaso\"");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String    entity = response.readEntity(String.class);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection><Persona><addresses/><age>37</age><name>Vaso</name></Persona></collection>", entity);
        }

    @Test
    public void testJsonPTDirectQuery()
            throws Exception
        {
        WebTarget webTarget = getWebTarget("dist-binary-named-query")
                                      .queryParam("q", "name is \"Vaso\"");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        JSONArray results   = new JSONArray(response.readEntity(String.class));
        ArrayList list      = new ArrayList(results.length());
        for (int i = 0; i < results.length(); i++)
            {
            JsonMap person  = new ObjectMapper().readValue(results.get(i).toString(), JsonMap.class);
            list.add(person);
            }
        assertEquals(1, list.size());
        assertTrue(list.contains(s_jsonMap3));
        }

    @Test
    public void testBadQuery()
        {
        WebTarget webTarget = getWebTarget("dist-test-named-query")
                                      .queryParam("q", "name");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), status);

        String    entity = response.readEntity(String.class);
        assertEquals("An exception occurred while processing the request.", entity);
        }

    @Test
    public void testNamedQuery()
        {
        WebTarget webTarget = getWebTarget("dist-test-named-query", "/age-37-query");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String  entity = response.readEntity(String.class);
        boolean orig   = entity.equals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection><Persona><addresses/><age>37</age><name>Aleks</name></Persona><Persona><addresses/><age>37</age><name>Vaso</name></Persona></collection>");
        boolean swap   = entity.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection><Persona><addresses/><age>37</age><name>Vaso</name></Persona><Persona><addresses/><age>37</age><name>Aleks</name></Persona></collection>");
        assertTrue(orig || swap);

        webTarget = getWebTarget("dist-test-named-query", "/age-37-query;p=age,name");

        response  = webTarget.request(MediaType.APPLICATION_XML).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        entity = response.readEntity(String.class);
        orig   = entity.equals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection><Persona><age>37</age><name>Aleks</name></Persona><Persona><age>37</age><name>Vaso</name></Persona></collection>");
        swap   = entity.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection><Persona><age>37</age><name>Vaso</name></Persona><Persona><age>37</age><name>Aleks</name></Persona></collection>");
        assertTrue(orig || swap);
        }

    @Test
    public void testJsonPTNamedQuery()
            throws Exception
        {
        WebTarget webTarget = getWebTarget("dist-binary-named-query", "/age-37-query");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        JSONArray results   = new JSONArray(response.readEntity(String.class));
        ArrayList list      = new ArrayList(results.length());
        for (int i = 0; i < results.length(); i++)
            {
            String sPerson = results.get(i).toString();
            JsonMap person  = new ObjectMapper().readValue(sPerson, JsonMap.class);
            list.add(person);
            }
        assertEquals(2, list.size());
        assertTrue(list.contains(s_jsonMap2));
        assertTrue(list.contains(s_jsonMap3));
        }

    @Test
    public void testNamedQueryEntries()
        {
        WebTarget webTarget = getWebTarget("dist-test-named-query", "/age-37-query/entries");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String  entity = response.readEntity(String.class);
        System.out.println(entity);
        boolean orig   = entity.equals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection><entry><key>3</key><value><Persona><addresses/><age>37</age><name>Vaso</name></Persona></value></entry><entry><key>2</key><value><Persona><addresses/><age>37</age><name>Aleks</name></Persona></value></entry></collection>");
        boolean swap   = entity.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection><entry><key>2</key><value><Persona><addresses/><age>37</age><name>Aleks</name></Persona></value></entry><entry><key>3</key><value><Persona><addresses/><age>37</age><name>Vaso</name></Persona></value></entry></collection>");

        assertTrue(orig || swap);

        webTarget = getWebTarget("dist-test-named-query", "/age-37-query/entries;p=age,name");

        response  = webTarget.request(MediaType.APPLICATION_XML).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        entity = response.readEntity(String.class);
        orig = entity.equals(
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection><entry><key>3</key><value><Persona><age>37</age><name>Vaso</name></Persona></value></entry><entry><key>2</key><value><Persona><age>37</age><name>Aleks</name></Persona></value></entry></collection>");
        swap = entity.equals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection><entry><key>2</key><value><Persona><age>37</age><name>Aleks</name></Persona></value></entry><entry><key>3</key><value><Persona><age>37</age><name>Vaso</name></Persona></value></entry></collection>");
        assertTrue(orig || swap);
        }

    @Test
    public void testNamedQueryParams()
        {
        WebTarget webTarget = getWebTarget("dist-test-named-query", "/name-query?name=Ivan");

        Response  response  = webTarget.request(MediaType.APPLICATION_XML).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String    entity    = response.readEntity(String.class);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><collection><Persona><addresses/><age>33</age><name>Ivan</name></Persona></collection>", entity);
        }

    @Test
    public void testJsonPTNamedQueryParams()
            throws Exception
        {
        WebTarget webTarget = getWebTarget("dist-binary-named-query", "/name-query?name=Ivan");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        JSONArray results   = new JSONArray(response.readEntity(String.class));
        ArrayList list      = new ArrayList(results.length());
        for (int i = 0; i < results.length(); i++)
            {
            JsonMap person  = new ObjectMapper().readValue(results.get(i).toString(), JsonMap.class);
            list.add(person);
            }
        assertEquals(1, list.size());
        assertTrue(list.contains(s_jsonMap1));
        }

    @Test
    public void testNamedQueryPartial()
        {
        WebTarget webTarget = getWebTarget("dist-test-named-query", "/age-37-query;p=name");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String  entity = response.readEntity(String.class);
        boolean orig   = entity.equals("[{\"name\":\"Aleks\"},{\"name\":\"Vaso\"}]");
        boolean swap   = entity.equals("[{\"name\":\"Vaso\"},{\"name\":\"Aleks\"}]");
        assertTrue(orig || swap);
        }

    @Test
    public void testJsonPTNamedQueryPartial()
        {
        WebTarget webTarget = getWebTarget("dist-binary-named-query", "/age-37-query;p=name");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String  entity = response.readEntity(String.class);
        boolean orig   = entity.equals("[{\"name\":\"Aleks\"},{\"name\":\"Vaso\"}]");
        boolean swap   = entity.equals("[{\"name\":\"Vaso\"},{\"name\":\"Aleks\"}]");
        assertTrue(orig || swap);
        }

    @Test
    public void testNamedQueryPaging()
        {
        WebTarget webTarget = getWebTarget("dist-test-named-query", "/age-37-query;start=0;count=1;p=name;sort=name:desc");
        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals("[{\"name\":\"Vaso\"}]", response.readEntity(String.class));

        webTarget = getWebTarget("dist-test-named-query", "/age-37-query;start=1;count=1;p=name;sort=name:desc");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        assertEquals(200 /* OK */, response.getStatus());
        assertEquals("[{\"name\":\"Aleks\"}]", response.readEntity(String.class));
        }

    @Test
    public void testNamedQuerySort()
        {
        WebTarget webTarget = getWebTarget("dist-test-named-query", "/age-37-query;p=name;sort=name:asc");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String    entity    = response.readEntity(String.class);
        assertEquals("[{\"name\":\"Aleks\"},{\"name\":\"Vaso\"}]", entity);

        webTarget = getWebTarget("dist-test-named-query", "/age-37-query;p=name;sort=name:desc");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        entity    = response.readEntity(String.class);
        assertEquals("[{\"name\":\"Vaso\"},{\"name\":\"Aleks\"}]", entity);
        }

    @Test
    public void testNamedQueryAggregators()
        {
        WebTarget webTarget = getWebTarget("dist-test-named-query", "/age-37-query/long-sum(age)");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        assertEquals("74", response.readEntity(String.class));
        }

    @Test
    public void testJsonPTNamedQueryAggregators()
        {
        WebTarget webTarget = getWebTarget("dist-binary-named-query", "/age-37-query/long-sum(age)");

        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        assertEquals("74", response.readEntity(String.class));
        }

    @Test
    public void testNamedQueryKeys()
        {
        WebTarget webTarget = getWebTarget("dist-test-named-query", "/name-query/keys?name=Ivan");
        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String         entity   = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);
        assertEquals("[1]", entity);

        webTarget = getWebTarget("dist-test-named-query", "/age-37-query/keys");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        entity    = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);
        assertTrue(entity.equals("[2,3]") || entity.equals("[3,2]"));
        }

    @Test
    public void testJsonPTNamedQueryKeys()
        {
        WebTarget webTarget = getWebTarget("dist-binary-named-query", "/name-query/keys?name=Ivan");
        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        String entity   = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);
        assertEquals("[\"1\"]", entity);

        webTarget = getWebTarget("dist-binary-named-query", "/age-37-query/keys");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);

        entity    = response.readEntity(String.class);
        assertEquals(200 /* OK */, status);
        assertTrue(entity.equals("[\"2\",\"3\"]") || entity.equals("[\"3\",\"2\"]"));
        }

    @Test
    public void testMaxResults()
            throws JSONException
        {
        NamedCache cache = getNamedCache("dist-test-max-results");
        for (int i = 0; i < 2000; i++)
            {
            cache.put(i, i);
            }

        WebTarget webTarget = getWebTarget("dist-test-max-results");
        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        JSONArray results = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(2000, results.length());

        webTarget = getWebTarget("dist-test-max-results;count=1111");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(1111, results.length());

        webTarget = getWebTarget("dist-test-max-results/less-than-1000");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(1000, results.length());

        webTarget = getWebTarget("dist-test-max-results/less-than-1000;count=222");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(222, results.length());

        webTarget = getWebTarget("dist-test-max-results/less-than-1000-limit");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(200, results.length());

        webTarget = getWebTarget("dist-test-max-results/less-than-1000-limit;count=111");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(111, results.length());

        webTarget = getWebTarget("dist-test-max-results").queryParam("q", "value() < 50");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(50, results.length());

        webTarget = getWebTarget("dist-test-max-results;count=20").queryParam("q", "value() < 50");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(20, results.length());

        cache = getNamedCache("dist-test-max-results2");
        for (int i = 0; i < 200; i++)
            {
            cache.put(i, i * 1.0);
            }

        webTarget = getWebTarget("dist-test-max-results2/less-than-100.0");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(100, results.length());

        webTarget = getWebTarget("dist-test-max-results2").queryParam("q", "value() < 50.0");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(50, results.length());
        }

    @Test
    public void testMaxResultsLimits()
            throws JSONException
        {
        NamedCache cache = getNamedCache("dist-test-max-results-limit");
        cache.clear();
        for (int i = 0; i < 2000; i++)
            {
            cache.put(i, i);
            }

        // cache resource; resource limit
        WebTarget webTarget = getWebTarget("dist-test-max-results-limit");
        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        JSONArray results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(900, results.length());

        // cache resource; url limit
        webTarget = getWebTarget("dist-test-max-results-limit;count=1111");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(900, results.length());

        // cache resource; url limit
        webTarget = getWebTarget("dist-test-max-results-limit;count=123");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(123, results.length());

        // named query; resource limit
        webTarget = getWebTarget("dist-test-max-results-limit/less-than-1000");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(900, results.length());

        // named query; url limit
        webTarget = getWebTarget("dist-test-max-results-limit/less-than-1000;count=222");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(222, results.length());

        // named query; url limit
        webTarget = getWebTarget("dist-test-max-results-limit/less-than-1000;count=950");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(900, results.length());

        // limited named query; query limit
        webTarget = getWebTarget("dist-test-max-results-limit/less-than-1000-limit");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(200, results.length());

        // limited named query; url limit
        webTarget = getWebTarget("dist-test-max-results-limit/less-than-1000-limit;count=15");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(15, results.length());

        // limited named query; url limit
        webTarget = getWebTarget("dist-test-max-results-limit/less-than-1000-limit;count=333");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(200, results.length());

        // direct query limit
        webTarget = getWebTarget("dist-test-max-results-limit").queryParam("q", "value() < 200");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(100, results.length());

        // direct query; url limit
        webTarget = getWebTarget("dist-test-max-results-limit;count=20").queryParam("q", "value() < 200");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(20, results.length());

        // direct query; url limit
        webTarget = getWebTarget("dist-test-max-results-limit;count=120").queryParam("q", "value() < 200");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(100, results.length());
        }

    @Test
    public void testPagination()
            throws JSONException
        {
        NamedCache cache = getNamedCache("dist-test-named-query");
        cache.clear();
        for (int i=0; i<200; i++)
            {
            cache.put(i, new Persona(String.valueOf(i), i));
            }

        WebTarget webTarget = getWebTarget("dist-test-named-query;start=0;sort=age:asc").queryParam("q", "age < 100");
        Response  response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        int       status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        JSONArray results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(100, results.length());
        for (int i=0; i<100; i++) Assert.assertEquals(i, results.getJSONObject(i).getInt("age"));

        webTarget = getWebTarget("dist-test-named-query;start=0;count=10;sort=age:asc").queryParam("q", "age < 100");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(10, results.length());
        for (int i=0; i<10; i++) Assert.assertEquals(i, results.getJSONObject(i).getInt("age"));

        webTarget = getWebTarget("dist-test-named-query;start=10;count=20;sort=age:asc").queryParam("q", "age < 100");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(20, results.length());
        for (int i=10; i<30; i++) Assert.assertEquals(i, results.getJSONObject(i-10).getInt("age"));

        webTarget = getWebTarget("dist-test-named-query;start=90;count=20;sort=age:asc").queryParam("q", "age < 100");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(10, results.length());
        for (int i=90; i<100; i++) Assert.assertEquals(i, results.getJSONObject(i-90).getInt("age"));

        webTarget = getWebTarget("dist-test-named-query;start=70;sort=age:asc").queryParam("q", "age < 100");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(30, results.length());
        for (int i=70; i<100; i++) Assert.assertEquals(i, results.getJSONObject(i-70).getInt("age"));

        webTarget = getWebTarget("dist-test-named-query;start=70;sort=age:asc").queryParam("q", "age < 100");
        response  = webTarget.request(MediaType.APPLICATION_JSON).get();
        status    = response.getStatus();
        assertEquals(200 /* OK */, status);
        results   = new JSONArray(response.readEntity(String.class));
        Assert.assertEquals(30, results.length());
        for (int i=70; i<100; i++) Assert.assertEquals(i, results.getJSONObject(i-70).getInt("age"));
        }

    @Test
     public void testBinaryPTValue()
             throws Exception
         {
         testBinaryPassThrough("dist-binaryvalue", 1, false /* fUseImage */);
         }

     @Test
     public void testBinaryPTKeyAndValue()
             throws Exception
         {
         testBinaryPassThrough("dist-binary", "Aleks", false /* fUseImage */);
         }

     @Test
     public void testBinaryPTValueWithImage()
             throws Exception
         {
         testBinaryPassThrough("dist-binaryvalue", 1, true /* fUseImage */);
         }

     @Test
     public void testBinaryPTKeyAndValueWithImage()
             throws Exception
         {
         testBinaryPassThrough("dist-binary", "Aleks", true /* fUseImage */);
         }

     @Test
     public void testJsonPTValue()
             throws Exception
         {
         testJsonPassThrough("dist-binaryvalue", 1);
         }

     @Test
     public void testJsonPTKeyAndValue()
             throws Exception
         {
         testJsonPassThrough("dist-binary", "Aleks");
         }

    protected void testBinaryPassThrough(String sCacheName, Object oKey, boolean fUseImage)
            throws IOException
        {
        byte[] abData;
        String sMediaType;

        if (fUseImage)
            {
            abData     = Base.read(getClass().getClassLoader().getResourceAsStream("test_image.jpg"));
            sMediaType = "image/jpeg";
            }
        else
            {
            abData     = "just testing binary pass-through and need some bytes...".getBytes();
            sMediaType = MediaType.APPLICATION_OCTET_STREAM;
            }

        WebTarget webTarget = getWebTarget(sCacheName + "/" + oKey);
        Response response = webTarget.request(MediaType.WILDCARD_TYPE).put(Entity.entity(abData, sMediaType));

        assertEquals(200 /* OK */, response.getStatus());
        Object o = getNamedCache(sCacheName).get(oKey);
        assertTrue(o instanceof StaticContent);
        assertEquals(sMediaType, ((StaticContent) o).getMediaType());

        response = webTarget.request(sMediaType).get();
        assertEquals(200 /* OK */, response.getStatus());

        byte[] abResponse = response.readEntity(byte[].class);
        assertArrayEquals(abResponse, abData);
        }

     protected void testJsonPassThrough(String sCacheName, Object oKey)
             throws Exception
         {
         WebTarget  webTarget = getWebTarget(sCacheName + "/" + oKey);
         NamedCache cache     = getNamedCache(sCacheName);
         cache.clear();

         Response   response  = webTarget.request(MediaType.APPLICATION_JSON)
                                      .put(Entity.entity(m_person, MediaType.APPLICATION_JSON));
         assertEquals(200 /* OK */, response.getStatus());
         assertTrue(cache.get(oKey) instanceof JsonMap);

         response = webTarget.request(MediaType.APPLICATION_JSON).get();
         assertEquals(200 /* OK */, response.getStatus());
         PortablePerson person = new ObjectMapper().readValue(
                 response.readEntity(String.class), PortablePerson.class);
         assertEquals(m_person.getName(), person.getName());
         }

    // ----- helper methods -------------------------------------------------

    /**
     * Starts a {@link CoherenceClusterMember}.
     *
     * @param sName        the name
     * @param sCacheConfig  the configuration
     *
     * @since 22.06
     */
    @SuppressWarnings("resource")
    protected static void doStartCacheServer(String sName, String sCacheConfig)
        {
        System.setProperty("coherence.override", "rest-tests-coherence-override.xml");

        Properties properties = new Properties();
        properties.put("com.tangosol.coherence.rest.server.DefaultResourceConfig.logging.enabled", "true");
        properties.put("java.util.logging.config.file", System.getProperty("java.util.logging.config.file", ""));

        CoherenceClusterMember clusterMember = startCacheServer(sName, "rest", sCacheConfig, properties);
        Eventually.assertDeferred(() -> clusterMember.isServiceRunning("ExtendHttpProxyService"), is(true));
        }

    /**
     * Create a new HTTP client.
     *
     * @return a new HTTP client
     */
    protected ClientBuilder createClient()
        {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.connectorProvider(new ApacheConnectorProvider());
        return ClientBuilder.newBuilder()
                .withConfig(clientConfig)
                .property(ClientProperties.CONNECT_TIMEOUT, 120000)
                .property(ClientProperties.READ_TIMEOUT, 120000)
                .register(JacksonMapperProvider.class)
                .register(JacksonFeature.class);
        }

    /**
     * Configure HTTPS client.
     *
     * @return an HTTPS client
     */
    protected ClientBuilder configureSSL(ClientBuilder client)
        {
        return client
                .hostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .sslContext(s_sslProviderClient.getDependencies().getSSLContext());
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the address that the embedded HttpServer is listening on.
     *
     * @return the listen address of the embedded HttpServer
     */
    public String getAddress()
        {
        return System.getProperty("test.extend.address.local", "127.0.0.1");
        }

    /**
     * Return the port that the embedded HttpServer is listening on.
     *
     * @return the listen port of the embedded HttpServer
     */
    public int getPort()
        {
        NamedCache cache = getNamedCache("dist-test1");
        int        nPort = 0;

        if (cache != null)
            {
            nPort = (Integer) cache.invoke(1, new GetPortProcessor());
            }

        return nPort;
        }

    /**
     * Return the protocol used for all tests.
     *
     * @return the protocol
     */
    public String getProtocol()
        {
        return "http";
        }

    /**
     * Return the context path of the REST Test application.
     *
     * @return context path
     */
    public String getContextPath()
        {
        return "/api/";
        }

    /**
     * Return the url of the specified resource
     *
     * @param resource test resource
     *
     * @return the resource url
     */
    public String getResourceUrl(String resource)
        {
        return getResourceUrl(getContextPath(), resource);
        }

    /**
     * Return the url of the specified resource
     *
     * @param contextPath  the context path
     * @param resource     test resource
     *
     * @return the resource url
     */
    public String getResourceUrl(String contextPath, String resource)
        {
        if (!contextPath.endsWith("/"))
            {
            contextPath += "/";
            }
        return getProtocol() + "://" + getAddress() + ":" + getPort()
                + contextPath + resource;
        }

    /**
     * Return the HTTP client.
     *
     * @return context path
     */
    public Client getClient()
        {
        if (m_client == null)
            {
            ClientBuilder builder = createClient();
            ((ClientConfig) builder.getConfiguration())
                    .register(new LoggingFeature(Logger.getLogger("coherence.rest.diagnostic"),
                                                 Level.INFO,
                                                 LoggingFeature.Verbosity.PAYLOAD_TEXT,
                                                 4096));
            m_client = builder.build();
            }
        return m_client;
        }

    // ----- data members ---------------------------------------------------

    protected static SSLSocketProvider s_sslProviderClient;
    protected static String  s_person1;
    protected static JsonMap s_jsonMap1;
    protected static JsonMap s_jsonMap2;
    protected static JsonMap s_jsonMap3;

    protected Client m_client;
    protected PortablePerson m_person;
    protected VersionablePortablePerson m_verPerson;
    protected Map<Integer, String> m_map;
    protected Collection<String> m_collection;
    }
