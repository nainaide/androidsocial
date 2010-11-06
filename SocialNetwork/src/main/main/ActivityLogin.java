package main.main;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

/**
 * This activity is the login screen. It enables creating a new user, selecting an existing user to login with, deleting a user,
 * enabling the option to automatically login from now on with the selected user (Thus not showing the login screen from now on. It can be shown
 * again by logging out from within the application) and, of course, logging in.
 */
public class ActivityLogin extends Activity implements OnClickListener
{
	private static final int CREATE_NEW_USER = 0x01;
	private String FILE_NAME_PREFS; // If I final it and assign its value here, it will crash
	private static final String USER_NAME_EMPTY = "";
	
	private String mUserName = USER_NAME_EMPTY;
	private ArrayAdapter<CharSequence> mArrayAdapter;
	
	private Spinner mSpinnerUserNames = null;
	private CheckBox mCheckBoxAutomaticallyLogin = null;
	
	private ApplicationSocialNetwork application = null;
	public static ActivityLogin instance = null;
	
	private SharedPreferences mPrefs = null;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

    	application = (ApplicationSocialNetwork)getApplication();
    	ActivityLogin.instance = this;
        FILE_NAME_PREFS = getResources().getString(R.string.file_name_prefs);

        mSpinnerUserNames = (Spinner) findViewById(R.id.SpinnerUserName);
        mCheckBoxAutomaticallyLogin = (CheckBox) findViewById(R.id.CheckBoxAutomaticallyLoginWithUser);
        
        // Get the last logged-in user name (and put it in mUserName)
		mPrefs = getSharedPreferences(FILE_NAME_PREFS, MODE_PRIVATE);
		
    	String prefNameLastLoggedInUserName = getResources().getString(R.string.pref_name_last_logged_in_user_name);
    	mUserName = mPrefs.getString(prefNameLastLoggedInUserName, USER_NAME_EMPTY);
    	
        // Check if the user checked to automatically login with a certain user.
        // If so, don't display this activity. Simply login with that user
        loginAutomaticallyIfNeeded();
        
        // Set all the listeners (e.g for buttons, dialogs) and adapters
        setListenersAndAdapters();
        
