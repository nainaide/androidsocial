package main.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;


public class OSFilesManager
{
	private static final String LOG_TAG = "SN.OSFilesManager";
	
	private static final String DEFAULT_DNS1 = "208.67.220.220";
	private static final String DEFAULT_DNS2 = "208.67.222.222";

	public String PATH_APP_DATA_FILES;


	private ApplicationSocialNetwork application;

	
	public void setPathAppDataFiles(String path)
	{
		this.PATH_APP_DATA_FILES = path;
	}

	public void chmodToFile(List<String> filenames) throws Exception
	{
		Process process = null;
		process = Runtime.getRuntime().exec("su");
		DataOutputStream os = new DataOutputStream(process.getOutputStream());

		for (String currFilename : filenames)
		{
			os.writeBytes("chmod 4755 " + this.PATH_APP_DATA_FILES + "/bin/" + currFilename + "\n");
		}

		os.writeBytes("exit\n");
		os.flush();
		os.close();
		process.waitFor();
	}

	public boolean doesHaveRootPermission()
	{
		Process process = null;
		DataOutputStream os = null;
		boolean rooted = true;
		
		try
		{
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
			
			if (process.exitValue() != 0)
			{
				rooted = false;
			}
		}
		catch (Exception e)
		{
			rooted = false;
		}
		finally
		{
			if (os != null)
			{
				try
				{
					os.close();
					process.destroy();
				}
				catch (Exception e)
				{
				}
			}
		}

		return rooted;
	}

	public boolean runRootCommand(String command)
	{
		Process process = null;
		DataOutputStream os = null;
		try
		{
			Log.d(LOG_TAG, "Running root command : " + command);

			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		}
		catch (Exception e)
		{
			return false;
		}
		finally
		{
			try
			{
				if (os != null)
				{
					os.close();
				}
				process.destroy();
			}
			catch (Exception e)
			{
			}
		}
		return true;
	}

	public synchronized void updateDnsmasqConf()
	{
		String dnsmasqConf = this.PATH_APP_DATA_FILES + "/conf/dnsmasq.conf";
		String newDnsmasq = new String();
		// Getting dns-servers
		String dns[] = new String[2];
		dns[0] = DEFAULT_DNS1;
		dns[1] = DEFAULT_DNS2;
		String currLine = null;
		BufferedReader br = null;
		boolean writeconfig = false;
		
		try
		{
			br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(dnsmasqConf))));

			int servercount = 0;
			while ((currLine = br.readLine()) != null)
			{
				if (currLine.contains("server"))
				{
					if (currLine.contains(dns[servercount]) == false)
					{
						currLine = "server=" + dns[servercount];
						writeconfig = true;
					}
					servercount++;
				}
				else if (currLine.contains("dhcp-leasefile=") && !currLine.contains(OSFilesManager.this.PATH_APP_DATA_FILES))
				{
					currLine = "dhcp-leasefile=" + OSFilesManager.this.PATH_APP_DATA_FILES + "/var/dnsmasq.leases";
					writeconfig = true;
				}
				else if (currLine.contains("pid-file=") && !currLine.contains(OSFilesManager.this.PATH_APP_DATA_FILES))
				{
					currLine = "pid-file=" + OSFilesManager.this.PATH_APP_DATA_FILES + "/var/dnsmasq.pid";
					writeconfig = true;
				}
				newDnsmasq += currLine + "\n";
			}
		}
		catch (Exception e)
		{
			writeconfig = false;
		}
		finally
		{
			if (br != null)
			{
				try
				{
					br.close();
				}
				catch (IOException e)
				{
				}
			}
		}
		if (writeconfig == true)
		{
			OutputStream out = null;
			try
			{
				out = new FileOutputStream(dnsmasqConf);
				out.write(newDnsmasq.getBytes());
			}
			catch (Exception e)
			{
			}
			finally
			{
				try
				{
					if (out != null)
					{
						out.close();
					}
				}
				catch (IOException e)
				{
				}
			}
		}
	}

	public String getMyDhcpAddress()
	{
		File fileDhcpLog = new File(this.PATH_APP_DATA_FILES + "/var/dhcp.log");
		FileInputStream fis = null;
		BufferedReader br = null;
		String myAddress = "";
		String currLine;
		
		try
		{
			if (fileDhcpLog.exists() && fileDhcpLog.canRead() && fileDhcpLog.length() > 0)
			{
				fis = new FileInputStream(fileDhcpLog);
				br = new BufferedReader(new InputStreamReader(fis));
				while ((currLine = br.readLine()) != null)
				{
					if (currLine != null && currLine.contains("leased"))
					{
						myAddress = currLine.split(" ")[2];
					}
				}
			}
		}
		catch (Exception e)
		{
			 Log.e(LOG_TAG, "getMyDhcpAddress() : An Exception has occurred. e.getMessage() = " + e.getMessage());
		}
		finally
		{
			try
			{
				if (fis != null)
				{
					fis.close();
				}
				if (br != null)
				{
					br.close();
				}
			}
			catch (Exception e)
			{
			}
		}
		return myAddress;
	}
	
	public void copyAllRawsIfNeeded(ApplicationSocialNetwork application)
	{
		List<String> listFilesNames = new ArrayList<String>();
		this.application = application;
		
		checkDirs();
		
		// netcontrol
		copyRawIfNeeded(PATH_APP_DATA_FILES + "/bin/netcontrol", R.raw.netcontrol);
		listFilesNames.add("netcontrol");
		
		// dnsmasq
		copyRawIfNeeded(PATH_APP_DATA_FILES + "/bin/dnsmasq", R.raw.dnsmasq);
		listFilesNames.add("dnsmasq");
		
		try
		{
			chmodToFile(listFilesNames);
		}
		catch (Exception e)
		{
			application.showToast(application, "Unable to change permission on binary files!");
		}
		
		// dnsmasq.conf
		copyRawIfNeeded(PATH_APP_DATA_FILES + "/conf/dnsmasq.conf", R.raw.dnsmasq_conf);
		
		// tiwlan.ini
		copyRawIfNeeded(PATH_APP_DATA_FILES + "/conf/tiwlan.ini", R.raw.tiwlan_ini);
	}

	private void copyRawIfNeeded(String filename, int resource)
	{
		File outFile = new File(filename);
		
		if (outFile.exists() == false)
		{
			InputStream is = application.getResources().openRawResource(resource);
			OutputStream out = null;
			byte buf[] = new byte[1024];
			int lengthLine = 0;
			
			try
			{
				out = new FileOutputStream(outFile);
				
				while ((lengthLine = is.read(buf)) > 0)
				{
					out.write(buf, 0, lengthLine);
				}
			}
			catch (IOException e)
			{
				application.showToast(application, "Couldn't install file - " + filename + " !");
			}
			finally
			{
				closeStream(out);
				closeStream(is);
			}
		}
	}

	private void closeStream(InputStream inStream)
	{
		try
		{
			if (inStream != null)
			{
				inStream.close();
			}
		}
		catch (IOException e)
		{
		}
	}
	
	private void closeStream(OutputStream outStream)
	{
		try
		{
			if (outStream != null)
			{
				outStream.close();
			}
		}
		catch (IOException e)
		{
		}
	}
	
	private void checkDirs()
	{
		createDir("bin");
		createDir("var");
		createDir("conf");
	}

	private void createDir(String dirName)
	{
		File dir = new File(PATH_APP_DATA_FILES + "/" + dirName);
		
		if (dir.exists() == false)
		{
			if (dir.mkdir() == false)
			{
				application.showToast(application, "Couldn't create " + dirName + " directory!");
			}
		}
	}

}
