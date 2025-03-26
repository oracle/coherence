<doc-view>

<h2 id="_securing_with_ssl">Securing with SSL</h2>
<div class="section">
<p>This guide walks you through how to secure Coherence communication between cluster members as well as Coherence*Extend clients.</p>

<p>Oracle Coherence supports Secure Sockets Layer (SSL) to secure TCMP communication between cluster nodes and to
secure the TCP communication between Oracle Coherence*Extend clients and proxies. Oracle Coherence supports
the Transport Layer Security (TLS) protocol, which superseded the SSL protocol; however, the term SSL is
used in this documentation because it is the more widely recognized term.</p>

<p>See the Coherence documentation links below for more detailed information on Coherence Security.</p>

<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/secure/introduction-oracle-coherence-security.html">Introduction to Coherence Security</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/secure/using-ssl-secure-communication.html">Using SSL to Secure Communication</a></p>

</li>
</ul>

<h3 id="_table_of_contents">Table of Contents</h3>
<div class="section">
<ul class="ulist">
<li>
<p><router-link to="#what-you-will-build" @click.native="this.scrollFix('#what-you-will-build')">What You Will Build</router-link></p>

</li>
<li>
<p><router-link to="#what-you-will-need" @click.native="this.scrollFix('#what-you-will-need')">What You Need</router-link></p>

</li>
<li>
<p><router-link to="#building" @click.native="this.scrollFix('#building')">Building the Example Code</router-link></p>

</li>
<li>
<p><router-link to="#example-config" @click.native="this.scrollFix('#example-config')">Review the Configuration</router-link></p>

</li>
<li>
<p><router-link to="#example-tests-1" @click.native="this.scrollFix('#example-tests-1')">Review the Test Classes</router-link></p>

</li>
<li>
<p><router-link to="#run-example-1" @click.native="this.scrollFix('#run-example-1')">Run the Examples</router-link></p>

</li>
<li>
<p><router-link to="#summary" @click.native="this.scrollFix('#summary')">Summary</router-link></p>

</li>
<li>
<p><router-link to="#see-also" @click.native="this.scrollFix('#see-also')">See Also</router-link></p>

</li>
</ul>
</div>

<h3 id="what-you-will-build">What You Will Build</h3>
<div class="section">
<p>In this example you will run tests that show a number of ways to configure secure communication via SSL by defining various SSL socket providers.</p>

<p>The tests carry out the following, for a variety of socket providers:</p>

<ol style="margin-left: 15px;">
<li>
Generate keys and self-signed certificates to be used in the test

</li>
<li>
Start 2 cache servers, one having a Proxy service enabled passing properties to point to the newly created keys and certificates

</li>
<li>
Run a basic put/get test over SSL via Coherence*Extend passing properties to point to the newly created keys and certificates

</li>
</ol>
<p>Each test showcases the different methods of configuring SSL:</p>

<ul class="ulist">
<li>
<p>Using Java key stores</p>

</li>
<li>
<p>Referring directly to keys and certificates on the file-system</p>

</li>
<li>
<p>Using custom loaders to load key stores, private keys and certificates</p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">Custom loaders can also be configured to be refreshed based upon intervals.</p>
</div>

<h4 id="what-you-will-need">What You Need</h4>
<div class="section">
<ul class="ulist">
<li>
<p>About 20 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://www.oracle.com/java/technologies/downloads/">JDK 17</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://maven.apache.org/download.cgi">Maven 3.8+</a> or <a id="" title="" target="_blank" href="https://gradle.org/install/">Gradle 4+</a>
Although the source comes with the Maven and Gradle wrappers included, so they can be built without first installing
either build tool.</p>

</li>
<li>
<p>You can also import the code straight into your IDE:</p>
<ul class="ulist">
<li>
<p><router-link to="/examples/setup/intellij">IntelliJ IDEA</router-link></p>

</li>
</ul>
</li>
</ul>
</div>

<h4 id="building">Building the Example Code</h4>
<div class="section">
<p>The source code for the guides and tutorials can be found in the
<a id="" title="" target="_blank" href="http://github.com/oracle/coherence/tree/master/prj/examples">Coherence CE GitHub repo</a></p>

