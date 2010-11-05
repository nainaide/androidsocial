package main.main;

import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivityUserDetails extends Activity
{
	private static final String LOG_TAG = "SN.UserDetails";
	
	private static final int REQUEST_CODE_BROWSE_PIC = 1;
	private static final String MENU_ITEM_TITLE_CHAT = "Chat With";
	private static final String FIELD_NOT_FILLED = "-- Not Filled By User --";

	public static final String DATE_ELEMENTS_SEPARATOR = "/";
    
	public static final String EXTRA_KEY_USER_IP = "UserIP";
	public static final String EXTRA_KEY_USER_NAME = "Username";
	public static final String EXTRA_KEY_IS_EDITABLE = "IsEditable";
	
    
    private String mUserName = "";
    private String mPictureFileName;
    private String lookingAtUserIp;
    
    private TextView mTextViewMainDetails = null;
    private EditText mEditTextHobbies = null;
    private EditText mEditTextFavoriteMusic = null;
    private TextView mTextViewDateBirth = null;
    private Button mButtonChat = null;
    private Button mButtonBrowse = null;
    private Button mButtonOK = null;
    private Button mButtonCancel = null;
    private ImageView mImageViewUserPicture = null;
    
	private Handler mHandler = null;
	private Handler mHandlerImage = null;
	private boolean isEditable;
	private int birthDay;
	private int birthMonth;
	private int birthYear;
	
	private ApplicationSocialNetwork application = null;
    
    public static ActivityUserDetails instance = null;

    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_details);
        
        Log.d(LOG_TAG, "on create");
        
        ActivityUserDetails.instance = this;
        application = (ApplicationSocialNetwork)getApplication();
        
        mTextViewMainDetails = (TextView) findViewById(R.id.TextViewMainDetails);
        mTextViewDateBirth = (TextView) findViewById(R.id.TextViewUserDetailsDateBirth);
        mEditTextHobbies = (EditText) findViewById(R.id.EditTextUserDetailsHobbies);
        mEditTextFavoriteMusic = (EditText) findViewById(R.id.EditTextUserDetailsFavoriteMusic);
        mButtonChat = (Button) findViewById(R.id.ButtonUserDetailsChat);
        mButtonBrowse = (Button) findViewById(R.id.ButtonUserDetailsBrowse);
        mButtonOK =  (Button) findViewById(R.id.ButtonUserDetailsOK);
        mButtonCancel =  (Button) findViewById(R.id.ButtonUserDetailsCancel);
        mImageViewUserPicture = (ImageView) findViewById(R.id.ImageViewUserPicture);
        
        setListenersAndHandlers();

        populateFields();
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch( requestCode) {
			case REQUEST_CODE_BROWSE_PIC :
			{
				if (resultCode == RESULT_OK)
				{
					// Set the ImageView to show the new selected picture
					mPictureFileName = data.getStringExtra(ActivityFileBrowser.EXTRA_KEY_FILENAME);
					mImageViewUserPicture.setImageBitmap(BitmapFactory.decodeFile(mPictureFileName));
				}
			}
			default :
			{
				break;
			}
		}
	}
    
	private void populateFields()
	{
		Bundle extras = getIntent().getExtras();
		String[] arrMainData = extras.getStringArray(getResources().getString(R.string.extra_key_main_data));
	        
		lookingAtUserIp = extras.getString(EXTRA_KEY_USER_IP);
		mUserName = extras.getString(EXTRA_KEY_USER_NAME);
		isEditable = extras.getBoolean(EXTRA_KEY_IS_EDITABLE);
		
		Log.d(LOG_TAG, "is editable:" + isEditable);
		
		if(isEditable)
		{
			User me = application.getMe();
			 
			birthDay = me.getDateBirth().get(GregorianCalendar.DAY_OF_MONTH);
			birthMonth = me.getDateBirth().get(GregorianCalendar.MONTH);
			birthYear = me.getDateBirth().get(GregorianCalendar.YEAR);
			mEditTextFavoriteMusic.setText(me.getFavoriteMusic());
			mEditTextHobbies.setText(me.getHobbies());
			
			String userFileName = application.getUserFileName(mUserName);
			mPictureFileName = application.readPropertyFromFile(userFileName, ApplicationSocialNetwork.USER_PROPERTY_PIC_FILE_NAME);
	        
	        if (mPictureFileName != null && mPictureFileName.equals("") == false)
	        {
	        	mImageViewUserPicture.setImageBitmap(BitmapFactory.decodeFile( mPictureFileName));
	        }
	        else
	        {
	        	mImageViewUserPicture.setImageResource(R.drawable.icon);
	        }
		}
		else
		{
			// Send a command to get the rest of the selected user's details
			Messages.MessageGetUserDetails msgGetUserDetails = new Messages.MessageGetUserDetails(lookingAtUserIp, mUserName);
			application.sendMessage(msgGetUserDetails.toString());
			
			birthDay = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.DAY_OF_MONTH);
			birthMonth = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.MONTH);
			birthYear = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.YEAR);

        	mImageViewUserPicture.setImageResource(R.drawable.icon);
		}
		
		if(mEditTextHobbies.getText().toString().equals(""))
			mEditTextHobbies.setText(FIELD_NOT_FILLED);
		if(mEditTextFavoriteMusic.getText().toString().equals(""))
			mEditTextFavoriteMusic.setText(FIELD_NOT_FILLED);	
		
		String dateBirth = birthDay + DATE_ELEMENTS_SEPARATOR + (birthMonth + 1) + DATE_ELEMENTS_SEPARATOR + birthYear;
        mTextViewDateBirth.setText(dateBirth);
        
        setForViewOrEdit(isEditable);
        
        String mainData = "";
        for (String currData : arrMainData)
        {
        	mainData += currData + "\n";
        }
        
        mTextViewMainDetails.setText(mainData);
	}

	private void setListenersAndHandlers()
	{
		mButtonChat.setOnClickListener(new OnClickListener() {
//			@Override
			public void onClick(View view)
			{
				chat();
			}
		});
		
		mButtonBrowse.setOnClickListener(new OnClickListener () {
			public void onClick(View view)
			{
				Intent intentFileBrowser = new Intent(ActivityUserDetails.this, ActivityFileBrowser.class);
				startActivityForResult(intentFileBrowser, REQUEST_CODE_BROWSE_PIC);
			}
		});
        
        mButtonOK.setOnClickListener(new OnClickListener() {
//			@Override
			public void onClick(View view)
			{
				// Save the details to mMe
				User me = application.getMe();

				me.setFavoriteMusic(mEditTextFavoriteMusic.getText().toString());
				me.setHobbies(mEditTextHobbies.getText().toString());

				// Delete the user's file and create a new updated one
				String userFileName = application.getUserFileName(me.getUsername());

				application.writePropertyToFile(userFileName, ApplicationSocialNetwork.USER_PROPERTY_FAVORITE_MUSIC, mEditTextFavoriteMusic.getText().toString());
				application.writePropertyToFile(userFileName, ApplicationSocialNetwork.USER_PROPERTY_HOBBIES, mEditTextHobbies.getText().toString());
				application.writePropertyToFile(userFileName, ApplicationSocialNetwork.USER_PROPERTY_PIC_FILE_NAME, mPictureFileName);
				
				application.setFileNameForManager(mPictureFileName);
				
				finish();
			}
		});
        
        mButtonCancel.setOnClickListener(new OnClickListener() {
//			@Override
			public void onClick(View view)
			{
				finish();
			}
		});
        
    	mHandler = new Handler() {
    		public void handleMessage(Message msg)
    		{
    			Log.d(LOG_TAG, "At the beginning of the handleMessage of ActivityUserDetails. msg = " + msg.toString());
    			Messages.MessageUserDetails msgUserDetails = (Messages.MessageUserDetails)msg.obj;
    			
    			updateUserDetails(msgUserDetails);
    		}
    	};
    	
    	mHandlerImage = new Handler() {
    		public void handleMessage(Message msg)
    		{
    			Log.d(LOG_TAG, "Image handleMessage : msg = " + (String)msg.obj);
    			
    			mImageViewUserPicture.setImageBitmap( BitmapFactory.decodeFile( "/sdcard/" + ((String)msg.obj) + ".jpg"));
    		}
    	};
    	
    	mEditTextFavoriteMusic.setOnClickListener(new OnClickListener() {
			public void onClick(View view)
			{
				handleHint(mEditTextFavoriteMusic);
			}
		});
    	
    	mEditTextFavoriteMusic.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event)
			{
				handleHint(mEditTextFavoriteMusic);
				
				// Returning false so that the keyboard will show up
				return false;
			}
    	});
    	
    	mEditTextHobbies.setOnClickListener(new OnClickListener() {
			public void onClick(View view)
			{
				handleHint(mEditTextHobbies);
			}
		});

    	mEditTextHobbies.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event)
			{
				handleHint(mEditTextHobbies);
				
				// Returning false so that the keyboard will show up
				return false;
			}
    	});
	}

	private void handleHint(EditText editText)
	{
		if(editText.getText().toString().equals(FIELD_NOT_FILLED))
		{
			editText.setText("");
		}
	}
	
    private void setForViewOrEdit(boolean isEditable)
    {
		mTextViewDateBirth.setEnabled(isEditable);
		mEditTextFavoriteMusic.setEnabled(isEditable);
		mEditTextHobbies.setEnabled(isEditable);
		
		int visibilityEditingViews = (isEditable) ? View.VISIBLE : View.GONE;
		int visibilityViewingViews = (isEditable) ? View.GONE : View.VISIBLE;
		
		mButtonOK.setVisibility(visibilityEditingViews);
		mButtonCancel.setText(isEditable ? "Cancel" : "Close");
		mButtonChat.setVisibility(visibilityViewingViews);
		mButtonBrowse.setVisibility(visibilityEditingViews);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);

    	menu.add(Menu.NONE, 0, 0, MENU_ITEM_TITLE_CHAT + " " + mUserName);
    	
    	return true;
    }
 
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	if (item.getTitle().toString().startsWith(MENU_ITEM_TITLE_CHAT))
    	{
    		chat();
    	}
    	
    	return true;
    }

    private void chat()
    {
    	Log.d(LOG_TAG, "on open chat from details with : " + mUserName + " and ip is : "+lookingAtUserIp);
    	
    	Intent intent = new Intent(this, ActivityChat.class);
    	intent.putExtra(ActivityChat.EXTRA_KEY_USER_NAME, mUserName);
    	intent.putExtra(ActivityChat.EXTRA_KEY_USER_IP, lookingAtUserIp);
    	startActivity(intent);
    }
    
	private void updateUserDetails(Messages.MessageUserDetails msgUserDetails)
	{
		String hobbies = msgUserDetails.getHobbies();
		String favoriteMusic = msgUserDetails.getFavoriteMusic();
			
		Log.d(LOG_TAG, "hobbies = " + hobbies + ", favourite music = " + favoriteMusic);

		if (hobbies.equals("") == false)
		{
			mEditTextHobbies.setText(hobbies);
		}

		if (favoriteMusic.equals("") == false)
		{
			mEditTextFavoriteMusic.setText(favoriteMusic);
		}
	}

	public Handler getUpdateHandler()
	{
		return mHandler;
	}

	public Handler getImageUpdateHandler() {
		return mHandlerImage;
	}
}
