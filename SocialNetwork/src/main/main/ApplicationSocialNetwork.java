package main.main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import main.imageManager.IImageNotifiable;
import main.imageManager.ImageCommunicator;
import main.imageManager.ImageManager;
import main.main.Messages.MessageChatMessage;
import android.app.Application;
import android.content.Context;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;


public class ApplicationSocialNetwork extends Application implements IImageNotifiable
{
//	private static final int NAP_TIME_ADHOC_CLIENT_ENABLE = 1000;
//	private static final int NAP_TIME_ADHOC_CLIENT_DISABLE = 1000;
//	private static final int NAP_TIME_ADHOC_SERVER_ENABLE = 1000;
//	private static final int NAP_TIME_ADHOC_SERVER_DISABLE = 1000;
	private static final int NAP_TIME_STALE_CHECKER = 1000;
//	private static final int NAP_TIME_MESSAGE_LOOP = 1000;
	private static final int NAP_TIME_GET_DATAGRAM_SOCKET = 1000;
	private static final int NAP_TIME_TOAST_AND_EXIT = 3000;
	private static final int NAP_TIME_RECONNECT = 1000;
	
	private static final long TIMEOUT_STALE_CLIENT = 100 * NAP_TIME_STALE_CHECKER;
	private static final long TIMEOUT_STALE_LEADER = 100 * NAP_TIME_STALE_CHECKER;
	private static final long TIMEOUT_RECONNECT = 30 * 1000; // 30 seconds
//	private final int  TIMEOUT_SOCKET_ACCEPT = 30000;

	public static final String USER_FILE_NAME_PREFIX = "user_";
	public static final String USER_FILE_NAME_SUFFIX = "";
	public static final String USER_FILE_NAME_EXTENSION = "";

	private static final String PROPERTY_VALUE_SEPARATOR = "=";
	
	private static final String LOG_TAG = "SN.Application";
	
	private static final String IP_LEADER = "192.168.2.1";
	private static final int PORT = 2222;
	
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
	private ImageManager imageManager;
//	private HashMap<String, Socket> mMapIPToSocket = null;
//	private Socket mSocketToLeader = null;
//	private String mIpBackup;
//	private boolean mAmIBackup;
	
	
	/** Called when the application is first created. */
	public void onCreate()
	{
		imageManager = new ImageManager( this);
		mOSFilesManager = new OSFilesManager();
		mOSFilesManager.setPathAppDataFiles(getApplicationContext().getFilesDir().getParent());

		mMapIPToUser = new HashMap<String, User>();
//		mMapIPToSocket = new HashMap<String, Socket>();
		openChats = new Hashtable<String, String>();

		// Check for root permissions
		if (mOSFilesManager.doesHaveRootPermission())
		{
			mOSFilesManager.copyRawsIfNeeded(this);
		}
		else
		{
			// Notify there are no root permissions and exit
			toastAndExit("You don't have root permissions. The application cannot run and will now quit. Good day !");
		}

		mCurrState = NetControlState.NOT_RUNNING;

//		mIpBackup = "";
//		mAmIBackup = false;
//		stopDnsmasq();
//		disableAdhocServer();
	}
	
//	public void stopDnsmasq()
//	{
//		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_dnsmasq");
//	}

	public void onTerminate()
	{
		super.onTerminate();
		
		stopService();
	}


	public void setFileNameForManager( String fileName) {
		imageManager.setFileName(fileName);
	}

