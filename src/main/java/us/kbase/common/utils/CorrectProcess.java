package us.kbase.common.utils;

import java.io.*;

public class CorrectProcess extends Process {
	Process proc;
	ProcessStreamHolder err_psh;
	ProcessStreamHolder out_psh;
	ProcessStreamHolder inp_psh;

	public CorrectProcess(Process proc) {
		this(proc,null,null);
	}
	
	public CorrectProcess(Process proc,OutputStream out_str,OutputStream err_str) {
		this(proc,out_str,"",err_str,"ERROR:\t");
	}
	
	public CorrectProcess(Process proc,OutputStream out_str,String out_prefix,
			OutputStream err_str,String err_prefix) {
		this.proc = proc;
		err_psh = new ProcessStreamHolder(proc.getErrorStream(),err_prefix,err_str);
		out_psh = new ProcessStreamHolder(proc.getInputStream(),out_prefix,out_str);
		err_psh.start();
		out_psh.start();
	}

	public void destroy() {
		if (inp_psh != null) 
			inp_psh.stopThread();
		this.err_psh.stopThread();
		this.out_psh.stopThread();
		this.proc.destroy();
	}
	
	public int exitValue() throws java.lang.IllegalThreadStateException {
		return this.proc.exitValue();
	}
	
	public InputStream getErrorStream() {
		return this.proc.getErrorStream();
	}
	
	public InputStream getInputStream() {
		return this.proc.getInputStream();
	}
	
	public OutputStream getOutputStream() {
		return this.proc.getOutputStream();
	}
	
	public int waitFor() throws java.lang.InterruptedException {
		if (inp_psh != null) 
			inp_psh.join();
		err_psh.join();
		out_psh.join();
		return this.proc.waitFor();
	}
	
	public void addInputRedirect(InputStream is) {
		this.inp_psh = new ProcessStreamHolder(is,this.getOutputStream());
	}
	
	public static String[] arr(String... items) {
		return items;
	}
	
	public static class ProcessStreamHolder extends Thread {
		protected InputStream is;
		protected String type;
		protected OutputStream os;
		protected boolean must_stop = false;
		protected boolean was_used = false;

		public ProcessStreamHolder(InputStream is) {
			this(is,"");
		}
		public ProcessStreamHolder(InputStream is, String type) {
			this(is, type, null);
		}
		public ProcessStreamHolder(InputStream is, OutputStream redirect) {
			this(is,"",redirect);
		}
		public ProcessStreamHolder(InputStream is, String type, OutputStream redirect) {
			this.is = is;
			this.type = type;
			this.os = redirect;
		}

		public void run() {
			try {
				PrintStream pw = System.out;
				if((os!=null)&&(os!=System.out)) pw = new PrintStream(os);
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				while(true) {
					if(this.must_stop)break;
					String line = br.readLine();
					if(line==null)break;
					pw.println(type+line);
					pw.flush();
					this.was_used = true;
				}
				if(pw!=System.out) pw.close();
				else pw.flush();
				br.close();
			}catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		public void stopThread() {
			this.must_stop = true;
		}
		
		public boolean wasUsed() {
			return this.was_used;
		}
	}
}
