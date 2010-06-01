package main.main;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import main.main.User.Sex;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
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

    public static ActivityUsersList instance = null;
    
	private ProgressDialog mProgressDialog = null;
	private ArrayList<User> mUsers = null;
	private UsersAdapter mUsersAdapter;
	private ApplicationSocialNetwork application = null;

	
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
		Runnable runnableGetUsers = new Runnable(){
			@Override
			public void run()
			{
				application.nap(1000);
				Messages.MessageNewUser msgNewUser1 = new Messages.MessageNewUser(application.getMyIP(), "username1", 1980, 3, 10, "Male", "pic1"); 
				application.sendMessage(msgNewUser1.toString(), application.getLeaderIP());

				application.nap(3000);
				Messages.MessageNewUser msgNewUser2 = new Messages.MessageNewUser(application.getMyIP(), "username2", 1982, 6, 15, "Female", "pic2"); 
				application.sendMessage(msgNewUser2.toString(), application.getLeaderIP());

				application.nap(5000);
				Messages.MessageNewUser msgNewUser3 = new Messages.MessageNewUser(application.getMyIP(), "username3", 1984, 9, 20, "Male", "pic3"); 
				application.sendMessage(msgNewUser3.toString(), application.getLeaderIP());
				//			getUsers();
			}
		};
//		Thread thread = new Thread(null, runnableGetUsers, "threadGetUsers");
//		thread.start();

		
	}


	private void getUsers()
	{
		// TODO : Actually get the real users list !
		mUsers = new ArrayList<User>();

		User user1 = new User("John", "Smith", Sex.MALE, 1980, Calendar.OCTOBER, 15, "192.168.1.10");
		User user2 = new User("Dianne", "Brown", Sex.FEMALE, 1985, Calendar.MARCH, 27,  "192.168.1.11");

		mUsers.add(user1);
		mUsers.add(user2);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			//			e.printStackTrace();
		}

		runOnUiThread(returnRes);
	}

	private  Runnable returnRes = new Runnable() {
		@Override
		public void run()
		{
			TextView textViewNoUsers = (TextView) findViewById(R.id.TextViewNoUsers);
			
			mProgressDialog.dismiss();
			
			if(mUsers == null || mUsers.size() <= 0)
			{
				textViewNoUsers.setVisibility(View.VISIBLE);
			}
			else
			{
				textViewNoUsers.setVisibility(View.GONE);
				
				// TODO : Is this notifyDataSetChanged needed ?
//				mUsersAdapter.notifyDataSetChanged();
				
				for(int indexUser = 0; indexUser < mUsers.size(); ++indexUser)
				{
					mUsersAdapter.add(mUsers.get(indexUser));
				}
			}
			
			mUsersAdapter.notifyDataSetChanged();
			
			// TODO : Listen to users connecting 
		}
	};

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
			
			// Send a command to get the rest of the selcted user's details
			Messages.MessageGetUserDetails msgGetUserDetails = new Messages.MessageGetUserDetails(user.getIPAddress());
			application.sendMessage(msgGetUserDetails.toString());
			
			intent.putExtra(getResources().getString(R.string.extra_key_main_data), arrMainDataValues);
			
			// TODO : Also pass the picture
			
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
//    		menuItemChat.setAlphabeticShortcut('a');
//    		menuItemChat.setIcon(R.drawable.alert_dialog_icon);
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
    		chat();
    	}
    	
    	return true;    
    }    
	
	private void chat()
	{
    	Intent intent = new Intent(this, ActivityChat.class);
    	
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
			mUsers = application.getUsers();
			mUsersAdapter = new UsersAdapter(ActivityUsersList.this, R.layout.user, mUsers);
			setListAdapter(mUsersAdapter);
			mUsersAdapter.notifyDataSetChanged();
//			ActivityUsersList.this.updateListView();
		}
	};

	public Handler getUpdateHandler()
	{
		return mHandler;
	}

//	protected void updateListView()
//	{
//		// TODO Auto-generated method stub
//	}
}
