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
	
	// TODO : If in fact it's useless, delete this NewUserReply message
//	// NewUser : Leader sends as a reply to NEW_USER with its own IP Address
//	//           Structure : NewUserReply leaderIPAddress
//	public static final String MSG_PREFIX_NEW_USER_REPLY = "NewUserReply";
	
	// NewUser : Client asks for another user's full details
	//           Structure : GetUserDetails targetIPAddress(The unique identifier)
	public static final String MSG_PREFIX_GET_USER_DETAILS = "GetUserDetails";
	
	// NewUser : Leader sends to user, asking for his details
	//           Structure : GiveDetails askerIPAddress
	public static final String MSG_PREFIX_GIVE_DETAILS = "GiveDetails";
	
	// NewUser : Client sends to answer the leader, giving his own details.
	//           Leader sends to reply the asking user with the details
	//           Structure : UserDetails askerIPAddress *all the details. TBD*
	public static final String MSG_PREFIX_USER_DETAILS = "UserDetails";
	
	// NewUser : Client (NOT Leader) sends the leader it is disconnecting
	//           Structure : UserDisconnected IPAddress
	public static final String MSG_PREFIX_USER_DISCONNECTED = "UserDisconnected"; 

	// ChatMessage : User sends a chat message to another user whenever the "Send" button is pressed in the chat Activity
	//               Structure : ChatMessage sourceIPAddress, targetIPAddress, messageContents
	public static final String MSG_PREFIX_CHAT_MESSAGE = "ChatMessage";
	
	// MakeUserBackup : The leader sends this message to a user to let him know that he is the backup (In case the leader disconnects)
	//               	Structure : MakeUserBackup
//	public static final String MSG_PREFIX_MAKE_CLIENT_BACKUP = "MakeUserBackup";
//	private static final String DUMMY_STRING = "DummyString";
	
//	public static final String MSG_EOM = "DONE";
	
	
	public static final String MSG_PARAMS_SEPARATOR = "~";

	private static final int INDEX_PARAM_PREFIX = 0;
	
	
	public static String getPrefix(String message)
	{
		return message.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_PREFIX];
	}
	
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
	// the message string couldn't directly be passed to their Ctor as often there is
	// already a Ctor receiving a String, such as a username
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
	public static class MessageNewUser
	{
		private final int INDEX_PARAM_IP_ADDRESS = 1;
		private final int INDEX_PARAM_USERNAME = 2;
		private final int INDEX_PARAM_DATE_BIRTH_YEAR = 3;
		private final int INDEX_PARAM_DATE_BIRTH_MONTH = 4;
		private final int INDEX_PARAM_DATE_BIRTH_DAY = 5;
		private final int INDEX_PARAM_SEX = 6;
		private final int INDEX_PARAM_PICTURE = 7;
		
//		private List<String> listParams = new LinkedList<String>();
		private HashMap<Integer, String> mMapIndexToValue = new HashMap<Integer, String>();
		
//		private String mUsername = "";
//		private String mIPAddress = "";
//		private int    mBirthYear;
//		private int    mBirthMonth;
//		private int    mBirthDay;
//		private String mSex = "";
//		private String mPicture = "";
		
		public MessageNewUser(String ipAddress, String username, int birthYear, int birthMonth, int birthDay, String sex, String picture)
		{
//			listParams.ad
			mMapIndexToValue.put(INDEX_PARAM_PREFIX, MSG_PREFIX_NEW_USER);
			mMapIndexToValue.put(INDEX_PARAM_IP_ADDRESS, ipAddress);
			mMapIndexToValue.put(INDEX_PARAM_USERNAME, username);
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_YEAR, String.valueOf(birthYear));
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_MONTH, String.valueOf(birthMonth));
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_DAY, String.valueOf(birthDay));
			mMapIndexToValue.put(INDEX_PARAM_SEX, sex);
			mMapIndexToValue.put(INDEX_PARAM_PICTURE, picture);
//			mIPAddress = ipAddress;
//			mUsername = username;
//			mBirthYear = birthYear;
//			mBirthMonth = birthMonth;
//			mBirthDay = birthDay;
//			mSex = sex;
//			mPicture = picture;
		}
		
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
			mMapIndexToValue.put(INDEX_PARAM_PICTURE, arrParams[INDEX_PARAM_PICTURE]);
