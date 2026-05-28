<%@page import="
java.io.File,
java.io.FileWriter,
java.io.BufferedReader,
java.text.SimpleDateFormat,
java.util.Calendar,
java.util.Collections,
java.util.Date,
java.util.Enumeration,
java.util.Hashtable,
java.util.StringTokenizer,
java.util.TimeZone,
java.util.regex.Matcher,
java.util.regex.Pattern,
java.sql.*,
java.util.Vector,
java.util.*
" %>
<%!
Connection conn  = null;
String id = null;
String jdbcClass = "com.mysql.jdbc.Driver";
String jdbcURL   = "jdbc:mysql://tamarac.us.oracle.com/infra_data";

public Connection getCon() {
  try {
    Properties prop = new Properties();
    prop.put("user", "readonly");
    prop.put("password", "onlyread");

    Driver myDriver = (Driver) Class.forName(jdbcClass).newInstance();
    conn = myDriver.connect(jdbcURL, prop);

  } catch (Exception e) {
        System.out.println(e);
  }
  return conn;
}
// replace a few likely SQL special characters that could cause security
// holes in values passed in via PUT or GET
String protectSQL(String value) {
    String returnValue = value;
    if (value != null) {
        returnValue = returnValue.replace(';', '_');
        //  returnValue = returnValue.replace(' ', '_');
        returnValue = returnValue.replace('\'', '_');
        returnValue = returnValue.replace('"', '_');
        returnValue = returnValue.replace('*', '_');
        returnValue = returnValue.replace('/', '_');
        returnValue = returnValue.replaceAll("\\.\\.", "_");
    }
    return returnValue;
}
%>
<%
String branch = "";
String branch_base = "";
String masterid = "";
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

String STATUS = ROOT + "status";
String ARCHIVE = ROOT + "archive.status";

File queueddir = new File(QUEUED);
if(! queueddir.isDirectory()) {
    out.print("<br><b>That isn't a directory!</b><br>");
}
String jobid = request.getParameter("id");
if (jobid != null) {
    Pattern idpat = Pattern.compile("^(job.[^-]*).*");
    Matcher idmat = idpat.matcher(jobid);
    if (idmat.find()) {
        masterid = idmat.group(1);
    }
    else {
        out.print("<br><b>can not get master id</b><br>");
    }
}
else {
    out.print("<br><b>No jobid?!</b><br>");
}
if (masterid != null) {
    String sqlStmt = "select id from rjob where id like '" + masterid + "%' and status != 'done'";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        conn = getCon();
        stmt = conn.createStatement();
        stmt.execute(sqlStmt);
        rs = stmt.getResultSet();
        while (rs.next()) {
            id = rs.getString(1);
            String filename = QUEUED + "/kill." + id;
            FileWriter killer = new FileWriter(filename);
            // need to do this to force the file to be created
            killer.write("");
            killer.close();
        }
    } catch (Exception e) {
        System.out.println(e);
    } finally {
        rs.close();
        stmt.close();
        conn.close();
    }
}
%>
<html>
<head>
<title>Kill Job <%= jobid %> and related jobs</title>
</head>
<body>
<h1> <%= jobid %> and its related jobs have been marked to be killed. </h1>
<p> It make take up to 60 seconds for it to actually be killed. </p>
<p> If it is <b>queued</b> but not running then it will just disappear without a trace. </p>
<p> If it is <b>running</b> on a machine then it will be killed and it will be marked as <i>failed</i> in the job status.  </p>
<p> If you have any doubt that the job has been killed it does not hurt to kill it multiple times. </p>
<p> <a href="job.jsp?id=<%= jobid %>"/>Job Status</a> <a href="remote.jsp"/>Remote Queue Status</a> </p>
</body>
</html>
