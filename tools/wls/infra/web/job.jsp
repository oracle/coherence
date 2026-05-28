<%@page import="
java.io.BufferedReader,
java.io.BufferedWriter,
java.io.File,
java.io.FileReader,
java.io.FileWriter,
java.io.InputStream,
java.io.InputStreamReader,
java.io.PrintWriter,
java.sql.*,
java.text.SimpleDateFormat,
java.text.ParsePosition,
java.util.Calendar,
java.util.Collections,
java.util.Date,
java.util.Enumeration,
java.util.Hashtable,
java.util.LinkedList,
java.util.List,
java.util.Locale,
java.util.Properties,
java.util.Random,
java.util.StringTokenizer,
java.util.TimeZone,
java.util.Vector,
java.util.regex.Matcher,
java.util.regex.Pattern
" %>
<%!

    Connection conn  = null;
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

    String protectSQL(String value) {
		String returnValue = value;
		if (value != null) {
		    returnValue = returnValue.replace(';', '_');
		    returnValue = returnValue.replace('\'', '_');
		    returnValue = returnValue.replace('"', '_');
		    returnValue = returnValue.replace('*', '_');
		    returnValue = returnValue.replace('/', '_');
		    returnValue = returnValue.replaceAll("\\.\\.", "_");
			}
		return returnValue;
    }
    
// formatDuration should be the same as in remote.jsp
String formatDuration(Date start) {
    Date endDate = new Date();

    // FYI: Date() is returned in UTC but all the other dates from mysql are PT so I need to get it to PT for Duration to work
    // what this does is get a Date() which is UTC and converts it to pacific time
    // format it as pacific time into String ds, then parse it out as UTC
    SimpleDateFormat df = new SimpleDateFormat();
    df.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
    String ds = df.format(endDate);
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    endDate = df.parse(ds, new ParsePosition(0));

    return formatDuration(start, endDate);
}