<p>The example source code is structured as both a Maven and a Gradle project and can be easily built with either
of those build tools. The examples are stand-alone projects so each example can be built from the
specific project directory without needing to build the whole Coherence project.</p>

<ul class="ulist">
<li>
<p>Build with Maven</p>

</li>
</ul>
<p>Using the included Maven wrapper the example can be built with the command:</p>

<markup
lang="bash"

>./mvnw clean package</markup>

<ul class="ulist">
<li>
<p>Build with Gradle</p>

</li>
</ul>
<p>Using the included Gradle wrapper the example can be built with the command:</p>

<markup
lang="bash"

>./gradlew build</markup>

</div>
</div>

<h3 id="example-config">Review the Configuration</h3>
<div class="section">

<h4 id="_socket_provider_definitions">Socket Provider Definitions</h4>
<div class="section">
<p>When configuring SSL, you define a <code>&lt;socket-provider&gt;</code> in the Coherence operational configuration and
refer to this in your operational and cache configuration.  The socket providers for this test are explained further below.</p>

<p>To enable SSL for cluster communication, add a reference to the socket provider in your <code>&lt;unicast-listener&gt;</code> element as
shown below:</p>

<markup
lang="xml"

>&lt;unicast-listener&gt;
  &lt;socket-provider system-property="test.socket.provider"&gt;provider&lt;/socket-provider&gt;
  &lt;well-known-addresses&gt;
    &lt;address system-property="coherence.wka"&gt;127.0.0.1&lt;/address&gt;
  &lt;/well-known-addresses&gt;
&lt;/unicast-listener&gt;</markup>

<p>To enable SSL on a Proxy server, specify a <code>&lt;socket-provider&gt;</code> in the <code>&lt;tcp-acceptor&gt;</code> element of the proxy scheme as shown below:</p>

<markup
lang="xml"

>&lt;proxy-scheme&gt;
  &lt;service-name&gt;Proxy&lt;/service-name&gt;
  &lt;acceptor-config&gt;
    &lt;tcp-acceptor&gt;
      &lt;socket-provider system-property="test.socket.provider"&gt;provider&lt;/socket-provider&gt;
      &lt;local-address&gt;
        &lt;address system-property="test.extend.address"/&gt;
        &lt;port system-property="test.extend.port"/&gt;
      &lt;/local-address&gt;
    &lt;/tcp-acceptor&gt;
  &lt;/acceptor-config&gt;
  &lt;autostart system-property="test.proxy.enabled"&gt;false&lt;/autostart&gt;
&lt;/proxy-scheme&gt;</markup>

<p>Finally, to enable SSL on a Coherence*Extend client, specify a <code>&lt;socket-provider&gt;</code> in the <code>&lt;tcp-initiator&gt;</code> element of
the <code>&lt;remote-cache-scheme&gt;</code> as shown below:</p>

<markup
lang="xml"

>&lt;remote-cache-scheme&gt;
  &lt;scheme-name&gt;remote&lt;/scheme-name&gt;
  &lt;service-name&gt;RemoteCache&lt;/service-name&gt;
  &lt;proxy-service-name&gt;Proxy&lt;/proxy-service-name&gt;
  &lt;initiator-config&gt;
    &lt;tcp-initiator&gt;
      &lt;socket-provider system-property="test.socket.provider"&gt;provider&lt;/socket-provider&gt;
      &lt;remote-addresses&gt;
        &lt;socket-address&gt;
          &lt;address system-property="test.extend.address"/&gt;
          &lt;port system-property="test.extend.port"/&gt;
        &lt;/socket-address&gt;
      &lt;/remote-addresses&gt;
    &lt;/tcp-initiator&gt;
  &lt;/initiator-config&gt;
&lt;/remote-cache-scheme&gt;</markup>

<p>For this example, we define a number of socket providers in the operational configuration <code>src/test/resources/tangosol-coherence-ssl.xml</code>.
Each test which is run sets the system property <code>test.socket.provider</code> to one of the following values to test the configuration:</p>

<ul class="ulist">
<li>
<p><code>sslKeyStore</code> - configure using Java key store and trust store</p>

</li>
<li>
<p><code>sslKeyAndCert</code> - configure using keys and certificates on the file system</p>

