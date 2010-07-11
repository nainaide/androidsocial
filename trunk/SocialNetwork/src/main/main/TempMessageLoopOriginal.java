//package main.main;
//
//import main.main.ApplicationSocialNetwork.NetControlState;
//import android.util.Log;
//
//public class TempMessageLoopOriginal {
//	if (mCurrState == NetControlState.CLIENT)
//	{
////		if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER_REPLY))
////		{
////			// Get the leader's IP
////			String leaderIP = new Messages.MessageNewUserReply(msgReceived).getLeaderIP(); // Messages.getMessageParamByIndex()
////			
//////			mLeaderIP = leaderIP;
////		}
//		if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
//		{
//			Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(msgReceived);
//			
//			// Check that the new user isn't me (When a user joins, the leader broadcasts it so he will
//			//                                   also get the message about it, and should ignore it)
//			if (msgNewUser.getIPAddress().equals(mMe.getIPAddress()) == false)
//			{
//				User newUser = new User(msgNewUser);
//				
//				addUser(newUser);
//			}
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DISCONNECTED))
//		{
//			Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(msgReceived);
//			User userDisconnected = mMapIPToUser.get(msgUserDisconnected.getIPAddress());
//			
//			removeUser(userDisconnected.getIPAddress());
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_GIVE_DETAILS))
//		{
//			// Send a UserDetails message to the leader with my details
//			Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(msgReceived);
//			Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(mMe, msgGiveDetails.getAskerIPAddress()); //, "Stam Hobbies", "Stam Fav Music");
//			
//			sendMessage(msgUserDetails.toString(), IP_LEADER); //mLeaderIP);
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DETAILS))
//		{
//			Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(msgReceived);
////			notifyActivityUserDetails(msgUserDetails.toString());
//			notifyActivityUserDetails(msgUserDetails);
//			// TODO : Complete this
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_CHAT_MESSAGE))
//		{
//			Messages.MessageChatMessage msgChat = new Messages.MessageChatMessage(msgReceived);
//			Log.d(LOG_TAG, "got chat and will update, source is : "+msgChat.getSourceUserIP());
//			UpdateOpenChats(msgChat.getChatMessageUser(),msgChat.getSourceUserIP(), msgChat.getChatMessageContents());
//			notifyActivityChat(msgChat);
//			// TODO : Complete this
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_PING))
//		{
//			// Send a pong message to show I'm alive
//			Messages.MessagePong msgPong = new Messages.MessagePong(mMe.getIPAddress());
//			
//			sendMessageUDP(msgPong.toString(), IP_LEADER); //mLeaderIP);
//			
//			mLeaderLastPing = System.currentTimeMillis();
//		}
//	}
//	
//	
//	else if (mCurrState == NetControlState.LEADER)
//	{
//		if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
//		{
//			Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(msgReceived);
//			String ipAddressNewUser = msgNewUser.getIPAddress();
//			User newUser = new User(msgNewUser);
//
//			// Send back to the user a "NewUserReply" message so he'd know my IP
////			Messages.MessageNewUserReply msgNewUserReply = new Messages.MessageNewUserReply(mMe.getIPAddress());
////			
////			sendMessage(msgNewUserReply.toString(), ipAddressNewUser);
//			
//			// Send the new user a "NewUser" message for every other client so he knows them
//			for (User currUser : mMapIPToUser.values())
//			{
//				Messages.MessageNewUser msgNewUserOfExistingUserForNewcomerToKnow = new Messages.MessageNewUser(currUser);
//				
//				sendMessage(msgNewUserOfExistingUserForNewcomerToKnow.toString(), ipAddressNewUser);
//			}
//			
//			// Send the user a "NewUser" message for me (The leader)
//			Messages.MessageNewUser msgNewUserLeader = new Messages.MessageNewUser(mMe);
//			
//			sendMessage(msgNewUserLeader.toString(), ipAddressNewUser);
//			
//			// Broadcast to everybody that this user has joined us, so they know him
//			broadcast(msgNewUser.toString());
//			
//			// Add the new users to my clients list
//			addUser(newUser);
////			mMapIPToUser.put(ipAddressNewUser, newUser);
////			notifyActivities();
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_GET_USER_DETAILS))
//		{
//			Messages.MessageGetUserDetails msgGetUserDetails = new Messages.MessageGetUserDetails(msgReceived);
//			String targetIPAddress = msgGetUserDetails.getTargetIPAddress();
////			String askerIPAddress = socket.getInetAddress().getHostAddress();
//			String askerIPAddress = packet.getAddress().getHostAddress();
//			Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(askerIPAddress);
//			
//			sendMessage(msgGiveDetails.toString(), targetIPAddress);
//			
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DETAILS))
//		{
//			// Get the asker's IP and send him the details
//			Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(msgReceived);
//			String askerIP = msgUserDetails.getAskerIPAddress();
//			
//Log.d(LOG_TAG, "Leader got user details. askerIP = " + askerIP + ", getMyIP() = " + getMyIP());
//
//			if (askerIP.equals(getMyIP())) // || askerIP.equals(LOCAL_HOST))
//			{
//				// If I'm the leader and I asked for the details, process them
////				notifyActivityUserDetails(msgUserDetails.toString());
//				notifyActivityUserDetails(msgUserDetails);
//			}
//			else
//			{
//				// If a client asked for the details, send them to him
//				sendMessage(strMsgReceived, askerIP);
//			}
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_PONG))
//		{
//			Messages.MessagePong msgPong = new Messages.MessagePong(msgReceived);
//			String ipAddressPongged = msgPong.getIPAddress();
//			
//			User userPongged = mMapIPToUser.get(ipAddressPongged);
//			
//			userPongged.setLastPongTime(System.currentTimeMillis());
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_CHAT_MESSAGE))
//		{
//			Messages.MessageChatMessage msgChat = new Messages.MessageChatMessage(msgReceived);
//			String targetIP = msgChat.getTargetUserIP();
//
//			if (targetIP.equals(getMyIP())) // || targetIP.equals(LOCAL_HOST))
//			{
//Log.d(LOG_TAG, "Chat for leader");									
//				UpdateOpenChats(msgChat.getChatMessageUser(),msgChat.getSourceUserIP(), msgChat.getChatMessageContents());
//				notifyActivityChat(msgChat);
//			}
//			else
//			{
//Log.d(LOG_TAG, "Chat NOT for leader, passing on");									
//				// If a client asked for the details, send them to him
//				sendMessage(strMsgReceived, targetIP);
//			}
//		}
//	}
//}
