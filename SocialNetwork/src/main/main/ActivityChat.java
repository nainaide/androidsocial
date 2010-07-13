package main.main;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ActivityChat extends Activity
{
	protected static final String LOG_TAG = "SN.Chat";
	Button mButtonSend;
	EditText mEditTextConversation;
	EditText mEditTextMyMessage;
	TextView mTextViewChatOtherUserDetails;
	
	private ApplicationSocialNetwork application =null;
	private String username;
	private String chatingWith;
	private String chatingWithIp;
	public static ActivityChat instance = null;

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		application  = (ApplicationSocialNetwork)getApplication();
		setContentView(R.layout.chat);
		ActivityChat.instance = this;
		username = ActivityLogin.instance.getUserName();
		mEditTextConversation = (EditText) findViewById(R.id.EditTextChatConversation);
		mEditTextMyMessage = (EditText) findViewById(R.id.EditTextChatMyMessage);
		mButtonSend = (Button) findViewById(R.id.ButtonChatSend);
//		Bundle extras = getIntent().getExtras();
		
		Log.d(LOG_TAG, "on create usrname:" + chatingWith + " in ip: "+chatingWithIp);
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
		chatingWith = extras.getString("ActivityChat.userName");
		chatingWithIp = extras.getString("ActivityChat.userIp");
		String res = application.addOpenChats(chatingWith,chatingWithIp);
		Log.d(LOG_TAG, "on resume chating with : " + chatingWith);
		Log.d(LOG_TAG, "on resume chating with ip : " + chatingWithIp);
		Log.d(LOG_TAG, "on resume got : " + res);
		mEditTextConversation.setText(res);

		// Set the other user's details
		mTextViewChatOtherUserDetails = (TextView)findViewById(R.id.TextViewChatOtherUserDetails);
		mTextViewChatOtherUserDetails.setText("Chatting With : " + chatingWith);
	}
	
	private void send()
	{
		if (mEditTextMyMessage.getText().toString().equals(""))
		{
			return;
		}
		
		String text = mEditTextConversation.getText().toString();
		String updatingWith = "";
		if(!text.equals(""))
			updatingWith+="\n";
		updatingWith += username+" sends:\n"+mEditTextMyMessage.getText().toString()+"\n";
		mEditTextConversation.setText(text+updatingWith);
		Log.d(LOG_TAG, "Updating chat from chat, chating with ip: " +chatingWithIp);		
		application.UpdateOpenChats(chatingWith, chatingWithIp, updatingWith);
		Messages.MessageChatMessage msgChat = new Messages.MessageChatMessage(updatingWith,username, application.getMyIP(),chatingWithIp);
		
		Message msg = ActivityUsersList.instance.getUpdateHandler().obtainMessage();
		msg.obj = msgChat;
		ActivityUsersList.instance.getUpdateHandler().sendMessage(msg);
		
		application.sendMessage(msgChat.toString());
		mEditTextMyMessage.setText("");
	}
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			Messages.MessageChatMessage msgChat = (Messages.MessageChatMessage)msg.obj;
			chatingWith = msgChat.getChatMessageUser();
			Log.d(LOG_TAG, "Chat from " +msgChat.getChatMessageUser());									
			Log.d(LOG_TAG, "Chat from in chatingWith : "+chatingWith);		
			Log.d(LOG_TAG, "Chat from ip " +msgChat.getSourceUserIP());			
			Log.d(LOG_TAG, "Chat from in chatingWithIp : "+chatingWithIp);		
			/*if(!msgChat.getChatMessageUser().equals(chatingWith))
			{
				return;
			}*/
//			String text = mEditTextConversation.getText().toString();
			//if(!text.equals(""))
			//	text+="\n";
			mEditTextConversation.setText(application.addOpenChats(chatingWith,msgChat.getSourceUserIP()));
					//text+msgChat.getChatMessageContents());
		}
	};

	public Handler getUpdateHandler()
	{
		return mHandler;
	}
}
