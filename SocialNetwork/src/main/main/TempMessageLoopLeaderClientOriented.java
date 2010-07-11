//package main.main;
//
//import main.main.ApplicationSocialNetwork.NetControlState;
//import android.util.Log;
//
//public class TempMessageLoopLeaderClientOriented {
//	// Deal with the messages that the leader takes care of as a leader
//	if (mCurrState == NetControlState.LEADER)
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
////			addUser(newUser);
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DISCONNECTED))
//		{
//			// As the leader, we should send to all the other users that he disconnected
//			Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(msgReceived);
////			String ipAddressUserDisconnected = msgUserDisconnected.getIPAddress();
////			User newUser = mMapIPToUser.get(ipAddressUserDisconnected);
//
//			// Broadcast to everybody that this user has disconnected, so they remove him from their list
//			broadcast(msgUserDisconnected.toString());
//			
//			// Remove the user from my clients list
////			removeUser(ipAddressUserDisconnected);
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_GET_USER_DETAILS))
//		{
//			Messages.MessageGetUserDetails msgGetUserDetails = new Messages.MessageGetUserDetails(msgReceived);
//			String targetIPAddress = msgGetUserDetails.getTargetIPAddress();
////			String askerIPAddress = socket.getInetAddress().getHostAddress();
//			String askerIPAddress = packet.getAddress().getHostAddress();
//			
//			// Check if the target user (whose details the asker wants) is the leader it self
//			if (targetIPAddress.equals(getMyIP()))
//			{
//				// Send the asker my details
//				Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(mMe, askerIPAddress);
//				
//				sendMessage(msgUserDetails.toString(), askerIPAddress);
//			}
//			else
//			{
//				// Send the target user that another user wants his details
//				Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(askerIPAddress);
//				
//				sendMessage(msgGiveDetails.toString(), targetIPAddress);
//			}						
//		}
//		else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DETAILS))
//		{
//			// Get the asker's IP and send him the details
//			Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(msgReceived);
//			String askerIP = msgUserDetails.getAskerIPAddress();
//			
//Log.d(LOG_TAG, "Leader got user details. askerIP = " + askerIP + ", getMyIP() = " + getMyIP());
//
//			// If it wasn't the leader who asked for the user's details, then pass the details to the asker
//			if (askerIP.equals(getMyIP()) == false)
//			{
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
//			// If the message isn't for the leader, then pass it to the target user
//			if (targetIP.equals(getMyIP()) == false)
//			{
//				sendMessage(strMsgReceived, targetIP);
//			}
//		}
//	}
//	// Deal with the messages that only a client who isn't also a leader takes care of
//	else if (mCurrState == NetControlState.CLIENT)
//	{
//		if (msgPrefix.equals(Messages.MSG_PREFIX_GIVE_DETAILS))
//		{
//			// Send a UserDetails message to the leader with my details
//			Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(msgReceived);
//			Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(mMe, msgGiveDetails.getAskerIPAddress()); //, "Stam Hobbies", "Stam Fav Music");
//			
//			sendMessage(msgUserDetails.toString(), IP_LEADER); //mLeaderIP);
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
//	// Deal with the rest (If we got here, then we're either a normal client or the leader but he should treat
//	// the message as a client, like a Chat message that's directed to him and not to another user via him)
//	if (msgPrefix.equals(Messages.MSG_PREFIX_NEW_USER))
//	{
//		Messages.MessageNewUser msgNewUser = new Messages.MessageNewUser(msgReceived);
//		
//		// Check that the new user isn't me (When a user joins, the leader broadcasts it so he will
//		//                                   also get the message about it, and should ignore it)
//		if (msgNewUser.getIPAddress().equals(mMe.getIPAddress()) == false)
//		{
//			User newUser = new User(msgNewUser);
//			
//			addUser(newUser);
//		}
//	}
//	else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DISCONNECTED))
//	{
//		Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(msgReceived);
//		User userDisconnected = mMapIPToUser.get(msgUserDisconnected.getIPAddress());
//		
//		removeUser(userDisconnected.getIPAddress());
//	}
//	else if (msgPrefix.equals(Messages.MSG_PREFIX_GIVE_DETAILS))
//	{
//		// Send a UserDetails message to the leader with my details
//		Messages.MessageGiveDetails msgGiveDetails = new Messages.MessageGiveDetails(msgReceived);
//		Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(mMe, msgGiveDetails.getAskerIPAddress()); //, "Stam Hobbies", "Stam Fav Music");
//		
//		sendMessage(msgUserDetails.toString(), IP_LEADER); //mLeaderIP);
//	}
//	else if (msgPrefix.equals(Messages.MSG_PREFIX_USER_DETAILS))
//	{
//		Messages.MessageUserDetails msgUserDetails = new Messages.MessageUserDetails(msgReceived);
////		notifyActivityUserDetails(msgUserDetails.toString());
//		notifyActivityUserDetails(msgUserDetails);
//		// TODO : Complete this
//	}
//	else if (msgPrefix.equals(Messages.MSG_PREFIX_CHAT_MESSAGE))
//	{
//		Messages.MessageChatMessage msgChat = new Messages.MessageChatMessage(msgReceived);
//		Log.d(LOG_TAG, "got chat and will update, source is : "+msgChat.getSourceUserIP());
//		UpdateOpenChats(msgChat.getChatMessageUser(),msgChat.getSourceUserIP(), msgChat.getChatMessageContents());
//		notifyActivityChat(msgChat);
//		// TODO : Complete this
//	}
//	else if (msgPrefix.equals(Messages.MSG_PREFIX_PING))
//	{
//		// Send a pong message to show I'm alive
//		Messages.MessagePong msgPong = new Messages.MessagePong(mMe.getIPAddress());
//		
//		sendMessageUDP(msgPong.toString(), IP_LEADER); //mLeaderIP);
//		
//		mLeaderLastPing = System.currentTimeMillis();
//	}
//
//}
