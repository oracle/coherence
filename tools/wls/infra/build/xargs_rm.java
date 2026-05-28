import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

// this is a drop in replacement for the shell command "xargs rm -f",
// feed it a string of files, one per line and it will delete them without
// complaining if a file does not exist.
//
// find . -type f | java -cp . xargs_rm

public class xargs_rm {

	public static void main(String[] argv) {
		BufferedReader in = null;
		if (argv.length == 0 || argv[0]=="") {
			in = new BufferedReader(new InputStreamReader(System.in));
		} else {
			try {
				in = new BufferedReader(new InputStreamReader(System.in,
						argv[0]));
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
                                // System.out.println(e1);
			        in = new BufferedReader(new InputStreamReader(System.in));
			}
		}
		String filename;
		try {
			while ((filename = in.readLine()) != null) {
				int start = 0;
				int end = filename.length();
				// remove "'s if the entire filename is quoted
				if (filename.startsWith("\"") && filename.endsWith("\"")) {
					start = 1;
					end = end - 1;
				}
				try {
					new File(filename.substring(start, end)).delete();
				} catch (SecurityException e) {
					System.err.println("Exception deleting "
							+ filename.substring(start, end) + "("
							+ e.getMessage() + ")");
				}
			}
		} catch (IOException e) {
			System.err.println("Error reading System.in");
		}
	}
}
