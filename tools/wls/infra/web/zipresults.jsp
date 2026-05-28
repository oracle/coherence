<%@page import="
java.io.File,
java.io.IOException,
java.util.zip.ZipEntry,
java.util.zip.ZipOutputStream
" contentType="application/zip"
%><%!
    void zipResults(HttpServletResponse response, String ROOT, int trimLength, String id, String timestamp)
      throws java.io.IOException {

      // make sure there are no / characters in the id or timestamp
      // to avoid URL hacking
      if (id.indexOf("/") != -1 || id.indexOf("\\") != -1) {
	  System.out.println(id + " has slashes in it.");
	  return;
      }
      if (timestamp.indexOf("/") != -1 || timestamp.indexOf("\\") != -1) {
	  System.out.println(timestamp + " has slashes in it.");
	  return;
      }
      File dirObj = new File(ROOT + "/remotetests/" + id + "/" + timestamp);
      if(!dirObj.isDirectory()) {
	  System.out.println(dirObj.getPath() + " is not a directory");
	  return;
      }
      
      response.setHeader("Content-Disposition", "filename=\"" + id + ".zip\"");
      try {
	  ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
	  addDir(dirObj, trimLength, out);
	  out.close();
      }
      catch (IOException e) {
	  e.printStackTrace();
	  return;
      }
  }
      
  void addDir(File dirObj, int trimLength, ZipOutputStream out)
    throws IOException {
      File[] files = dirObj.listFiles();
      byte[] tmpBuf = new byte[1024];
	  
      for (int i=0; i<files.length; i++) {
	  if(files[i].isDirectory()) {
	      addDir(files[i], trimLength, out);
	      continue;
	  }
	
	FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
	String filename = files[i].getCanonicalPath();
	out.putNextEntry(new ZipEntry(filename.substring(trimLength,filename.length())));
	
	int len;
	while((len = in.read(tmpBuf)) > 0) {
	    out.write(tmpBuf, 0, len);
	}
	out.closeEntry();
	in.close();
    }
}

%><%

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

int trimLength = new File(ROOT).getCanonicalPath().length() + 1;

String id = request.getParameter("id");
if (id == null) id = "job.9.99999999999999.999";
String timestamp = request.getParameter("timestamp");
if (timestamp == null) timestamp = "99999999-999999";

zipResults(response, ROOT, trimLength, id, timestamp);
%>
