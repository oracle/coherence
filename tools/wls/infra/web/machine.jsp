<%@page import="
java.io.File,
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
java.util.Vector
" %>
<%!
    String nowUTC() {
	Date today = new Date();
	SimpleDateFormat df;
	df = new SimpleDateFormat("EEE MMM'&nbsp;'d'&nbsp;'HH:mm:ss'&nbsp;'zzz");
	df.setTimeZone(TimeZone.getTimeZone("UTC"));
	return df.format(today);
    }

    String nowUTCtstamp() {
	return nowUTCtstamp(new Date());
    }

    String nowUTCtstamp(Date the_date) {
	SimpleDateFormat df;
	df = new SimpleDateFormat("yyyyMMdd");
	df.setTimeZone(TimeZone.getTimeZone("UTC"));
	return df.format(the_date);
    }

    String nowUTCtstamp(String base, int day_difference) {
	int year = Integer.parseInt(base.substring(0, 4));
	int month = Integer.parseInt(base.substring(4, 6));
	int day = Integer.parseInt(base.substring(6));
	
	Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	cal.set(year, (month - 1), day); 
	long foobar = cal.getTimeInMillis();
	foobar += day_difference * 24 * 60 * 60 * 1000;
	return nowUTCtstamp(new Date(foobar));
    }

%>
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

String STATUS = ROOT + "status";
String ARCHIVE = ROOT + "archive.status";

String filter = request.getParameter("filter");
if (filter == null) filter = "";
String format = request.getParameter("format");
if (format == null) format = "html";

File statusdir = new File(STATUS);
if(! statusdir.isDirectory()) {
    out.print("<br><b>That isn't a directory!</b><br>");
}
String machine = request.getParameter("machine");

String tstamp = request.getParameter("tstamp");
if (tstamp == null) tstamp = nowUTCtstamp();

if (machine != null) {
    String filename = STATUS + "/machine." + tstamp + "." + machine;
    if (! (new File(filename).exists())) {
	filename = ARCHIVE + "/machine." + tstamp + "." + machine;
    }

    StringBuffer chars = null;
    String cmd = "";
    StringBuffer log = null;

    Pattern linepat;
    Matcher linemat;

    linepat = Pattern.compile("(.*: )(job\\.[0-9\\.]*)(: .*)");

    if (new File(filename).exists()) {
	BufferedReader job = new BufferedReader(new FileReader(filename));
	String line = job.readLine();
	while (line != null) {
	    if (log == null) {
		log = new StringBuffer();
	    } else {
		log.append("<br>");
	    }
	    if (line.startsWith("# ")) {
		line = line.substring(2);
	    }
	    linemat = linepat.matcher(line);
	    if (linemat.find()) {
		line = linemat.group(1) + "<a href=\"job.jsp?id=" +
		    linemat.group(2) + "\">" + linemat.group(2) + "</a>" +
		    linemat.group(3);
	    }
	    log.append(line);
	    line = job.readLine();
	}
	job.close();
    }
	if (format.equals("html")) {
%>

<html>
<head>
<title>Machine <%= machine %> </title>
</head>
<body>

<%
	}
	String headtail = "";
	headtail += "<a href=\"machine.jsp?machine=" + machine + "&tstamp=" + nowUTCtstamp(tstamp, -1) + "\">One Day Back</a>";
	headtail += "&nbsp;&nbsp;&nbsp;&nbsp;";
	headtail += "<b>Current Time: " + nowUTC() + " </b>";
	headtail += "&nbsp;&nbsp;&nbsp;&nbsp;";
	headtail += "<a href=\"machine.jsp?machine=" + machine + "&tstamp=" + nowUTCtstamp(tstamp, 1) + "\">One Day Ahead</a>";
	out.print(headtail);

	if (log != null) {
%>

<pre><%= log %></pre>

<%
     } else {
	 out.print("<h3>Can not find machine timestamp " + tstamp + "</h3>");
     }
     out.print(headtail);
     if (format.equals("html")) {
%>
</body>
</html>
    <%
	}
} else {
    out.print("<br><b>No machine?!</b><br>");
}
%>