</li>
<li>
<p><code>sslCustomKeyAndCert</code> - configure using custom private key a certificate loaders (This is especially useful in Kubernetes environments to load from secrets)</p>

</li>
<li>
<p><code>sslCustomKeyStore</code> - configure using a custom key store loader</p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">when specifying a trust store, you get two-way SSL.</p>
</div>
<p>Each configuration option is outlined below:</p>

<ul class="ulist">
<li>
<p><code>sslKeyStore</code> - configure SSL socket provider using Java key store and trust store</p>
<markup
lang="xml"

>&lt;socket-provider id="sslKeyStore"&gt;
  &lt;ssl&gt;
    &lt;protocol&gt;TLS&lt;/protocol&gt;
    &lt;identity-manager&gt;
      &lt;key-store&gt;  <span class="conum" data-value="1" />
        &lt;url system-property="test.server.keystore"&gt;file:server.jks&lt;/url&gt;
        &lt;password-provider&gt;  <span class="conum" data-value="2" />
          &lt;class-name&gt;com.oracle.coherence.guides.ssl.CustomPasswordProvider&lt;/class-name&gt;
          &lt;init-params&gt;
            &lt;init-param&gt;
              &lt;param-name&gt;type&lt;/param-name&gt;
              &lt;param-value&gt;identity-keystore&lt;/param-value&gt;
            &lt;/init-param&gt;
          &lt;/init-params&gt;
        &lt;/password-provider&gt;
      &lt;/key-store&gt;
      &lt;password-provider&gt;  <span class="conum" data-value="3" />
        &lt;class-name&gt;com.oracle.coherence.guides.ssl.CustomPasswordProvider&lt;/class-name&gt;
        &lt;init-params&gt;
          &lt;init-param&gt;
            &lt;param-name&gt;type&lt;/param-name&gt;
            &lt;param-value&gt;identity-key&lt;/param-value&gt;
          &lt;/init-param&gt;
        &lt;/init-params&gt;
      &lt;/password-provider&gt;
    &lt;/identity-manager&gt;

    &lt;trust-manager&gt;
      &lt;algorithm&gt;SunX509&lt;/algorithm&gt;
      &lt;key-store&gt;  <span class="conum" data-value="4" />
        &lt;url system-property="test.trust.keystore"&gt;file:trust.jks&lt;/url&gt;
        &lt;password-provider&gt;  <span class="conum" data-value="5" />
          &lt;class-name&gt;com.oracle.coherence.guides.ssl.CustomPasswordProvider&lt;/class-name&gt;
          &lt;init-params&gt;
            &lt;init-param&gt;
              &lt;param-name&gt;type&lt;/param-name&gt;
              &lt;param-value&gt;trust-keystore&lt;/param-value&gt;
            &lt;/init-param&gt;
          &lt;/init-params&gt;
        &lt;/password-provider&gt;
      &lt;/key-store&gt;
    &lt;/trust-manager&gt;
    &lt;socket-provider&gt;tcp&lt;/socket-provider&gt;
  &lt;/ssl&gt;
&lt;/socket-provider&gt;</markup>

<ul class="colist">
<li data-value="1">Identity manager using Java key store</li>
<li data-value="2">Identity manager key store password using custom <a id="" title="" target="_blank" href="https://docs.coherence.community/coherence/docs/25.03/api//com/tangosol/net/PasswordProvider.html">PasswordProvider</a> implemenation</li>
<li data-value="3">Identity private key password using custom <a id="" title="" target="_blank" href="https://docs.coherence.community/coherence/docs/25.03/api//com/tangosol/net/PasswordProvider.html">PasswordProvider</a> implementation</li>
<li data-value="4">Trust manager using Java key store</li>
<li data-value="5">Trust manager key store password using custom <a id="" title="" target="_blank" href="https://docs.coherence.community/coherence/docs/25.03/api//com/tangosol/net/PasswordProvider.html">PasswordProvider</a> implementation</li>
</ul>
</li>
<li>
<p><code>sslKeyAndCert</code> - configure SSL socket provider using key and certificate files only</p>
<markup
lang="xml"