	public void startService()
	{
		switch (mCurrState)
		{
			case NOT_RUNNING: {
				toastAndExit("Error connecting to an existing network or creating a new one. Exiting...");
				break;
			}
			case LEADER: {
				Log.d(LOG_TAG, "About to be a leader");
				setMyIPAddress(IP_LEADER);
				Log.d(LOG_TAG, "Set my ip as leader");
				break;
			}
			case CLIENT: {
				mLeaderLastPing = SystemClock.uptimeMillis();
				break;
			}
		}
		
		mThreadMessagLoop = new Thread(new ThreadMessageLoop());
		mThreadMessagLoop.start();
		mThreadStaleChecker = new Thread(new ThreadStaleChecker());
		mThreadStaleChecker.start();
		new Thread( imageManager, "Image manager").start( );
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

			mMapIPToUser.clear();
			//TODO : clear chat messages
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
//			int potentialDebugBreakPoint = 3;
		}
	}

	public void enableAdhocLeader()
	{
		mOSFilesManager.updateDnsmasqConf();
		
		if (mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol start_server " + mOSFilesManager.PATH_APP_DATA_FILES) == false)
		{
			Log.d(LOG_TAG, "enableAdhocLeader : runRootCommand = false");
			
			// TODO : Notify the user
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
//			int debugPoint = 3;
			
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
				nap(500);
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
								Log.d(LOG_TAG, "The leader is stale");
								
								// Remove the leader from the list of users
								mMapIPToUser.remove(getLeaderIP());
								
								Thread threadReconnect = new Thread(new ThreadReconnect());
								threadReconnect.start();
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

				nap(NAP_TIME_STALE_CHECKER);
			}
		}
		
		private boolean isLeaderStale()
		{
			//return false;
			return (SystemClock.uptimeMillis() - mLeaderLastPing) > TIMEOUT_STALE_LEADER;
		}

		private void updateStaleClients()
		{
			Collection<User> users =  new LinkedList<User>(mMapIPToUser.values());
			for (User currUser : users)
			{
				if ((SystemClock.uptimeMillis() - currUser.getLastPongTime()) > TIMEOUT_STALE_CLIENT)
				{
					Log.d(LOG_TAG, "There's a stale user : " + currUser.getFullName() + ", hasn't answered for - " + (SystemClock.uptimeMillis() - currUser.getLastPongTime()));

					String currIPAddress = currUser.getIPAddress();
					
					// Broadcast a message to all users that this user has disconnected
					Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(currIPAddress);
					
					broadcast(msgUserDisconnected.toString());
					
					// Remove the user from the users list
					removeUser(currIPAddress);
					
//					 // Close that user's socket to the leader, and remove its entrance from the map from IP to Socket
////					try
////					{
////						Socket socketToRemove = mMapIPToSocket.get(currIPAddress);
////						socketToRemove.close();
////						mMapIPToSocket.remove(currIPAddress);
////					}
////					catch (IOException e)
////					{
////						e.printStackTrace();
////					}
				 }
			}
		}
	}

	
	private class ThreadReconnect implements Runnable
	{
		// @Override
		public void run()
		{
			boolean isDone = false;
			String ipNewLeaderToBe = "";
			
			// Remove the leader from the users list since he is disconnected (Or we wouldn't have been here)
//			removeUser(getLeaderIP());
			
			while (isDone == false)
			{
				// Someone else should try and connect as the leader. Find out who that someone is
				ipNewLeaderToBe = calcIPBackup();
				
				// If I should be the new leader, connect as a leader
				if (getMyIP().equals(ipNewLeaderToBe))
				{
					enableAdhocLeader();
					isDone = true;
				}
				else
				{
					boolean isClientEnabled = false;
					long timeStart = SystemClock.uptimeMillis();
					long timePassed = 0;
					
					while (isClientEnabled == false && timePassed < TIMEOUT_RECONNECT)
					{
						nap(NAP_TIME_RECONNECT);
						
						isClientEnabled = enableAdhocClient();
						
						timePassed = SystemClock.uptimeMillis() - timeStart;
					}

					// Check if we were able to connect as a client
					if (isClientEnabled)
					{
						isDone = true;
					}
					else
					{
						// We were not able to connect as a client, meaning the backup disconnected before
						// he managed to connect as the leader, so remove him from the users list
						removeUser(ipNewLeaderToBe);
					}
				}
			}
			
			// Remove all the users from the users list because they will connect to the leader and will be
			// added to the list again
			mMapIPToUser.clear();
		}
		

		private String calcIPBackup()
		{
			String ipBackupToReturn = "999.999.999.999";
			
			// Find the minimal IP of all clients. It will be the backup
			Set<String> setIpsClients = mMapIPToUser.keySet();
			for (String currIP : setIpsClients)
			{
				if (currIP.compareTo(ipBackupToReturn) < 0)
				{
					ipBackupToReturn = currIP;
				}
			}
			
			return ipBackupToReturn;
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

			socket = createDatagramSocket();

			while (!Thread.currentThread().isInterrupted()) {
				try {
					packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					String strMsgReceived = new String(packet.getData(), 0, packet.getLength());
					Log.d(LOG_TAG, "Recevied Msg : " + strMsgReceived);
					String msgPrefix = Messages.getPrefix(strMsgReceived);
					Messages.Message msgReceived = new Messages.Message(strMsgReceived);
					
					if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER)) {
						String ipSender = packet.getAddress().getHostAddress();
						Log.d(LOG_TAG, "New user : ipSender = " + ipSender);
						
						// Check if we're the leader and if the leader broadcasted this message to all the clients.
						// If so, we don't need to deal with the message, only the client that will receive it.
						if ((mCurrState == NetControlState.LEADER && ipSender.equals(IP_LEADER)) == false) {
							Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(msgReceived);
							
							if (mCurrState == NetControlState.LEADER) {
								String ipAddressNewUser = msgNewUser.getIPAddress();
								Log.d(LOG_TAG, "New user : ipAddressNewUser = " + ipAddressNewUser);
								
//								// The leader checks if he doesn't already have a backup. If not, the new user becomes the backup
//								if (mIpBackup == null || mIpBackup == "")
//								{
//									// Set the backup ip to be the new user's ip
//									mIpBackup = ipAddressNewUser;
//									
//									// Send the new user a message to notify him that he is the backup
//									Messages.MessageMakeClientBackup msgmMakeUserBackup = new Messages.MessageMakeClientBackup();
//									sendMessage(msgmMakeUserBackup.toString());
//								}
								
								// Send the new user a "NewUser" message for every other client so he knows them
								for (User currUser : mMapIPToUser.values()) {
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
							if (msgNewUser.getIPAddress().equals(mMe.getIPAddress()) == false) {
								User newUser = new User(msgNewUser);
								addUser(newUser);
							}
						}
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DISCONNECTED)) {
						String ipSender = packet.getAddress().getHostAddress();
						// Check if we're the leader and if the leader broadcasted this message to all the clients.
						// If so, we doesn't need to deal with the message, only the client that will receive it.
				
						if (mCurrState != NetControlState.NOT_RUNNING && (mCurrState == NetControlState.LEADER && ipSender.equals(IP_LEADER)) == false) {
							Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(msgReceived);
							User userDisconnected = mMapIPToUser.get(msgUserDisconnected.getIPAddress());
							if (userDisconnected != null) {
								if (mCurrState == NetControlState.LEADER) {
									// Broadcast to everybody that this user has disconnected, so they remove him from their list
									broadcast(msgUserDisconnected.toString());
								}
								removeUser(userDisconnected.getIPAddress());
							}
						}
					}
					// Only the leader gets this message
					else if (msgPrefix.equals(Messages.MSG_PREFIX_GET_USER_DETAILS)) {
						final Messages.MessageGetUserDetails msgGetUserDetails = new Messages.MessageGetUserDetails(msgReceived);
						final String targetIPAddress = msgGetUserDetails.getTargetIPAddress();
						final String askerIPAddress = packet.getAddress().getHostAddress();
						
						// Check if the target user (whose details the asker wants) is the leader itself
						if (targetIPAddress.equals(getMyIP())) {
							// Send the asker my details
							Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(mMe, askerIPAddress);
							sendMessage(msgUserDetails.toString(), askerIPAddress);
						}
						else {
							// Send the target user that another user wants his details
							Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(askerIPAddress);
							sendMessage(msgGiveDetails.toString(), targetIPAddress);
						}	
						new Thread( ) {
							@Override
							public void run( ) {
								setName( "Leader image send/receiver");
								ImageCommunicator imageOwner = new ImageCommunicator( targetIPAddress, ImageCommunicator.IMAGE_SERVER_PORT);
								ImageCommunicator imageAsker = new ImageCommunicator( askerIPAddress, ImageCommunicator.IMAGE_SERVER_PORT);
								imageOwner.requestImage( msgGetUserDetails.getTargetUserName( ));
								imageAsker.sendImage( "/sdcard/" + msgGetUserDetails.getTargetUserName( ) + ".jpg", msgGetUserDetails.getTargetUserName( ));
							}
						}.start( );
					}
					// Only a client gets this message 
					else if (msgPrefix.equals(Messages.MSG_PREFIX_GIVE_DETAILS)) {
						// Send a UserDetails message to the leader with my details
						Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(msgReceived);
						Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(mMe, msgGiveDetails.getAskerIPAddress());
						sendMessage(msgUserDetails.toString(), IP_LEADER);
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DETAILS)) {
						Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(msgReceived);
						String askerIP = msgUserDetails.getAskerIPAddress();
						
						if (mCurrState == NetControlState.LEADER && askerIP.equals(getMyIP()) == false) {
							// If it wasn't the leader who asked for the user's details, then pass the details to the asker
							sendMessage(strMsgReceived, askerIP);
						}
						else {
							// Else, if we're the leader but we're the ones who asked for the details, or if we're a client
							// (So if we got here, we're surely the ones who asked for them), process the data
							notifyActivityUserDetails(msgUserDetails);
						}
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_CHAT_MESSAGE)) {
						Messages.MessageChatMessage msgChat = new Messages.MessageChatMessage(msgReceived);
						String targetIP = msgChat.getTargetUserIP();
						// If the message isn't for the leader, then pass it to the target user
						if (mCurrState == NetControlState.LEADER && targetIP.equals(getMyIP()) == false) {
							sendMessage(strMsgReceived, targetIP);
						}
						else {
							// If either we're the leader and the message is for us, or we're a client (So if we're here,
							// the message surely is for us), process the data
							Log.d(LOG_TAG, "got chat and will update, source is : "+msgChat.getSourceUserIP());
							UpdateOpenChats(msgChat.getChatMessageUser(),msgChat.getSourceUserIP(), msgChat.getChatMessageContents());
							notifyActivityChat(msgChat);
						}
					}
					else if (msgPrefix.equals(Messages.MSG_PREFIX_PING)) {
						// The leader also gets this message, because he broadcasts it to everyone including himself.
						// So check if I'm not the leader since only a client should respond to this message
						if (mMe.getIPAddress().equals(IP_LEADER) == false) {
							// Send a pong message to show I'm alive
							Messages.MessagePong msgPong = new Messages.MessagePong(mMe.getIPAddress());
							sendMessageUDP(msgPong.toString(), IP_LEADER);
							// Update the last ping time for leader-stale checking
							mLeaderLastPing = SystemClock.uptimeMillis();
						}
					}
					// Only the leader gets this message
					else if (msgPrefix.equals(Messages.MSG_PREFIX_PONG)) {
						Messages.MessagePong msgPong = new Messages.MessagePong(msgReceived);
						String ipAddressPongged = msgPong.getIPAddress();
						
						User userPongged = mMapIPToUser.get(ipAddressPongged);
						// Check the user still exists (Maybe he disconnected since Ponging)
						if (userPongged != null)
						{
							userPongged.setLastPongTime(SystemClock.uptimeMillis());
						}
					}
					// Only a client gets this message from the leader, notifying that this client is now the backup
//					else if (msgPrefix.equals(Messages.MSG_PREFIX_MAKE_USER_BACKUP)) {
//						mAmIBackup = true;
//					}
				}
				catch (Exception e)
				{
//					e.printStackTrace();
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
			boolean isDoneCreating = false;
			
			while (isDoneCreating == false && Thread.currentThread().isInterrupted() == false)
			{
				isDoneCreating = true;
				
				try
				{
					socket = new DatagramSocket(PORT);
				}
				catch (SocketException e)
				{
					e.printStackTrace();
					isDoneCreating = false;
				}
				
				if (isDoneCreating == false)
				{
					nap (NAP_TIME_GET_DATAGRAM_SOCKET);
//					try
//					{
//						Thread.sleep(NAP_TIME_GET_DATAGRAM_SOCKET);
//					}
//					catch (InterruptedException e)
//					{
//						Thread.currentThread().interrupt();
//					}
				}
			}
			
			return socket;
		}
	}


	public synchronized void showToast(Context context, String toastMessage)
