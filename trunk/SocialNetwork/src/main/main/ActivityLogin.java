package main.main;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

public class ActivityLogin extends Activity implements OnClickListener
{
	private static final int CREATE_NEW_USER = 0x01;
	private String FILE_NAME_PREFS; // If I final it and assign its value here, it will crash
	private static final String USER_NAME_EMPTY = "";
	
	private String mUserName = USER_NAME_EMPTY;
	private ArrayAdapter<CharSequence> mAdapter;
	
	ApplicationSocialNetwork application = null;
	public static ActivityLogin instance = null;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

    	application = (ApplicationSocialNetwork)getApplication();
    	ActivityLogin.instance = this;
        FILE_NAME_PREFS = getResources().getString(R.string.file_name_prefs);

        // Get the last logged-in user name (and put it in mUserName)
		SharedPreferences settings = getSharedPreferences(FILE_NAME_PREFS, MODE_PRIVATE);
    	String prefNameLastLoggedInUserName = getResources().getString(R.string.pref_name_last_logged_in_user_name);
    	mUserName = settings.getString(prefNameLastLoggedInUserName, USER_NAME_EMPTY);
    	
        // Check if the user checked to always login with a certain user.
        // If so, don't display this screen. Simply login with that user
        loginAutomaticallyIfNeeded();
        
        // Else, if it wasn't set to login automatically, continue (show the activity)
        
        // Set all the listeners (e.g for buttons, dialogs) and adapters
        setListenersAndAdapters();
        
