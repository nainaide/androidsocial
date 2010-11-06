package main.main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import main.imageManager.IImageNotifiable;
import main.imageManager.ImageCommunicator;
import main.imageManager.ImageManager;
import main.imageManager.ImageReceiver;
import main.main.Messages.MessageChatMessage;
import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

/**
 * This class extends the Android's Application class. It contains all the logic not related to a specific activity.
 */
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
	
	private static final int TIMEOUT_STALE_CLIENT = 5 * NAP_TIME_STALE_CHECKER;
	private static final int TIMEOUT_STALE_LEADER = 5 * NAP_TIME_STALE_CHECKER;
	private static final int TIMEOUT_RECONNECT = 40 * 1000; // 30 seconds
//	private final int  TIMEOUT_SOCKET_ACCEPT = 30000;

	public static final int TIMEOUT_SOCKET_RECEIVE = TIMEOUT_STALE_LEADER + 100;
	
	public static final String USER_FILE_NAME_PREFIX = "user_";
	public static final String USER_FILE_NAME_SUFFIX = "";
	public static final String USER_FILE_NAME_EXTENSION = "";

	public static final String USER_PROPERTY_USERNAME		= "Username";
	public static final String USER_PROPERTY_SEX			= "Sex";
	public static final String USER_PROPERTY_DATE_OF_BIRTH	= "Date of Birth";
	public static final String USER_PROPERTY_PIC_FILE_NAME	= "Picture File Name";
	public static final String USER_PROPERTY_HOBBIES		= "Hobbies";
	public static final String USER_PROPERTY_FAVORITE_MUSIC	= "Favorite Music";
	
	
	private static final String LOG_TAG_PREFIX = "SN.Application";
	private static String LOG_TAG = LOG_TAG_PREFIX;
	
	private static final String IP_LEADER = "192.168.2.1";
	private static final int PORT = 2222;
	
	
	private enum NetControlState
	{
		NOT_RUNNING, LEADER, CLIENT
	};

	private NetControlState mCurrState = NetControlState.NOT_RUNNING;

	private Thread mThreadStaleChecker = null;
	private Thread mThreadReconnect = null;
	private Thread mThreadMessagLoop = null;
	
	private OSFilesManager mOSFilesManager = null;

	private String mMyDhcpAddress = "";

	private User mMe = null;
	private HashMap<String, User> mMapIPToUser = null;
	private long mLeaderLastPing = -1;
	private String CHAT_SEPERATOR = "@";
