package main.main;

import java.io.File;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivityChat extends Activity
{
	private static final String LOG_TAG = "SN.Chat";
	
	public static final String EXTRA_KEY_USER_IP = "UserIP";
	public static final String EXTRA_KEY_USER_NAME = "Username";

	private Button mButtonSend;
	private EditText mEditTextConversation;
	private EditText mEditTextMyMessage;
	private TextView mTextViewChatOtherUserDetails;
	
	private String mUsername;
	private String chattingWith;
	private String chattingWithIp;
	
	private ApplicationSocialNetwork application =null;
	
	public static ActivityChat instance = null;

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
		
		application = (ApplicationSocialNetwork)getApplication();
		ActivityChat.instance = this;
		
		mUsername = ActivityLogin.instance.getUserName();
		
		mTextViewChatOtherUserDetails = (TextView)findViewById(R.id.TextViewChatOtherUserDetails);
		mEditTextConversation = (EditText) findViewById(R.id.EditTextChatConversation);
		mEditTextMyMessage = (EditText) findViewById(R.id.EditTextChatMyMessage);
		mButtonSend = (Button) findViewById(R.id.ButtonChatSend);
		
		Log.d(LOG_TAG, "on create usrname:" + chattingWith + " in ip: "+chattingWithIp);
		
		mButtonSend.setOnClickListener(new OnClickListener() {
			public void onClick(View view)
			{
				send();
			}
		});
    }
    
	@Override
	public void onResume()   
	{
		super.onResume();
		
		Bundle extras = getIntent().getExtras();
		chattingWith = extras.getString(EXTRA_KEY_USER_NAME);
		chattingWithIp = extras.getString(EXTRA_KEY_USER_IP);
		
		String res = application.addOpenChats(chattingWith, chattingWithIp);
		mEditTextConversation.setText(res);
		
		Log.d(LOG_TAG, "on resume chatting with : " + chattingWith + ", chatting with ip : " + chattingWithIp);
		Log.d(LOG_TAG, "on resume got : " + res);

		// Set the other user's details
		mTextViewChatOtherUserDetails.setText("Chatting With : " + chattingWith);
		
		// Show the other user's picture, if we have it
		ImageView imageViewOtherUserPic = (ImageView) findViewById(R.id.ImageViewChatOtherUserPicture);
		String fileNameUserPic = "/sdcard/" + chattingWith + ".jpg";
		File fileTesting = new File(fileNameUserPic);
		if (fileTesting.exists())
		{
			imageViewOtherUserPic.setImageBitmap(BitmapFactory.decodeFile(fileNameUserPic));
			imageViewOtherUserPic.setVisibility(View.VISIBLE);
		}
		else
		{
			imageViewOtherUserPic.setVisibility(View.GONE);
		}
		
		// Scroll to the end of the chat
		if(mEditTextConversation.length()!=0)
			mEditTextConversation.setSelection(mEditTextConversation.length()-1);
	}
	
	private void send()
	{
		// Make sure the message to be sent isn't empty
		if (mEditTextMyMessage.getText().toString().equals(""))
		{
			return;
		}
		
		String conversation = mEditTextConversation.getText().toString();
		String updatingWith = "";
		
		if(!conversation.equals(""))
			updatingWith += "\n";
		
		updatingWith += mUsername+" sends:\n"+mEditTextMyMessage.getText().toString()+"\n";
		mEditTextConversation.setText(conversation + updatingWith);
		
		Log.d(LOG_TAG, "Updating chat from chat, chatting with ip: " +chattingWithIp);
		
		application.UpdateOpenChats(chattingWith, chattingWithIp, updatingWith);
		
		Messages.MessageChatMessage msgChat = new Messages.MessageChatMessage(updatingWith,mUsername, application.getMyIP(),chattingWithIp);
		Message msg = ActivityUsersList.instance.getUpdateHandler().obtainMessage();
		msg.obj = msgChat;
		ActivityUsersList.instance.getUpdateHandler().sendMessage(msg);
		
		application.sendMessage(msgChat.toString());
		
		mEditTextMyMessage.setText("");
		
		// Scroll to the end of the chat
		if(mEditTextConversation.length()!=0)
			mEditTextConversation.setSelection(mEditTextConversation.length()-1);
	}
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			Messages.MessageChatMessage msgChat = (Messages.MessageChatMessage)msg.obj;
			chattingWith = msgChat.getChatMessageUser();
			
			Log.d(LOG_TAG, "Chat from user " + msgChat.getChatMessageUser() + ", chattingWith = " + chattingWith);									
			Log.d(LOG_TAG, "Chat from ip " + msgChat.getSourceUserIP() + ", chattingWithIp = " + chattingWithIp);			

			mEditTextConversation.setText(application.addOpenChats(chattingWith,msgChat.getSourceUserIP()));

			// Scroll to the end of the chat
			if(mEditTextConversation.length()!=0)
				mEditTextConversation.setSelection(mEditTextConversation.length()-1);
		}
	};

	public Handler getUpdateHandler()
	{
		return mHandler;
	}
}
