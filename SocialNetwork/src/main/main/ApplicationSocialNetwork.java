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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import main.main.Messages.MessageChatMessage;
import android.app.Application;
import android.content.Context;
import android.os.Message;
import android.text.style.LeadingMarginSpan;
import android.util.Log;
import android.widget.Toast;


public class ApplicationSocialNetwork extends Application
{
//	private final int NAP_TIME_ADHOC_CLIENT_ENABLE = 1000;
//	private final int NAP_TIME_ADHOC_CLIENT_DISABLE = 1000;
//	private final int NAP_TIME_ADHOC_SERVER_ENABLE = 1000;
//	private final int NAP_TIME_ADHOC_SERVER_DISABLE = 1000;
	private final int NAP_TIME_STALE_CHECKER = 1000;
//	private final int NAP_TIME_MESSAGE_LOOP = 1000;
	private final int NAP_TIME_GET_DATAGRAM_SOCKET = 1000;
	private final int NAP_TIME_TOAST_AND_EXIT = 3000;
	
	private final long TIMEOUT_STALE_CLIENT = 5 * NAP_TIME_STALE_CHECKER;
	private final long TIMEOUT_STALE_LEADER = 5 * NAP_TIME_STALE_CHECKER;
//	private final int  TIMEOUT_SOCKET_ACCEPT = 30000;

	public final String USER_FILE_NAME_PREFIX = "user_";
	public final String USER_FILE_NAME_SUFFIX = "";
	public final String USER_FILE_NAME_EXTENSION = "";

	private final String PROPERTY_VALUE_SEPARATOR = "=";
	
	private final String LOG_TAG = "SN.Application";
	
	private final String IP_LEADER = "192.168.2.1";
	private final int PORT = 2222;
	
	public enum NetControlState
	{
		NOT_RUNNING, LEADER, CLIENT
	};

	private NetControlState mCurrState = NetControlState.NOT_RUNNING;

	private Thread mThreadStaleChecker = null;
	private Thread mThreadMessagLoop = null;
//	private Thread mThreadLeaderSocketListener = null;
	
	public OSFilesManager mOSFilesManager = null;

	String mMyDhcpAddress = "";

	private User mMe = null;
	private HashMap<String, User> mMapIPToUser = null;
	private long mLeaderLastPing = -1;
	private String CHAT_SEPERATOR = "@";
	private boolean mDidRunBefore = false;
	private Dictionary<String ,String > openChats = null;
//	private HashMap<String, Socket> mMapIPToSocket = null;
//	private Socket mSocketToLeader = null;
	
	
	/** Called when the application is first created. */
	public void onCreate()
	{
		mOSFilesManager = new OSFilesManager();
		mOSFilesManager.setPathAppDataFiles(getApplicationContext().getFilesDir().getParent());

		mMapIPToUser = new HashMap<String, User>();
//		mMapIPToSocket = new HashMap<String, Socket>();
		openChats = new Hashtable<String, String>();

		// Check for root permissions
		if (mOSFilesManager.doesHaveRootPermission())
		{
			copyRawsIfNeeded();
		}
		else
		{
			// Notify there are no root permissions and exit
			toastAndExit("You don't have root permissions. The application cannot run and will now quit. Good day !");
			
			// TODO : Is this enough or should something else be done ?
		}

		mCurrState = NetControlState.NOT_RUNNING;

//		stopDnsmasq();
//		disableAdhocServer();
	}
	
	public void stopDnsmasq()
	{
		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_dnsmasq");
//		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_int");
//		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_wifi");
//		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol start_wifi");
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
		createDir("bin");
		createDir("var");
		createDir("conf");
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
Log.d(LOG_TAG, "About to be a leader");
//				if (mThreadLeaderSocketListener == null)
//				{
//					mThreadLeaderSocketListener = new Thread(new LeaderSocketListener());
//					mThreadLeaderSocketListener.start();
//				}
				
				setMyIPAddress(IP_LEADER);
				
				Log.d(LOG_TAG, "Set my ip as leader");
				break;
			}
			
			case CLIENT:
			{
//				// Open a socket to the server
////				try
////				{
////Log.d(LOG_TAG, "About to open a socket to the leader");
////					mSocketToLeader = new Socket(IP_LEADER, PORT);
////Log.d(LOG_TAG, "Opened a socket to the leader");
////				}
//////				catch (UnknownHostException e)
//////				{
//////					e.printStackTrace();
//////				}
////				catch (IOException e)
////				{
////					e.printStackTrace();
////					
////					toastAndExit("Cannot establish a connection with the network. Exiting");
////				}
//				
//				Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(mMe);
//
//				sendMessage(msgNewUser.toString()); //, IP_LEADER);

				mLeaderLastPing = System.currentTimeMillis();
				
				break;
			}
		}
		
		// Start the threads for sending and receiving messages