>&lt;socket-provider id="sslKeyAndCert"&gt;
  &lt;ssl&gt;
    &lt;identity-manager&gt;  <span class="conum" data-value="1" />
      &lt;key system-property="test.server.key"/&gt;
      &lt;cert system-property="test.server.cert"/&gt;
    &lt;/identity-manager&gt;
    &lt;trust-manager&gt;  <span class="conum" data-value="2" />
      &lt;cert system-property="test.server.ca.cert"/&gt;
      &lt;cert system-property="test.client.ca.cert"/&gt;
    &lt;/trust-manager&gt;
    <span class="conum" data-value="3" />
    &lt;!--
        &lt;refresh-period&gt;24h&lt;/refresh-period&gt;
    --&gt;
  &lt;/ssl&gt;
&lt;/socket-provider&gt;</markup>

<ul class="colist">
<li data-value="1">Identity manager using key and certificate directly</li>
<li data-value="2">Trust manager using key and certificate directly</li>
<li data-value="3">Optional refresh period for keys and certificates</li>
</ul>
</li>
<li>
<p><code>sslCustomKeyAndCert</code> - configure SSL socket provider using custom private key and certificate loaders</p>
<markup
lang="xml"

>&lt;socket-provider id="sslCustomKeyAndCert"&gt;
  &lt;ssl&gt;
    &lt;identity-manager&gt;
      &lt;key-loader&gt;  <span class="conum" data-value="1" />
        &lt;class-name&gt;com.oracle.coherence.guides.ssl.loaders.CustomPrivateKeyLoader&lt;/class-name&gt;
        &lt;init-params&gt;
          &lt;init-param&gt;
            &lt;param-type&gt;string&lt;/param-type&gt;
            &lt;param-value system-property="test.server.key"/&gt;
          &lt;/init-param&gt;
        &lt;/init-params&gt;
      &lt;/key-loader&gt;
      &lt;cert-loader&gt;  <span class="conum" data-value="2" />
        &lt;class-name&gt;com.oracle.coherence.guides.ssl.loaders.CustomCertificateLoader&lt;/class-name&gt;
        &lt;init-params&gt;
          &lt;init-param&gt;
            &lt;param-type&gt;string&lt;/param-type&gt;
            &lt;param-value system-property="test.server.cert"/&gt;
          &lt;/init-param&gt;
        &lt;/init-params&gt;
      &lt;/cert-loader&gt;
    &lt;/identity-manager&gt;
    &lt;trust-manager&gt;
      &lt;cert-loader&gt;  <span class="conum" data-value="3" />
        &lt;class-name&gt;com.oracle.coherence.guides.ssl.loaders.CustomCertificateLoader&lt;/class-name&gt;
        &lt;init-params&gt;
          &lt;init-param&gt;
            &lt;param-type&gt;string&lt;/param-type&gt;
            &lt;param-value system-property="test.server.ca.cert"/&gt;
          &lt;/init-param&gt;
        &lt;/init-params&gt;
      &lt;/cert-loader&gt;
    &lt;/trust-manager&gt;
  &lt;/ssl&gt;
&lt;/socket-provider&gt;</markup>

<ul class="colist">
<li data-value="1">Identity manager using custom private key loader</li>
<li data-value="2">Identity manager using custom certificate key loader</li>
<li data-value="3">Trust manager using custom certificate key loader</li>
</ul>
</li>
<li>
<p><code>sslCustomKeyStore</code> - configure SSL socket provider using a custom key store loader</p>
<markup
lang="xml"