//			mIPAddress = arrParams[INDEX_PARAM_IP_ADDRESS];
//			mUsername = arrParams[INDEX_PARAM_USERNAME];
//			mBirthYear = arrParams[INDEX_PARAM_DATE_BIRTH_YEAR];
//			mBirthMonth = arrParams[INDEX_PARAM_DATE_BIRTH_MONTH];
//			mBirthDay = arrParams[INDEX_PARAM_DATE_BIRTH_DAY];
//			mSex = arrParams[INDEX_PARAM_SEX];
//			mPicture = arrParams[INDEX_PARAM_PICTURE];
		}
		
		public MessageNewUser(User currUser)
		{
			mMapIndexToValue.put(INDEX_PARAM_PREFIX, MSG_PREFIX_NEW_USER);
			mMapIndexToValue.put(INDEX_PARAM_IP_ADDRESS, currUser.getIPAddress());
			mMapIndexToValue.put(INDEX_PARAM_USERNAME, currUser.getFullName());
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_YEAR, String.valueOf(currUser.getDateBirth().get(GregorianCalendar.YEAR)));
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_MONTH, String.valueOf(currUser.getDateBirth().get(GregorianCalendar.MONTH)));
			mMapIndexToValue.put(INDEX_PARAM_DATE_BIRTH_DAY, String.valueOf(currUser.getDateBirth().get(GregorianCalendar.DAY_OF_MONTH)));
			mMapIndexToValue.put(INDEX_PARAM_SEX, currUser.getSex());
			// TODO : Get the real picture data
			mMapIndexToValue.put(INDEX_PARAM_PICTURE, "StubPic"); //currUser.getPicture());
//			mIPAddress = currUser.getIPAddress();
//			mUsername = currUser.getFullName();
//			mDateBirth = currUser.getDateBirth().toString();
//			mSex = currUser.getSex();
//			mPicture = "PicData";
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
		
		public String getPicture()
		{
			return mMapIndexToValue.get(INDEX_PARAM_PICTURE);
		}
		
		public String toString()
		{
			return Messages.makeMessageString(mMapIndexToValue);
//			return MSG_PREFIX_NEW_USER + MSG_PARAMS_SEPARATOR +
//						mUsername  + MSG_PARAMS_SEPARATOR + mIPAddress + MSG_PARAMS_SEPARATOR +
//						mDateBirth + MSG_PARAMS_SEPARATOR + mSex       + MSG_PARAMS_SEPARATOR + mPicture; 
		}
	}
	
	// TODO : If in fact it's useless, delete this NewUserReply message
//	public static class MessageNewUserReply
//	{
//		private final int INDEX_PARAM_LEADER_IP = 1;
//		
//		private String mLeaderIP = "";
//		
//		public MessageNewUserReply(String leaderIP)
//		{
//			mLeaderIP = leaderIP;
//		}
//		
//		public MessageNewUserReply(Message message)
//		{
//			String strMessage = message.toString();
//			
//			mLeaderIP = strMessage.split(MSG_PARAMS_SEPARATOR)[INDEX_PARAM_LEADER_IP];
//		}
//		
//		
//		public String getLeaderIP()
//		{
//			return mLeaderIP;
//		}
//		
//		public String toString()
//		{
//			return MSG_PREFIX_NEW_USER_REPLY + MSG_PARAMS_SEPARATOR +
//						mLeaderIP;
//		}
//	}
	
	public static class MessageGetUserDetails
	{
		private final int INDEX_PARAM_TARGET_IP_ADDRESS = 1;
		private final int INDEX_PARAM_TARGET_USERNAME = 2;
		
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
		private final int INDEX_PARAM_ASKER_IP_ADDRESS = 1;
		
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
	
	// Structure : UserDetails askerIPAddress *all the details. TBD*
	public static class MessageUserDetails
	{
	    private final String FIELD_NOT_FILLED = "-- Not Filled By User --";

	    private final int INDEX_PARAM_ASKER_IP_ADDRESS = 1;
		private final int INDEX_PARAM_IP_ADDRESS = 2;
		private final int INDEX_PARAM_HOBBIES = 3;
		private final int INDEX_PARAM_FAVORITE_MUSIC = 4;
		
		private String mAskerIPAddress = "";
		private String mIPAddress = "";
		private String mHobbies = FIELD_NOT_FILLED;
		private String mFavoriteMusic = FIELD_NOT_FILLED;
		
		
//		public MessageUserDetails(String askerIPAddress)
//		{
//			mAskerIPAddress = askerIPAddress;
//		}
		
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
		private final int INDEX_PARAM_IP_ADDRESS = 1;
		
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
		private final int INDEX_PARAM_CHAT_USER = 1;
		private final int INDEX_PARAM_CHAT_SOURCE_USER_IP = 2;
		private final int INDEX_PARAM_CHAT_TARGET_USER_IP = 3;
		private final int INDEX_PARAM_CHAT_MESSAGE_CONTENTS = 4;

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
	
	
//	public static class MessageMakeClientBackup
//	{
//		public MessageMakeClientBackup()
//		{
//		}
//		
//
//		public String toString()
//		{
//			return MSG_PREFIX_MAKE_CLIENT_BACKUP + MSG_PARAMS_SEPARATOR + DUMMY_STRING;
//		}
//	}
	
//	public static String createMessageAskForUsers()
//	{
//		String msg = MSG_PREFIX_NEW_USER;
//		
//		return msg;
//	}
}
