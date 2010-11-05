package main.main;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ActivityUsersList extends ListActivity
{
	private static final String MENU_ITEM_TITLE_CHAT = "Chat";
	private static final String[] mArrContextMenuItemsTitles = {MENU_ITEM_TITLE_CHAT};
	
	private static final String CTXT_MENU_ITEM_TITLE_EDIT_DETAILS = "Edit My Details";
	private static final String CTXT_MENU_ITEM_TITLE_LOGOUT = "Logout";
	private static final String[] mArrMenuItemsTitles = {CTXT_MENU_ITEM_TITLE_EDIT_DETAILS, CTXT_MENU_ITEM_TITLE_LOGOUT};

	private static final String LOG_TAG = "SN.UsersList";

    private ProgressDialog mProgressDialog = null;

	private ArrayList<User> mUsers = null;
	private UsersAdapter mUsersAdapter = null;
	private Button chatButton = null;
	private TextView mTextViewNoUsers = null;

	private boolean mIsClientEnabled;
	
	private ApplicationSocialNetwork application = null;
	public static ActivityUsersList instance = null;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.users_list);

	    ActivityUsersList.instance = this;
	    application = (ApplicationSocialNetwork)getApplication();
		
		chatButton = (Button) findViewById(R.id.OpenChatsButton);
		chatButton.setVisibility(View.INVISIBLE);
		mTextViewNoUsers = (TextView) findViewById(R.id.TextViewNoUsers);
		mTextViewNoUsers.setVisibility(View.GONE);
		
		mUsers = new ArrayList<User>();
		mUsersAdapter = new UsersAdapter(this, R.layout.user, mUsers);
		setListAdapter(mUsersAdapter);

		connect();
	}

	public void onStart()
	{
		super.onStart();
		
		mUsersAdapter.notifyDataSetChanged();
	}
	    
	private void connect()
	{
        mProgressDialog = ProgressDialog.show(this, "", "Searching for an existing network. Please wait...", true);
                
		// Search for a network
		Runnable runnableSearchForNetwork = new Runnable(){
			public void run()
			{
				// First try to connect as a client
				mIsClientEnabled = application.enableAdhocClient();
				
				mProgressDialog.dismiss();
				
				// Check if couldn't connect as a client
				if (mIsClientEnabled == false)
				{
					Log.d(LOG_TAG, "connect : Not a client");

					// Notify the user that we're about to create a new network and then connect as a leader
					Looper.prepare(); // Apparently it's needed for showing a dialog (next line) in a thread
					application.showToast(ActivityUsersList.this, "No network could be found. Creating a new one...");
//					mProgDialog = ProgressDialog.show(ActivityConnect.this, "", "No network could be found. Starting a new one...", true);
					application.enableAdhocLeader();
//					mProgDialog.dismiss();
				}
				
				// Run the thread created below in the thread that can handle the GUI
				runOnUiThread(mRunnableReturnNetwork);
			}
		};
		Thread thread = new Thread(null, runnableSearchForNetwork, "threadSearchForNetwork");
		thread.start();
    }


	private  Runnable mRunnableReturnNetwork = new Runnable() {
		public void run()
		{
			Log.d(LOG_TAG, "Got to mRunnableReturnNetwork");

			if (mIsClientEnabled == false)
			{
				// Android will crash if another thread touches the UI other then the thread who created it,
				// so we make the TextView visible here
				mTextViewNoUsers.setVisibility(View.VISIBLE);
				mTextViewNoUsers.setText(R.string.no_users_are_connected);
			}
				
			// Start the application's threads of sending and receiving messages
			application.startService();
		}
	};

	public void onListItemClick(ListView parent, View view, int position, long id) 
	{
//		super.onListItemClick(parent, view, position, id);
		
		User user = mUsers.get(position);
		
		if (user != null)
		{
			List<String> listMainDataValues = new LinkedList<String>();
			listMainDataValues.add("Name: " + user.getUsername());
			listMainDataValues.add(user.getSex() + ", " + user.getAge());
			
			String[] arrMainDataValues = listMainDataValues.toArray(new String[0]);
			Intent intent = new Intent(this, ActivityUserDetails.class);
			
			intent.putExtra(getResources().getString(R.string.extra_key_main_data), arrMainDataValues);
			intent.putExtra(ActivityUserDetails.EXTRA_KEY_USER_IP, user.getIPAddress());
			intent.putExtra(ActivityUserDetails.EXTRA_KEY_USER_NAME, user.getUsername());
			intent.putExtra(ActivityUserDetails.EXTRA_KEY_IS_EDITABLE, false);

			Log.d(LOG_TAG, "selected username :" + user.getUsername() + " and his ip is : 0"+ user.getIPAddress());
			
			startActivity(intent);
		}
	}
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);

     	for (int indexMenuItem = 0; indexMenuItem < mArrMenuItemsTitles.length; ++indexMenuItem)
    	{
    		menu.add(Menu.NONE, indexMenuItem, indexMenuItem, mArrMenuItemsTitles[indexMenuItem]);
    	}

     	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	if (item.getTitle().equals(CTXT_MENU_ITEM_TITLE_EDIT_DETAILS))
    	{
    		User user = application.getMe();
    		
    		if (user != null)
    		{
    			List<String> listMainDataValues = new LinkedList<String>();
    			listMainDataValues.add("Name: " + user.getUsername());
    			listMainDataValues.add(user.getSex() + ", " + user.getAge());
    			
    			String[] arrMainDataValues = listMainDataValues.toArray(new String[0]);
    			Intent intent = new Intent(this, ActivityUserDetails.class);
    			
    			intent.putExtra(getResources().getString(R.string.extra_key_main_data), arrMainDataValues);
    			intent.putExtra(ActivityUserDetails.EXTRA_KEY_USER_IP, user.getIPAddress());
    			intent.putExtra(ActivityUserDetails.EXTRA_KEY_USER_NAME, user.getUsername());
    			intent.putExtra(ActivityUserDetails.EXTRA_KEY_IS_EDITABLE, true);
    		
    			startActivity(intent);
    		}
    	}
    	else if (item.getTitle().equals(CTXT_MENU_ITEM_TITLE_LOGOUT))
    	{
    		logout();
    	}
    	
    	return true;
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
    	// If the user presses the BACK button, log out
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) 
		{     
			logout();
			
			return true;        
		}
		
		return super.onKeyDown(keyCode, event);    
	}
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) 
    {
         super.onCreateContextMenu(menu, view, menuInfo);
         
     	for (int indexMenuItem = 0; indexMenuItem < mArrContextMenuItemsTitles.length; ++indexMenuItem)
    	{
    		menu.add(Menu.NONE, indexMenuItem, indexMenuItem, mArrContextMenuItemsTitles[indexMenuItem]);
    	}
    }
 
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
       	if (item.getTitle().equals(MENU_ITEM_TITLE_CHAT))
    	{
       		// TODO : Check if this fixes touching the users list
       		User user = mUsers.get(getListView().getSelectedItemPosition());
       		
       		if (user == null)
       		{
       			Log.d(LOG_TAG, "User is null");
       		}
       		else
       		{
       			Log.d(LOG_TAG, "User name : " + user.getUsername() + ", User IP : " + user.getIPAddress());
       			
       			chat(user.getUsername(), user.getIPAddress());
       		}
    	}
    	
    	return true;    
    }    
	
    private void logout()
    {
		Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(application.getMyIP());
		application.sendMessage(msgUserDisconnected.toString());
		
		application.stopService();
		
		finish();
    }
    
	private void chat(String username, String ip)
	{
    	Intent intent = new Intent(this, ActivityChat.class);
    	intent.putExtra(ActivityChat.EXTRA_KEY_USER_IP, ip);
    	intent.putExtra(ActivityChat.EXTRA_KEY_USER_NAME, username);
    	startActivity(intent);
	}

	

	private class UsersAdapter extends ArrayAdapter<User>
	{
		private ArrayList<User> mItems;

		public UsersAdapter(Context context, int textViewResourceId, ArrayList<User> items)
		{
			super(context, textViewResourceId, items);
			mItems = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			
			if (view == null)
			{
				LayoutInflater layoutInflater =
					(LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = layoutInflater.inflate(R.layout.user, null);
			}
			
			User user = mItems.get(position);
			
			if (user != null)
			{
				// Set the user's picture
				ImageView imageViewUserPicture = (ImageView) view.findViewById(R.id.ImageViewUserIcon);
				// TODO : Get the user's real picture if we have it
				String fileNameUserPic = "/sdcard/" + user.getUsername() + ".jpg";
				File fileTesting = new File(fileNameUserPic);
				if (fileTesting.exists())
				{
					imageViewUserPicture.setImageBitmap(BitmapFactory.decodeFile(fileNameUserPic));
				}
				else
				{
					imageViewUserPicture.setImageResource(R.drawable.icon);
				}
				
				
				// Set the user's full name
				TextView textViewUserFullName = (TextView) view.findViewById(R.id.TextViewUserFullName);
				textViewUserFullName.setText("Name: " + user.getUsername());
				
				// Set the user's sex and age
				TextView textViewSexAndAge = (TextView) view.findViewById(R.id.TextViewSexAndAge);
				textViewSexAndAge.setText(user.getSex() + ", " + user.getAge());
			}
			
			// Assign the context menu to the current list item
			view.setOnCreateContextMenuListener(ActivityUsersList.this);
			
			view.setOnClickListener(new OnClickListener() {
				public void onClick(View v)
				{
					// TODO : This is a try to make the touch work
Log.d(LOG_TAG, "Clicked on a user. Will touch work ???");					
				}
			});
			
			return view;
		}
	}


	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			Boolean flag = false;
			
			Log.d(LOG_TAG, "In ActivityUsersList's handleMessage");
			
			try{
			 Messages.MessageChatMessage msgChat = (Messages.MessageChatMessage)msg.obj;
			 chat(msgChat.getChatMessageUser(), msgChat.getSourceUserIP());
			}
			catch(Exception ex)
			{
				flag = true;
			}
			
			Log.d(LOG_TAG, flag.toString());
			
			if(!flag)
			{
				chatButton.setVisibility(View.VISIBLE);
				chatButton.setBackgroundColor(Color.GREEN);
				chatButton.setOnClickListener(new OnClickListener() {
					public void onClick(View view)
						{
						showOpenChats();
						}
				});
				return;
			}
			mUsers = application.getUsers();
			for(User s: mUsers)
				Log.d(LOG_TAG, s.getUsername());
			mUsersAdapter = new UsersAdapter(ActivityUsersList.this, R.layout.user, mUsers);
			setListAdapter(mUsersAdapter);
			mUsersAdapter.notifyDataSetChanged();
//			ActivityUsersList.this.updateListView();
			
			if (mUsersAdapter.isEmpty())
			{
				mTextViewNoUsers.setVisibility(View.VISIBLE);
			}
			else
			{
				mTextViewNoUsers.setVisibility(View.GONE);
			}
		}
	};
	
	private void showOpenChats() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pick a chat to resume");
//		Log.d(LOG_TAG, "before get and set items for chat" );
//		CharSequence[] res =  application.GetOpenChatUsers();
//		for(CharSequence s : res)
//		{
//			Log.d(LOG_TAG, "got item :"+s );
//		}
		builder.setItems(application.GetOpenChatUsers(), new DialogInterface.OnClickListener() 
		{    
			public void onClick(DialogInterface dialog, int item) {      
				String user = application.GetOpenChatUsers()[item].toString();
                
				chat(user,application.GetOpenChatsIP(user));
			}
		});
//		Log.d(LOG_TAG, "after get and set items for chat" );
		AlertDialog alert = builder.create();
		alert.show();
	}

	public Handler getUpdateHandler()
	{
		return mHandler;
	}
}