>&lt;socket-provider id="sslCustomKeyStore"&gt;
  &lt;ssl&gt;
    &lt;identity-manager&gt;
      &lt;key-store&gt;
        &lt;key-store-loader&gt;  <span class="conum" data-value="1" />
          &lt;class-name&gt;com.oracle.coherence.guides.ssl.loaders.CustomKeyStoreLoader&lt;/class-name&gt;
          &lt;init-params&gt;
            &lt;init-param&gt;
              &lt;param-type&gt;string&lt;/param-type&gt;
              &lt;param-value system-property="test.server.keystore"&gt;file:client.jks&lt;/param-value&gt;
            &lt;/init-param&gt;
          &lt;/init-params&gt;
        &lt;/key-store-loader&gt;
        &lt;password system-property="test.server.keystore.password"&gt;password&lt;/password&gt;
      &lt;/key-store&gt;
      &lt;password system-property="test.server.key.password"&gt;private&lt;/password&gt;
    &lt;/identity-manager&gt;
    &lt;trust-manager&gt;
      &lt;algorithm&gt;SunX509&lt;/algorithm&gt;
      &lt;key-store&gt;
        &lt;key-store-loader&gt;  <span class="conum" data-value="2" />
          &lt;class-name&gt;com.oracle.coherence.guides.ssl.loaders.CustomKeyStoreLoader&lt;/class-name&gt;
          &lt;init-params&gt;
            &lt;init-param&gt;
              &lt;param-type&gt;string&lt;/param-type&gt;
              &lt;param-value system-property="test.trust.keystore"&gt;file:trust.jks&lt;/param-value&gt;
            &lt;/init-param&gt;
          &lt;/init-params&gt;
        &lt;/key-store-loader&gt;
        &lt;password system-property="test.trust.keystore.password"&gt;password&lt;/password&gt;
      &lt;/key-store&gt;
    &lt;/trust-manager&gt;
  &lt;/ssl&gt;
&lt;/socket-provider&gt;</markup>

<ul class="colist">
<li data-value="1">Identity manager using custom key store loader</li>
<li data-value="2">Identity manager using custom key store loader</li>
</ul>
</li>
</ul>
</div>
</div>

<h3 id="example-tests-1">Review the Test Classes</h3>
<div class="section">
<p>The example code comprises the following classes, which are explained below:</p>

<ul class="ulist">
<li>
<p><code>AbstractSSLExampleTest</code> - abstract test implementation to create SSL configuration files and startup cluster using the required socket provider</p>

</li>
<li>
<p><code>KeyStoreSSLExampleTest</code> -  test with socket provider using Java key-store</p>

</li>
<li>
<p><code>KeyStoreAndCertSSLExampleTest</code> - test with socket provider using key and certificate files only</p>

</li>
<li>
<p><code>CustomKeyStoreSSLExampleTest</code> - test with socket provider using a custom private key and certificate loaders</p>

</li>
<li>
<p><code>CustomCertificateLoader</code> - a custom certificate loader class</p>

</li>
<li>
<p><code>CustomKeyStoreLoader</code> - a custom key store loader class</p>

</li>
<li>
<p><code>CustomPrivateKeyLoader</code> - a custom private key loader class</p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">The tests use the Oracle Bedrock <code>KeyTool</code> utility to generate the require keys, stores and certificates.
You should use your own generated artefacts and not use these for production usage.</p>
</div>
<ol style="margin-left: 15px;">
<li>
Review the <code>AbstractSSLExampleTest</code> class
<p>This abstract class contains various utilities used by all tests. A few snippets are included below:</p>

<p><strong>Generate Test Certificates</strong></p>

<markup
lang="java"

>/**
 * Create the required certificates.
 *
 * @throws Exception if any errors creating certificates.
 */
@BeforeAll
public static void setupSSL() throws Exception {
    // only initialize once
    if (tmpDir == null) {
        KeyTool.assertCanCreateKeys();

        tmpDir = FileHelper.createTempDir();

        serverCACert = KeyTool.createCACert(tmpDir, "server-ca", "PKCS12");
        serverKeyAndCert = KeyTool.createKeyCertPair(tmpDir, serverCACert, "server");
        clientCACert = KeyTool.createCACert(tmpDir, "client-ca", "PKCS12");
        clientKeyAndCert = KeyTool.createKeyCertPair(tmpDir, clientCACert, "client");
    }
}</markup>

<p><strong>Set Cache Server Options</strong></p>

<markup
lang="java"

>/**
 * Create options to start cache servers.
 *
 * @param clusterPort     cluster port
 * @param proxyPort       proxy port
 * @param socketProvider  socket provider to use
 * @param memberName      member name
 *
 * @return new {@link OptionsByType}
 */
