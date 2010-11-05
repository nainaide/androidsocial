package main.main;

import java.util.GregorianCalendar;
import java.util.HashMap;

public class Messages
{
	// Ping : Leader sends periodically to see if a client is still connected
	//        Structure : Ping
	public static final String MSG_PREFIX_PING = "Ping";
	
	// Pong : Client answers the leader's ping
	//        Structure : Ping ipAddress
	public static final String MSG_PREFIX_PONG = "Pong";
	
	// NewUser : Client sends to say he joined the network.
	//           Leader sends to a user to notify that another user joined, and sends the basic information
	//           Structure : NewUser IPAddress username dateBirth Sex Picture
	public static final String MSG_PREFIX_NEW_USER = "NewUser";
	
	// GetUserDetails : Client asks for another user's full details
	//           		Structure : GetUserDetails targetIPAddress(The unique identifier)
	public static final String MSG_PREFIX_GET_USER_DETAILS = "GetUserDetails";
	
	// GiveDetails : Leader sends to user, asking for his details
	//           	 Structure : GiveDetails askerIPAddress
	public static final String MSG_PREFIX_GIVE_DETAILS = "GiveDetails";
	
	// UserDetails : Client sends to answer the leader, giving his own details.
	//           	 Leader sends to reply the asking user with the details
	//           	 Structure : UserDetails askerIPAddress ipAddress hobbies favoriteMusic
	public static final String MSG_PREFIX_USER_DETAILS = "UserDetails";
	
	// UserDisconnected : Client (NOT Leader) sends the leader it is disconnecting
	//           		  Structure : UserDisconnected IPAddress
	public static final String MSG_PREFIX_USER_DISCONNECTED = "UserDisconnected"; 

	// ChatMessage : User sends a chat message to another user whenever the "Send" button is pressed in the chat Activity
	//               Structure : ChatMessage sourceIPAddress, targetIPAddress, messageContents
	public static final String MSG_PREFIX_CHAT_MESSAGE = "ChatMessage";
	
	
	public static final String MSG_PARAMS_SEPARATOR = "~";

