<doc-view>

<h2 id="_java_development_kit_requirements">Java Development Kit Requirements</h2>
<div class="section">
<p>Coherence CE 14.1.2.0.0 requires a minimum of version
17 of the Java Development Kit (JDK).</p>


<h3 id="_concerning_java_platform_module_jpms_options">Concerning Java Platform Module (JPMS) Options</h3>
<div class="section">
<p>JPMS JDK command line options such as <strong>--add-opens</strong>, <strong>--add-exports</strong> and <strong>--add-reads</strong> of standard JDK modules to <strong>com.oracle.coherence</strong> module documented in Section <strong>Using Java Modules to Build a Coherence Application</strong> of Coherence commercial release 14.1.1.2206 are no longer required.</p>

<p>A new JPMS requirement is an application module containing Coherence remote lambda(s) must open itself to module <strong>com.oracle.coherence</strong> so the remote lambda(s) can be resolved to the application&#8217;s lambda(s) during deserialization.</p>

</div>
</div>
</doc-view>
