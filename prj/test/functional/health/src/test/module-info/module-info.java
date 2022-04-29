open module com.oracle.coherence.functional
    {
    requires com.oracle.coherence;
    requires com.oracle.coherence.testing;

    requires com.oracle.bedrock.runtime.coherence;
    requires com.oracle.bedrock.testsupport;

    requires org.hamcrest;
    requires java.net.http;

    requires org.junit.jupiter.api;

    provides com.tangosol.util.HealthCheck
            with com.oracle.coherence.functional.health.WrapperHealthCheck;
    }