protected static OptionsByType createCacheServerOptions(int clusterPort, int proxyPort, String socketProvider, String memberName) {
    OptionsByType optionsByType = OptionsByType.empty();

    optionsByType.addAll(JMXManagementMode.ALL,
            JmxProfile.enabled(),
            LocalStorage.enabled(),
            WellKnownAddress.of(hostName),
            Multicast.ttl(0),
            CacheConfig.of(SERVER_CACHE_CONFIG),
            OperationalOverride.of(OVERRIDE),
            Logging.at(6),
            ClusterName.of("ssl-cluster"),
            MemberName.of(memberName),
            SystemProperty.of("test.socket.provider", socketProvider),
            SystemProperty.of("test.server.keystore", serverKeyAndCert.getKeystoreURI()),
            SystemProperty.of("test.trust.keystore", serverCACert.getKeystoreURI()),
            SystemProperty.of("test.server.keystore.password", serverKeyAndCert.storePasswordString()),
            SystemProperty.of("test.server.key.password", serverKeyAndCert.keyPasswordString()),
            SystemProperty.of("test.trust.keystore.password", serverCACert.storePasswordString()),

            SystemProperty.of("test.client.ca.cert", clientCACert.getCertURI()),
            SystemProperty.of("test.server.key", serverKeyAndCert.getKeyPEMNoPassURI()),
            SystemProperty.of("test.server.cert", serverKeyAndCert.getCertURI()),
            SystemProperty.of("test.server.ca.cert", serverCACert.getCertURI()),

            ClusterPort.of(clusterPort));

    // enable proxy server if a proxy port is not -1
    if (proxyPort != -1) {
        optionsByType.addAll(SystemProperty.of("test.extend.address", hostName),
                SystemProperty.of("test.extend.port", proxyPort),
                SystemProperty.of("test.proxy.enabled", "true")
        );
    }

    return optionsByType;
}</markup>

<p><strong>Run the Simple Test</strong></p>

<markup
lang="java"

>/**
 * Run a simple test using Coherence*Extend with the given socket-provider to validate
 * that SSL communications for the cluster and proxy are working.
 *
 * @param socketProvider socket provider to use
 */
protected void runTest(String socketProvider) {
    _startup(socketProvider);

    NamedCache&lt;Integer, String&gt; cache = getCache(socketProvider);
    cache.clear();
    cache.put(1, "one");
    assertEquals("one", cache.get(1));
}</markup>

</li>
<li>
Review the <code>KeyStoreSSLExampleTest</code> class which tests with a socket provider using Java key-store
<markup
lang="java"

>/**
 * Test SSL using Java key-store and trust-store.
 *
 * @author Tim Middleton 2022.06.15
 */
public class KeyStoreSSLExampleTest
        extends AbstractSSLExampleTest {

    @Test
    public void testKeyStoreSocketProvider() {
        runTest("sslKeyStore");  <span class="conum" data-value="1" />
    }
}</markup>

<ul class="colist">
<li data-value="1">Specify the SSL socket provider</li>
</ul>
</li>
<li>
Review the <code>KeyStoreAndCertSSLExampleTest</code> class which tests with socket provider using key and certificate files only
<markup
lang="java"

>/**
 * Test SSL using Key and Certificate.
 *
 * @author Tim Middleton 2022.06.15
 */
public class KeyStoreAndCertSSLExampleTest
        extends AbstractSSLExampleTest {

    @Test
    public void testKeyAndCertSocketProvider() throws Exception {
        runTest("sslKeyAndCert");   <span class="conum" data-value="1" />
    }
}</markup>

<ul class="colist">
<li data-value="1">Specify the SSL socket provider</li>
</ul>
</li>
<li>
Review the <code>CustomKeyStoreSSLExampleTest</code> class which tests with socket provider using a custom private key and certificate loader
<markup
lang="java"

>/**
 * Test SSL using custom key-store loader.
 *
 * @author Tim Middleton 2022.06.16
 */
public class CustomKeyStoreSSLExampleTest
        extends AbstractSSLExampleTest {

    @Test
    public void testCustomKeyStoreSocketProvider() {
        runTest("sslCustomKeyStore");
    }
}</markup>

<ul class="colist">
<li data-value="1">Specify the SSL socket provider</li>
</ul>
</li>
<li>
Review the <code>CustomKeyAndCertSSLExampleTest</code> class which tests with socket provider using a custom key store loader
<markup
lang="java"

>/**
 * Test SSL using custom key and certificate loader.
 *
 * @author Tim Middleton 2022.06.16
 */
