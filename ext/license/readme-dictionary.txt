The processor-dictionary.xml file is shipped as part of tangosol.jar and allows Coherence to calculate
the number of physical CPU sockets per machine.  The dictionary contains a number of CPU templates which
are matched against the the machines CPU descriptor's at runtime.  If a CPU descriptor does not match
a template in the dictionary it is assumed to be single core (unless the descriptor itself includes a
core count).

For a descriptor to match a template it must have match all elements included in the CPU template,
except for <execution-units> and <signature>.  Note the descriptor will generally have more elements
then the template.  By default matching is simple string equality, but if the template elemenent
specifies an attribute of regex="true", then java.util.Pattern matching is used.

There is also a special provision for CPUs which are both multi-core and optionally multi-threaded.
For such CPUs the execution-units value should be cores + threads, and the element should have a
thread-count attribute which identifies how many of cores are threads (i.e. virtual cores).

If a descriptor matches multiple templates the template with the most matches will be used.

If a customer runs into a case where their multi-core processor does not match any template they can
send us the descriptor output so that we can either update the dictionary.  The newly signed template
can be merged into their existing dictionary file.

The offical dictionary should only contain templates which we know to be valid, and they should avoid
being overly specific (for instance there is generally no need to include CPU revision numbers).

If a customer's descriptor does not include any information which can be used to generate a generic
template for the offical dictionary, a customer specific template may be created by includeing a
<coherence-uid> template element.  This template will only be useable in conjunction with that
customer's license.  This element must still be signed.

Each dictionary template must be signed in order to be considered a valid dictionary element.  The
com.xtangosol.license.ProcessorEncoder can be used to sign/re-sign the file.

The CPU descriptor for a machine may be obtained by running com.tangosol.license.ProcessorInfo.  This
tool also supports analysis of an saved descriptor from another machine by suppliying the descriptor
filename as the first argument to the tool.


See the following links for usefull information on expanding/maintaining the dictionary:

AMD
http://www.amd.com/us-en/assets/content_type/white_papers_and_tech_docs/25759.pdf

Intel
http://download.intel.com/design/Xeon/applnots/24161831.pdf
http://www.intel.com/products/processor_number/proc_info_table.pdf
Note: Unfortunately Intel family/model numbers aren't always enough to differentiate CPUs with
      different core counts.  In this case the CPU flags are needed, but this is not available
      on Windows.