        // Get the list of users on the current machine and populate the spinner
        populateSpinnerUserNames();
	}

	private void loginAutomaticallyIfNeeded()
	{
		SharedPreferences settings = getSharedPreferences(FILE_NAME_PREFS, MODE_PRIVATE);
        String prefNameShouldLoginAutomatically = getResources().getString(R.string.pref_name_should_login_automatically);
        boolean shouldLoginAutomatically = settings.getBoolean(prefNameShouldLoginAutomatically, false);

        if (shouldLoginAutomatically)
        {
        	login(mUserName);
        }
	}
    
	private void setListenersAndAdapters()
	{
		Button buttonCreateUser = (Button)findViewById(R.id.ButtonCreateUser);
        buttonCreateUser.setOnClickListener(this);

        Button buttonDeleteUser = (Button) findViewById(R.id.ButtonDeleteUser);
        buttonDeleteUser.setOnClickListener(this);
        
        Button buttonLogin = (Button) findViewById(R.id.ButtonLogin);
        buttonLogin.setOnClickListener(this);
        
		Spinner spinnerUserNames = (Spinner) findViewById(R.id.SpinnerUserName);
		mAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);  
		mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerUserNames.setAdapter(mAdapter);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch ( requestCode) {
			case CREATE_NEW_USER : {
				if ( resultCode == RESULT_OK) {
					String userName = mUserName =  data.getStringExtra( "userName");
					String sex		= data.getStringExtra( "sex");
					String dateOfBirth = data.getStringExtra( "birthday");
					String pictureFileName = data.getStringExtra( "pictureFileName");
					String userFileName = application.getUserFileName(userName);
					application.writePropertyToFile( userFileName, "Username", userName);
					application.writePropertyToFile( userFileName, "Sex", sex);
					application.writePropertyToFile( userFileName, "Date of Birth", dateOfBirth);
					application.writePropertyToFile( userFileName, "Picture file name", pictureFileName);
					application.setFileNameForManager( pictureFileName);
					Spinner spinnerUserNames = (Spinner) findViewById(R.id.SpinnerUserName);
					
					if (userName.equals("")) {
						Toast.makeText(this, "User name cannot be empty", Toast.LENGTH_LONG).show();
					} else {
						// Check if the spinner already contains the created user
						if (mAdapter.getPosition(userName) >= 0) {
							Toast.makeText(this, "User already exists", Toast.LENGTH_LONG).show();
						} else {
							// Add the new user name to the spinner and sort the values
							mAdapter.add(userName);
							mAdapter.sort(null);

							// Set the new user name in the spinner
							spinnerUserNames.setSelection(mAdapter.getPosition(userName));
						}
					}
				}
			}
			default : {
				break;
			}
		}
	}
	
	// Implement the OnClickListener callback
	public void onClick(View view)
	{
    	switch (view.getId())
    	{
    		case R.id.ButtonCreateUser :
    		{
    			Intent createUserActivity = new Intent( this, ActivityCreateUser.class);
    			startActivityForResult( createUserActivity, CREATE_NEW_USER);
    			break;
    		}
    		
    		case R.id.ButtonDeleteUser :
    		{
				Spinner spinnerUserNames = (Spinner) findViewById(R.id.SpinnerUserName);
				String userNameToDelete = (String) spinnerUserNames.getSelectedItem();
				
				// TODO : Ask the user if he is sure he wants to delete the selected user
				
				// Delete the user file
				getApplicationContext().deleteFile(application.getUserFileName(userNameToDelete));
				
				// Remove the user name from the spinner
				mAdapter.remove(userNameToDelete);
    			
    			break;
    		}
    		
    		case R.id.ButtonLogin :
    		{
    			// Get the selected user
    			Spinner spinnerUserName = (Spinner) findViewById(R.id.SpinnerUserName);
    			String selectedUserName = (String) spinnerUserName.getSelectedItem();
    			
    			if (selectedUserName == null || selectedUserName.equals(USER_NAME_EMPTY))
    			{
    				Toast.makeText(this, "You must select a user in order to login", Toast.LENGTH_SHORT).show();
    			}
    			else
    			{
//    				if (application.didRunBefore() && application.getCurrentState() == NetControlState.LEADER)
//    				{
//    					application.disableAdhocLeader();
//    				}
    					
    				// First, set the preference whether we should auto-login the next time
    				SharedPreferences prefs = getSharedPreferences(FILE_NAME_PREFS, MODE_PRIVATE);
    				SharedPreferences.Editor prefEditor = prefs.edit();
    				String prefNameShouldLoginAutomatically = getResources().getString(R.string.pref_name_should_login_automatically);
    				CheckBox checkBoxAutomaticallyLoginWithUser = (CheckBox) findViewById(R.id.CheckBoxAutomaticallyLoginWithUser);

    				prefEditor.putBoolean(prefNameShouldLoginAutomatically, checkBoxAutomaticallyLoginWithUser.isChecked());
    				
    				prefEditor.commit();
    				
    				// Login with the selected user name
    				login(selectedUserName);
    			}
    			
    			break;
    		}
    	}
    }

	private void login(String userNameToLogin)
	{
		if (userNameToLogin.equals(USER_NAME_EMPTY) == false)
		{
			// First, set the preference whether we should auto-login the next time
			SharedPreferences prefs = getSharedPreferences(FILE_NAME_PREFS, MODE_PRIVATE);
			SharedPreferences.Editor prefEditor = prefs.edit();
        	String prefNameLastLoggedInUserName = getResources().getString(R.string.pref_name_last_logged_in_user_name);
        	
			prefEditor.putString(prefNameLastLoggedInUserName, userNameToLogin);
			
			prefEditor.commit();

			// Get the application to load the user's data, if available
			application.loadMyDetails(application.getUserFileName(userNameToLogin));
			
			// Open the opening screen's activity
			Intent intent = new Intent(this, ActivityUsersList.class);
//			intent.putExtra(getResources().getString(R.string.extra_key_login_user_name), userNameToLogin);
			startActivity(intent);
		}
	}
	
	private void populateSpinnerUserNames()
	{
		Spinner spinnerUserNames = (Spinner) findViewById(R.id.SpinnerUserName);
		
		// Check the list of users files. Each file will have the name of the relevant
		// user. This name will be entered to the spinner
		File dirUsers = getApplicationContext().getFilesDir();
		String[] userFilesNames = dirUsers.list(new FilenameFilter() {
			public boolean accept(File dir, String fileName)
			{
				boolean shouldAccecpt = false;
				
				// For each file, check by its name if it is in fact a user details file
				if (fileName.startsWith(ApplicationSocialNetwork.USER_FILE_NAME_PREFIX) &&
					fileName.endsWith(ApplicationSocialNetwork.USER_FILE_NAME_SUFFIX + ApplicationSocialNetwork.USER_FILE_NAME_EXTENSION))
				{
					shouldAccecpt = true;
				}

				return shouldAccecpt;
			}
		});
		
		// Add all the users to the spinner
		for (String currUserFileName : userFilesNames)
		{
			mAdapter.add(application.getUserNameFromUserFileName(currUserFileName));
		}

		// Sort the users by their names
		mAdapter.sort(null);

		// Select the last logged-in user in the spinner
		if (mUserName.equals(USER_NAME_EMPTY) == false)
		{
			// Check if the spinner contains the last logged-in user
			if (mAdapter.getPosition(mUserName) >= 0)
			{
				spinnerUserNames.setSelection(mAdapter.getPosition(mUserName));
			}
			else
			{
				// The last logged-in user doesn't exist anymore (perhaps the user logged-out
				// and then deleted its own user), so assign an empty user name
				mUserName = USER_NAME_EMPTY;
			}
		}
	}


//	private Handler mHandler = new Handler() {
//		public String getUserName()
//		{
//			return mUserName;
//		}
//	};
//
//	public Handler getUpdateHandler()
//	{
//		return mHandler;
//	}
	
	public String getUserName()
	{
		return mUserName;
	}
}