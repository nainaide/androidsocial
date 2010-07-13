package main.main;

import java.io.File;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivityUserDetails extends Activity
{
    private final String MENU_ITEM_TITLE_CHAT = "Chat With";
    private final String LOG_TAG = "SN.UserDetails";
    private final String FIELD_NOT_FILLED = "-- Not Filled By User --";
    
//	String[] mArrMenuItemsTitles = {MENU_ITEM_TITLE_CHAT};
	
    private String mUserName = "";
    private String lookingAtUserIp;
    
    private TextView mTextViewMainDetails = null;
//    private TextView mTextViewHobbies = null;
    private EditText mEditTextHobbies = null;
	private ApplicationSocialNetwork application = null;
//    private TextView mTextViewFavoriteMusic = null;
    private EditText mEditTextFavoriteMusic = null;
    private DatePicker mDatePickerBirth = null;
    private Button mButtonChat = null;
    private Button mButtonOK = null;
    private Button mButtonCancel = null;
    
	private Handler mHandler = null;
	private boolean isEditable;
	private int birthDay;
	private int birthMonth;
	private int birthYear;
//	private String currHobbies;
//	private String currFavorMusic;
    
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
//        Bundle extras = getIntent().getExtras();
//        
//        String[] arrMainData = extras.getStringArray(getResources().getString(R.string.extra_key_main_data));
//        lookingAtUserIp = extras.getString("ActivityDetails.userIp");
//        mUserName = extras.getString("ActivityDetails.userName");
//        isEditable = extras.getBoolean("ActivityDetails.isEditable");
//        if(isEditable)
//        {
//        	birthDay = application.getMe().getDateBirth().get(GregorianCalendar.DAY_OF_MONTH);
//        	birthMonth = application.getMe().getDateBirth().get(GregorianCalendar.MONTH);
//        	birthYear = application.getMe().getDateBirth().get(GregorianCalendar.YEAR);
//        }
//        else
//        {
//        	birthDay = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.DAY_OF_MONTH);
//        	birthMonth = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.MONTH);
//        	birthYear = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.YEAR);
//        }
//        Log.d(LOG_TAG, "on create lookingAtUserIp is : "+lookingAtUserIp);
        
        mTextViewMainDetails = (TextView) findViewById(R.id.TextViewMainDetails);
        if(mEditTextHobbies!=null)
        	Log.d(LOG_TAG, "on create hobbies is NOT null");
        else
        	Log.d(LOG_TAG, "on create hobbies is null");
//        mTextViewHobbies = (TextView) findViewById(R.id.TextViewUserDetailsHobbies);
        mEditTextHobbies = (EditText) findViewById(R.id.EditTextUserDetailsHobbies);
      //  mEditTextHobbies.setText(FIELD_NOT_FILLED);
        
//        mTextViewFavoriteMusic = (TextView) findViewById(R.id.TextViewUserDetailsFavoriteMusic);
        mEditTextFavoriteMusic = (EditText) findViewById(R.id.EditTextUserDetailsFavoriteMusic);
       // mEditTextFavoriteMusic.setText(FIELD_NOT_FILLED);
        
        mDatePickerBirth = (DatePicker) findViewById(R.id.DatePickerUserDetailsDateBirth);
        
        mButtonChat = (Button) findViewById(R.id.ButtonUserDetailsChat);
        mButtonOK =  (Button) findViewById(R.id.ButtonUserDetailsOK);
        mButtonCancel =  (Button) findViewById(R.id.ButtonUserDetailsCancel);
        
        setListenersAndHandlers();

//        ListView listView = (ListView) findViewById(R.id.ListView01);
        
//        setAreDetailsEditable(isEditable);
//
//        String mainData = "";
//        for (String currData : arrMainData)
//        {
//        	// TODO : Check if needs to make this new line character a global thing
//        	mainData += currData + System.getProperty("line.separator");
//        }
//        
//        mTextViewMainDetails.setText(mainData);
        
        // TODO : Also get and show the user picture
        ImageView imageViewUserPicture = (ImageView) findViewById(R.id.ImageViewUserPicture);
        imageViewUserPicture.setImageResource(R.drawable.icon);
        
        populateFields();
    }
    
	private void populateFields()
	{
//		boolean isEditable;
		Bundle extras = getIntent().getExtras();
	        
		String[] arrMainData = extras.getStringArray(getResources().getString(R.string.extra_key_main_data));
		lookingAtUserIp = extras.getString("ActivityDetails.userIp");
		mUserName = extras.getString("ActivityDetails.userName");
		isEditable = extras.getBoolean("ActivityDetails.isEditable");
		Log.d(LOG_TAG, "is editable:" + isEditable);
		if(isEditable)
		{
			User me = application.getMe();
			 
			birthDay = me.getDateBirth().get(GregorianCalendar.DAY_OF_MONTH);
			birthMonth = me.getDateBirth().get(GregorianCalendar.MONTH);
			birthYear = me.getDateBirth().get(GregorianCalendar.YEAR);
			mEditTextFavoriteMusic.setText(me.getFavoriteMusic());
			mEditTextHobbies.setText(me.getHobbies());
		}
		else
		{
			// Send a command to get the rest of the selected user's details
			Messages.MessageGetUserDetails msgGetUserDetails = new Messages.MessageGetUserDetails(lookingAtUserIp);
			application.sendMessage(msgGetUserDetails.toString());
			
			birthDay = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.DAY_OF_MONTH);
			birthMonth = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.MONTH);
			birthYear = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.YEAR);
			//mEditTextFavoriteMusic.setText(currFavorMusic);
			//mEditTextHobbies.setText(currHobbies);
		}
		
		if(mEditTextHobbies.getText().toString().equals(""))
				mEditTextHobbies.setText(FIELD_NOT_FILLED);
		if(mEditTextFavoriteMusic.getText().toString().equals(""))
			mEditTextFavoriteMusic.setText(FIELD_NOT_FILLED);	
		
        mDatePickerBirth.updateDate(birthYear, birthMonth, birthDay);
        
        setAreDetailsEditable(isEditable);
        
        String mainData = "";
        for (String currData : arrMainData)
        {
        	// TODO : Check if needs to make this new line character a global thing
        	mainData += currData + System.getProperty("line.separator");
        }
        
        mTextViewMainDetails.setText(mainData);

	}
	/*
    public void onResume()
    {
    	super.onResume();
    	Log.d(LOG_TAG, "on resume");
//    	 Bundle extras = getIntent().getExtras();
//         
//         lookingAtUserIp = extras.getString("ActivityDetails.userIp");
//         mUserName = extras.getString("ActivityDetails.userName");
//         isEditable = extras.getBoolean("ActivityDetails.isEditable");
//         if(isEditable)
//         {
//        	 User me = application.getMe();
//        	 
//        	 birthDay = me.getDateBirth().get(GregorianCalendar.DAY_OF_MONTH);
//        	 birthMonth = me.getDateBirth().get(GregorianCalendar.MONTH);
//        	 birthYear = me.getDateBirth().get(GregorianCalendar.YEAR);
//        	 mEditTextFavoriteMusic.setText(me.getFavoriteMusic());
//        	 mEditTextHobbies.setText(me.getHobbies());
//         }
//         else
//         {
//         	birthDay = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.DAY_OF_MONTH);
//         	birthMonth = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.MONTH);
//         	birthYear = application.getUserByIp(lookingAtUserIp).getDateBirth().get(GregorianCalendar.YEAR);
//         }
//         mDatePickerBirth.updateDate(birthYear, birthMonth, birthDay);
//         
//         setAreDetailsEditable(isEditable);
    	
        populateFields();
    }
    */
	private void setListenersAndHandlers()
	{
		mButtonChat.setOnClickListener(new OnClickListener() {
//			@Override
			public void onClick(View view)
			{
				chat();
			}
		});
        
        mButtonOK.setOnClickListener(new OnClickListener() {
//			@Override
			public void onClick(View view)
			{
				// Save the details to mMe
				User me = application.getMe();
				String sex = me.getSex();
				int birthYear = mDatePickerBirth.getYear();
				int birthMonth = mDatePickerBirth.getMonth();
				int birthDay = mDatePickerBirth.getDayOfMonth();
				
				me.setDateBirth(new GregorianCalendar(birthYear, birthMonth, birthDay));
				me.setFavoriteMusic(mEditTextFavoriteMusic.getText().toString());
				me.setHobbies(mEditTextHobbies.getText().toString());

				// Delete the user's file and create a new updated one
				String userFileName = application.getUserFileName(me.getFullName());
				File userFile = new File(userFileName);
				
				if (userFile != null)
				{
					userFile.delete();
				}

				application.writePropertyToFile(userFileName, "Username", mUserName);
				application.writePropertyToFile(userFileName, "Sex", sex);
				application.writePropertyToFile(userFileName, "Date of Birth", birthYear + " " + birthMonth + " " + birthDay);
				application.writePropertyToFile(userFileName, "Hobbies", mEditTextHobbies.getText().toString());
				application.writePropertyToFile(userFileName, "FavoriteMusic", mEditTextFavoriteMusic.getText().toString());
				
				finish();
			}
		});
        
        mButtonCancel.setOnClickListener(new OnClickListener() {
//			@Override
			public void onClick(View view)
			{
				// TODO Auto-generated method stub
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
	}        

    private void setAreDetailsEditable(boolean isEditable)
    {
		// TODO Auto-generated method stub
//		mDatePickerBirth.setClickable(isEditable);
		mDatePickerBirth.setEnabled(isEditable);
		mEditTextFavoriteMusic.setEnabled(isEditable);
		mEditTextHobbies.setEnabled(isEditable);
		
		int visibilityEditingViews = View.GONE;
		int visibilityViewingViews = View.VISIBLE;
		
		if (isEditable)
		{
			visibilityEditingViews = View.VISIBLE;
			visibilityViewingViews = View.GONE;
		}
		
//		mEditTextFavoriteMusic.setVisibility(visibilityEditingViews);
//		mEditTextHobbies.setVisibility(visibilityEditingViews);
		mButtonOK.setVisibility(visibilityEditingViews);
		mButtonCancel.setVisibility(visibilityEditingViews);
		
//		mTextViewFavoriteMusic.setVisibility(visibilityViewingViews);
//		mTextViewHobbies.setVisibility(visibilityViewingViews);
		mButtonChat.setVisibility(visibilityViewingViews);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);

//    	int[] arrMenuItemsOrderIDs = {
    	
    	// Set whether the shortcuts will be alphabetical instead of numerical
//        menu.setQwertyMode(true);

    	menu.add(Menu.NONE, 0, 0, MENU_ITEM_TITLE_CHAT + " " + mUserName);
//    	for (int indexMenuItem = 0; indexMenuItem < mArrMenuItemsTitles.length; ++indexMenuItem)
//    	{
//    		menu.add(Menu.NONE, indexMenuItem, indexMenuItem, mArrMenuItemsTitles[indexMenuItem]);
////    		menuItemChat.setAlphabeticShortcut('a');
////    		menuItemChat.setIcon(R.drawable.alert_dialog_icon);
//    	}
    	
    	return true;
    }
 
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
//    	int indexMenuItemInArray = -1;
//    	
//    	// Find the id of the menu item selected
//    	for (int indexCurrMenuItem = 0; indexCurrMenuItem < mArrMenuItemsTitles.length; ++indexCurrMenuItem)
//    	{
//    		if (mArrMenuItemsTitles[indexCurrMenuItem].equals(item.getTitle()))
//    		{
//    			indexMenuItemInArray = indexCurrMenuItem;
//    			
//    			break;
//    		}
//    	}
    	
    	if (item.getTitle().toString().startsWith(MENU_ITEM_TITLE_CHAT))
    	{
    		chat();
    	}
    	
    	return true;
    }

    private void chat()
    {
    	Intent intent = new Intent(this, ActivityChat.class);
    	intent.putExtra("ActivityChat.userName", mUserName);
    	Log.d(LOG_TAG, "on open chat from details with : " + mUserName + " and ip is : "+lookingAtUserIp);
    	intent.putExtra("ActivityChat.userIp", lookingAtUserIp);
    	startActivity(intent);
    }
    
	private void updateUserDetails(Messages.MessageUserDetails msgUserDetails)
	{
		String hobbies = msgUserDetails.getHobbies();
		String favoriteMusic = msgUserDetails.getFavoriteMusic();
			
Log.d(LOG_TAG, "hobbies = " + hobbies + ", favourite music = " + favoriteMusic);

		if (hobbies.equals("") == false)
		{
//			mTextViewHobbies.setVisibility(View.VISIBLE);
//			currHobbies =hobbies;
	        mEditTextHobbies.setText(hobbies);
		}
		
		if (favoriteMusic.equals("") == false)
		{
//			currFavorMusic = favoriteMusic;
//			mTextViewFavoriteMusic.setVisibility(View.VISIBLE);
	        mEditTextFavoriteMusic.setText(favoriteMusic);
		}
	}

	public Handler getUpdateHandler()
	{
		return mHandler;
	}
}