        // Get the list of users on the current machine and populate the spinner
        populateSpinnerUserNames();
	}

    /**
     * If the user has previously checked the option to automatically login, then login without showing the activity.
     */
	private void loginAutomaticallyIfNeeded()
	{
        String prefNameShouldLoginAutomatically = getResources().getString(R.string.pref_name_should_login_automatically);
        boolean shouldLoginAutomatically = mPrefs.getBoolean(prefNameShouldLoginAutomatically, false);

        // Check or uncheck the check box as needed (We check it even when we are going to login and go to a different activity so
        // when the user logs out, he will see it as selected)
        mCheckBoxAutomaticallyLogin.setChecked(shouldLoginAutomatically);
        
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
        
		mArrayAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);  
		mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinnerUserNames.setAdapter(mArrayAdapter);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch ( requestCode) {
			case CREATE_NEW_USER : {
				if ( resultCode == RESULT_OK) {
					String userName = mUserName = data.getStringExtra(ActivityCreateUser.EXTRA_KEY_USERNAME);
					String sex		= data.getStringExtra(ActivityCreateUser.EXTRA_KEY_SEX);
					String dateOfBirth = data.getStringExtra(ActivityCreateUser.EXTRA_KEY_DATE_BIRTH);
					String pictureFileName = data.getStringExtra(ActivityCreateUser.EXTRA_KEY_PIC_FILENAME);
					String userFileName = application.getUserFileName(userName);
					
					application.writePropertyToFile( userFileName, ApplicationSocialNetwork.USER_PROPERTY_USERNAME, userName);
					application.writePropertyToFile( userFileName, ApplicationSocialNetwork.USER_PROPERTY_SEX, sex);
					application.writePropertyToFile( userFileName, ApplicationSocialNetwork.USER_PROPERTY_DATE_OF_BIRTH, dateOfBirth);
					application.writePropertyToFile( userFileName, ApplicationSocialNetwork.USER_PROPERTY_PIC_FILE_NAME, pictureFileName);
					
					application.setFileNameForManager( pictureFileName);
					
					// Validation checks
					if (userName.equals("")) {
						application.showToast(this, "User name cannot be empty");
					} else {
						// Check if the spinner already contains the created user
						if (mArrayAdapter.getPosition(userName) >= 0) {
							application.showToast(this, "User already exists");
						} else {
							// Add the new user name to the spinner and sort the values
							mArrayAdapter.add(userName);
							mArrayAdapter.sort(null);

							// Set the new user name in the spinner
							mSpinnerUserNames.setSelection(mArrayAdapter.getPosition(userName));
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
		// All the clickable views set "this" as their handler, so we need to first find out which view was clicked
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
				final String userNameToDelete = (String) mSpinnerUserNames.getSelectedItem();
				
				// Ask the user if he is sure he wants to delete the selected user
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				
				builder.setMessage("Are you sure you want to delete the user \"" + userNameToDelete + "\" ?");
				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id)
					{
						// Delete the user file
						getApplicationContext().deleteFile(application.getUserFileName(userNameToDelete));
						
						// Remove the user name from the spinner
						mArrayAdapter.remove(userNameToDelete);
					}
				});
				builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id)
					{
						dialog.cancel();
					}
				});
				
				AlertDialog alert = builder.create();
				alert.show();
				
    			break;
    		}
    		
    		case R.id.ButtonLogin :
    		{
    			// Get the selected user
    			String selectedUserName = (String) mSpinnerUserNames.getSelectedItem();
    			
    			if (selectedUserName == null || selectedUserName.equals(USER_NAME_EMPTY))
    			{
    				application.showToast(this, "You must select a user in order to login");
    			}
    			else
    			{
//    				if (application.didRunBefore() && application.getCurrentState() == NetControlState.LEADER)
//    				{
//    					application.disableAdhocLeader();
//    				}
    					
    				// First, set the preference whether we should auto-login the next time
    				Editor prefEditor = mPrefs.edit();
    				String prefNameShouldLoginAutomatically = getResources().getString(R.string.pref_name_should_login_automatically);

    				prefEditor.putBoolean(prefNameShouldLoginAutomatically, mCheckBoxAutomaticallyLogin.isChecked());
    				
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
		// Another check that the username isn't empty, to be sure
		if (userNameToLogin.equals(USER_NAME_EMPTY) == false)
		{
			// First, set the preference whether we should auto-login the next time
			Editor prefEditor = mPrefs.edit();
        	String prefNameLastLoggedInUserName = getResources().getString(R.string.pref_name_last_logged_in_user_name);
        	
			prefEditor.putString(prefNameLastLoggedInUserName, userNameToLogin);
			
			prefEditor.commit();

			mUserName = userNameToLogin;
			
			// Get the application to load the user's data, if available
			application.loadMyDetails(application.getUserFileName(userNameToLogin));
			
			// Open the opening screen's activity - the users list
			Intent intent = new Intent(this, ActivityUsersList.class);
			startActivity(intent);
		}
	}
	
	/**
	 * Populates the users spinner with all the known users. This is done by searching for all the user-data files available,
	 * each one for a different user.
	 */
	private void populateSpinnerUserNames()
	{
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
			mArrayAdapter.add(application.getUserNameFromUserFileName(currUserFileName));
		}

		// Sort the users by their names
		mArrayAdapter.sort(null);

		// Select the last logged-in user in the spinner
		if (mUserName.equals(USER_NAME_EMPTY) == false)
		{
			// Check if the spinner contains the last logged-in user
			if (mArrayAdapter.getPosition(mUserName) >= 0)
			{
				mSpinnerUserNames.setSelection(mArrayAdapter.getPosition(mUserName));
			}
			else
			{
				// The last logged-in user doesn't exist anymore (perhaps the user logged-out
				// and then deleted its own user), so assign an empty user name
				mUserName = USER_NAME_EMPTY;
			}
		}
	}

	public String getUserName()
	{
		return mUserName;
	}
}