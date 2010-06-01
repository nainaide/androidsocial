package main.main;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ActivityChat extends Activity
{
	Button mButtonSend;
	EditText mEditTextConversation;
	EditText mEditTextMyMessage;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.chat);

		mEditTextConversation = (EditText) findViewById(R.id.EditTextChatConversation);
		mEditTextMyMessage = (EditText) findViewById(R.id.EditTextChatMyMessage);
		mButtonSend = (Button) findViewById(R.id.ButtonChatSend);
		
		mButtonSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view)
			{
				send();
			}

		});

		// TODO : Create a connection with the selected user
    }
    
	private void send()
	{
		// TODO Auto-generated method stub
		
	}
}