public class CustomKeyAndCertSSLExampleTest
        extends AbstractSSLExampleTest {

    @Test
    public void testCustomKeyAndCertSocketProvider() {
        runTest("sslCustomKeyAndCert");  <span class="conum" data-value="1" />
    }
}</markup>

<ul class="colist">
<li data-value="1">Specify the SSL socket provider</li>
</ul>
</li>
<li>
Review the <code>CustomCertificateLoader</code>
<markup
lang="java"

>/**
 * An example implementation of a {@link CertificateLoader} which loads a certificate from a file.
 *
 * @author Tim Middleton 2022.06.16
 */
public class CustomCertificateLoader
        extends AbstractCertificateLoader
    {
    public CustomCertificateLoader(String url)
        {
        super(url);
        }

    @Override
    protected InputStream getInputStream() throws IOException
        {
        try
            {
            return Resources.findInputStream(m_sName);
            }
        catch (IOException e)
            {
            throw new IOException(e);
            }
        }
    }</markup>

</li>
<li>
Review the <code>CustomKeyStoreLoader</code>
<markup
lang="java"

>/**
 * An example implementation of a {@link KeyStoreLoader} which loads a key store from a file.
 *
 * @author Tim Middleton 2022.06.16
 */
public class CustomKeyStoreLoader
        extends AbstractKeyStoreLoader
    {
    public CustomKeyStoreLoader(String url)
        {
        super(url);
        }

    @Override
    protected InputStream getInputStream() throws IOException
        {
        try
            {
            return Resources.findInputStream(m_sName);
            }
        catch (IOException e)
            {
            throw new IOException(e);
            }
        }
    }</markup>

</li>
<li>
Review the <code>CustomPrivateKeyLoader</code>
<markup
lang="java"

>/**
 * An example implementation of a {@link PrivateKeyLoader} which loads a private key from a file.
 *
 * @author Tim Middleton 2022.06.16
 */
public class CustomPrivateKeyLoader
        extends AbstractPrivateKeyLoader
    {
    public CustomPrivateKeyLoader(String url)
        {
        super(url);
        }

    @Override
    protected InputStream getInputStream() throws IOException
        {
        try
            {
            return Resources.findInputStream(m_sName);
            }
        catch (IOException e)
            {
            throw new IOException(e);
            }
        }
    }</markup>

</li>
</ol>
</div>

<h3 id="run-example-1">Run the Examples</h3>
<div class="section">
<p>Run the examples using the test case below.</p>

<ol style="margin-left: 15px;">
<li>
Run directly from your IDE by running either of the following test classes in the <code>com.oracle.coherence.guides.ssl</code> package.

</li>
<li>
Run using Maven or Gradle
<p>E.g. for Maven use:</p>

<markup
lang="bash"

>./mvnw clean verify</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean test</markup>

</li>
</ol>
<p>When the test has run you will see output from the various parts of the test code. See below for some
key items to look out for.</p>

<ol style="margin-left: 15px;">
<li>
Messages indicating the cluster is using SSL socket provider
<markup
lang="bash"

>TCMP bound to /127.0.0.1:51684 using TCPDatagramSocketProvider[Delegate: SSLSocketProvider(SSLSocketProvider())]</markup>

</li>
<li>
Cluster members connecting using <code>tmbs</code> (TCP Message bus over SSL)
<markup
lang="bash"

>tmbs://127.0.0.1:52311.51395 opening connection with tmbs://127.0.0.1:52315.47215 using
SSLSocket(null /127.0.0.1:866404240, buffered{clear=0 encrypted=0 out=0},
handshake=NOT_HANDSHAKING, jobs=0</markup>

</li>
<li>
Cluster musing two way (key and trust stores) for communication
<markup
lang="bash"

> instantiated SSLSocketProviderDependencies: SSLSocketProvider(auth=two-way, identity=SunX509/.../examples/guides/210-ssl/target/test-classes/certs/server.jks,
trust=SunX509//.../examples/guides/210-ssl/target/test-classes/certs/server-ca-ca.jks)</markup>

</li>
</ol>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this guide you learned how to secure Coherence communication between cluster members as well as Coherence*Extend clients.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/secure/introduction-oracle-coherence-security.html">Introduction to Coherence Security</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/secure/using-ssl-secure-communication.html">Using SSL to Secure Communication</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
