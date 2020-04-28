package com.tangosol.coherence.reporter.extractor;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.URL;

import static org.junit.Assert.fail;

/**
 * Unit tests for the {@link SubQueryExtractor}.
 *
 * @author jf 2020.04.27
 */
public class SubQueryExtractorTest
    {
    @Test(expected = IOException.class)
    public void shouldNotJavaDeserialize()
        throws Throwable
        {
        InputStream       is  = getResourceAsInputStream("com/tangosol/coherence/reporter/extractor/SubQueryExtractor_java.ser");
        ObjectInputStream ois = new ObjectInputStream(is);
        ois.readObject();
        fail("instances of SubQueryExtractor must not deserialize");
        }

    @Test(expected = NotSerializableException.class)
    public void shouldNotJavaSerialize()
        throws Throwable
        {
        ObjectOutputStream os = new ObjectOutputStream(new ByteArrayOutputStream());
        os.writeObject(createSubQueryExtractor());
        fail("instances of SubQueryExtractor must not serialize");
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return InputStream for specified resource.
     *
     * Since expected failure for these tests is {@link IOException}, throw a different
     * exception if this method fails to locate or open input resource.
     *
     * @param sResourceName  the resource name
     *
     * @return InputStream for specified resource name
     */
    private InputStream getResourceAsInputStream(String sResourceName)
        {
        try
            {
            URL         url = this.getClass().getClassLoader().getResource(sResourceName);
            InputStream is  = url.openStream();
            return is;
            }
        catch (IOException e)
            {
            //ignored
            }
        throw new IllegalArgumentException("failed to locate and open deserialization input resource: " + sResourceName);
        }

    /**
     * Create an instance of {@link SubQueryExtractor}.
     *
     * @return an instance of {@link SubQueryExtractor}
     */
    private SubQueryExtractor createSubQueryExtractor()
        {
        ValueExtractor[] aVE = new ValueExtractor[1];
        aVE[0] = new ReflectionExtractor("methodName");

        XmlElement xmlQuery = new SimpleElement("root");
        xmlQuery.getElementList().add(new SimpleElement(SubQueryExtractor.TAG_PATTERN, "somePattern"));
        return new SubQueryExtractor(aVE, xmlQuery, null, "ColumnId");
        }
    }
