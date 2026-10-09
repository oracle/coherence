/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package graal;

import com.oracle.coherence.io.json.JsonSerializer;
import com.oracle.coherence.io.json.genson.JsonBindingException;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.aggregator.ScriptAggregator;
import com.tangosol.util.filter.ScriptFilter;
import com.tangosol.util.processor.ScriptProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

@RunWith(Parameterized.class)
public class AbstractScriptSerializerTest
    {
    public AbstractScriptSerializerTest(String ignored, Serializer serializer)
        {
        f_serializer = serializer;
        }

    @Parameterized.Parameters(name = "serializer={0}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]{ {"pof", new ConfigurablePofContext("pof-config.xml")},
                {"java", new DefaultSerializer()},
                {"json", new JsonSerializer()} });
        }

    @Test
    public void shouldSerializeScriptProcessor()
        {
        ScriptProcessor<?, ?, ?> processor = new ScriptProcessor<>("js", "foo", "A", "B");
        Binary binary = ExternalizableHelper.toBinary(processor, f_serializer);
        if (assertJsonRejection(binary, ScriptProcessor.class))
            {
            return;
            }
        Object oResult = ExternalizableHelper.fromBinary(binary, f_serializer);
        assertThat(oResult, is(instanceOf(ScriptProcessor.class)));
        ScriptProcessor<?, ?, ?> result = (ScriptProcessor<?, ?, ?>) oResult;
        assertThat(result.getLanguage(), is(processor.getLanguage()));
        assertThat(result.getName(), is(processor.getName()));
        assertThat(result.getArgs(), is(processor.getArgs()));
        }

    @Test
    public void shouldSerializeScriptFilter()
        {
        ScriptFilter<?> filter = new ScriptFilter<>("js", "foo", "A", "B");
        Binary binary = ExternalizableHelper.toBinary(filter, f_serializer);
        if (assertJsonRejection(binary, ScriptFilter.class))
            {
            return;
            }
        Object oResult = ExternalizableHelper.fromBinary(binary, f_serializer);
        assertThat(oResult, is(instanceOf(ScriptFilter.class)));
        ScriptFilter<?> result = (ScriptFilter<?>) oResult;
        assertThat(result.getLanguage(), is(filter.getLanguage()));
        assertThat(result.getName(), is(filter.getName()));
        assertThat(result.getArgs(), is(filter.getArgs()));
        }

    @Test
    public void shouldSerializeScriptAggregator()
        {
        ScriptAggregator<?, ?, ?, ?> aggregator = new ScriptAggregator<>("js", "DummyAggregator", 19, "A", "B");
        Binary binary = ExternalizableHelper.toBinary(aggregator, f_serializer);
        if (assertJsonRejection(binary, ScriptAggregator.class))
            {
            return;
            }
        Object oResult = ExternalizableHelper.fromBinary(binary, f_serializer);
        assertThat(oResult, is(instanceOf(ScriptAggregator.class)));
        ScriptAggregator<?, ?, ?, ?> result = (ScriptAggregator<?, ?, ?, ?>) oResult;
        assertThat(result.getLanguage(), is(aggregator.getLanguage()));
        assertThat(result.getName(), is(aggregator.getName()));
        assertThat(result.getArgs(), is(aggregator.getArgs()));
        assertThat(result.characteristics(), is(aggregator.characteristics()));
        }

    /**
     * Verify the hardened rejection of untyped JSON script metadata.
     */
    private boolean assertJsonRejection(Binary binary, Class<?> type)
        {
        if (!(f_serializer instanceof JsonSerializer) || !CoherenceMode.isSecurityHardeningEnabled())
            {
            return false;
            }

        Throwable cause = assertThrows(RuntimeException.class,
                () -> ExternalizableHelper.fromBinary(binary, f_serializer));
        while (cause.getCause() != null)
            {
            cause = cause.getCause();
            }
        assertThat(cause, is(instanceOf(JsonBindingException.class)));
        assertThat(cause.getMessage(), is("Unable to de-serialize " + type.getName()));
        return true;
        }

    private final Serializer f_serializer;
    }
