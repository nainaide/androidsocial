package main.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.widget.Toast;


public class ApplicationSocialNetwork extends Application
{
	private final int NAP_TIME_WIFI_ENABLE = 1000;
	private final int NAP_TIME_WIFI_DISABLE = 2000;
	private final int NAP_TIME_ADHOC_CLIENT_ENABLE = 1000;
	private final int NAP_TIME_ADHOC_CLIENT_DISABLE = 1000;
	private final int NAP_TIME_ADHOC_SERVER_ENABLE = 1000;
	private final int NAP_TIME_ADHOC_SERVER_DISABLE = 1000;
	private final int NAP_TIME_RECEIVER = 1000;
	private final int NAP_TIME_SENDER = 1000;
	private final int NAP_TIME_GET_DATAGRAM_SOCKET = 1000;
	
	private final long STALE_TIMEOUT_CLIENT = 10 * NAP_TIME_RECEIVER;
	private final long STALE_TIMEOUT_LEADER = 10 * NAP_TIME_RECEIVER;
	
	private final String IP_LEADER = "192.168.2.1";
	private final int PORT = 5678;
	
	public enum NetControlState
	{
		NOT_RUNNING, LEADER, CLIENT
	};

	private NetControlState mCurrState;

	private WifiManager mWifiManager;
	private Thread mClientSenderThread = null;
	private Thread mClientReceiverThread = null;
	
	public OSFilesManager mOSFilesManager = null;

	String mMyDhcpAddress = "";

	private User mMe = null;
	private HashMap<String, User> mMapIPToUser = null;
	private String mLeaderIP = "";
	private long mLeaderLastPing;
	
	
	/** Called when the application is first created. */
	public void onCreate()
	{
		mOSFilesManager = new OSFilesManager();
		mOSFilesManager.setPathAppDataFiles(getApplicationContext().getFilesDir().getParent());

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		mMapIPToUser = new HashMap<String, User>();

		// Check for root permissions
		if (mOSFilesManager.doesHaveRootPermission())
		{
			copyRawsIfNeeded();
		}
		else
		{
			// TODO : Notify there are no root permissions and exit
		}

		mCurrState = NetControlState.NOT_RUNNING;

		disableAdhocClient();
		disableAdhocServer();
	}

	public void onTerminate()
	{
		stopService();
	}

