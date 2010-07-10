package main.main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ActivityUsersList extends ListActivity
{
	private final String MENU_ITEM_TITLE_CHAT = "Chat";
	private final String[] mArrContextMenuItemsTitles = {MENU_ITEM_TITLE_CHAT};
	
	private final String CTXT_MENU_ITEM_TITLE_EDIT_DETAILS = "Edit My Details";
	private final String CTXT_MENU_ITEM_TITLE_LOGOUT = "Logout";
	private final String[] mArrMenuItemsTitles = {CTXT_MENU_ITEM_TITLE_EDIT_DETAILS, CTXT_MENU_ITEM_TITLE_LOGOUT};

	private final String LOG_TAG = "SN.UsersList";
	
    public static ActivityUsersList instance = null;

    private ProgressDialog mProgDialog = null;

//	private ProgressDialog mProgressDialog = null;
	private ArrayList<User> mUsers = null;
	private UsersAdapter mUsersAdapter = null;
	private ApplicationSocialNetwork application = null;
	private Button chatButton = null;
	private TextView mTextViewNoUsers = null;

	boolean mIsClientEnabled;
	
//	private static void setCurrent(ActivityUsersList current){
//		ActivityUsersList = current;
//	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.users_list);

	    ActivityUsersList.instance = this;
	    application = (ApplicationSocialNetwork)getApplication();
		
		mUsers = new ArrayList<User>();
		mUsersAdapter = new UsersAdapter(this, R.layout.user, mUsers);
		chatButton = (Button) findViewById(R.id.OpenChatsButton);
		chatButton.setVisibility(View.INVISIBLE);
		mTextViewNoUsers = (TextView) findViewById(R.id.TextViewNoUsers);
		mTextViewNoUsers.setVisibility(View.GONE);
//		mUsersAdapter = new UsersAdapter(this, R.layout.user, application.getUsers());
		setListAdapter(mUsersAdapter);

		


//		Runnable runnableViewUsers = new Runnable(){
//			@Override
//			public void run()
//			{
//				getUsers();
//			}
//		};
//		Thread thread = new Thread(null, runnableViewUsers, "threadGetUsers");
//		thread.start();
//		mProgressDialog = ProgressDialog.show(ActivityUsersList.this,    
//				"Please wait...", "Retrieving data ...", true);
		
		// TODO : This is just a simulation. Delete this
//		Runnable runnableGetUsers = new Runnable(){
//			public void run()
//			{
//				application.nap(1000);
//				Messages.MessageNewUser msgNewUser1 = new Messages.MessageNewUser(application.getMyIP(), "username1", 1980, 3, 10, "Male", "pic1"); 
//				application.sendMessage(msgNewUser1.toString(), application.getLeaderIP());
//
//				application.nap(3000);
//				Messages.MessageNewUser msgNewUser2 = new Messages.MessageNewUser(application.getMyIP(), "username2", 1982, 6, 15, "Female", "pic2"); 
//				application.sendMessage(msgNewUser2.toString(), application.getLeaderIP());
//
//				application.nap(5000);
//				Messages.MessageNewUser msgNewUser3 = new Messages.MessageNewUser(application.getMyIP(), "username3", 1984, 9, 20, "Male", "pic3"); 
//				application.sendMessage(msgNewUser3.toString(), application.getLeaderIP());
//				//			getUsers();
//			}
//		};
//		Thread thread = new Thread(null, runnableGetUsers, "threadGetUsers");
//		thread.start();

		connect();
	}

	
//	final WeatherDataListAdapter listModelView = new WeatherDataListAdapter(ctx, listview);

	  // bind a selection listener to the view
