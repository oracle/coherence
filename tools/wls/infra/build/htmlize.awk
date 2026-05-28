#
# HTML'ize monkey email herrlich@bea.com Feb 24, 2003
#
# take a full mail message, headers and body in text with http:// URL's.
# Output both a text and html version of the message as a multipart email
# message.
#
BEGIN {
  # is is a flag to indicate whether or not we are parsing the mail headers
  # or not
  header = 1;
  # number of mail message body lines we have saved in the "line" array
  lines = 0;
  # boundary string to use to delimite the multipart Content encoding
  boundary="-----------------MONKEY1234567890";
}

# when parsing the header and we see a blank line then we have found the
# end of the header.  Add a content-type header and output the first part
# of the message in text
header == 1 && $0 ~ /^\r*$/ {
  header = 0;
  print "MIME-Version: 1.0";
  print "Content-Type: multipart/alternative;";
  print " boundary=\"" boundary "\"";
  print "";
  print "";
  print "--" boundary;
  print "Content-Type: text/plain; charset=us-ascii";
  print "Content-Transfer-Encoding: 7bit";
  print "";
  next;
}

# just echo the header
header == 1 { print; next}

# looking at the body of the message, just echo and save away in the line
# array.  We will output it again later and HTML'ize it.
header == 0 { 
  lines++;
  line[lines] = $0;
  if ($0 !~ /^#HTMLIZE/) {
    # remove some common html entities
    gsub(/<font[^>]*>/, "");
    gsub(/<\/font>/, "");
    gsub(/<span[^>]*>/, "");
    gsub(/<\/span>/, "");
    gsub(/<div[^>]*>/, "");
    gsub(/<\/div>/, "");
    gsub(/<hr>/, "");
    gsub(/<b>/, "");
    gsub(/<\/b>/, "");
    gsub(/<pre>/, "");
    gsub(/<\/pre>/, "");
    gsub(/<a [^>]*>/, "");
    gsub(/<\/a>/, "");
    gsub(/&lt;/, "<");
    gsub(/&gt;/, ">");
    print;
  }
  next;
}

# All done with the text version of the message.  Finish the boundry for that
# and start an html encoding of the same message.
END {
  print "";
  print "";
  print "--" boundary;
  print "Content-Type: text/html; charset=us-ascii";
  print "Content-Transfer-Encoding: 7bit";
  print "";
  print "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">";
  print "<html>";
  print "<head>";
  print "<link type=\"text/css\" rel=\"stylesheet\" href=\"http://tamarac.us.oracle.com/style.css\">";
  print "</head>";
  print "<body>";
  print "<pre>";
  # a <pre> and </pre> can be used in the input to avoid htmlization here,
  # mainly used by addtopsuspects
  htmlize = 1;
  for (i = 1; i <= lines; i++) {
    # new_line will accumulate the new html'ized line
    # old_line is what is left to process
    # stuff will be moved from old_line to new_line as we discover URL's
    # and process them.  This is to handle multiple http://'s in a single
    # line of text.
    new_line = "";
    old_line = line[i];
    if (old_line ~ /^#HTMLIZE OFF/) {
      htmlize = 0;
    }
    if (htmlize) {
      gsub(/&/, "\\&amp;", old_line);
      gsub(/</, "\\&lt;",  old_line);
      gsub(/>/, "\\&gt;",  old_line);
      while (old_line ~ /http:\/\//) {
	url_pos = match(old_line, /http:\/\/[^ \t\)]*/);
	new_line = new_line substr(old_line, 1, url_pos - 1);
	url = substr(old_line, RSTART, RLENGTH);
	old_line = substr(old_line, url_pos + RLENGTH);
        # grab whatever is at the end of the URL after the last / or = as
        # the tag to display in the URL
	match(url, /[^\/=]*$/);
	tag = substr(url, RSTART, RLENGTH);
	if (tag == "") {
	  tag = url;
	}
	new_line = new_line "<a href=\"" url "\">" tag "</a>";
      }
    }
    if (old_line ~ /^#HTMLIZE ON/) {
      htmlize = 1;
    }
    if (old_line !~ /^#HTMLIZE/) {
      new_line = new_line old_line;
      print new_line;
    }
  }
  print "</pre>";
  print "</body>";
  print "</html>";
  print "";
  print "--" boundary "--";
  print "";
}
