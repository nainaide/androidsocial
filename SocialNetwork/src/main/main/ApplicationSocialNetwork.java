package main.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import main.main.Messages.MessageChatMessage;
import android.app.Application;
import android.content.Context;
import android.graphics.LinearGradient;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.util.Log;
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
	private final int NAP_TIME_TOAST_AND_EXIT = 3000;
	
	private final long TIMEOUT_STALE_CLIENT = 5 * NAP_TIME_RECEIVER;
	private final long TIMEOUT_STALE_LEADER = 5 * NAP_TIME_RECEIVER;
	private final int  TIMEOUT_SOCKET_ACCEPT = 30000;
	
	private final String LOG_TAG = "SN.Application";
	
	private final String IP_LEADER = "192.168.2.1";
	private final int PORT = 5678;
	
	public enum NetControlState
	{
		NOT_RUNNING, LEADER, CLIENT
	};

	private NetControlState mCurrState = NetControlState.NOT_RUNNING;

	private WifiManager mWifiManager;
	private Thread mThreadClientSender = null;
	private Thread mThreadClientReceiver = null;
	private Thread mThreadLeaderSocketListener = null;
	
	public OSFilesManager mOSFilesManager = null;

	String mMyDhcpAddress = "";

	private User mMe = null;
	private HashMap<String, User> mMapIPToUser = null;
//	private String mLeaderIP = "";
	private long mLeaderLastPing = -1;
	private String CHAT_SEPERATOR = "@";
	private Dictionary<String ,String > openChats = null;
	private HashMap<String, Socket> mMapIPToSocket = null;
	private Socket mSocketToLeader = null;
	
	boolean mWasWifiEnabledBeforeApp;
	
	
	/** Called when the application is first created. */
	public void onCreate()
	{
		mOSFilesManager = new OSFilesManager();
		mOSFilesManager.setPathAppDataFiles(getApplicationContext().getFilesDir().getParent());

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mWasWifiEnabledBeforeApp = mWifiManager.isWifiEnabled();
		
		mMapIPToUser = new HashMap<String, User>();
		mMapIPToSocket = new HashMap<String, Socket>();
		openChats = new Hashtable<String, String>();

		// Check for root permissions
		if (mOSFilesManager.doesHaveRootPermission())
		{
			copyRawsIfNeeded();
		}
		else
		{
			// Notify there are no root permissions and exit
//			showToast("You don't have root permissions. The application cannot run and will now quit. Good day !");
//			System.exit(1);
			toastAndExit("You don't have root permissions. The application cannot run and will now quit. Good day !");
			
			// TODO : Is this enough or should something else be done ?
		}

		mCurrState = NetControlState.NOT_RUNNING;

		resetWifi();
//		disableAdhocServer();
//		disableAdhocClient();
	}

	private void resetWifi()
	{
		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_dnsmasq");
//		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_int");
//		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_wifi");
//		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol start_wifi");
//
//		if (mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol reset_wifi") == false)
//		{
//			// TODO : Notify the user
//		}
	}

	public void onTerminate()
	{
		super.onTerminate();
		
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
			showToast(this, "Unable to change permission on binary files!");
		}
		
		// dnsmasq.conf
		copyRaw(mOSFilesManager.PATH_APP_DATA_FILES + "/conf/dnsmasq.conf", R.raw.dnsmasq_conf);
		
		// tiwlan.ini
		copyRaw(mOSFilesManager.PATH_APP_DATA_FILES + "/conf/tiwlan.ini", R.raw.tiwlan_ini);
		