	private void copyRawsIfNeeded()
	{
		List<String> listFilesNames = new ArrayList<String>();
		
		checkDirs();
		
		// netcontrol
		copyRaw(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol", R.raw.netcontrol);
		listFilesNames.add("netcontrol");
		
		// dnsmasq
		copyRaw(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/dnsmasq", R.raw.dnsmasq);
		listFilesNames.add("dnsmasq");
		
		try
		{
			mOSFilesManager.chmodToFile(listFilesNames);
		}
		catch (Exception e)
		{
			showToast("Unable to change permission on binary files!");
		}
		
		// dnsmasq.conf
		copyRaw(mOSFilesManager.PATH_APP_DATA_FILES + "/conf/dnsmasq.conf", R.raw.dnsmasq_conf);
		
		// tiwlan.ini
		copyRaw(mOSFilesManager.PATH_APP_DATA_FILES + "/conf/tiwlan.ini", R.raw.tiwlan_ini);
		
		showToast("Binaries and config-files are installed");
	}

	private void copyRaw(String filename, int resource)
	{
		File outFile = new File(filename);
		
//		if (outFile.exists() == false)
//		{
		
		// TODO : The deletion of files is because I've changed the files and I want to make sure they get re-copied.
		//        After the files will be final, this deletion can be deleted.
		if (outFile.exists())
		{
			if (outFile.delete() == false)
			{
				int potentialDebugBreakPoint = 3;
			}
		}
			InputStream is = getResources().openRawResource(resource);
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
				showToast("Couldn't install file - " + filename + " !");
			}
			finally
			{
				closeStream(out);
				closeStream(is);
			}
//		}
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
		File dir = new File(mOSFilesManager.PATH_APP_DATA_FILES);
		
		if (dir.exists() == false)
		{
			showToast("The application's data directory doesn't exist !");
			
			// TODO : Exit the application ??
		}
		else
		{
			createDir("bin");
			createDir("var");
			createDir("conf");
		}
	}

	private void createDir(String dirName)
	{
		File dir = new File(mOSFilesManager.PATH_APP_DATA_FILES + "/" + dirName);
		
		if (dir.exists() == false)
		{
			if (!dir.mkdir())
			{
				showToast("Couldn't create " + dirName + " directory!");
			}
		}
	}

	public void startService()
	{
		switch (mCurrState)
		{
			case NOT_RUNNING:
			{
				mCurrState = NetControlState.CLIENT;

				break;
			}

			// Unexpected states
			case LEADER:
			case CLIENT:
			{
				break;
			}
		}
		
		// Start the threads for sending and receiving messages
		if (mClientReceiverThread == null)
		{
			mClientReceiverThread = new Thread(new ClientReceiver());
			mClientReceiverThread.start();
		}

		if (mClientSenderThread == null)
		{
			mClientSenderThread = new Thread(new ClientSender());
			mClientSenderThread.start();
		}
	}

	public void stopService()
	{
		try
		{
			mClientReceiverThread.interrupt();
			mClientSenderThread.interrupt();
		}
		catch (Exception e)
		{
			int potentialDebugBreakPoint = 3;
		}
	}


	public void disableWifi()
	{
		mWifiManager.setWifiEnabled(false);
	}

	public void enableWifi()
	{
		mWifiManager.setWifiEnabled(true);
	}

	public void enableAdhocServer()
	{
		mOSFilesManager.updateDnsmasqConf();
		
		if (mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol start_server " + mOSFilesManager.PATH_APP_DATA_FILES) == false)
		{
			// TODO : Notify the user
			int debug = 3;
		}
		
		mCurrState = NetControlState.LEADER;
	}

	public void disableAdhocServer()
	{
		if (mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_server " + mOSFilesManager.PATH_APP_DATA_FILES) == false)
		{
			// TODO : Notify the user
		}
	}

	public boolean enableAdhocClient()
	{
		mCurrState = NetControlState.CLIENT;
		boolean isDiscovered = false;
		
		mMyDhcpAddress = "";
		if (mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol start_client " + mOSFilesManager.PATH_APP_DATA_FILES) == false)
		{
			// TODO : Notify the user
			// It doesn't necessarily mean the lease failed
		}

		// If I have a new DHCP Address, it means there's a leader who already ran the DHCP server and I'm a client
		mMyDhcpAddress = mOSFilesManager.getMyDhcpAddress();
		if (mMyDhcpAddress.length() > 0)
		{
			isDiscovered = true;
			setMyIPAddress(mMyDhcpAddress);
		}
		
		return isDiscovered;
	}

	public void disableAdhocClient()
	{
		if (mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_client " + mOSFilesManager.PATH_APP_DATA_FILES) == false)
		{
			// TODO : Notify the user
		}
	}

	private void setMyIPAddress(String IPaddress)
	{
		mMe.setIPAddress(IPaddress);	    	
	}


	private class ClientSender implements Runnable
	{
		// @Override
		public void run()
		{
			// When the thread begins to run, it means the user has just logged in.
			// If this is a client, ask for the list of users
			if (mCurrState == NetControlState.CLIENT)
			{
				Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(mMe);

				sendMessage(msgNewUser.toString(), IP_LEADER);
			}
			else if (mCurrState == NetControlState.LEADER)
			{
				setMyIPAddress(IP_LEADER);
			}

//			Looper.prepare();
			while (!Thread.currentThread().isInterrupted())
			{
				try
				{
					switch (mCurrState)
					{
						case CLIENT :
						{
							// Check if the Leader still exists
							if (isLeaderStale())
							{
								// The leader is stale.
								// TODO : What now ?
							}
							
							break;
						}
						
						case LEADER :
						{
							// Send a Ping message
//							try
//							{
								Messages.MessagePing msgPing = new Messages.MessagePing();
					
								broadcast(msgPing.toString());
//							}
//							catch (Exception e)
//							{
//							}
							
							break;
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				updateStaleClients();

				try
				{
					Thread.sleep(NAP_TIME_SENDER);
				}
				catch (InterruptedException e)
				{
					Thread.currentThread().interrupt();
				}
			}
		}
		
		private boolean isLeaderStale()
		{
			// TODO Auto-generated method stub
			return false;
		}

		private void updateStaleClients()
		{
			for (User currUser : mMapIPToUser.values())
			{
				if ( (currUser.getLastPongTime() - System.currentTimeMillis()) > STALE_TIMEOUT_CLIENT)
				{
					String currIPAddress = currUser.getIPAddress();
					
					// Broadcast a message to all users that this user has disconnected
					Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(currIPAddress);
					
					broadcast(msgUserDisconnected.toString());
					
					// Remove the user from the users list
					removeUser(currIPAddress);
				}
			}
		}

	}


	private class ClientReceiver implements Runnable
	{

		// @Override
		public void run()
		{
			byte[] buffer = new byte[1048576];
			DatagramPacket packet = null;
			DatagramSocket socket = null;

//			Looper.prepare();

			socket = getDatagramSocket();

			while (!Thread.currentThread().isInterrupted())
			{
				try
				{
					packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);

					String strMsgReceived = new String(packet.getData(), 0, packet.getLength());

					if (packet.getAddress().getHostAddress().equals(mMe.getIPAddress()))
					{
						// The packet was received from me
						// TODO : What to do ?
					}
					else
					{
						String msgPrefix = Messages.getPrefix(strMsgReceived);
						Messages.Message msgReceived = new Messages.Message(strMsgReceived);
						
						if (mCurrState == NetControlState.CLIENT)
						{
							if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER_REPLY))
							{
								// Get the leader's IP
								String leaderIP = new Messages.MessageNewUserReply(msgReceived).getLeaderIP(); // Messages.getMessageParamByIndex()
								
								mLeaderIP = leaderIP;
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
							{
								Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(msgReceived);
								
								// Check that the new user isn't me (When a user joins, the leader broadcasts it so he will
								//                                   also get the message about it, and should ignore it)
								if (msgNewUser.getIPAddress().equals(mMe.getIPAddress()) == false)
								{
									User newUser = new User(msgNewUser);
									
									addUser(newUser);
////									mUsers.add(newUser);
//									mMapIPToUser.put(msgNewUser.getIPAddress(), newUser);
//									notifiyActivities();
								}
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DISCONNECTED))
							{
								Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(msgReceived);
								User userDisconnected = mMapIPToUser.get(msgUserDisconnected.getIPAddress());
								
								removeUser(userDisconnected.getIPAddress());
////							mUsers.remove(userDisconnected);
//								mMapIPToUser.remove(userDisconnected);
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_GIVE_DETAILS))
							{
								// Send a UserDetails message to the leader with my details
								Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(msgReceived);
								Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(mMe, msgGiveDetails.getAskerIPAddress());
								
								sendMessage(msgUserDetails.toString(), mLeaderIP);
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DETAILS))
							{
								Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(msgReceived);
								notifyActivityUserDetails(msgUserDetails.toString());
								// TODO : Complete this
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_PING))
							{
								// Send a pong message to show I'm alive
								Messages.MessagePong msgPong = new Messages.MessagePong(mMe.getIPAddress());
								
								sendMessage(msgPong.toString(), mLeaderIP);
								
								mLeaderLastPing = System.currentTimeMillis();
							}
						}
						
						
						else if (mCurrState == NetControlState.LEADER)
						{
							if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
							{
								Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(msgReceived);
								String ipAddressNewUser = msgNewUser.getIPAddress();
								User newUser = new User(msgNewUser);

								// Send back to the user a "NewUserReply" message so he'd know my IP
								Messages.MessageNewUserReply msgNewUserReply = new Messages.MessageNewUserReply(mMe.getIPAddress());
								
								sendMessage(msgNewUserReply.toString(), ipAddressNewUser);
								
								// Send the new user a "NewUser" message for every other client so he knows them
								for (User currUser : mMapIPToUser.values())
								{
									Messages.MessageNewUser msgNewUserOfExistingUserForNewcomerToKnow = new Messages.MessageNewUser(currUser);
									
									sendMessage(msgNewUserOfExistingUserForNewcomerToKnow.toString(), ipAddressNewUser);
								}
								
								// Send the user a "NewUser" message for me (The leader)
								Messages.MessageNewUser msgNewUserLeader = new Messages.MessageNewUser(mMe);
								
								sendMessage(msgNewUserLeader.toString(), ipAddressNewUser);
								
								// Broadcast to everybody that this user has joined us, so they know him
								broadcast(msgNewUser.toString());
								
								// Add the new users to my clients list
								addUser(newUser);
//								mMapIPToUser.put(ipAddressNewUser, newUser);
//								notifyActivities();
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_GET_USER_DETAILS))
							{
								Messages.MessageGetUserDetails msgGetUserDetails = new Messages.MessageGetUserDetails(msgReceived);
								String targetIPAddress = msgGetUserDetails.getTargetIPAddress();
//								String askerIPAddress = socket.getInetAddress().getHostAddress();
								String askerIPAddress = packet.getAddress().getHostAddress();
								Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(askerIPAddress);
								
								sendMessage(msgGiveDetails.toString(), targetIPAddress);
								
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DETAILS))
							{
								// Get the asker's IP and send him the details
								Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(msgReceived);
								String askerIP = msgUserDetails.getAskerIPAddress();
								
								sendMessage(strMsgReceived, askerIP);
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_PONG))
							{
								Messages.MessagePong msgPong = new Messages.MessagePong(msgReceived);
								String ipAddressPongged = msgPong.getIPAddress();
								
								User userPongged = mMapIPToUser.get(ipAddressPongged);
								
								userPongged.setLastPongTime(System.currentTimeMillis());
							}
						}
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		private DatagramSocket getDatagramSocket()
		{
			DatagramSocket socket = null;
			boolean done = false;
			
			while (!done && !Thread.currentThread().isInterrupted())
			{
				done = true;
				
				try
				{
					socket = new DatagramSocket(PORT);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					done = false;
				}
				
				if (!done)
				{
					try
					{
						Thread.sleep(NAP_TIME_GET_DATAGRAM_SOCKET);
					}
					catch (InterruptedException e)
					{
						Thread.currentThread().interrupt();
					}
				}
			}
			
			return socket;
		}
		
//		private void broadcastMessage(String string)
//		{
//			// TODO Auto-generated method stub
//			
//		}
	}


	public synchronized void showToast(String toastMessage)
	{
		Toast.makeText(this, toastMessage, Toast.LENGTH_LONG);
	}

	private synchronized void addUser(User userToAdd)
	{
		mMapIPToUser.put(userToAdd.getIPAddress(), userToAdd);
		notifyActivityUsersList();
	}
	
	private synchronized void removeUser(String ipAddressToRemove)
	{
		mMapIPToUser.remove(ipAddressToRemove);
		notifyActivityUsersList();
	}
	
	private synchronized void notifyActivityUsersList()
	{
		if (ActivityUsersList.instance != null)
		{
			ActivityUsersList.instance.getUpdateHandler().sendMessage(new Message());
		}
		
	}

	private synchronized void notifyActivityUserDetails(String msgUserDetails)
	{
		if (ActivityUserDetails.instance != null)
		{
			Message msg = ActivityUserDetails.instance.getUpdateHandler().obtainMessage();
			msg.obj = msgUserDetails; 
			ActivityUserDetails.instance.getUpdateHandler().sendMessage(msg);
		}
	}
	
	public synchronized void loadMyDetails(String userFileName) //, String userName)
	{
		// TODO : Make consts out of everything
		String userName = readPropertyFromFile(userFileName, "Username");
		User.Sex sex = User.Sex.valueOf(readPropertyFromFile(userFileName, "Sex").toUpperCase());
		String[] arrDateBirthElements = readPropertyFromFile(userFileName, "Date of Birth").split(" "); 
		int birthYear = Integer.parseInt(arrDateBirthElements[0]);
		int birthMonth = Integer.parseInt(arrDateBirthElements[1]);
		int birthDay = Integer.parseInt(arrDateBirthElements[2]);
		
		// TODO : Deal with the whole full name thing
		mMe = new User(userName, "", sex, birthYear, birthMonth, birthDay, "");
	}
	
//	public synchronized void sendMessage(String message, InetAddress dest)
	public synchronized void sendMessage(String message, String destIP)
	{
		byte[] buf = new byte[message.length()];
		DatagramPacket pkt = null;
		DatagramSocket sock = null;
		InetAddress dest = null;
		
		try
		{
			dest = InetAddress.getByName(destIP);
			buf = message.getBytes();
			pkt = new DatagramPacket(buf, buf.length, dest, PORT);
			sock = new DatagramSocket();
			sock.send(pkt);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public synchronized void sendMessage(String message)
	{
		sendMessage(message, getLeaderIP());
	}
	
	private synchronized void broadcast(String message) // throws SocketException, IOException
	{
		byte[] buffer = new byte[1024];
		DatagramPacket packet = null;
		DatagramSocket socket = null;
		InetAddress dest = null;
		
		buffer = message.getBytes();
		packet = new DatagramPacket(buffer, buffer.length, dest, PORT);
		try
		{
			dest = InetAddress.getByName(calcBroadcastAddress(IP_LEADER)); //InetAddress.getByName("192.168.2.255");;
			socket = new DatagramSocket();
			socket.setBroadcast(true);
			socket.send(packet);
		}
		catch (SocketException e)
		{
			e.printStackTrace();
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private synchronized String calcBroadcastAddress(String ipAddress)
	{
		int pos = ipAddress.lastIndexOf('.');
		String broadcastAddress = ipAddress.substring(0, pos) + ".255";
		
		return broadcastAddress;
	}
	
	public synchronized void nap(long milliSecs)
	{
		try
		{
			Thread.sleep(milliSecs);
		}
		catch (InterruptedException e)
		{
		}
	}

	public synchronized String getLeaderIP()
	{
		return mLeaderIP;
	}
	
	public synchronized String getMyIP()
	{
		return mMe.getIPAddress();
	}
	
	public synchronized ArrayList<User> getUsers()
	{
		ArrayList<User> arrayListUsers = new ArrayList<User>(mMapIPToUser.values());
		
		return arrayListUsers; //(ArrayList<User>)mMapIPToUser.values();
	}
	
	public synchronized NetControlState getCurrentState()
	{
		return mCurrState;
	}

	public void writePropertyToFile(String fileName, String propertyName, String value)
	{
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		
		try {
			 fos = openFileOutput(fileName, MODE_APPEND); //MODE_PRIVATE);
			 osw = new OutputStreamWriter(fos);
//			 BufferedWriter bw = new BufferedWriter(osw);
			 // TODO : Make this "=" a constant
			 osw.write(propertyName + "=" + value + "\n");
//			 bw.write(propertyName + "=" + value + "\n");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				osw.flush();
				fos.close();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String readPropertyFromFile(String fileName, String propertyName)
	{
		FileInputStream fis = null;
//		InputStreamReader isr = null;
		BufferedReader br = null;
		String currLine = "";
		String returnedValue = "";
		
		try {
			
			fis = openFileInput(fileName);

//			isr = new InputStreamReader(fis);
			br = new BufferedReader(new InputStreamReader(fis));

			// TODO : Make this "=" a constant
			while ( (currLine = br.readLine()) != null)
			{
				if (currLine.contains(propertyName))
				{
					// TODO : Make this "=" a constant
					returnedValue = currLine.split("=")[1];
					
					break;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fis.close();
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return returnedValue;
	}
}
