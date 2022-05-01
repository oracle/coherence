open module com.oracle.coherence.functional
    {
    requires com.oracle.coherence.testing;
    requires com.oracle.coherence;

    requires com.oracle.bedrock.runtime.coherence;
    requires com.oracle.bedrock.runtime.coherence.testing;
    requires com.oracle.bedrock.testsupport;

    requires transitive junit;

    requires org.hamcrest;
    requires org.mockito;

    requires java.net.http;
    requires java.management;
    }