//		showToast("Binaries and config-files are installed");
	}

	private void copyRaw(String filename, int resource)
	{
		File outFile = new File(filename);
		
		// TODO : The deletion of files is because I've changed the files and I want to make sure they get re-copied.
		//        After the files will be final, this deletion can be deleted.
		if (outFile.exists())
		{
			if (outFile.delete() == false)
			{
				int potentialDebugBreakPoint = 3;
			}
		}
		
//		if (outFile.exists() == false)
//		{
		
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
				showToast(this, "Couldn't install file - " + filename + " !");
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
//			showToast("The application's data directory doesn't exist !");
			
			// TODO : Exit the application ??
			toastAndExit("The application's data directory doesn't exist !");
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
				showToast(this, "Couldn't create " + dirName + " directory!");
			}
		}
	}

	public void startService()
	{
		switch (mCurrState)
		{
			case NOT_RUNNING:
			{
				toastAndExit("Error connecting to an existing network or creating a new one. Exiting...");

				break;
			}

			case LEADER:
			{
				Log.d(LOG_TAG, "About to open a leader socket listner");
				if (mThreadLeaderSocketListener == null)
				{
					mThreadLeaderSocketListener = new Thread(new LeaderSocketListener());
					mThreadLeaderSocketListener.start();
				}
				
				setMyIPAddress(IP_LEADER);
				Log.d(LOG_TAG, "Set my ip as leader");
				break;
			}
			
			case CLIENT:
			{
				// Open a socket to the server
				try
				{
Log.d(LOG_TAG, "About to open a socket to the leader");
					mSocketToLeader = new Socket(IP_LEADER, PORT);
Log.d(LOG_TAG, "Opened a socket to the leader");
				}
//				catch (UnknownHostException e)
//				{
//					e.printStackTrace();
//				}
				catch (IOException e)
				{
					e.printStackTrace();
					
					// TODO : What to do here ?
					toastAndExit("Cannot establish a connection with the network. Exiting");
				}
				
				break;
			}
		}
		
		// Start the threads for sending and receiving messages
		if (mThreadClientReceiver == null)
		{
			mThreadClientReceiver = new Thread(new ClientReceiver());
			mThreadClientReceiver.start();
		}

		if (mThreadClientSender == null)
		{
			mThreadClientSender = new Thread(new ClientSender());
			mThreadClientSender.start();
		}
	}

	public void stopService()
	{
		try
		{
			// Restore the Wifi state to the way it was before running this application
			mWifiManager.setWifiEnabled(mWasWifiEnabledBeforeApp);
			
			if (mCurrState == NetControlState.CLIENT)
			{
				disableAdhocClient();
			}
			else if (mCurrState == NetControlState.LEADER)
			{
				disableAdhocServer();
			}

			if (mMapIPToSocket != null)
			{
				for (Socket currSocket : mMapIPToSocket.values())
				{
					currSocket.close();
				}
			}
			
			if (mSocketToLeader != null)
			{
				mSocketToLeader.close();
			}
			
			mThreadClientReceiver.interrupt();
			mThreadClientSender.interrupt();
			mThreadLeaderSocketListener.interrupt();
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

	private class LeaderSocketListener implements Runnable
	{
		// @Override
		public void run()
		{
			try
			{ 
				ServerSocket serverSocket = new ServerSocket(PORT);
				
				// Set a time out for listening for new connections, so that if the leader logs out, this thread
				// won't continue running
				serverSocket.setSoTimeout(TIMEOUT_SOCKET_ACCEPT);
				
				while (mCurrState == NetControlState.LEADER && !Thread.currentThread().isInterrupted())
				{
					try
					{
						Socket socket = serverSocket.accept();
						
Log.d(LOG_TAG, "A client opened a connection");

						// A new connection was made. Add it to the leader's global map of IP to Socket
						String remoteIP = socket.getInetAddress().getHostAddress();
						mMapIPToSocket.put(remoteIP, socket);
					}
					catch (SocketTimeoutException s)
					{
						// The socket timed out while listening to new connections. Try again
//Log.d(LOG_TAG, "Connection accepting timed out");
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
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

				sendMessage(msgNewUser.toString()); //, IP_LEADER);
			}
			else if (mCurrState == NetControlState.LEADER)
			{
				// Nothing to do there (For now. FFU)
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
								// The leader is dead. We should quit the network, wait a random time and then try to connect again

								// TODO : What now ?
							}
							
							break;
						}
						
						case LEADER :
						{
							updateStaleClients();

							// Send a Ping message
							Messages.MessagePing msgPing = new Messages.MessagePing();
				
							broadcastUDP(msgPing.toString());
							
							break;
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

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
			return mLeaderLastPing > TIMEOUT_STALE_LEADER;
		}

		private void updateStaleClients()
		{
			for (User currUser : mMapIPToUser.values())
			{
				if ( (currUser.getLastPongTime() - System.currentTimeMillis()) > TIMEOUT_STALE_CLIENT)
				{
					
Log.d(LOG_TAG, "There's a stale user :" + currUser.getFullName());

					String currIPAddress = currUser.getIPAddress();
					
					// Broadcast a message to all users that this user has disconnected
					Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(currIPAddress);
					
					broadcast(msgUserDisconnected.toString());
					
					// Remove the user from the users list
					removeUser(currIPAddress);
					
					// Close that user's socket to the leader, and remove its entrance from the map from IP to Socket
					try
					{
						Socket socketToRemove = mMapIPToSocket.get(currIPAddress);
						socketToRemove.close();
						mMapIPToSocket.remove(currIPAddress);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}


	private class ClientReceiver implements Runnable
	{

//		private String LOCAL_HOST = ""; //InetAddress.getLocalHost().getHostAddress(); //"127.0.0.1";

		// @Override
		public void run()
		{
			byte[] buffer = new byte[1048576];
			DatagramPacket packet = null;
			DatagramSocket socket = null;

//			try {
//				LOCAL_HOST = InetAddress.getLocalHost().getHostAddress(); //"127.0.0.1";
//			} catch (UnknownHostException e1) {
//				e1.printStackTrace();
//			}

//			Looper.prepare();

//			BufferedReader in = new BufferedReader(new InputStreamReader(skt.getInputStream()));
//			System.out.print("Received string: '");
//			
//			while (!in.ready())
//			{
//				
//			}
//			System.out.println(in.readLine());
	              
	              
			socket = getDatagramSocket();

			while (!Thread.currentThread().isInterrupted())
			{
				try
				{
					packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);

					String strMsgReceived = new String(packet.getData(), 0, packet.getLength());
					
Log.d(LOG_TAG, "Recevied Msg : " + strMsgReceived);

					//if (packet.getAddress().getHostAddress().equals(mMe.getIPAddress()))
					//{
						// The packet was received from me
						// TODO : What to do ?
					//}
					//else
					//{
						String msgPrefix = Messages.getPrefix(strMsgReceived);
						Messages.Message msgReceived = new Messages.Message(strMsgReceived);
						
						if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
						{
						}
						else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DISCONNECTED))
						{
						}
						else if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
						{
						}
						else if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
						{
						}
						else if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
						{
						}
						else if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
						{
						}
						else if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
						{
						}
						else if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
						{
						}
						else if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
						{
						}
						else if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
						{
						}

						
// ********************* BEGIN ****************************************************						
						if (mCurrState == NetControlState.CLIENT)
						{
//							if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER_REPLY))
//							{
//								// Get the leader's IP
//								String leaderIP = new Messages.MessageNewUserReply(msgReceived).getLeaderIP(); // Messages.getMessageParamByIndex()
//								
////								mLeaderIP = leaderIP;
//							}
							if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
							{
								Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(msgReceived);
								
								// Check that the new user isn't me (When a user joins, the leader broadcasts it so he will
								//                                   also get the message about it, and should ignore it)
								if (msgNewUser.getIPAddress().equals(mMe.getIPAddress()) == false)
								{
									User newUser = new User(msgNewUser);
									
									addUser(newUser);
								}
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DISCONNECTED))
							{
								Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(msgReceived);
								User userDisconnected = mMapIPToUser.get(msgUserDisconnected.getIPAddress());
								
								removeUser(userDisconnected.getIPAddress());
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_GIVE_DETAILS))
							{
								// Send a UserDetails message to the leader with my details
								Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(msgReceived);
								Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(mMe, msgGiveDetails.getAskerIPAddress(), "Stam Hobbies", "Stam Fav Music");
								
								sendMessage(msgUserDetails.toString(), IP_LEADER); //mLeaderIP);
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DETAILS))
							{
								Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(msgReceived);
//								notifyActivityUserDetails(msgUserDetails.toString());
								notifyActivityUserDetails(msgUserDetails);
								// TODO : Complete this
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_CHAT_MESSAGE))
							{
								Messages.MessageChatMessage msgChat = new Messages.MessageChatMessage(msgReceived);
								Log.d(LOG_TAG, "got chat and will update, source is : "+msgChat.getSourceUserIP());
								UpdateOpenChats(msgChat.getChatMessageUser(),msgChat.getSourceUserIP(), msgChat.getChatMessageContents());
								notifyActivityChat(msgChat);
								// TODO : Complete this
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_PING))
							{
								// Send a pong message to show I'm alive
								Messages.MessagePong msgPong = new Messages.MessagePong(mMe.getIPAddress());
								
								sendMessageUDP(msgPong.toString(), IP_LEADER); //mLeaderIP);
								
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
//								Messages.MessageNewUserReply msgNewUserReply = new Messages.MessageNewUserReply(mMe.getIPAddress());
//								
//								sendMessage(msgNewUserReply.toString(), ipAddressNewUser);
								
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
								
Log.d(LOG_TAG, "Leader got user details. askerIP = " + askerIP + ", getMyIP() = " + getMyIP());

								if (askerIP.equals(getMyIP())) // || askerIP.equals(LOCAL_HOST))
								{
									// If I'm the leader and I asked for the details, process them
//									notifyActivityUserDetails(msgUserDetails.toString());
									notifyActivityUserDetails(msgUserDetails);
								}
								else
								{
									// If a client asked for the details, send them to him
									sendMessage(strMsgReceived, askerIP);
								}
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_PONG))
							{
								Messages.MessagePong msgPong = new Messages.MessagePong(msgReceived);
								String ipAddressPongged = msgPong.getIPAddress();
								
								User userPongged = mMapIPToUser.get(ipAddressPongged);
								
								userPongged.setLastPongTime(System.currentTimeMillis());
							}
							else if (msgPrefix.equals(Messages.MSG_PREFIX_CHAT_MESSAGE))
							{
								Messages.MessageChatMessage msgChat = new Messages.MessageChatMessage(msgReceived);
								String targetIP = msgChat.getTargetUserIP();

								if (targetIP.equals(getMyIP())) // || targetIP.equals(LOCAL_HOST))
								{
Log.d(LOG_TAG, "Chat for leader");									
									UpdateOpenChats(msgChat.getChatMessageUser(),msgChat.getSourceUserIP(), msgChat.getChatMessageContents());
									notifyActivityChat(msgChat);
								}
								else
								{
Log.d(LOG_TAG, "Chat NOT for leader, passing on");									
									// If a client asked for the details, send them to him
									sendMessage(strMsgReceived, targetIP);
								}
							}
						}
// ********************* END ****************************************************						
					}
				//}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		private void notifyActivityChat(MessageChatMessage msgChat) {
			Log.d(LOG_TAG, "chat content :" + msgChat.getChatMessageContents());	
			if (ActivityUsersList.instance != null)
			{
				Log.d(LOG_TAG, "Activity User List != null");	
				Message msg = ActivityUsersList.instance.getUpdateHandler().obtainMessage();
				msg.obj = msgChat;
				ActivityUsersList.instance.getUpdateHandler().sendMessage(msg);
			}
			if (ActivityChat.instance != null)
			{
				Log.d(LOG_TAG, "Activity Caht != null");	
				Message msg = ActivityChat.instance.getUpdateHandler().obtainMessage();
				msg.obj = msgChat; 
				ActivityChat.instance.getUpdateHandler().sendMessage(msg);
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


	public synchronized void showToast(Context context, String toastMessage)
	{
		Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
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

	private synchronized void notifyActivityUserDetails(Messages.MessageUserDetails msgUserDetails) //String msgUserDetails)
	{
Log.d(LOG_TAG, "About to notify ActivityUserDetails. Msg = " + msgUserDetails.toString());

		// TODO : Check if this while is ok
		while (ActivityUserDetails.instance != null)
		{
			// Do nothing. Just wait. If we're in this function, this means that the ActivityUserDetails will shortly appear
			// so just wait until it does and there's an instance of it
		}
		
//		if (ActivityUserDetails.instance != null)
//		{
			Message msg = ActivityUserDetails.instance.getUpdateHandler().obtainMessage();
			msg.obj = msgUserDetails;
			
Log.d(LOG_TAG, "Right before notifying ActivityUserDetails");

			while (ActivityUserDetails.instance.getUpdateHandler() != null)
			{
			}
			
			ActivityUserDetails.instance.getUpdateHandler().sendMessage(msg);
//		}
	}
	
	public String GetOpenChatsIP(String user)
	{
		Enumeration<String> keys = this.openChats.keys();
		while(keys.hasMoreElements())
		{
			String[] chat =keys.nextElement().split(CHAT_SEPERATOR);
			String chatUser = chat[1];
		    String ip = chat[0];
			if(chatUser.equals(user))
				return ip;
		}
		
		return null;
 	}
	
	public CharSequence[] GetOpenChatUsers()
	{
		Enumeration<String> keys = openChats.keys();
		List<CharSequence> itemsList = new ArrayList<CharSequence>();
		
	    int i=0;
	    Log.d(LOG_TAG, "Going throught all open chats, have :"+openChats.size() +" open chats");
	    while(keys.hasMoreElements())
	    	{
	    	 String key = keys.nextElement();
	    	 //items[i] = key.split(CHAT_SEPERATOR)[1];
	    	 itemsList.add(key.split(CHAT_SEPERATOR)[1]);
	    	 Log.d(LOG_TAG, "open chat with key  :"+key);
	    	 i++;
	    	}
	    CharSequence[] items = new CharSequence[itemsList.size()];
	    for(int j=0; j<itemsList.size();j++)
	    	items[j] = itemsList.get(j);
	    
	    for(CharSequence s : items)
		{
			Log.d(LOG_TAG, "got item :"+s );
		}
	    return items;
	}
	
	public synchronized String addOpenChats(String user, String ip)
	{ user = user.trim();
		Enumeration<String> keys = this.openChats.keys();
		boolean flag = false;
		String res ="";
		String key = ip + CHAT_SEPERATOR + user;
		Log.d(LOG_TAG, "in addOpenChats lookin for key:"+key);
		while(keys.hasMoreElements())
		{
			String currKey =keys.nextElement(); 
			Log.d(LOG_TAG, "curr key is :"+currKey);
			if(currKey.equals(key))
				{
				flag = true;
				break;
				}
		}
		if(!flag)
			{
			 this.openChats.put(key, "");
			 Log.d(LOG_TAG, "added key :"+key);
			 return res;
			}
		return this.openChats.get(key);
	}
	
	public void UpdateOpenChats(String user,String ip, String value)
	{
		user = user.trim();
		addOpenChats(user, ip);
		Log.d(LOG_TAG, "Update chat to user :"+user +"    with ip: "+ip);
		Log.d(LOG_TAG, "UpdateOpenChats value prev updating:" +this.openChats.get(ip + CHAT_SEPERATOR + user) );
		Log.d(LOG_TAG,"Value to add= " + value);
		this.openChats.put(ip + CHAT_SEPERATOR + user,this.openChats.get(ip + CHAT_SEPERATOR + user)+value);
		Log.d(LOG_TAG, "UpdateOpenChats value after updating:" +this.openChats.get(ip + CHAT_SEPERATOR + user) );	
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
		// TODO : Temporarily to see that it works. When it does, sendMessageTCP's contents will be here
		
		sendMessageUDP(message, destIP);
	}
	
	private synchronized void sendMessageUDP(String message, String destIP)
	{
		byte[] buf = new byte[message.length()];
		DatagramPacket pkt = null;
		DatagramSocket sock = null;
		InetAddress dest = null;
		
Log.d(LOG_TAG, "About to send Msg : " + message);
Log.d(LOG_TAG, "About to send Msg to ip: " + destIP);

		try
		{
			dest = InetAddress.getByName(destIP);
			buf = message.getBytes();
			pkt = new DatagramPacket(buf, buf.length, dest, PORT);
			sock = new DatagramSocket();
			sock.send(pkt);
			
Log.d(LOG_TAG, "Sending Msg : " + message);

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private synchronized void sendMessageTCP(String message, String destIP)
	{
		Socket socketToUse = null;
		
		if (destIP.equals(IP_LEADER))
		{
			socketToUse = mSocketToLeader;
		}
		else
		{
			socketToUse = mMapIPToSocket.get(destIP);
		}
		
		DataOutputStream dataOutputStream;
		try
		{
			dataOutputStream = new DataOutputStream(socketToUse.getOutputStream());
			dataOutputStream.writeBytes(message);
		}
		catch (IOException e)
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
		broadcastUDP(message);
	}
	
	private synchronized void broadcastUDP(String message) // throws SocketException, IOException
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

	private synchronized void broadcastTCP(String message) // throws SocketException, IOException
	{
		// TODO : Go over the sockets and send them one by one
		for (String currDestIP : mMapIPToSocket.keySet())
		{
			sendMessage(message, currDestIP);
		}
	}

	private synchronized String calcBroadcastAddress(String ipAddress)
	{
		int pos = ipAddress.lastIndexOf('.');
		String broadcastAddress = ipAddress.substring(0, pos) + ".255";
		
		return broadcastAddress;
	}
	
	private void toastAndExit(String message)
	{
		showToast(this, message);
		nap(NAP_TIME_TOAST_AND_EXIT);
		System.exit(1);
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
		return IP_LEADER; //mLeaderIP;
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
