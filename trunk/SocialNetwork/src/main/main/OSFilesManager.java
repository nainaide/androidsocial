package main.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import android.util.Log;


public class OSFilesManager
{
	private final String LOG_TAG = "SN.OSFilesManager";
	
	public String PATH_APP_DATA_FILES;

	private static final String defaultDNS1 = "208.67.220.220";
	private static final String defaultDNS2 = "208.67.222.222";


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
		dns[0] = defaultDNS1;
		dns[1] = defaultDNS2;
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
		catch (Exception ex)
		{
			// Log.d(MSG_TAG, "Unexpected error: "+ex.getMessage());
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
			catch (Exception ex)
			{
				// nothinh
			}
		}
		return myAddress;
	}
}
