package main.main;

import android.app.Activity;
import android.content.Intent;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class ActivityUserDetails extends Activity
{
	
    private final String MENU_ITEM_TITLE_CHAT = "Chat With";
	String[] mArrMenuItemsTitles = {MENU_ITEM_TITLE_CHAT};
	
    String mUserName = "";
    
    TextView mTextViewMainDetails = null;
    TextView mTextViewHobbies = null;
    
    public static ActivityUserDetails instance = null;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_details);
        
        ActivityUserDetails.instance = this;
        
        Bundle extras = getIntent().getExtras();
        
        String[] arrMainData = extras.getStringArray(getResources().getString(R.string.extra_key_main_data));
        mTextViewMainDetails = (TextView) findViewById(R.id.TextViewMainDetails);
        mTextViewHobbies = (TextView) findViewById(R.id.TextViewUserDetailsHobbies);
        mTextViewHobbies.setVisibility(View.GONE);
        
        String mainData = "";
        for (String currData : arrMainData)
        {
        	// TODO : Check if needs to make this new line character a global thing
        	mainData += currData + System.getProperty("line.separator");
        }
        
        mTextViewMainDetails.setText(mainData);
        
        // TODO : Also get and show the user picture
        ImageView imageViewUserPicture = (ImageView) findViewById(R.id.ImageViewUserPicture);
        imageViewUserPicture.setImageResource(R.drawable.icon);
        
        // TODO : Request all the details from the selected user
    }        

    @Override
	public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);

//    	int[] arrMenuItemsOrderIDs = {
    	
    	// Set whether the shortcuts will be alphabetical instead of numerical
//        menu.setQwertyMode(true);

    	for (int indexMenuItem = 0; indexMenuItem < mArrMenuItemsTitles.length; ++indexMenuItem)
    	{
    		menu.add(Menu.NONE, indexMenuItem, indexMenuItem, mArrMenuItemsTitles[indexMenuItem] + mUserName);
//    		menuItemChat.setAlphabeticShortcut('a');
//    		menuItemChat.setIcon(R.drawable.alert_dialog_icon);
    	}
    	
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
    	
    	if (item.getTitle().equals(MENU_ITEM_TITLE_CHAT))
    	{
    		chat();
    	}
    	
    	return true;
    }

    private void chat()
    {
    	Intent intent = new Intent(this, ActivityChat.class);
    	
    	startActivity(intent);
    }
    
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			Messages.MessageUserDetails msgUserDetails = (Messages.MessageUserDetails)msg.obj;

			String hobbies = msgUserDetails.getHobbies();
			if (hobbies.equals("") == false)
			{
		        mTextViewHobbies.setVisibility(View.VISIBLE);
		        mTextViewHobbies.setText(hobbies);
			}
			
			// TODO : Continue implementing this
			
//			mUsers = application.getUsers();
//			mUsersAdapter = new UsersAdapter(ActivityUsersList.this, R.layout.user, mUsers);
//			setListAdapter(mUsersAdapter);
//			mUsersAdapter.notifyDataSetChanged();
////			ActivityUsersList.this.updateListView();
		}
	};

	public Handler getUpdateHandler()
	{
		return mHandler;
	}
}
