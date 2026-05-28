<%@page import="
java.io.File,
org.apache.commons.fileupload.FileItem,
org.apache.commons.fileupload.disk.DiskFileItemFactory,
org.apache.commons.fileupload.servlet.ServletFileUpload,
java.util.regex.Matcher,
java.util.regex.Pattern,
java.lang.Runtime,
java.nio.file.attribute.PosixFilePermission,
java.nio.file.Files,
java.nio.file.Paths,
" %><%!
private String branch, branch_base, jobid;
private File file;
private FileItem jobFI = null, zipFI = null;
%><%
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
//ROOT = "/tmp/src2/";

String QUEUED = ROOT + "queued/";
String RTZIPS = ROOT + "remotetests/zips/";

//out.println("branch=" + branch);
//out.println("contentType=" +  request.getContentType());
jobid = request.getParameter("id");
//out.println("id=" + jobid);
if (jobid == null) {
    out.println("ERROR: no id given");
    response.setStatus(500);
    return;
} else {
    Pattern idpat;
    Matcher idmat;
    idpat = Pattern.compile(".*[/%].*");
    idmat = idpat.matcher(jobid);
    if (idmat.find()) {
	out.println("ERROR: mangled id");
	response.setStatus(500);
	return;
    }
}

if (ServletFileUpload.isMultipartContent(request)) {
    //out.println("it is multipart");
    
    DiskFileItemFactory factory = new DiskFileItemFactory ();
    
    // Create a new file upload handler
    ServletFileUpload upload = new ServletFileUpload (factory);
    
    try {
	// Parse the request to get file items.
	List fileItems = upload.parseRequest(request);
	
	// Process the uploaded file items
	Iterator i = fileItems.iterator();
	
	while (i.hasNext ()) {
	    FileItem fi = (FileItem)i.next();
	    if (!fi.isFormField ()) {
		// Get the uploaded file parameters
		String fieldName = fi.getFieldName();
		//out.println("fieldName: " + fieldName);
		if (fieldName.equals("job")) {
		    //out.println("found job");
		    jobFI = fi;
		} else if (fieldName.equals("zip")) {
		    //out.println("found zip");
		    zipFI = fi;
		}
	    }
	}
	
	//out.println("jobFI = " + jobFI);
	//out.println("zipFI = " + zipFI);

	if (jobFI == null) {
	    out.println("ERROR: no job file");
	}
	if (zipFI == null) {
	    out.println("ERROR: no zip file");
	}
	if (jobFI == null || zipFI == null) {
	    response.setStatus(500);
	    return;
	}

	// always create the zip file first and only *then* the job file.
	// it is ok to have orphaned zip files without a job file but
	// jobs will start and fail if they don't have a zip file
	file = new File(RTZIPS + jobid + ".zip");
	if (file.exists()) {
	    out.println("ERROR: zip file already exists (" + RTZIPS + jobid + ".zip)");
	    response.setStatus(500);
	    return;
	}
	//out.println("writing zip file");
	zipFI.write(file);
	Set<PosixFilePermission> perms = Files.getPosixFilePermissions(Paths.get(RTZIPS + jobid + ".zip"));
	perms.add(PosixFilePermission.OWNER_READ);
	perms.add(PosixFilePermission.GROUP_READ);
	perms.add(PosixFilePermission.OTHERS_READ);
	Files.setPosixFilePermissions(Paths.get(RTZIPS + jobid + ".zip"), perms);
	file = new File(QUEUED + jobid);
	if (file.exists()) {
	    out.println("ERROR: job file already exists (" + QUEUED + jobid + ")");
	    response.setStatus(500);
	    return;
	}
	//out.println("writing job file");
	jobFI.write(file);
	perms = Files.getPosixFilePermissions(Paths.get(QUEUED + jobid));
	perms.add(PosixFilePermission.OWNER_READ);
	perms.add(PosixFilePermission.OWNER_WRITE);
	perms.add(PosixFilePermission.OWNER_EXECUTE);
	perms.add(PosixFilePermission.GROUP_READ);
	perms.add(PosixFilePermission.GROUP_WRITE);
	perms.add(PosixFilePermission.GROUP_EXECUTE);
	perms.add(PosixFilePermission.OTHERS_READ);
	perms.add(PosixFilePermission.OTHERS_WRITE);
	perms.add(PosixFilePermission.OTHERS_EXECUTE);
	Files.setPosixFilePermissions(Paths.get(QUEUED + jobid), perms);
	out.println(jobid + " successfully enqueued!");
    } catch(Exception ex) {
	ex.printStackTrace();
    }
} else {
    //out.println("it is NOT multipart");
}
%>