String formatDuration(Date start, Date end) {
    return formatDuration(start, end, false);
}

	String formatDuration(Date start, Date end, boolean textonly) {
	    final long ONE_SECOND = 1000;
	    final long ONE_MINUTE = (ONE_SECOND * 60);
	    final long ONE_HOUR = (ONE_MINUTE * 60);
	    final long ONE_DAY = (ONE_HOUR * 24);
	    StringBuffer returnval = new StringBuffer();
	    String color = "";
	    
	    if ((start == null) || (end == null)) {
		return "unknown";
	    }
	    
	    final long totalDuration = end.getTime() - start.getTime();
	    long duration = totalDuration;
	    long sign = 1;
	    
	    if (duration < 0) {
		sign = -1;
		duration = - duration;
	    }
	    long days = duration / ONE_DAY;
	    if (days > 0) {
		returnval.append(days).append("d");
		duration -= (days * ONE_DAY);
	    }
	    long hours = duration / ONE_HOUR;
	    if (hours > 0) {
	        if (returnval.length() > 0) {
		    returnval.append(" ");
		}
		returnval.append(hours).append("h");
		duration -= (hours * ONE_HOUR);
	    }
	    long minutes = duration / ONE_MINUTE;
	    if (minutes > 0) {
	        if (returnval.length() > 0) {
		    returnval.append(" ");
		}
		returnval.append(minutes).append("m");
		duration -= (minutes * ONE_MINUTE);
	    }
	    long seconds = duration / ONE_SECOND;
	    if (seconds > 0 && returnval.length() == 0) {
		returnval.append(seconds).append("s");
	    }
	    if (sign == -1) {
		returnval.insert(0, "-");
	    }
	    if (! textonly) {
		if (totalDuration >= ONE_HOUR) {
		    returnval.insert(0, "<b>");
		    returnval.append("</b>");
		} else if (totalDuration <= (10 * ONE_MINUTE)) {
		    returnval.insert(0, "<i>");
		    returnval.append("</i>");
		}
	    }
	    
	    return new String(returnval);
	}

    String getdesc(String sqlStmt) {
        String desc = "";
        try {
            conn = getCon();
            Statement stmt = conn.createStatement();
            stmt.execute(sqlStmt);
            ResultSet rs = stmt.getResultSet();
            rs.next();
            desc = rs.getString(1);
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return desc;
    }

    String shortencmd(String cmd) {
	String shortcmd = null;
	Pattern cmdpat;
	Matcher cmdmat;

	if (shortcmd == null) {
	    cmdpat = Pattern.compile(" ?s?h? ?.?/?infra/test/testrel.sh [^ ]+ [^ ]+ CONFIG=([^ ]*_2nd\\.cfg)");
	    cmdmat = cmdpat.matcher(cmd);
	    if (cmdmat.find()) shortcmd = "2NDARY " + cmdmat.group(1);
	}

	if (shortcmd == null) {
	    cmdpat = Pattern.compile(" ?s?h? ?.?/?infra/test/testrel.sh [^ ]+ [^ ]+ CONFIG=([^ ]*\\.cfg)");
	    cmdmat = cmdpat.matcher(cmd);
	    if (cmdmat.find()) shortcmd = "NIGHTLY " + cmdmat.group(1);
	}

	if (shortcmd == null) {
	    cmdpat = Pattern.compile(" ?s?h? ?.?/?infra/test/abl2.sh [^ ]+ [^ ]+ ([^ ]*) ([^ ]*)");
	    cmdmat = cmdpat.matcher(cmd);
	    if (cmdmat.find()) shortcmd = "ABL2 " + cmdmat.group(1) + " " + cmdmat.group(2);
	}

	if (shortcmd == null) {
	    cmdpat = Pattern.compile(" ?s?h? ?.?/?infra/test/remotetest.sh ([^ ]*) ([^ ]*) job\\.");
	    cmdmat = cmdpat.matcher(cmd);
	    if (cmdmat.find()) shortcmd = "REMOTE " + cmdmat.group(1) + " " + cmdmat.group(2);
	}

	if (shortcmd == null) {
	    cmd = new StringTokenizer(cmd, ">").nextToken();
	    if (cmd.startsWith(" ")) cmd = cmd.substring(1);
	    if (cmd.startsWith("sh ./infra/test/")) {
		cmd = cmd.substring(16);
	    }
	    if (cmd.startsWith("infra/test/")) {
		cmd = cmd.substring(11);
	    }
	    if (cmd.startsWith("remotetest")) {
		cmd = cmd.replaceAll("\\b(JV|SUBMIT|SYNCTO|INTEGFROM|SPIN|PARALLEL|YIM|PUBLISH|MAILTO|RTARGETS|REVERT|METADATA|BAREMETAL|METATIME|REMOTETESTENVSYNCTO|JV4TEST|WLTEST|ENCODING|SECURITY|FUNCTEST|STAGE_NAME)=(\\s|$)", " ");
	    }
	    return cmd;
	} else { 
	    return shortcmd;
	}
    }

    String nowUTC() {
	Date today = new Date();
	SimpleDateFormat df;
	df = new SimpleDateFormat("EEE MMM'&nbsp;'d'&nbsp;'HH:mm:ss'&nbsp;'zzz");
	df.setTimeZone(TimeZone.getTimeZone("UTC"));
	return df.format(today);
    }
    
        class Job {
	private String ROOT;
	private String AVAILABLE = ROOT + "available";
	private String QUEUED = ROOT + "queued";
	private String STATUS = ROOT + "status";
	private String ARCHIVE = ROOT + "archive.status";

	public String jobid;
	private String filename;
	private String jobresultdir;
	public StringBuffer chars = null;
	public String cmd = "";
	public String fullcmd = "";
	public String description = "";
	public StringBuffer logsummary = null;
	public StringBuffer log = null;
	public boolean jobdone = false;
	public boolean rqjob = false;
	public Job (String branch, String id) throws java.io.FileNotFoundException, java.io.IOException {
	    jobid = protectSQL(id);
	    if (jobid != null) {
		ROOT = "/mounts/centralrq_data/" + branch + "/";
		AVAILABLE = ROOT + "available";
		QUEUED = ROOT + "queued";
		STATUS = ROOT + "status";
		ARCHIVE = ROOT + "archive.status";
		filename = QUEUED + "/" + jobid;
		if (! (new File(filename).exists())) {
		    filename = STATUS + "/" + jobid;
		}
		if (! (new File(filename).exists())) {
		    filename = ARCHIVE + "/" + jobid;
		}
		String descsql = "select description from rjob where id='" + jobid + "'";
		description = getdesc(descsql);
		
		if (new File(filename).exists()) {
		    BufferedReader job = new BufferedReader(new FileReader(filename));
		    String firstline = job.readLine();
		    if (firstline != null) {
			if (firstline.startsWith("#")) {
			    StringTokenizer charstoken = new StringTokenizer(firstline, ",");
			    String characteristic = "";
			    if (charstoken.hasMoreTokens()) {
				charstoken.nextToken();
			    }
			    while (charstoken.hasMoreTokens()) {
				if (chars == null) {
				    chars = new StringBuffer();
				} else {
				    chars.append("<br>\n");
				}
				characteristic = charstoken.nextToken();
				if (characteristic.equals("SITE=san-francisco")) {
				    characteristic = "SITE=sf";
				} else if (characteristic.equals("SITE=burlington")) {
				    characteristic = "SITE=bu";
				} else if (characteristic.equals("SITE=liberty-corner")) {
				    characteristic = "SITE=lc";
				}
				chars.append(characteristic);
			    }
			    fullcmd = job.readLine();
			} else {
			    fullcmd = firstline;
			}
			cmd = shortencmd(fullcmd);
			if (cmd.startsWith("remotetest")) {
			    rqjob = true;
			}
		    }
		    Pattern interesting = Pattern.compile("^.*: (FATAL:|failed|done|INFO: Change .* submitted\\.|(INFO|WARNING): (build|test) .* (successful|passed|failed )|INFO: retry .*$|INFO: re-enqueued.*)");
		    Pattern jobpat = Pattern.compile("^(.*: INFO: )(job.[^ ]*)( started on .*| created to run .*)$");
		    Pattern jobfailed = Pattern.compile("^.*: (FATAL:|WARNING: test .* failed|WARNING: Submit flag is on, but the tests did not pass)");
		    Pattern jobsubmit = Pattern.compile("^.*: INFO: Change .* submitted\\.$");
		    Pattern jobbegin = Pattern.compile("^(.* (UTC|GMT) 20[0-9][0-9]): .*: starting$");
		    Pattern jobend   = Pattern.compile("^(.* (UTC|GMT) 20[0-9][0-9]): .*: done$");
		    Pattern jobpartbegin = Pattern.compile("^.*([A-Z][a-z][a-z] [A-Z][a-z][a-z] [0-9][0-9] [0-9][0-9]:[0-9][0-9]:[0-9][0-9] (UTC|GMT) 20[0-9][0-9]): .* INFO: (p4 resolving files\\.\\.\\.|doing p4clean\\.\\.\\.|.* starting\\.\\.\\.)");
		    Pattern jobpartend   = Pattern.compile("^.*([A-Z][a-z][a-z] [A-Z][a-z][a-z] [0-9][0-9] [0-9][0-9]:[0-9][0-9]:[0-9][0-9] (UTC|GMT) 20[0-9][0-9]): .* (WARNING: .* failed .*|INFO: .* (passed|successful))$");
		    Pattern jobresultdirline = Pattern.compile("^.* INFO:.*home\\.us\\.oracle\\.com(.*)\">Results directory.*$");
		    Matcher jobmat;
		    Date startingJob = null;
		    Date startingPart = null;
		    boolean just_handled_subjob = false;
		    String line = job.readLine();
		    while (line != null) {
			if (log == null) {
			    log = new StringBuffer();
			} else if (!just_handled_subjob) {
			    log.append("<br>\n");
			}
			just_handled_subjob = false;

			if (line.endsWith(" starting")) {
					jobdone = false;
			}
			if (line.endsWith(" done") || line.endsWith(" failed")) {
			    jobdone = true;
			}
			if (line.startsWith("# ")) {
			    line = line.substring(2);
			}
			jobmat = jobfailed.matcher(line);
			if (jobmat.find()) {
			    line="<font color=\"red\">"+line+"</font>";
			}
			if (line.indexOf(" failed") != -1) {
			    line="<font color=\"red\">"+line+"</font>";
			}
			jobmat = jobsubmit.matcher(line);
			if (jobmat.find()) {
			    line="<span style=\"background:#99FF66\"><b>"+line+"</b></span>";
			}
			jobmat = jobpat.matcher(line);
			if (jobmat.find()) {
			    line = jobmat.group(1) + "<a href=\"?id=" + jobmat.group(2) + "\">" + jobmat.group(2) + "</a>" + jobmat.group(3);
			    if (jobid != jobmat.group(2)) {
				Job subjob = new Job(branch, jobmat.group(2));
				Random rand = new Random();
				int hideme = rand.nextInt();
				line = line + " <button onclick=\"togglehide(this,'hideme" + hideme + "')\">Show DETAILS</button>";
				line = line + "\n<p id=\"hideme" + hideme + "details\" style=\"display:none; margin: 0px 40px\">";
				if (subjob.log != null) {
				   line = line + subjob.log;
				}
  				line = line + "</p>";
				line = line + "\n<p id=\"hideme" + hideme + "summary\" style=\"margin: 0px 40px\">";
				if (subjob.logsummary != null) {
				   line = line + subjob.logsummary;
				}
  				line = line + "</p>";
				just_handled_subjob = true;
			    }
			}
			jobmat = jobbegin.matcher(line);
			if (jobmat.find()) {
			    SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd kk:mm:ss zzz yyyy", Locale.ENGLISH);
			    startingPart = null;
			    try {
				Date date = format.parse(jobmat.group(1));
				startingJob = date;
			    } catch (Exception e) {
				line=line + " <font color=\"dimgray\">(" + e + ")</font>";
			    }
			}
			jobmat = jobend.matcher(line);
			if (jobmat.find()) {
			    SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd kk:mm:ss zzz yyyy", Locale.ENGLISH);
			    try {
				Date date = format.parse(jobmat.group(1));
				line=line + " <font color=\"dimgray\">(" + formatDuration(startingJob, date) + ")</font>";
			    } catch (Exception e) {
				line=line + " <font color=\"dimgray\">(" + jobmat.group(1) + ")(" + e + ")</font>";
			    }
			    startingJob = null;
			}
			jobmat = jobpartbegin.matcher(line);
			if (jobmat.find()) {
			    SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd kk:mm:ss zzz yyyy", Locale.ENGLISH);
			    startingPart = null;
			    try {
				Date date = format.parse(jobmat.group(1));
				startingPart = date;
			    } catch (Exception e) {
				line=line + " <font color=\"dimgray\">(" + e + ")</font>";
			    }
			}
			jobmat = jobpartend.matcher(line);
			if (jobmat.find()) {
			    SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd kk:mm:ss zzz yyyy", Locale.ENGLISH);
			    try {
				Date date = format.parse(jobmat.group(1));
			       	line=line + " <font color=\"dimgray\">(" + formatDuration(startingPart, date) + ")</font>";
			    } catch (Exception e) {
				line=line + " <font color=\"dimgray\">(" + jobmat.group(1) + ")(" + e + ")</font>";
			    }
			    startingPart = null;
			}
			jobmat = interesting.matcher(line);
			if (jobmat.find()) {
			    if (logsummary == null) {
				logsummary = new StringBuffer();
			    } else {
				logsummary.append("<br>\n");
			    }
			    logsummary.append(line);
			}
			
			jobmat = jobresultdirline.matcher(line);
            if (jobmat.find()) {
			  jobresultdir = "/mounts" + jobmat.group(1);
			}

			log.append(line);
			line = job.readLine();
		    }
		    job.close();
		    if (chars == null) {
			chars = new StringBuffer("<i>none</i>");
		    }
		    if (log == null) {
			log = new StringBuffer("<i>none, job still queued</i>");
		    } else {
			if (false) { // not displaying properly but this is close to what I want
			    if (startingPart != null || startingJob != null) {
				log.append("<div align=\"center\"><font color=\"dimgray\">(");
			    }
			    if (startingPart != null) {
				log.append("build/test " + formatDuration(startingPart));
			    }
			    if (startingPart != null && startingJob != null) {
				log.append(", ");
			    }
			    if (startingJob != null) {
				log.append("total " + formatDuration(startingJob));
			    }
			    if (startingPart != null || startingJob != null) {
				log.append(")</font></div>");
			    }
			}
		    }
		}
	    }
	}
    }%>
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

String AVAILABLE = ROOT + "available";
String QUEUED = ROOT + "queued";
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
    
    Job thejob = new Job(branch, request.getParameter("id"));
	if (format.equals("html")) { %>

<html>
<head>
<title>Job Status <%= thejob.jobid %> </title>
</head>
<body>
<script type="text/javascript">
function togglehide(theButton,id) {
    var details = document.getElementById(id + "details");
    var summary = document.getElementById(id + "summary");

    if (details.style.display != 'none') {
       details.style.display = 'none';
       summary.style.display = '';
       theButton.innerHTML = "Show DETAILS";
    } else {
       details.style.display = '';
       summary.style.display = 'none';
       theButton.innerHTML = "Show summary";
    }
}
</script>
<%
	}
	if (thejob.chars != null) { %>
<table>
<tr><td> </td> <td> </td> <td align=center> <a href="remote.jsp">RQ Main Page </a></td> </tr>
<tr><th valign=top align=right>Job:</th> <td> <%= thejob.jobid %> </td>
    <th valign=top align=right>Current Time:</th> <td> <%= nowUTC() %> </td>
    <td align=right> <a href="kill.jsp?id=<%= thejob.jobid %>">Click here to <b>KILL THIS JOB</b></a> </td>
<tr><th></th><td></td><th></th><td></td><td align=right> <a href="killall.jsp?id=<%= thejob.jobid %>">Click here to <b>KILL THIS JOB AND RELATED JOBS</b></a> </td>
<%
        if (thejob.description != null) {
           // I wish this protected against XSS but it is more to display descriptions that contain HTML chars
           thejob.description = thejob.description.replaceAll("&", "&amp;");
           thejob.description = thejob.description.replaceAll("\"", "&quot;");
           thejob.description = thejob.description.replaceAll("<", "&lt;");
           thejob.description = thejob.description.replaceAll(">", "&gt;");
           thejob.description = thejob.description.replaceAll("\n", "<br>");
%>
<tr><th valign=top align=right>Description:</th> <td colspan=4> <%= thejob.description %> </td>
<%
        }
%>
<tr><th valign=top align=right>Characteristics:</th> <td colspan=4> <%= thejob.chars %> </td>
<tr><th valign=top align=right>Command:</th> <td colspan=4> <%= thejob.cmd %> </td>
<tr><th valign=top align=right>Full Command:</th> <td colspan=4> <font size=-2> <%= thejob.fullcmd %> </font> </td>
<tr><th valign=top align=right>Activity:</th> <td style="word-break:break-all" colspan=4> <%= thejob.log %> </td>
</table>
<%
if (thejob.rqjob && thejob.jobdone) { %>
<hr>
<p>You can Click <a href="retry.jsp?id=<%= thejob.jobid %>">here</a> to retry the job</p>
<p>The auto-submit feature will be removed from original command</p>
<p>Please DON'T retry job repeatly</p>
<%
}
     } else {
	 out.print("<h3>Job not found!</h3>");
	 out.print("<h3>Bad URL or job was killed.</h3>");
     }
	if (format.equals("html")) { %>
</body>
</html>
    <%
	}
%>