	private static final int INDEX_PARAM_PREFIX = 0;
	
	
	public static String getPrefix(String message)
	{
		return message.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_PREFIX];
	}
	
	/**
	 * Gets a map of Index to Value, and makes a String out of all the values with the MSG_PARAMS_SEPARATOR as the separator
	 * between the values
	 * 
	 * @param mapIndexToValue - The map to create the string out of its values
	 * @return The string of values
	 */
	private static String makeMessageString(HashMap<Integer, String> mapIndexToValue)
	{
		String strMsg = "";
		
		for (String currValue : mapIndexToValue.values())
		{
			strMsg += MSG_PARAMS_SEPARATOR + currValue;
		}
		
		// Remove the first MSG_PARAM_SEPARATOR
		strMsg = strMsg.replaceFirst(MSG_PARAMS_SEPARATOR, "");
		
		return strMsg;
	}
	
	// This class was made as a way to initialize other Message___ objects, since
	// the message string couldn't directly be passed to their Constructor as often there is
	// already a Constructor receiving a String, such as a username
	public static class Message
	{
		String mMessage = "";
		
		public Message(String message)
		{
			mMessage = message;
		}
		
		public String getMessage()
		{
			return mMessage;
		}
		
		public String toString()
		{
			return getMessage();
		}
	}
	
	public static class MessagePing
	{
		public MessagePing()
		{
		}
		
		
		public String toString()
		{
			return MSG_PREFIX_PING;
		}
	}
	
	public static class MessagePong
	{
		private final int INDEX_PARAM_USERNAME = 1;
		
		private String mIPAddress = "";
		
		public MessagePong(String ipAddress)
		{
			mIPAddress = ipAddress;
		}
		
		public MessagePong(Message message)
		{
			String strMessage = message.getMessage();
			
			mIPAddress = strMessage.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_USERNAME];
		}

		public String getIPAddress()
		{
			return mIPAddress;
		}
		
		public String toString()
		{
			return MSG_PREFIX_PONG + MSG_PARAMS_SEPARATOR + mIPAddress;
		}
	}
	
	// Structure : NewUser username IPAddress dateBirth Sex Picture
	// Note : This message uses a different implementation using a HashMap instead of an array like all the rest (The code for using an array is
	//        still present and is commented). This was a test and it worked. The other messages can also be converted to also use the more general and
	//        less order-dependent HashTable.
	public static class MessageNewUser
	{
		private static final int INDEX_PARAM_IP_ADDRESS = 1;
		private static final int INDEX_PARAM_USERNAME = 2;
		private static final int INDEX_PARAM_DATE_BIRTH_YEAR = 3;
		private static final int INDEX_PARAM_DATE_BIRTH_MONTH = 4;
		private static final int INDEX_PARAM_DATE_BIRTH_DAY = 5;
		private static final int INDEX_PARAM_SEX = 6;
		
//		private List<String> listParams = new LinkedList<String>();
		private HashMap<Integer, String> mMapIndexToValue = new HashMap<Integer, String>();
		
//		private String mUsername = "";
//		private String mIPAddress = "";
//		private int    mBirthYear;
//		private int    mBirthMonth;
//		private int    mBirthDay;
//		private String mSex = "";
		
		public MessageNewUser(Message message)
		{
			String strMessage = message.toString();
			String[] arrParams = strMessage.split(MSG_PARAMS_SEPARATOR);
			
			mMapIndexToValue.put(INDEX_PARAM_PREFIX, MSG_PREFIX_NEW_USER);
			mMapIndexToValue.put(INDEX_PARAM_IP_ADDRESS, arrParams[INDEX_PARAM_IP_ADDRESS]);
			mMapIndexToValue.put(INDEX_PARAM_USERNAME, arrParams[INDEX_PARAM_USERNAME]);
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_YEAR, arrParams[INDEX_PARAM_DATE_BIRTH_YEAR]);
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_MONTH, arrParams[INDEX_PARAM_DATE_BIRTH_MONTH]);
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_DAY, arrParams[INDEX_PARAM_DATE_BIRTH_DAY]);
			mMapIndexToValue.put(INDEX_PARAM_SEX, arrParams[INDEX_PARAM_SEX]);
//			mIPAddress = arrParams[INDEX_PARAM_IP_ADDRESS];
//			mUsername = arrParams[INDEX_PARAM_USERNAME];
//			mBirthYear = arrParams[INDEX_PARAM_DATE_BIRTH_YEAR];
//			mBirthMonth = arrParams[INDEX_PARAM_DATE_BIRTH_MONTH];
//			mBirthDay = arrParams[INDEX_PARAM_DATE_BIRTH_DAY];
//			mSex = arrParams[INDEX_PARAM_SEX];
		}
		
		public MessageNewUser(User currUser)
		{
			mMapIndexToValue.put(INDEX_PARAM_PREFIX, MSG_PREFIX_NEW_USER);
			mMapIndexToValue.put(INDEX_PARAM_IP_ADDRESS, currUser.getIPAddress());
			mMapIndexToValue.put(INDEX_PARAM_USERNAME, currUser.getUsername());
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_YEAR, String.valueOf(currUser.getDateBirth().get(GregorianCalendar.YEAR)));
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_MONTH, String.valueOf(currUser.getDateBirth().get(GregorianCalendar.MONTH)));
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_DAY, String.valueOf(currUser.getDateBirth().get(GregorianCalendar.DAY_OF_MONTH)));
			mMapIndexToValue.put(INDEX_PARAM_SEX, currUser.getSex());
