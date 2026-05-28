/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import java.io.InputStream;

class rbt implements Runnable {

    private String user, pass, host, cmd;
    private boolean verbose;

    public rbt (boolean verbose,
		String user,
		String pass,
		String host,
		String cmd) {
	this.verbose = verbose;
	this.user = user;
	this.pass = pass;
	this.host = host;
	this.cmd = cmd;
    }

    public void run() {
	JSch jsch = null;

        if (verbose) System.err.print(cmd + "\n");
	try {
	    try {
		jsch = new JSch();
	    } catch (Exception ie){
		ie.printStackTrace();
		System.exit(1);
	    }
	    
	    Session session=jsch.getSession(user, host, 22);
	    
	    session.setPassword(pass);
	    session.setConfig("StrictHostKeyChecking", "no");
	    session.setConfig("PreferredAuthentications", "password");
	    
	    session.connect();
	    
	    Channel channel=session.openChannel("exec");
	    ((ChannelExec)channel).setCommand(cmd);
	    
	    channel.setInputStream(System.in);
	    
	    ((ChannelExec)channel).setErrStream(System.err);
	    
	    InputStream in=channel.getInputStream();
	    int exitStatus = 0;
	    channel.connect();
	    
	    byte[] tmp=new byte[1024];
	    while(true){
		while(in.available()>0){
		    int i=in.read(tmp, 0, 1024);
		    if(i<0)break;
		    System.out.print(new String(tmp, 0, i));
		}
		if(channel.isClosed()){
		    exitStatus = channel.getExitStatus();
                    //System.err.println("exit-status: " + exitStatus);
		    break;
		}
		try{Thread.sleep(1000);}catch(Exception ee){}
	    }
	    channel.disconnect();
	    session.disconnect();
	    System.exit(exitStatus);
	}
	catch(Exception e) {
          if (verbose)
            e.printStackTrace();
          else
            System.err.println(e);
          System.exit(1);
	}
	
    }

    public static void main(String[] args) {
	String cmd;
	int base_arg = 0; // grab all arguments after this for cmd
	boolean verbose = false;
	int timeout = 0;

	if (args.length < 4) {
	    System.out.println("Usage: rbt [-v] [-t nnnn] user pass host cmd");
	} else {
	    if (args[0].equals("-v")) {
		verbose = true;
		base_arg ++;
	    }
	    if (args[base_arg].equals("-t")) {
		timeout = Integer.parseInt(args[base_arg+1]);
		base_arg += 2;
	    }
	    cmd = args[base_arg + 3];
	    for (int i = (base_arg + 4); i < args.length; i++)
		cmd += " " + args[i];

	    Thread t = new Thread(new rbt(verbose, args[base_arg], args[base_arg + 1], args[base_arg + 2], cmd));
	    t.start();

	    // timeout mechanism so that the checking won't go on indefinitely
	    if (timeout > 0) {
		try {
		    Thread.sleep(timeout*1000);
		    System.out.println("rbt: WARNING: Timeout after " + timeout + " seconds");
		    System.exit(1);
		} catch (InterruptedException ie) {
		    ie.printStackTrace();
		    System.exit(1);
		}
	    } else {
		try {
		    t.join();
		} catch (InterruptedException e) {
		    System.out.println("Thread.join failed on rbt thread because of " + e);
		}
	    }
	}
    }
}
