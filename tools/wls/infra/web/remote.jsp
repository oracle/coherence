<%@page import="
java.sql.*,
java.util.Date,
java.util.Properties,
java.util.TimeZone,
java.io.File,
java.text.SimpleDateFormat,
java.text.ParsePosition,
java.util.StringTokenizer,
" %>
<%!
Connection conn  = null;
String jdbcClass = "com.mysql.jdbc.Driver";
String jdbcURL   = "jdbc:mysql://tamarac.us.oracle.com/infra_data";
String branch	 = "";
String branch_base = "";

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

String formatDate(Date theDate) {
    Date today = new Date();
    SimpleDateFormat df;
    if ((today.getMonth() == theDate.getMonth()) && 
	(today.getDate() == theDate.getDate())) {
	df = new SimpleDateFormat("HH:mm'&nbsp;'zzz");
    } else {
	df = new SimpleDateFormat("MMM'&nbsp;'dd'&nbsp;'HH:mm'&nbsp;'zzz");
    }
    df.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
    return df.format(theDate);
}

// formatDuration should be the same as in job.jsp
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
	returnval.append(days).append("d&nbsp;");
	duration -= (days * ONE_DAY);
    }
    long hours = duration / ONE_HOUR;
    if (hours > 0) {
	returnval.append(hours).append("h&nbsp;");
	duration -= (hours * ONE_HOUR);
    }
    long minutes = duration / ONE_MINUTE;
    if (minutes > 0) {
	returnval.append(minutes).append("m&nbsp;");
	duration -= (minutes * ONE_MINUTE);
    }
    long seconds = duration / ONE_SECOND;
    if (seconds > 0 && returnval.length() == 0) {
	returnval.append(seconds).append("s&nbsp;");
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

StringBuffer outputResult(String sqlStmt) {
   return outputResult(sqlStmt, null);
}

StringBuffer outputResult(String sqlStmt, String caption) {
    StringBuffer returnValue = new StringBuffer();

    try {
	Statement stmt = conn.createStatement();
	stmt.execute(sqlStmt);
	ResultSet rs = stmt.getResultSet();
	ResultSetMetaData rsmd = rs.getMetaData();

	int colCount = rsmd.getColumnCount();
	returnValue.append("<table class=r border=4>");
	if (caption != null) {
	    returnValue.append("<caption>" + caption + "</caption>");
	}
	returnValue.append("<tr class=caption>");
	for (int i = 1; i <= colCount; i++) {
	    String name = rsmd.getColumnLabel(i);
	    if (name.equals("target+id")) {
		returnValue.append("<th class=r>Target</th>");
	    } else if (name.equals("user+")) {
		returnValue.append("<th class=r>User</th>");
	    } else if (name.equals("change+")) {
		returnValue.append("<th class=r>Change</th>");
	    } else if (name.equals("waited+") || name.equals("waited++")) {
		returnValue.append("<th class=r>Waited</th>");
	    } else if (name.equals("elapsed+")) {
		returnValue.append("<th class=r>Elapsed</th>");
	    } else if (name.equals("took+")) {
		returnValue.append("<th class=r>Took</th>");
	    } else if (name.equals("idle+")) {
		returnValue.append("<th class=r>Idle</th>");
	    } else if (name.equals("machine+")) {
		returnValue.append("<th class=r>Machine</th>");
	    } else if (!name.endsWith(",silent")) {
		returnValue.append("<th class=r>" + name + "</th>");
	    }
	}
	returnValue.append("</tr>");
	while (rs.next()) {
	    returnValue.append("<tr>");
	    for (int i = 1; i <= colCount; i++) {
		String name = rsmd.getColumnLabel(i);
		int type = rsmd.getColumnType(i);
		String value = rs.getString(i);
		if (rs.wasNull() && !name.endsWith(",silent")) {
		    returnValue.append("<td class=r></td>");
		} else if (name.equals("target+id")) {
		    returnValue.append("<td class=r><a title=\"on machine " + rs.getString("machine,silent") + "\" href=\"http://home.us.oracle.com/internal/" + branch + "/job.jsp?id=" +
			rs.getString("id,silent") + "\">" + value + "</a></td>");
		} else if (name.equals("user+")) {
		    returnValue.append("<td nowrap class=r><a href=\"?user=" +
			rs.getString("user,silent") + "\">" + value + "</a></td>");
		} else if (name.equals("change+")) {
		    returnValue.append("<td class=r><a href=\"http://perforce-coh-swarm.us.oracle.com/changes/" + value + "\">" + value + "</a></td>");
		} else if (name.equals("waited+")) {
		    returnValue.append("<td class=r>" + formatDuration(rs.getTimestamp("queued,silent")) + "</td>");
		} else if (name.equals("Description")) {
                    returnValue.append("<td nowrap class=r>" + value.
                    replaceAll("&", "&amp;").
                    replaceAll("<", "&lt;").
                    replaceAll(">", "&gt;").
                    replaceAll("\"", "&quot;").
                    replaceAll("'", "&apos;").
                    replaceAll("\\b(?i)((at|@|revert|changes|change|changelist|cl)[# ]*([0-9][0-9][0-9][0-9][0-9]+))\\b","<a href=\"http://perforce-coh-swarm.us.oracle.com/changes/$3\">$1</a>").
                    replaceAll("(#review-([0-9][0-9][0-9][0-9][0-9]+))\\b","<a href=\"http://perforce-coh-swarm.us.oracle.com/reviews/$2\">$1</a>").
                    replaceAll("\\b(?i)(bugd?b?[-:# ]*([0-9][0-9][0-9][0-9][0-9]+))\\b","<a href=\"https://bug.oraclecorp.com/pls/bug/webbug_print.show?c_rptno=$2\">$1</a>").
                    replaceAll("\\[([0-9][0-9][0-9][0-9][0-9]+)(-&gt;[0-9\\.]+\\]+)","[<a href=\"https://bug.oraclecorp.com/pls/bug/webbug_print.show?c_rptno=$1\">$1</a>$2").
                    replaceAll("\\b(?i)JRFCAF[- ]*([0-9][0-9][0-9]+)\\b","<a href=\"https://jira.oraclecorp.com/jira/browse/JRFCAF-$1\">$0</a>").
                    replaceAll("\\b(?i)OWLS[- ]*([0-9][0-9][0-9]+)\\b","<a href=\"https://jira.oraclecorp.com/jira/browse/OWLS-$1\">$0</a>").
                    replaceAll("\\b(?i)COH[- ]*([0-9][0-9][0-9]+)\\b","<a href=\"https://jira.oraclecorp.com/jira/browse/COH-$1\">$0</a>").
                    replaceAll("\\b(?i)ENH[- ]*([0-9][0-9][0-9]+)\\b","<a href=\"https://bug.oraclecorp.com/pls/bug/webbug_print.show?c_rptno=$1\">$0</a>").
                    replaceAll("\\b(?i)DOC[- ]*([0-9][0-9][0-9]+)\\b","<a href=\"https://bug.oraclecorp.com/pls/bug/webbug_print.show?c_rptno=$1\">$0</a>").
                    replaceAll("\\b(?i)CVE[- ]*([0-9][0-9][0-9]+-[0-9][0-9][0-9]+)\\b","<a href=\"https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-$1\">$0</a>") + "</td>");
		} else if (name.equals("waited++")) {
		    returnValue.append("<td class=r>" + formatDuration(rs.getTimestamp("queued,silent"), rs.getTimestamp("started,silent")) + "</td>");
		} else if (name.equals("idle+")) {
		    returnValue.append("<td class=r>" + formatDuration(rs.getTimestamp("changed,silent")) + "</td>");
		} else if (name.equals("elapsed+")) {
		    returnValue.append("<td class=r>" + formatDuration(rs.getTimestamp("started,silent")) + "</td>");
		} else if (name.equals("took+")) {
		    returnValue.append("<td class=r>" + formatDuration(rs.getTimestamp("started,silent"), rs.getTimestamp("completed")) + "</td>");
		} else if (name.equals("machine+")) {
		    returnValue.append("<td nowrap class=r><a href=\"http://home.us.oracle.com/internal/" + branch + "/machine.jsp?machine=" +
			rs.getString("machine+") + "\">" + value + "</a></td>");
		} else if (!name.endsWith(",silent")) {
		    if (type == java.sql.Types.TIMESTAMP) {
			returnValue.append("<td class=r>" + formatDate(rs.getTimestamp(i)) + "</td>");
		    } else {
			returnValue.append("<td class=r>" + value + "</td>");
		    }
		}
	    }
	    returnValue.append("</tr>");
        }
	returnValue.append("</table>");
	rs.close();
	stmt.close();
	return returnValue;
    } catch (Exception e) {
	System.out.print(e);
        return new StringBuffer("<font color=red" + returnValue + "</font><br>\n<hr><h1>ERROR " + e + "</h1>");
    }
}

int getCount(String sqlStmt) {
    int returnValue;
    try {
	Statement stmt = conn.createStatement();
	stmt.execute(sqlStmt);
	ResultSet rs = stmt.getResultSet();
	ResultSetMetaData rsmd = rs.getMetaData();
	
//	assert rsmd.getColumnCount() == 1;
		
	rs.next();
//	assert rs.isLast();
	returnValue = rs.getInt(1);
	rs.close();
	stmt.close();
    } catch (Exception e) {
	System.out.print(e);
	returnValue = -1;
    }
    return returnValue;
}

String getString(String sqlStmt) {
    String returnValue;
    try {
	Statement stmt = conn.createStatement();
	stmt.execute(sqlStmt);
	ResultSet rs = stmt.getResultSet();
	ResultSetMetaData rsmd = rs.getMetaData();
	
//	assert rsmd.getColumnCount() == 1;
		
	rs.next();
//	assert rs.isLast();
	returnValue = rs.getString(1);
	rs.close();
	stmt.close();
    } catch (Exception e) {
	System.out.print(e);
	returnValue = null;
    }
    return returnValue;
}

// replace a few likely SQL special characters that could cause security
// holes in values passed in via PUT or GET
String protectSQL(String value) {
    String returnValue = value;
    if (value != null) {
	returnValue = returnValue.replace(';', '_');
//	returnValue = returnValue.replace(' ', '_');
	returnValue = returnValue.replace('\'', '_');
	returnValue = returnValue.replace('"', '_');
	returnValue = returnValue.replace('*', '_');
    }
    return returnValue;
}

%>

<%
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

	conn = getCon();

	String idlewinsql = "from mstatus, machine where mstatus.branch = '" + branch + "' and mstatus.machine = machine.machine and status = 'available' and platform = 'ok' and ( shortos like 'W2K%' or shortos = 'XP' or shortos = 'VISTA' ) and rbt_excluded is null and rbt_rq_excluded is null and updated > date_sub(curdate(), interval 1 day)";
	String idlenotwinsql = "from mstatus, machine where mstatus.branch = '" + branch + "' and mstatus.machine = machine.machine and status = 'available' and platform = 'ok' and shortos not like 'W2K%' and shortos != 'XP' and shortos != 'VISTA' and rbt_excluded is null and rbt_rq_excluded is null and updated > date_sub(curdate(), interval 1 day)";
	String runningsql = "from rjob, p4user where p4user.user != 'bt' and p4user.user = rjob.user and status = 'running' and rjob.branch = '" + branch + "'";
	String queuedsql = "from rjob,p4user " +
	  "where p4user.user != 'bt' and p4user.user = rjob.user and " +
	  "status = 'queued' and rjob.branch = '" + branch + "'";

	String format = request.getParameter("format");
	if (format != null && format.equals("text")) {
	  out.print("Remote Test Summary Status for " + branch + "\n");
	  out.print("Queued Jobs: " + getCount("select count(*) " + queuedsql) + ", ");
	  out.print("Idle Windows Machines: " + getCount("select count(*) " + idlewinsql) + ", ");
	  out.print("Idle Non-Windows Machines: " + getCount("select count(*) " + idlenotwinsql) + ", ");
	  out.print("Running Jobs: " + getCount("select count(*) " + runningsql) + "\n");
	  out.print("(newly queued jobs might not immediately be counted)\n");
	  out.print("\n");

	  conn.close();

	  return;
	}
        

%>
<html>
<head>
<title>Remote Queue Info <%= branch %> </title>
<link type="text/css" rel="stylesheet" href="style.css">
</head>
<body>
<%

	String ROOT1 = "\\\\centralrq.subnet3ad2phx.devweblogicphx.oraclevcn.com\\rqueue\\" + branch + "\\";
	String ROOT2 = "/net/centralrq-data.subnet3ad2phx.devweblogicphx.oraclevcn.com/centralrq_data" + branch + "/";
	String ROOT = "/mounts/centralrq_data/" + branch + "/";
        if (new File(ROOT1).isDirectory()) {
            ROOT = ROOT1;
        } else if (new File(ROOT2).isDirectory()) {
            ROOT = ROOT2;
        }
        String QUEUED = ROOT + "queued" + "/";
	String RESTART = QUEUED + "RESTART";
	if (new File(RESTART).exists()) {
	    out.print("<p><font color=red><b>You see this because remote boxes are restarting, your jobs may be stuck in queue for a while even if you see there are idle machines, please be patient and wait while the restarting completes.</b></font></p>");
	}
	String PAUSE = QUEUED + "PAUSE";
	if (new File(PAUSE).exists()) {
	    out.print("<p><font color=red><b>The remote queue has been paused.  No new jobs will start until it is resumed.  This could be due to some PDIT Semi-annual maintenance or some other reason.  If you still do not understand you can ask <a href=\"mailto:wls-infra_us_grp@oracle.com\">wls-infra_us_grp@oracle.com<a> or the wls-infra Slack channel</b></font></p>");
	}

	out.print("<table style=\"width:100%\"><tr><td>");
	out.print("<a href=\"remote.html\">Remote Test Documentation for " + branch + "</a><br>");
	out.print("</td><td>");
	Statement mystmt = conn.createStatement();
	mystmt.execute("select branch from rqbranch where branch like 'coherence/%' and max_queued > date_sub(now(), interval 2 month) order by max_queued desc");
	ResultSet myrs = mystmt.getResultSet();
	out.print("\n<p>Switch Branch: <select onchange=\"location = this.value;\">");
	// because of the way "onchange=" works you can never select the default/first value so make this branch the first one and don't display it in the natural order the mysql returns
	out.print("<option value=\"http://home.us.oracle.com/internal/" + branch + "/remote.jsp\">" + branch + "</option> ");
	while (myrs.next()) {
	    String value = myrs.getString(1);
	    if (!value.equals(branch)) {
	        out.print("<option value=\"http://home.us.oracle.com/internal/" + value + "/remote.jsp\">" + value + "</option> ");
	    }
	}
	out.print("</select>");
	out.print("</td></tr></table>");

	String user = protectSQL(request.getParameter("user"));
	String fulluser = "";
	String email = "";
	String sqluser = "";
        String userurl = "";
	if (user != null) {
	    sqluser = " and rjob.user = '" + user + "' ";
	    fulluser = getString("select fullname from p4user where user = '" + user + "'");
	    email = getString("select email from p4user where user = '" + user + "'");
	    userurl = "&user=" + user;
	}
	String site = protectSQL(request.getParameter("site"));
	if (site != null) {
	    sqluser = sqluser + " and p4user.ldap_site = '" + site + "' ";
	    userurl = userurl + "&site=" + site;
	}
	String spin = protectSQL(request.getParameter("spin"));
	String sqlspin = "";
	String spinurl = "";
	if (spin != null) {
	   sqlspin = " and rjob.spin = '" + spin + "' ";
	   spinurl = "&spin=" + spin;
	}
	String machine = protectSQL(request.getParameter("machine"));
	String sqlmachine = "";
	String machineurl = "";
	if (machine != null) {
	   sqlmachine = " and rjob.machine = '" + machine + "' ";
	   machineurl = "&machine=" + machine;
	}
	String target = protectSQL(request.getParameter("target"));
	String sqltarget = "";
	String targeturl = "";
	if (target != null) {
	   sqltarget = " and rjob.target = '" + target + "' ";
	   targeturl = "&target=" + target;
	}
	String status = protectSQL(request.getParameter("status"));
	String sqlstatus = "";
	String statusurl = "";
	if (status != null) {
	   // create two "special" values for status that check the failure column
	   if (status.equals("success")) {
	      sqlstatus = " and rjob.status = 'done' and rjob.failure is null";
	   } else if (status.equals("failure")) {
	      sqlstatus = " and rjob.status = 'done' and rjob.failure is not null";
	   } else {
	      sqlstatus = " and rjob.status = '" + status + "' ";
	   }
	   statusurl = "&status=" + status;
	}
	String stats = request.getParameter("stats");
	if (stats == null) stats = "";

	out.print("<table> <tr>");

	out.print("<td valign=top>");
	out.print(outputResult(
	    "select p4user.fullname as 'user+', rjob.user as 'user,silent', rjob.machine as 'machine,silent', rjob.id as 'id,silent', target as 'target+id', queued as 'queued,silent', 'waited+' " + queuedsql + sqluser + sqlspin + sqlmachine + sqltarget + sqlstatus + " order by id", "Queued Jobs"));
	out.print("</td>");

	if (user != null) {
	    out.print("<td width=\"40%\"><b><a href=\"http://people.us.oracle.com/pls/oracle/find_person?p_string=" + email + "\">" + fulluser + "'s</a></b> jobs,<br>to see data for all users click " +
		"<a href=\"" + request.getRequestURI() + "\">here</a>.</td>");
	}

	out.print("</tr></table>");

	out.print("<table><tr>");

	out.print("<td valign=top>");
	out.print(outputResult("select mstatus.machine as 'machine+', updated as 'updated,silent', changed as 'changed,silent', 'idle+'" + idlewinsql + " order by rbt_nightly_excluded desc, mstatus.machine", "Idle Machines"));
	out.print(outputResult("select mstatus.machine as 'machine+', updated as 'updated,silent', changed as 'changed,silent', 'idle+', machine.shortos as 'OS' " + idlenotwinsql + " order by rbt_nightly_excluded desc, mstatus.machine", "Idle Non-Windows Machines (not&nbsp;the&nbsp;default)"));
	out.print("</td>");

	out.print("<td valign=top>");
	out.print(outputResult("select p4user.fullname as 'user+', rjob.user as 'user,silent', machine as 'machine,silent', target as 'target+id', chng as 'change+', started as 'started,silent', id as 'id,silent', 'elapsed+', machine as 'machine+', left(description,40) as Description " + runningsql + " " + sqluser + sqlspin + sqlmachine + sqltarget + sqlstatus + " order by 'started,silent' desc", "Running Jobs"));
	out.print("</td>");

	out.print("</tr></table>");

	out.print(outputResult("select p4user.fullname as 'user+', rjob.machine as 'machine,silent', rjob.user as 'user,silent', target as 'target+id', chng as 'change+', failure as Failure, submit as Submit, queued as 'queued,silent', 'waited++', 'took+', started as 'started,silent', completed as Completed, id as 'id,silent', machine as 'machine+', left(description,40) as Description from rjob, p4user where p4user.user != 'bt' and p4user.user = rjob.user and status = 'done' and rjob.branch = '" + branch + "' " + sqluser + sqlspin + sqlmachine + sqltarget + sqlstatus + " order by completed desc limit 200", "Completed Jobs"));

	out.print("<p><a name=statistics href=\"?stats=");
        if (stats.equals("true")) {
	    out.print("false" + userurl + "\">Hide Statistics</a></p>");
	} else {
	    out.print("true#statistics"  + userurl + "\">Show Statistics</a></p>");
	}

	out.print("<table cellspacing=20> <tr>");

	if (stats.equals("true")) {

	    out.print("<td valign=top>");
	    out.print(outputResult("select machine as 'Machine', count(*) as 'Count' from rjob where user != 'bt' and machine is not null and branch = '" + branch + "' group by machine order by Count desc", "Remote Test Runs<br>per Machine"));
	    out.print("</td>");
	    
	    out.print("<td valign=top>");
	    out.print(outputResult("select target as 'Test', count(*) as 'Count' from rjob where user != 'bt' and target is not null and branch = '" + branch + "' group by target order by Count desc", "Remote Test Runs<br>per Test"));
	    out.print("</td>");
	    
	    out.print("<td valign=top>");
	    out.print(outputResult("select p4user.fullname as 'user+', rjob.user as 'user,silent', count(*) as Count from rjob,p4user where rjob.user != 'bt' and rjob.user = p4user.user and rjob.branch = '" + branch + "' group by rjob.user order by Count desc limit 60", "Remote Queue Usage<br>by User"));
	    out.print("</td>");
	    
	    out.print("<td valign=top>");
	    
	    try {
		int total = getCount("select count(*) from rjob where user != 'bt' and submit = 'true' and branch = '" + branch + "'");
		Statement stmt = conn.createStatement();
		stmt.execute("select failure, count(*) from rjob where user != 'bt' and submit = 'true' and branch = '" + branch + "' group by failure order by 'count(*)' desc");
		ResultSet rs = stmt.getResultSet();
		
		out.print("<table class=r border=4>");
		out.print("<caption>Reasons for failure<br>of auto-submit jobs</caption>");
		out.print("<tr class=caption>");
		out.print("<th class=r>Result</th>");
		out.print("<th class=r>Count</th>");
		out.print("<th class=r>%</th>");
		out.print("</tr>");
		while (rs.next()) {
		    out.print("<tr>");
		    String value = rs.getString(1);
		    out.print("<td>");
		    if (rs.wasNull()) {
			out.print("successful");
		    } else if (value.equals("killed")) {
			out.print(value);
		    } else {
			out.print(value + " failure");
		    }
		    out.print("</td>");
		    int count = rs.getInt(2);
		    out.print("<td>" + count + "</td>");
		    out.print("<td>" + ( count * 100 / total )  + "%</td>");
		    out.print("</tr>");
		}
		out.print("<tr><th>Total</th><th colspan=2>" + total + "</th></tr>");
		out.print("</table>");
		rs.close();
		stmt.close();
		
	    } catch (Exception e) {
		System.out.print(e);
		out.print("\n<h1>ERROR</h1>\n");
	    }
	    
	    try {
		int total = getCount("select count(*) from rjob where user != 'bt' and branch = '" + branch + "'");
		Statement stmt = conn.createStatement();
		stmt.execute("select failure, count(*) from rjob where user != 'bt' and branch = '" + branch + "' group by failure order by 'count(*)' desc");
		ResultSet rs = stmt.getResultSet();
		
		out.print("<table class=r border=4>");
		out.print("<caption>Reasons for failure<br>of all jobs</caption>");
		out.print("<tr class=caption>");
		out.print("<th class=r>Result</th>");
		out.print("<th class=r>Count</th>");
		out.print("<th class=r>%</th>");
		out.print("</tr>");
		while (rs.next()) {
		    out.print("<tr>");
		    String value = rs.getString(1);
		    out.print("<td>");
		    if (rs.wasNull()) {
			out.print("successful");
		    } else if (value.equals("killed")) {
			out.print(value);
		    } else {
			out.print(value + " failure");
		    }
		    out.print("</td>");
		    int count = rs.getInt(2);
		    out.print("<td>" + count + "</td>");
		    out.print("<td>" + ( count * 100 / total )  + "%</td>");
		    out.print("</tr>");
		}
		out.print("<tr><th>Total</th><th colspan=2>" + total + "</th></tr>");
		out.print("</table>");
		rs.close();
		stmt.close();
		
	    } catch (Exception e) {
		System.out.print(e);
		out.print("\n<h1>ERROR</h1>\n");
	    }
	    
	    out.print(outputResult("select date_format(completed, '%b %e - %a') as Completed, count(*) as Count from rjob where rjob.user != 'bt' and Completed is not null and branch = '" + branch + "' group by DATE_FORMAT(completed, '%Y-%m-%d') order by rjob.completed desc limit 60", "Remote Tests per Day"));
	    
	    if (user != null) {
		out.print(outputResult("select target as Target, failure as Failure, count(*) as Count from rjob where 1=1" + sqluser + sqlspin + sqlmachine + sqltarget + sqlstatus +" and branch = '" + branch + "' group by target, failure", "Test results<br>for " + fulluser));
	    }
	    
	    out.print("</td>");

	}

	out.print("</tr></table>");

	conn.close();
%>
</body>
</html>