//	  listview.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//	    public void onItemSelected(AdapterView parentView, View childView, int position, long id) {
//	      mUsersAdapter.setSelected(position);
//	    }
//	    public void onNothingSelected(AdapterView parentView) {
//	      listModelView.setSelected(-1);
//	    }
//	  });
	    
	    
	private void connect()
	{
        application.disableWifi();

        mProgDialog = ProgressDialog.show(this, "", "Searching for an existing network. Please wait...", true);
                
		// Search for a network
		Runnable runnableSearchForNetwork = new Runnable(){
			public void run()
			{
				mIsClientEnabled = application.enableAdhocClient();
				
				mProgDialog.dismiss();
				
				if (mIsClientEnabled == false)
				{
//					application.disableAdhocClient();
					Looper.prepare(); // Apparently it's needed for showing a dialog (next line) in a thread
					application.showToast(ActivityUsersList.this, "No network could be found. Creating a new one...");
//					mProgDialog = ProgressDialog.show(ActivityConnect.this, "", "No network could be found. Starting a new one...", true);
					application.enableAdhocServer();
//					mProgDialog.dismiss();
				}
				
				runOnUiThread(mRunnableReturnNetwork);
			}
		};
		Thread thread = new Thread(null, runnableSearchForNetwork, "threadSearchForNetwork");
		thread.start();
    }


	private  Runnable mRunnableReturnNetwork = new Runnable() {
		public void run()
		{
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

	
//	private void getUsers()
//	{
//		// TODO : Actually get the real users list !
//		mUsers = new ArrayList<User>();
//
//		User user1 = new User("John", "Smith", Sex.MALE, 1980, Calendar.OCTOBER, 15, "192.168.1.10");
//		User user2 = new User("Dianne", "Brown", Sex.FEMALE, 1985, Calendar.MARCH, 27,  "192.168.1.11");
//
//		mUsers.add(user1);
//		mUsers.add(user2);
//
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e) {
//			//			e.printStackTrace();
//		}
//
//		runOnUiThread(returnRes);
//	}
//
//	private  Runnable returnRes = new Runnable() {
//		public void run()
//		{
//			TextView textViewNoUsers = (TextView) findViewById(R.id.TextViewNoUsers);
//			
////			mProgressDialog.dismiss();
//			
//			if(mUsers == null || mUsers.size() <= 0)
//			{
//				textViewNoUsers.setVisibility(View.VISIBLE);
//			}
//			else
//			{
//				textViewNoUsers.setVisibility(View.GONE);
//				
//				// TODO : Is this notifyDataSetChanged needed ?
////				mUsersAdapter.notifyDataSetChanged();
//				
//				for(int indexUser = 0; indexUser < mUsers.size(); ++indexUser)
//				{
//					mUsersAdapter.add(mUsers.get(indexUser));
//				}
//			}
//			
//			mUsersAdapter.notifyDataSetChanged();
//			
//			// TODO : Listen to users connecting 
//		}
//	};

	public void onListItemClick(ListView parent, View view, int position, long id) 
	{
//		super.onListItemClick(parent, view, position, id);
		
		Intent intent = new Intent(this, ActivityUserDetails.class);
		User user = mUsers.get(position);
		
		if (user != null)
		{
			List<String> listMainDataValues = new LinkedList<String>();
			listMainDataValues.add("Name: " + user.getFullName());
			listMainDataValues.add(user.getSex() + ", " + user.getAge());
			
			String[] arrMainDataValues = listMainDataValues.toArray(new String[0]);
			
			intent.putExtra(getResources().getString(R.string.extra_key_main_data), arrMainDataValues);
			intent.putExtra("ActivityDetails.userIp", user.getIPAddress());
			intent.putExtra("ActivityDetails.userName", user.getFullName());
			// TODO : Also pass the picture
			Log.d(LOG_TAG, "selected username :"+user.getFullName() + " and his ip is : 0"+ user.getIPAddress() );
			startActivity(intent);
			
			// Send a command to get the rest of the selcted user's details
			Messages.MessageGetUserDetails msgGetUserDetails = new Messages.MessageGetUserDetails(user.getIPAddress());
			application.sendMessage(msgGetUserDetails.toString());
		}
	}
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);

     	for (int indexMenuItem = 0; indexMenuItem < mArrMenuItemsTitles.length; ++indexMenuItem)
    	{
    		menu.add(Menu.NONE, indexMenuItem, indexMenuItem, mArrMenuItemsTitles[indexMenuItem]);
//    		menuItemChat.setAlphabeticShortcut('a');
//    		menuItemChat.setIcon(R.drawable.alert_dialog_icon);
    	}

     	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	if (item.getTitle().equals(CTXT_MENU_ITEM_TITLE_EDIT_DETAILS))
    	{
    		
    	}
    	else if (item.getTitle().equals(CTXT_MENU_ITEM_TITLE_LOGOUT))
    	{
    		Messages.MessageUserDisconnected msgUserDisconnected = new Messages.MessageUserDisconnected(application.getMyIP());
    		
    		application.sendMessage(msgUserDisconnected.toString());
    		
    		application.stopService();
    		
    		finish();
    	}
    	
    	return true;
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) 
    {
         super.onCreateContextMenu(menu, view, menuInfo);
         
//         String[] arrContextMenuItemsTitles = {MENU_ITEM_TITLE_CHAT};
         
     	for (int indexMenuItem = 0; indexMenuItem < mArrContextMenuItemsTitles.length; ++indexMenuItem)
    	{
    		menu.add(Menu.NONE, indexMenuItem, indexMenuItem, mArrContextMenuItemsTitles[indexMenuItem]);
//    		menuItemChat.setAlphabeticShortcut('a');
//    		menuItemChat.setIcon(R.drawable.alert_dialog_icon);
    	}
    }
 
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
       	if (item.getTitle().equals(MENU_ITEM_TITLE_CHAT))
    	{
    		chat(mUsers.get(item.getItemId()).getFullName(),mUsers.get(item.getItemId()).getIPAddress());
    	}
    	
    	return true;    
    }    
	
	private void chat(String username, String ip)
	{
    	Intent intent = new Intent(this, ActivityChat.class);
    	intent.putExtra("ActivityChat.userIp", ip);
    	intent.putExtra("ActivityChat.userName",username );
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
				// TODO : Get the user's real picture
				imageViewUserPicture.setImageResource(R.drawable.icon);
				
				// Set the user's full name
				TextView textViewUserFullName = (TextView) view.findViewById(R.id.TextViewUserFullName);
				textViewUserFullName.setText("Name: " + user.getFullName());
				
				// Set the user's sex and age
				TextView textViewSexAndAge = (TextView) view.findViewById(R.id.TextViewSexAndAge);
				textViewSexAndAge.setText(user.getSex() + ", " + user.getAge());
			}
			
			// Assign the context menu to the current list item
			view.setOnCreateContextMenuListener(ActivityUsersList.this);
			
			return view;
		}
	}


	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			Boolean flag = false;
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
				Log.d(LOG_TAG, s.getFullName());
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
		Log.d(LOG_TAG, "before get and set items for chat" );
		CharSequence[] res =  application.GetOpenChatUsers();
		for(CharSequence s : res)
		{
			Log.d(LOG_TAG, "got item :"+s );
		}
		builder.setItems(application.GetOpenChatUsers(), new DialogInterface.OnClickListener() 
		{    
			public void onClick(DialogInterface dialog, int item) {      
				String user = application.GetOpenChatUsers()[item].toString();
                
				chat(user,application.GetOpenChatsIP(user));
			}
		});
	   Log.d(LOG_TAG, "after get and set items for chat" );
	   AlertDialog alert = builder.create();
	   alert.show();
	}

	public Handler getUpdateHandler()
	{
		return mHandler;
	}

//	protected void updateListView()
//	{
//		// TODO Auto-generated method stub
//	}
}
