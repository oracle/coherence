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
java.util.Vector,
java.util.Random,
java.io.BufferedWriter,
java.io.BufferedInputStream,
java.io.BufferedOutputStream
" %>
<%!
    
    String nowUTC() {
	Date today = new Date();
	SimpleDateFormat df;
	df = new SimpleDateFormat("yyyyMMddHHmmss");
	df.setTimeZone(TimeZone.getTimeZone("UTC"));
	return df.format(today);
    }

    void copyFile(String src, String dest) throws Exception {
	BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
        File file=new File(dest);
        if(!file.exists()){
            file.createNewFile();
	    }

	BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        int c;
        byte buffer[]=new byte[1024];
        while((c=in.read(buffer))!=-1){
            for(int i=0;i<c;i++)
                out.write(buffer[i]);       
        }
        in.close();
        out.close();
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
String ZIP = ROOT +"remotetests/zips";
String QUEUE = ROOT + "queued";

String filter = request.getParameter("filter");
if (filter == null) filter = "";
String format = request.getParameter("format");
if (format == null) format = "html";

File statusdir = new File(STATUS);
if (! statusdir.isDirectory()) {
    out.print("That isn't a directory!");
}

String jobid = request.getParameter("id");

if (jobid != null) {
    String filename = STATUS + "/" + jobid;
    if (! (new File(filename).exists())) {
        filename = ARCHIVE + "/" + jobid;
    }
    String zipfilename = ZIP + "/" + jobid + ".zip";

    String firstline = "";
    String secondline = "";
    
    if (new File(filename).exists() && new File(zipfilename).exists()) {
        BufferedReader job = new BufferedReader(new FileReader(filename));
	firstline = job.readLine();
	secondline = job.readLine();
        //only remotetest job, allow retry
        if (secondline.startsWith("infra/test/remotetest")) {
	    //check if the job is done
	    boolean jobdone = false;
            String line = "";
            while (line != null) {
                if (line.endsWith(" done") || line.endsWith(" failed")) {
                    jobdone = true;
	        }
	        line = job.readLine();
            }
            job.close();
	    if (jobdone) {    
                //generate new job file name
	        String currenttime = nowUTC();
	        Random   rand=new   Random();
	        String newjobid = "job.9." + currenttime +"." + rand.nextInt(1000);
	        String newjobfilename = QUEUE + "/" + newjobid;
	        String newzipfile = ZIP + "/" + newjobid + ".zip";
	        //copy zip file, check result
	        copyFile(zipfilename, newzipfile);
	        //remove auto-submit
	        secondline = secondline.replaceAll("SUBMIT=[T|t][R|r][U|u][E|e]\\s", "SUBMIT= ");
		secondline = secondline.replaceAll("SUBMIT=[Y|y][E|e][S|s]\\s", "SUBMIT= ");
                //create new job file
                BufferedWriter newjobfile = new BufferedWriter(new FileWriter(newjobfilename));
                // need to do this to force the file to be created
                newjobfile.write(firstline);
                newjobfile.newLine();
                newjobfile.write(secondline);
                newjobfile.newLine();
                String commentline = "# This job is retried from: " + jobid; 
                newjobfile.write(commentline);
                newjobfile.newLine();
                newjobfile.close();
                if (format.equals("html")) {
 %>
	            <html>
	               <head>
	                   <title>Retry Job <%= jobid %> </title>
	               </head>
	               <body>
	                   <h1> New job id is <%= newjobid %>  </h1>
	                   <p> It needs time for new job to come in queue, please wait. </p>
	                   <p> And it is FORCE to run WITHOUT auto-submit feature.  </p>
	                   <p> Please don't retry a job repeatly. </p>
	                   <p> <a href="job.jsp?id=<%= newjobid %>"/>New Job Status</a> </p>
	               </body>
	            </html>
<%
	        }
	    } else {
	            //job not finish yet
	            out.print("<br><b>The job is killed or not done yet, can't retry!!</b><br>");
 	    } 
        } else {
	        job.close();
	        //the job isn't remotetest job
                out.print("<br><b>only remotetest job need to be retry!!</b><br>");
        }
    } else {
             //the job is removed from server
             out.print("<br><b>Can't find source job files!!</b><br>");
    }
} else {
        out.print("<br><b>No jobid?!</b><br>");
       }
%>
