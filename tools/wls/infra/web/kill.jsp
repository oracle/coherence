<%@page import="java.io.File" %>
<%
String branch = "";
String branch_base = "";

String URI = request.getRequestURI();
StringTokenizer token = new StringTokenizer(URI, "/");

if (token.countTokens() == 4) {
   branch_base = "coherence/";
} else if (token.countTokens() == 5) {
   // I think this should be able to be implemented better by grabbing parts of the URI from the tokenizer and make it completely branch independent
   if (URI.indexOf("/release/") != -1) {
     branch_base = "coherence/release/";
   } else if (URI.indexOf("/coherence-ce/") != -1) {
     branch_base = "coherence/coherence-ce/";
   } else {
     branch_base = "coherence/UNKNOWN/";
   }
} else if (token.countTokens() == 6) {
   if (URI.indexOf("/release/") != -1 && URI.indexOf("/coherence-ce/") != -1) {
     branch_base = "coherence/coherence-ce/release/";
   } else {
     branch_base = "coherence/UNKNOWN/";
   }
}

while (token.countTokens() >= 2) {
    branch = branch_base + token.nextToken();
}

String ROOT1 = "\\\\centralrq.subnet3ad2phx.devweblogicphx.oraclevcn.com\\rqueue\\" + branch + "\\";
String ROOT2 = "/net/centralrq-data.subnet3ad2phx.devweblogicphx.oraclevcn.com/centralrq_data" + branch + "/";
String ROOT = "/mounts/centralrq_data/" + branch + "/";

if (new File(ROOT1).isDirectory()) {
    ROOT = ROOT1;
} else if (new File(ROOT2).isDirectory()) {
    ROOT = ROOT2;
}

String QUEUED = ROOT + "queued";

File queueddir = new File(QUEUED);
if(! queueddir.isDirectory()) {
    out.print("<br><b>That isn't a directory!</b><br>");
}
String jobid = request.getParameter("id");

if (jobid != null) {

    String filename = QUEUED + "/kill." + jobid;

    FileWriter killer = new FileWriter(filename);
    // need to do this to force the file to be created
    killer.write("");
    killer.close();
%>

<html>
<head>
<title>Kill Job <%= jobid %> </title>
</head>
<body>

<h1> <%= jobid %> has been marked to be killed. </h1>

<p> It make take up to 60 seconds for it to actually be killed. </p>

<p> If it is <b>queued</b> but not running then it will just disappear without a trace. </p>

<p> If it is <b>running</b> on a machine then it will be killed and it will be marked as <i>failed</i> in the job status.  </p>

<p> If you have any doubt that the job has been killed it does not hurt to kill it multiple times. </p>

<p> <a href="job.jsp?id=<%= jobid %>"/>Job Status</a> </p>

</body>
</html>
<%
} else {
    out.print("<br><b>No jobid?!</b><br>");
}
%>