//		if (mThreadClientReceiver == null)
//		{
			mThreadMessagLoop = new Thread(new ThreadMessageLoop());
			mThreadMessagLoop.start();
//		}
//
//		if (mThreadClientSender == null)
//		{
			mThreadStaleChecker = new Thread(new ThreadStaleChecker());
			mThreadStaleChecker.start();
//		}
	}

	public void stopService()
	{
		try
		{
			// Restore the Wifi state to the way it was before running this application
//			mWifiManager.setWifiEnabled(mWasWifiEnabledBeforeApp);
			
			if (mCurrState == NetControlState.CLIENT)
			{
//				disableAdhocClient();
			}
			else if (mCurrState == NetControlState.LEADER)
			{
				disableAdhocLeader();
			}

			mCurrState = NetControlState.NOT_RUNNING;

//			if (mMapIPToSocket != null)
//			{
//				for (Socket currSocket : mMapIPToSocket.values())
//				{
//					currSocket.close();
//				}
//			}
//			
//			if (mSocketToLeader != null)
//			{
//				mSocketToLeader.close();
//			}
			
			mThreadMessagLoop.interrupt();
			mThreadStaleChecker.interrupt();
//			mThreadLeaderSocketListener.interrupt();
		}
		catch (Exception e)
		{
			int potentialDebugBreakPoint = 3;
		}
	}

	public void enableAdhocLeader()
	{
		mOSFilesManager.updateDnsmasqConf();
		
		if (mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol start_server " + mOSFilesManager.PATH_APP_DATA_FILES) == false)
		{
			Log.d(LOG_TAG, "enableAdhocLeader : runRootCommand = false");
			
			// TODO : Notify the user
			int debug = 3;
		}
		Log.d(LOG_TAG, "Running as server");
		
		mDidRunBefore = true;
		mCurrState = NetControlState.LEADER;
	}

	public void disableAdhocLeader()
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
			// It doesn't necessarily mean the lease failed
			// TODO : Notify the user
			int debugPoint = 3;
			
		}

		// If I have a new DHCP Address, it means there's a leader who already ran the DHCP server and I'm a client
		mMyDhcpAddress = mOSFilesManager.getMyDhcpAddress();
		if (mMyDhcpAddress.length() > 0)
		{
Log.d(LOG_TAG, "Running as client");
			
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

//	private class LeaderSocketListener implements Runnable
//	{
//		// @Override
//		public void run()
//		{
//			try
//			{ 
//				ServerSocket serverSocket = new ServerSocket(PORT);
//				
//				// Set a time out for listening for new connections, so that if the leader logs out, this thread
//				// won't continue running
//				serverSocket.setSoTimeout(TIMEOUT_SOCKET_ACCEPT);
//				
//				while (mCurrState == NetControlState.LEADER && !Thread.currentThread().isInterrupted())
//				{
//					try
//					{
//						Socket socket = serverSocket.accept();
//						
//Log.d(LOG_TAG, "A client opened a connection");
//
//						// A new connection was made. Add it to the leader's global map of IP to Socket
//						String remoteIP = socket.getInetAddress().getHostAddress();
//						mMapIPToSocket.put(remoteIP, socket);
//					}
//					catch (SocketTimeoutException s)
//					{
//						// The socket timed out while listening to new connections. Try again
////Log.d(LOG_TAG, "Connection accepting timed out");
//					}
//				}
//			}
//			catch (IOException e)
//			{
//				e.printStackTrace();
//			}
//		}
//	}
	
	private class ThreadStaleChecker implements Runnable
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
//Log.d(LOG_TAG, "The leader is stale");

								// TODO : What now ?
								
							}
							
							break;
						}
						
						case LEADER :
						{
//							updateStaleClients();

							// Send a Ping message
							Messages.MessagePing msgPing = new Messages.MessagePing();
				
//							broadcastUDP(msgPing.toString());
							
							break;
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				nap(NAP_TIME_STALE_CHECKER);
				
//				try
//				{
//					Thread.sleep(NAP_TIME_SENDER);
//				}
//				catch (InterruptedException e)
//				{
//					Thread.currentThread().interrupt();
//				}
			}
		}
		
		private boolean isLeaderStale()
		{
			return (System.currentTimeMillis() - mLeaderLastPing) > TIMEOUT_STALE_LEADER;
		}

		private void updateStaleClients()
		{
			for (User currUser : mMapIPToUser.values())
			{
				if ((System.currentTimeMillis() - currUser.getLastPongTime()) > TIMEOUT_STALE_CLIENT)
				{
					
Log.d(LOG_TAG, "There's a stale user : " + currUser.getFullName() + ", now = " + System.currentTimeMillis() + ", User's lastPongTime = " + currUser.getLastPongTime());

					String currIPAddress = currUser.getIPAddress();
					
					// Broadcast a message to all users that this user has disconnected
					Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(currIPAddress);
					
					broadcast(msgUserDisconnected.toString());
					
					// Remove the user from the users list
					removeUser(currIPAddress);
					
					// Close that user's socket to the leader, and remove its entrance from the map from IP to Socket
//					try
//					{
//						Socket socketToRemove = mMapIPToSocket.get(currIPAddress);
//						socketToRemove.close();
//						mMapIPToSocket.remove(currIPAddress);
//					}
//					catch (IOException e)
//					{
//						e.printStackTrace();
//					}
				}
			}
		}
	}


	private class ThreadMessageLoop implements Runnable
	{
		// @Override
		public void run()
		{
			byte[] buffer = new byte[1048576];
			DatagramPacket packet = null;
			DatagramSocket socket = null;

//			Looper.prepare();

			// Getting a TCP packet
//			BufferedReader in = new BufferedReader(new InputStreamReader(skt.getInputStream()));
//			System.out.print("Received string: '");
//			
//			while (!in.ready())
//			{
//				
//			}
//			System.out.println(in.readLine());
	              
	              
			socket = createDatagramSocket();

			while (!Thread.currentThread().isInterrupted())
			{
				try
				{
					packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);

					String strMsgReceived = new String(packet.getData(), 0, packet.getLength());
					
Log.d(LOG_TAG, "Recevied Msg : " + strMsgReceived);

					String msgPrefix = Messages.getPrefix(strMsgReceived);
					Messages.Message msgReceived = new Messages.Message(strMsgReceived);
					
					
					if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
					{
						String ipSender = packet.getAddress().getHostAddress();
Log.d(LOG_TAG, "New user : ipSender = " + ipSender);

						// Check if we're the leader and if the leader broadcasted this message to all the clients.
						// If so, we doesn't need to deal with the message, only the client that will receive it.
						if ((mCurrState == NetControlState.LEADER && ipSender.equals(IP_LEADER)) == false)
						{
							Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(msgReceived);
	
							if (mCurrState == NetControlState.LEADER)
							{
								String ipAddressNewUser = msgNewUser.getIPAddress();
	
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
							}
							
							// Check that the new user isn't me (When a user joins, the leader broadcasts it so he will
							//                                   also get the message about it, and should ignore it)
							// Note : The condition is only needed when we're not the leader (Otherwise we're obviously not the new user)
							if (msgNewUser.getIPAddress().equals(mMe.getIPAddress()) == false)
							{
								User newUser = new User(msgNewUser);
								
								addUser(newUser);
							}
						}
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DISCONNECTED))
					{
						String ipSender = packet.getAddress().getHostAddress();
						
						// Check if we're the leader and if the leader broadcasted this message to all the clients.
						// If so, we doesn't need to deal with the message, only the client that will receive it.
						if ((mCurrState == NetControlState.LEADER && ipSender.equals(IP_LEADER)) == false)
						{
							Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(msgReceived);
							User userDisconnected = mMapIPToUser.get(msgUserDisconnected.getIPAddress());
		
							if (userDisconnected != null)
							{
								if (mCurrState == NetControlState.LEADER)
								{
									// Broadcast to everybody that this user has disconnected, so they remove him from their list
									broadcast(msgUserDisconnected.toString());
								}
								
								removeUser(userDisconnected.getIPAddress());
							}
						}
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_GET_USER_DETAILS)) // Only the leader gets this message
					{
						Messages.MessageGetUserDetails msgGetUserDetails = new Messages.MessageGetUserDetails(msgReceived);
						String targetIPAddress = msgGetUserDetails.getTargetIPAddress();
						String askerIPAddress = packet.getAddress().getHostAddress();
						
						// Check if the target user (whose details the asker wants) is the leader itself
						if (targetIPAddress.equals(getMyIP()))
						{
							// Send the asker my details
							Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(mMe, askerIPAddress);
							
							sendMessage(msgUserDetails.toString(), askerIPAddress);
						}
						else
						{
							// Send the target user that another user wants his details
							Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(askerIPAddress);
							
							sendMessage(msgGiveDetails.toString(), targetIPAddress);
						}	
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_GIVE_DETAILS)) // Only a client gets this message
					{
						// Send a UserDetails message to the leader with my details
						Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(msgReceived);
						Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(mMe, msgGiveDetails.getAskerIPAddress());
						
						sendMessage(msgUserDetails.toString(), IP_LEADER);
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DETAILS))
					{
						Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(msgReceived);
						String askerIP = msgUserDetails.getAskerIPAddress();
						
						if (mCurrState == NetControlState.LEADER && askerIP.equals(getMyIP()) == false)
						{
							// If it wasn't the leader who asked for the user's details, then pass the details to the asker
							sendMessage(strMsgReceived, askerIP);
						}
						else
						{
							// Else, if we're the leader but we're the ones who asked for the details, or if we're a client
							// (So if we got here, we're surely the ones who asked for them), process the data
							notifyActivityUserDetails(msgUserDetails);
						}
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_CHAT_MESSAGE))
					{
						Messages.MessageChatMessage msgChat = new Messages.MessageChatMessage(msgReceived);
						String targetIP = msgChat.getTargetUserIP();

						// If the message isn't for the leader, then pass it to the target user
						if (mCurrState == NetControlState.LEADER && targetIP.equals(getMyIP()) == false)
						{
							sendMessage(strMsgReceived, targetIP);
						}
						else
						{
							// If either we're the leader and the message is for us, or we're a client (So if we're here,
							// the message surely is for us), process the data
							Log.d(LOG_TAG, "got chat and will update, source is : "+msgChat.getSourceUserIP());
							UpdateOpenChats(msgChat.getChatMessageUser(),msgChat.getSourceUserIP(), msgChat.getChatMessageContents());
							notifyActivityChat(msgChat);
						}
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_PING))
					{
						// The leader also gets this message, because he broadcasts it to everyone including himself.
						// So check if I'm not the leader since only a client should respond to this message
						if (mMe.getIPAddress().equals(IP_LEADER) == false)
						{
							// Send a pong message to show I'm alive
							Messages.MessagePong msgPong = new Messages.MessagePong(mMe.getIPAddress());
							
							sendMessageUDP(msgPong.toString(), IP_LEADER);
							
							// Update the last ping time for leader-stale checking
							mLeaderLastPing = System.currentTimeMillis();
						}
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_PONG)) // Only the leader gets this message
					{
						Messages.MessagePong msgPong = new Messages.MessagePong(msgReceived);
						String ipAddressPongged = msgPong.getIPAddress();
						
						User userPongged = mMapIPToUser.get(ipAddressPongged);
					
						// Check the user still exists (Maybe he disconnected since Ponging)
						if (userPongged != null)
						{
							userPongged.setLastPongTime(System.currentTimeMillis());
						}
					}
				}
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

		private DatagramSocket createDatagramSocket()
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
				catch (SocketException e)
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
	}


	public synchronized void showToast(Context context, String toastMessage)
	{
		Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
		Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
	}

	private void addUser(User userToAdd)
	{
		mMapIPToUser.put(userToAdd.getIPAddress(), userToAdd);
		notifyActivityUsersList();
	}
	
	private synchronized void removeUser(String ipAddressToRemove)
	{
		mMapIPToUser.remove(ipAddressToRemove);
		notifyActivityUsersList();
	}
	
	public User getUserByIp(String ipAddress)
	{
		return mMapIPToUser.get(ipAddress);
	}
	
	private void notifyActivityUsersList()
	{
		if (ActivityUsersList.instance != null)
		{
			ActivityUsersList.instance.getUpdateHandler().sendMessage(new Message());
		}
	}

	private synchronized void notifyActivityUserDetails(Messages.MessageUserDetails msgUserDetails) //String msgUserDetails)
	{
Log.d(LOG_TAG, "About to notify ActivityUserDetails. Msg = " + msgUserDetails.toString());

		// TODO : I think these 2 whiles are not needed anymore. Try to delete
//		while (ActivityUserDetails.instance == null)
//		{
//			// Do nothing. Just wait. If we're in this function, this means that the ActivityUserDetails will shortly appear
//			// so just wait until it does and there's an instance of it
//		}
//		
//		while (ActivityUserDetails.instance.getUpdateHandler() == null)
//		{
//		}
		
		Message msg = ActivityUserDetails.instance.getUpdateHandler().obtainMessage();
		msg.obj = msgUserDetails;
			
Log.d(LOG_TAG, "Right before notifying ActivityUserDetails");

		ActivityUserDetails.instance.getUpdateHandler().sendMessage(msg);
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
	
	public String addOpenChats(String user, String ip)
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
	
	public void loadMyDetails(String userFileName) //, String userName)
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
	
	public void sendMessage(String message, String destIP)
	{
		// TODO : After choosing between UDP and TCP, the only implementation will be here instead of this function call to sendMessageUPD/TCP.
		//        (If we end up using both, then there WILL be a function call here, calling the default, most used, one)
		sendMessageUDP(message, destIP);
	}
	
	private void sendMessageUDP(String message, String destIP)
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
			
Log.d(LOG_TAG, "Sent Msg : " + message);

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
//	private synchronized void sendMessageTCP(String message, String destIP)
//	{
//		Socket socketToUse = null;
//		
//		if (destIP.equals(IP_LEADER))
//		{
//			socketToUse = mSocketToLeader;
//		}
//		else
//		{
//			socketToUse = mMapIPToSocket.get(destIP);
//		}
//		
//		DataOutputStream dataOutputStream;
//		try
//		{
//			dataOutputStream = new DataOutputStream(socketToUse.getOutputStream());
//			dataOutputStream.writeBytes(message);
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//	}
	
	
	public void sendMessage(String message)
	{
		sendMessage(message, getLeaderIP());
	}
	
	private void broadcast(String message)
	{
		broadcastUDP(message);
	}
	
	private void broadcastUDP(String message)
	{
		byte[] buffer = new byte[1024];
		DatagramPacket packet = null;
		DatagramSocket socket = null;
		InetAddress dest = null;
		
		buffer = message.getBytes();
		try
		{
			dest = InetAddress.getByName(calcBroadcastAddress(IP_LEADER)); //InetAddress.getByName("192.168.2.255");;
			packet = new DatagramPacket(buffer, buffer.length, dest, PORT);
Log.d(LOG_TAG, "Broadcast : dest = " + dest + " (calcBroadcastAddress(IP_LEADER) = " + calcBroadcastAddress(IP_LEADER) + ")");

			socket = new DatagramSocket();
			socket.setBroadcast(true);
			socket.send(packet);
		}
		catch (SocketException e)
		{
Log.d(LOG_TAG, "Broadcast : Exception !!! SocketException");			
			e.printStackTrace();
		}
		catch (UnknownHostException e)
		{
Log.d(LOG_TAG, "Broadcast : Exception !!! UnknownHostException");			
			e.printStackTrace();
		}
		catch (IOException e)
		{
Log.d(LOG_TAG, "Broadcast : Exception !!! IOException");			
			e.printStackTrace();
		}
	}

//	private synchronized void broadcastTCP(String message) // throws SocketException, IOException
//	{
//		// TODO : Go over the sockets and send them one by one
//		for (String currDestIP : mMapIPToSocket.keySet())
//		{
//			sendMessage(message, currDestIP);
//		}
//	}

//	private synchronized String calcBroadcastAddress(String ipAddress)
	private String calcBroadcastAddress(String ipAddress)
	{
		int pos = ipAddress.lastIndexOf(".");
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
		return IP_LEADER;
	}
	
	public synchronized User getMe()
	{
		return mMe;
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
			 fos = openFileOutput(fileName, MODE_APPEND);
			 osw = new OutputStreamWriter(fos);
			 
			 osw.write(propertyName + PROPERTY_VALUE_SEPARATOR + value + "\n");
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
	
	public String readPropertyFromFile(String fileName, String propertyName)
	{
		FileInputStream fis = null;
		BufferedReader br = null;
		String currLine = "";
		String returnedValue = "";
		
		try {
			
			fis = openFileInput(fileName);

			br = new BufferedReader(new InputStreamReader(fis));

			while ( (currLine = br.readLine()) != null)
			{
				if (currLine.contains(propertyName))
				{
					returnedValue = currLine.split(PROPERTY_VALUE_SEPARATOR)[1];
					
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
	
	public String getUserFileName(String userName)
	{
		return USER_FILE_NAME_PREFIX + userName + USER_FILE_NAME_SUFFIX + USER_FILE_NAME_EXTENSION;
	}

	public String getUserNameFromUserFileName(String userFileName)
	{
		return userFileName.substring(USER_FILE_NAME_PREFIX.length(),
									  userFileName.length() - (USER_FILE_NAME_SUFFIX.length() + USER_FILE_NAME_EXTENSION.length())); 
	}
	
	public boolean didRunBefore()
	{
		return mDidRunBefore;
	}
}