//			mIPAddress = currUser.getIPAddress();
//			mUsername = currUser.getFullName();
//			mDateBirth = currUser.getDateBirth().toString();
//			mSex = currUser.getSex();
		}

		public String getUsername()
		{
			return mMapIndexToValue.get(INDEX_PARAM_USERNAME);
		}
		
		public String getIPAddress()
		{
			return mMapIndexToValue.get(INDEX_PARAM_IP_ADDRESS);
		}
		
		public String getDateYear()
		{
			return mMapIndexToValue.get(INDEX_PARAM_DATE_BIRTH_YEAR);
		}
		
		public String getDateMonth()
		{
			return mMapIndexToValue.get(INDEX_PARAM_DATE_BIRTH_MONTH);
		}
		
		public String getDateDay()
		{
			return mMapIndexToValue.get(INDEX_PARAM_DATE_BIRTH_DAY);
		}
		
		public String getSex()
		{
			return mMapIndexToValue.get(INDEX_PARAM_SEX);
		}
		
		public String toString()
		{
			return Messages.makeMessageString(mMapIndexToValue);
//			return MSG_PREFIX_NEW_USER + MSG_PARAMS_SEPARATOR +
//						mUsername  + MSG_PARAMS_SEPARATOR + mIPAddress + MSG_PARAMS_SEPARATOR +
//						mDateBirth + MSG_PARAMS_SEPARATOR + mSex; 
		}
	}
	
	public static class MessageGetUserDetails
	{
		private static final int INDEX_PARAM_TARGET_IP_ADDRESS = 1;
		private static final int INDEX_PARAM_TARGET_USERNAME = 2;
		
		private String mTargetIPAddress = "";
		private String mTargetUserName = "";
		
		
		public MessageGetUserDetails(String targetIPAddress, String targetUsername)
		{
			mTargetIPAddress = targetIPAddress;
			mTargetUserName = targetUsername;
		}
		
		public MessageGetUserDetails(Message message)
		{
			String strMessage = message.toString();
			
			mTargetIPAddress = strMessage.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_TARGET_IP_ADDRESS];
			mTargetUserName = strMessage.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_TARGET_USERNAME];
		}
		
		public String getTargetUserName( ) {
			return mTargetUserName;
		}
		
		public String getTargetIPAddress()
		{
			return mTargetIPAddress;
		}
		
		public String toString()
		{
			return MSG_PREFIX_GET_USER_DETAILS + MSG_PARAMS_SEPARATOR +
						mTargetIPAddress + MSG_PARAMS_SEPARATOR +
						mTargetUserName;
		}
	}
	
	public static class MessageGiveDetails
	{
		private static final int INDEX_PARAM_ASKER_IP_ADDRESS = 1;
		
		private String mAskerIPAddress = "";
		
		public MessageGiveDetails(String askerIPAddress)
		{
			mAskerIPAddress = askerIPAddress;
		}
		
		public MessageGiveDetails(Message message)
		{
			String strMessage = message.toString();
			
			mAskerIPAddress = strMessage.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_ASKER_IP_ADDRESS];
		}
		
		
		public String getAskerIPAddress()
		{
			return mAskerIPAddress;
		}
		
		public String toString()
		{
			return MSG_PREFIX_GIVE_DETAILS + MSG_PARAMS_SEPARATOR + 
						mAskerIPAddress;
		}
	}
	
	// Structure : UserDetails askerIPAddress ipAddress hobbies favoriteMusic
	public static class MessageUserDetails
	{
	    private static final String FIELD_NOT_FILLED = "-- Not Filled By User --";

	    private static final int INDEX_PARAM_ASKER_IP_ADDRESS = 1;
		private static final int INDEX_PARAM_IP_ADDRESS = 2;
		private static final int INDEX_PARAM_HOBBIES = 3;
		private static final int INDEX_PARAM_FAVORITE_MUSIC = 4;
		
		private String mAskerIPAddress = "";
		private String mIPAddress = "";
		private String mHobbies = FIELD_NOT_FILLED;
		private String mFavoriteMusic = FIELD_NOT_FILLED;
		
		
		public MessageUserDetails(Message message)
		{
			String strMessage = message.toString();
			String[] arrParams = strMessage.split(MSG_PARAMS_SEPARATOR);
			
			mAskerIPAddress = arrParams[INDEX_PARAM_ASKER_IP_ADDRESS]; 
			mIPAddress = arrParams[INDEX_PARAM_IP_ADDRESS];
			mHobbies = arrParams[INDEX_PARAM_HOBBIES];
			mFavoriteMusic = arrParams[INDEX_PARAM_FAVORITE_MUSIC];
		}
		
		public MessageUserDetails(User user, String askerIPAddress)
		{
			mIPAddress = user.getIPAddress();
			mAskerIPAddress = askerIPAddress;
			mHobbies = user.getHobbies().equals("") ? mHobbies : user.getHobbies();
			mFavoriteMusic = user.getFavoriteMusic().equals("") ? mFavoriteMusic : user.getFavoriteMusic();
		}
		
		
		public String getAskerIPAddress()
		{
			return mAskerIPAddress;
		}
		
		public String getIPAddress()
		{
			return mIPAddress;
		}
		
		public String getHobbies()
		{
			return mHobbies;
		}
		
		public String getFavoriteMusic()
		{
			return mFavoriteMusic;
		}
		
		public String toString()
		{
			return MSG_PREFIX_USER_DETAILS + MSG_PARAMS_SEPARATOR +
						mAskerIPAddress + MSG_PARAMS_SEPARATOR +
						mIPAddress + MSG_PARAMS_SEPARATOR +
						mHobbies + MSG_PARAMS_SEPARATOR +
						mFavoriteMusic;
		}
	}

	// Structure : UserDisconnected IPAddress
	public static class MessageUserDisconnected
	{
		private static final int INDEX_PARAM_IP_ADDRESS = 1;
		
		String mIPAddress = "";
		
		public MessageUserDisconnected(String ipAddress)
		{
			mIPAddress = ipAddress;
		}
		
		public MessageUserDisconnected(Message message)
		{
			String strMessage = message.toString();
			
			mIPAddress = strMessage.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_IP_ADDRESS];
		}
		
		
		public String getIPAddress()
		{
			return mIPAddress;
		}
		
		public String toString()
		{
			return MSG_PREFIX_USER_DISCONNECTED + MSG_PARAMS_SEPARATOR + mIPAddress;
		}
	}
	
	public static class MessageChatMessage
	{
		private static final int INDEX_PARAM_CHAT_USER = 1;
		private static final int INDEX_PARAM_CHAT_SOURCE_USER_IP = 2;
		private static final int INDEX_PARAM_CHAT_TARGET_USER_IP = 3;
		private static final int INDEX_PARAM_CHAT_MESSAGE_CONTENTS = 4;

		private String mChatMessageContents = "";
		private String mUsername = "";
		private String mSourceUserIp = "";
		private String mTargetUserIp ="";
		
		public MessageChatMessage(String chatMessageContents, String username, String sourceIp, String targetIp)
		{
			mChatMessageContents = chatMessageContents;
			mUsername = username;
			mSourceUserIp = sourceIp;
			mTargetUserIp = targetIp;
		}
		
		public MessageChatMessage(Message message)
		{
			String strMessage = message.toString();
			
			mChatMessageContents = strMessage.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_CHAT_MESSAGE_CONTENTS];
			mUsername = strMessage.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_CHAT_USER];
			mSourceUserIp = strMessage.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_CHAT_SOURCE_USER_IP];
			mTargetUserIp = strMessage.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_CHAT_TARGET_USER_IP];
		}
		
		
		public String getChatMessageContents()
		{
			return mChatMessageContents;
		}
		
		public String getChatMessageUser()
		{
			return mUsername;
		}
		
		public String getSourceUserIP()
		{
			return mSourceUserIp;
		}
		
		public String getTargetUserIP()
		{
			return mTargetUserIp;
		}
		
		public String toString()
		{
			return MSG_PREFIX_CHAT_MESSAGE + MSG_PARAMS_SEPARATOR +
					mUsername + MSG_PARAMS_SEPARATOR +
					mSourceUserIp + MSG_PARAMS_SEPARATOR +
					mTargetUserIp + MSG_PARAMS_SEPARATOR +
					mChatMessageContents;
		}
	}
}