//	public void showToast(Context context, String toastMessage)
	{
		Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
//		Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
	}

	private void addUser(User userToAdd)
	{
		mMapIPToUser.put(userToAdd.getIPAddress(), userToAdd);
		notifyActivityUsersList();
	}
	
	private void removeUser(String ipAddressToRemove)
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
	{
		user = user.trim();
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
		Log.d(LOG_TAG, "Value to add= " + value);
		this.openChats.put(ip + CHAT_SEPERATOR + user,this.openChats.get(ip + CHAT_SEPERATOR + user)+value);
		Log.d(LOG_TAG, "UpdateOpenChats value after updating:" +this.openChats.get(ip + CHAT_SEPERATOR + user) );	
	}
	
	public void loadMyDetails(String userFileName) //, String userName)
	{
		// TODO : Make consts out of everything
		String userName = readPropertyFromFile(userFileName, "Username");
		User.Sex sex = User.Sex.valueOf(readPropertyFromFile(userFileName, "Sex").toUpperCase());
		String[] arrDateBirthElements = readPropertyFromFile(userFileName, "Date of Birth").split(" ");
		String pictureFileName = readPropertyFromFile( userFileName, "Picture file name");
		if ( pictureFileName != null && pictureFileName.length() > 0 ) {
			imageManager.setFileName( pictureFileName);
		}
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
			Thread.currentThread().interrupt();
		}
	}

	public String getLeaderIP()
	{
		return IP_LEADER;
	}
	
	public synchronized User getMe()
	{
		return mMe;
	}
	
	public  String getMyIP()
	{
		return mMe.getIPAddress();
	}
	
	public ArrayList<User> getUsers()
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
		} catch (Exception e) {
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

	public void imageReady( String imageName) {
		Message msg = ActivityUsersList.instance.getUpdateHandler().obtainMessage();
		msg.obj = imageName;
		// TODO Update activity with user image
		Log.d( "Notify", "Image received: " + imageName);
		ActivityUserDetails.instance.getImageUpdateHandler().sendMessage(msg);
	}
}