//	private boolean mDidRunBefore = false;
	private Hashtable<String ,String > openChats = null;
	private ImageManager imageManager;
	
	
	/** Called when the application is first created. */
	public void onCreate()
	{
		imageManager = new ImageManager( this);
		mOSFilesManager = new OSFilesManager();
		mOSFilesManager.setPathAppDataFiles(getApplicationContext().getFilesDir().getParent());

		mMapIPToUser = new HashMap<String, User>();
		openChats = new Hashtable<String, String>();

		// Check for root permissions
		if (mOSFilesManager.doesHaveRootPermission())
		{
			mOSFilesManager.copyAllRawsIfNeeded(this);
		}
		else
		{
			// Notify there are no root permissions and exit
			toastAndExit("You don't have root permissions. The application cannot run and will now quit. Good day !");
		}

		mCurrState = NetControlState.NOT_RUNNING;

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
		
		LOG_TAG = LOG_TAG_PREFIX + " : " + mCurrState.toString();
		
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
			if (mCurrState == NetControlState.LEADER)
			{
				disableAdhocLeader();
			}

			mMapIPToUser.clear();
			openChats.clear();
			
			mCurrState = NetControlState.NOT_RUNNING;

			mThreadStaleChecker.interrupt();
			if (mThreadReconnect != null)
			{
				mThreadReconnect.interrupt();
			}
			mThreadMessagLoop.interrupt();
			imageManager.shutdown();
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, "stopService() - An exception has occurred. e.getMessage() = " + e.getMessage());
		}
	}

	public void enableAdhocLeader()
	{
		mOSFilesManager.updateDnsmasqConf();
		
		if (mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol start_server " + mOSFilesManager.PATH_APP_DATA_FILES) == false)
		{
			Log.d(LOG_TAG, "enableAdhocLeader : runRootCommand = false");
			
			// Notify the user that a network could not be created and the application will now quit
			toastAndExit("There was an error trying to create a network. The application will exit. Please try again shortly.");
		}
		
		Log.d(LOG_TAG, "Running as server");
		
//		mDidRunBefore = true;
		setMyIPAddress(IP_LEADER);
		mCurrState = NetControlState.LEADER;
	}

	public void disableAdhocLeader()
	{
		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_server " + mOSFilesManager.PATH_APP_DATA_FILES);
	}

	public boolean enableAdhocClient()
	{
		mCurrState = NetControlState.CLIENT;
		boolean isDiscovered = false;
		
		mMyDhcpAddress = "";
		
		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol start_client " + mOSFilesManager.PATH_APP_DATA_FILES);

		// If I have a new DHCP Address, it means there's a leader who already ran the DHCP server and I'm a client
		mMyDhcpAddress = mOSFilesManager.getMyDhcpAddress();
		if (mMyDhcpAddress.length() > 0)
		{
			Log.d(LOG_TAG, "Running as client. My ip = " + mMyDhcpAddress);
			
			isDiscovered = true;
			setMyIPAddress(mMyDhcpAddress);
		}
		
		return isDiscovered;
	}

	public void disableAdhocClient()
	{
		mOSFilesManager.runRootCommand(mOSFilesManager.PATH_APP_DATA_FILES + "/bin/netcontrol stop_client " + mOSFilesManager.PATH_APP_DATA_FILES);
	}

	private void setMyIPAddress(String IPaddress)
	{
		mMe.setIPAddress(IPaddress);	    	
	}

	
	/**
	 * This thread periodically checks if either the clients or the leader are stale.
	 * If we are a client, it also notifies once (when it starts) the leader that we joined the network.
	 * If we are the leader, it also periodically broadcasts a ping message to all clients so they know the leader isn't stale
	 */
	private class ThreadStaleChecker implements Runnable
	{
		// @Override
		public void run()
		{
			// When the thread begins to run, it means the user has just logged in.
			// If this is a client, ask for the list of users
			if (mCurrState == NetControlState.CLIENT)
			{
//				nap(500);
				Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(mMe);

				sendMessage(msgNewUser.toString());
			}
			
//			Looper.prepare();
			while (Thread.currentThread().isInterrupted() == false)
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
								removeUser(getLeaderIP());
								
								// TODO : Should we just delete the ThreadReconnect and put its contents as a function here ?
								mThreadReconnect = new Thread(new ThreadReconnect());
								mThreadReconnect.start();
								mThreadReconnect.join();
							}
							
							break;
						}
						
						case LEADER :
						{
							updateStaleClients();

							// Send a Ping message
							Messages.MessagePing msgPing = new Messages.MessagePing();
				
							broadcast(msgPing.toString());
							
							break;
						}
					}
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, "ThreadStaleChecker : run() : e.getMessage() = " + e.getMessage());
				}

				nap(NAP_TIME_STALE_CHECKER);
			}
			
			Log.d(LOG_TAG, "ThreadStaleChecker : Interrupted and about to finish running");
		}
		
		private boolean isLeaderStale()
		{
			return (SystemClock.uptimeMillis() - mLeaderLastPing) > TIMEOUT_STALE_LEADER;
		}

		/**
		 * Checks for each client if he is stale by checking if a certain timeout has passed since the client's last pong response
		 */
		private void updateStaleClients()
		{
			Collection<User> users =  new LinkedList<User>(mMapIPToUser.values());
			for (User currUser : users)
			{
				if ((SystemClock.uptimeMillis() - currUser.getLastPongTime()) > TIMEOUT_STALE_CLIENT)
				{
					Log.d(LOG_TAG, "There's a stale user : " + currUser.getUsername() + ", hasn't answered for - " + (SystemClock.uptimeMillis() - currUser.getLastPongTime()));

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

	
	/**
	 * This thread is run when a client identifies that the leader is stale. It finds the IP of a client that will now try
	 * to connect as the new leader (All the clients have the same list of IPs so they all agree on the next leader).
	 * The client with this IP tries to connect as the leader. If a certain timeout passes and he is not the leader yet, it
	 * is assumed he got disconnected and the next IP is found, and so on.
	 */
	private class ThreadReconnect implements Runnable
	{
		// @Override
		public void run()
		{
			boolean isDone = false;
			String ipNewLeaderToBe = "";
			
			while (Thread.currentThread().isInterrupted() == false && isDone == false)
			{
				// Someone else should try and connect as the leader. Find out who that someone is
				ipNewLeaderToBe = calcIPBackup();
				
				Log.d(LOG_TAG, "Reconnect : ipNewLeaderToBe = " + ipNewLeaderToBe);
				
				// If I should be the new leader, connect as a leader
				if (getMyIP().equals(ipNewLeaderToBe))
				{
					Log.d(LOG_TAG, "Reconnect : I'm the new leader. About to enable leader");
					
					disableAdhocClient();
					enableAdhocLeader();
					
					Log.d(LOG_TAG, "Reconnect : I'm the new leader. Returned from enabling leader");

					isDone = true;
				}
				else
				{
					boolean isClientEnabled = false;
					long timeStart = SystemClock.uptimeMillis();
					long timePassed = 0;
					
					Log.d(LOG_TAG, "Reconnect : I'm not the new leader. About to try to connect as a client");
					
					while (isClientEnabled == false && timePassed < TIMEOUT_RECONNECT)
					{
						nap(NAP_TIME_RECONNECT);
						
						isClientEnabled = enableAdhocClient();
						
						timePassed = SystemClock.uptimeMillis() - timeStart;
						
						Log.d(LOG_TAG, "Reconnect : Tried to enable client. isClientEnabled = " + isClientEnabled + ", timePassed = " + timePassed);
					}

					Log.d(LOG_TAG, "Reconnect : Out of the inner while. isClientEnabled = " + isClientEnabled);
					
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
		

		/**
		 * Find the IP to be the next leader.
		 */
		private String calcIPBackup()
		{
			String ipBackupToReturn = "999.999.999.999";
			List<String> setIpsClients = new LinkedList<String>(mMapIPToUser.keySet());
			
			// Add my ip to the set of ips because it doesn't appear in the mMapIPToUser
			setIpsClients.add(getMyIP());
			
			// Find the minimal IP of all clients. It will be the next leader
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
	

	/**
	 * This thread runs the whole messages protocol. It waits for a message and then processes it.
	 */
	private class ThreadMessageLoop implements Runnable
	{
		// @Override
		public void run()
		{
			byte[] buffer = new byte[1048576];
			DatagramPacket packet = null;
			DatagramSocket socket = null;

			socket = createDatagramSocket();
//			try {
//				socket.setSoTimeout(TIMEOUT_SOCKET_RECEIVE);
//			} catch (SocketException e) {
//			}

			while (Thread.currentThread().isInterrupted() == false) {
				try {
					packet = new DatagramPacket(buffer, buffer.length);
					socket.receive(packet);
					String strMsgReceived = new String(packet.getData(), 0, packet.getLength());
					
					if (shouldLog(strMsgReceived))
					{
						Log.d(LOG_TAG, "Recevied Msg : " + strMsgReceived);
					}
					
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
						// If so, we don't need to deal with the message, only the client that will receive it.
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
						
						// First ask for the picture since it works in a separate thread
						new Thread( ) {
							@Override
							public void run( ) {
								setName( "Leader image send/receiver");
								String targetUsername = msgGetUserDetails.getTargetUserName();
								ImageCommunicator imageOwner = new ImageCommunicator( targetIPAddress, ImageCommunicator.IMAGE_SERVER_PORT);
								ImageCommunicator imageAsker = new ImageCommunicator( askerIPAddress, ImageCommunicator.IMAGE_SERVER_PORT);
								// TODO : If the leader is the target ip, just create a copy of the picture instead of having him sending his
								//        own picture to himself
								imageOwner.requestImage(targetUsername);
								imageAsker.sendImage( "/sdcard/" + targetUsername + ".jpg", targetUsername);
							}
						}.start( );
						
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
						if (mMe.getIPAddress().equals(IP_LEADER) == false)
						{
							// Send a pong message to show I'm alive
							Messages.MessagePong msgPong = new Messages.MessagePong(mMe.getIPAddress());
							sendMessage(msgPong.toString(), IP_LEADER);
							
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
				}
				catch (InterruptedIOException e)
				{
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, "ThreadMessageLoop : Exception. e.getMessage() = " + e.getMessage());
				}
			}
			
			socket.close();
			
			Log.d(LOG_TAG, "ThreadMessageLoop : Interrupted and about to finish running");
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
				Log.d(LOG_TAG, "Activity Chat != null");
				
				Message msg = ActivityChat.instance.getUpdateHandler().obtainMessage();
				msg.obj = msgChat; 
				ActivityChat.instance.getUpdateHandler().sendMessage(msg);
			}
		}

		// TODO : Check for several runs of the program if there is an error here. If not, just delete this method and open the socket where it is called
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
					Log.e(LOG_TAG, "createDatagramSocket : SocketException. e.getMessage() = " + e.getMessage());
					
					isDoneCreating = false;
				}
				
				if (isDoneCreating == false)
				{
					nap (NAP_TIME_GET_DATAGRAM_SOCKET);
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

//	private synchronized void notifyActivityUserDetails(Messages.MessageUserDetails msgUserDetails) //String msgUserDetails)
	private void notifyActivityUserDetails(Messages.MessageUserDetails msgUserDetails) //String msgUserDetails)
	{
		Log.d(LOG_TAG, "About to notify ActivityUserDetails. Msg = " + msgUserDetails.toString());

		Message msg = ActivityUserDetails.instance.getUpdateHandler().obtainMessage();
		msg.obj = msgUserDetails;

		ActivityUserDetails.instance.getUpdateHandler().sendMessage(msg);
	}
	
	public String getOpenChatsIP(String user)
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
	
	public CharSequence[] getOpenChatUsers()
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
		String res = "";
		String key = ip + CHAT_SEPERATOR + user;
		
		Log.d(LOG_TAG, "in addOpenChats lookin for key:"+key);
		
		while(keys.hasMoreElements())
		{
			String currKey = keys.nextElement(); 
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
	
	public void loadMyDetails(String userFileName)
	{
		String userName = readPropertyFromFile(userFileName, USER_PROPERTY_USERNAME);

		User.Sex sex = User.Sex.valueOf(readPropertyFromFile(userFileName, USER_PROPERTY_SEX).toUpperCase());

		String[] arrDateBirthElements = readPropertyFromFile(userFileName, USER_PROPERTY_DATE_OF_BIRTH).split(ActivityUserDetails.DATE_ELEMENTS_SEPARATOR);
		int birthYear = Integer.parseInt(arrDateBirthElements[0]);
		int birthMonth = Integer.parseInt(arrDateBirthElements[1]);
		int birthDay = Integer.parseInt(arrDateBirthElements[2]);

		String pictureFileName = readPropertyFromFile( userFileName, USER_PROPERTY_PIC_FILE_NAME);
		if ( pictureFileName != null && pictureFileName.length() > 0 ) {
			imageManager.setFileName( pictureFileName);
		}
		
		mMe = new User(userName, sex, birthYear, birthMonth, birthDay);
		
		mMe.setFavoriteMusic(readPropertyFromFile(userFileName, USER_PROPERTY_FAVORITE_MUSIC));
		mMe.setHobbies(readPropertyFromFile(userFileName, USER_PROPERTY_HOBBIES));
	}
	
	public void sendMessage(String message, String destIP)
	{
		byte[] buf = new byte[message.length()];
		DatagramPacket packet = null;
		DatagramSocket socket = null;
		InetAddress dest = null;
		
		if (shouldLog(message))
		{
			Log.d(LOG_TAG, "About to send Msg : " + message);
			Log.d(LOG_TAG, "About to send Msg to ip: " + destIP);
		}
		
		try
		{
			dest = InetAddress.getByName(destIP);
			buf = message.getBytes();
			packet = new DatagramPacket(buf, buf.length, dest, PORT);
			socket = new DatagramSocket();
			socket.send(packet);
			
			if (shouldLog(message))
			{
				Log.d(LOG_TAG, "Sent Msg : " + message);
			}
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, "sendMessage failed. e.getMessage() = " + e.getMessage());
		}
		finally
		{
			if (socket != null)
			{
				socket.close();
			}
		}
	}
	
	public void sendMessage(String message)
	{
		sendMessage(message, getLeaderIP());
	}
	
	private void broadcast(String message)
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
			
			if (shouldLog(message))
			{
				Log.d(LOG_TAG, "Broadcast : message = " + message);
			}
			
			socket = new DatagramSocket();
			socket.setBroadcast(true);
			socket.send(packet);
		}
		catch (SocketException e)
		{
			Log.e(LOG_TAG, "Broadcast() : SocketException, message = " + message + ", e.getMessage() = " + e.getMessage());			
		}
		catch (UnknownHostException e)
		{
			Log.e(LOG_TAG, "Broadcast() : UnknownHostException, message = " + message + ", e.getMessage() = " + e.getMessage());			
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "Broadcast() : IOException, message = " + message + ", e.getMessage() = " + e.getMessage());			
		}
		finally
		{
			if (socket != null)
			{
				socket.close();
			}
		}
	}

	private boolean shouldLog(String message)
	{
		return message.startsWith(Messages.MSG_PREFIX_PING) == false && message.startsWith(Messages.MSG_PREFIX_PONG) == false;
	}
	
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

	public synchronized void nap(long milliSecsToSleep)
	{
		try
		{
			Thread.sleep(milliSecsToSleep);
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
	
	public User getMe()
	{
		return mMe;
	}
	
	public String getMyIP()
	{
		return mMe.getIPAddress();
	}
	
	public ArrayList<User> getUsers()
	{
		ArrayList<User> arrayListUsers = new ArrayList<User>(mMapIPToUser.values());
		
		return arrayListUsers;
	}
	
	public void writePropertyToFile(String fileName, String propertyName, String value)
	{
		Properties properties = new Properties();
		FileInputStream fis = null;
		FileOutputStream fos = null;
		
		try {
			// Load the properties from the properties file
			try {
				fis = openFileInput(fileName);
				properties.load(fis);
			} catch (FileNotFoundException e) {
			}
			
			// Set the new value of the property
			properties.setProperty(propertyName, value);
			
			fos = openFileOutput(fileName, MODE_PRIVATE);
			properties.store(fos, null);
			
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "writePropertyToFile : Properties file was not found");
		} catch (IOException e) {
			Log.e(LOG_TAG, "writePropertyToFile : An IOException has accured. StackTrace = " + e.getStackTrace().toString());
		} finally {
			try {
				fos.close();
				fis.close();
			} catch (Exception e) {
			}
		}
	}
	
	public String readPropertyFromFile(String fileName, String propertyName)
	{
		Properties properties = new Properties();
		FileInputStream fis = null;
		String returnedValue = "";
		
		try {
			fis = openFileInput(fileName);
			properties.load(fis);

			returnedValue = properties.getProperty(propertyName);
			
			if (returnedValue == null)
			{
				returnedValue = "";
			}
			
		} catch (FileNotFoundException e) {
			Log.e(LOG_TAG, "readPropertyFromFile : Properties file was not found");
		} catch (IOException e) {
			Log.e(LOG_TAG, "readPropertyFromFile : An IOException has accured. StackTrace = " + e.getStackTrace().toString());
		} finally {
			try {
				fis.close();
			} catch (Exception e) {
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
	
//	public boolean didRunBefore()
//	{
//		return mDidRunBefore;
//	}

	/**
	 * When receiving from the leader a picture of another user, the {@link ImageReceiver} calls this method when he has finished getting the
	 * picture from the leader and it's ready to be used.
	 */
	public void imageReady( String imageName) {
		// The Looper.prepare() method is needed for a thread to get a handler, otherwise it crashes
		Looper.prepare();
		Message msg = ActivityUserDetails.instance.getImageUpdateHandler().obtainMessage();
		msg.obj = imageName;
		
		Log.d(LOG_TAG, "Image received: " + imageName);
		
		// Update activity with user image
		ActivityUserDetails.instance.getImageUpdateHandler().sendMessage(msg);
	}
}
