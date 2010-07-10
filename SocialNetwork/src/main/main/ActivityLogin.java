package main.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

public class ActivityLogin extends Activity implements OnClickListener
{
	private final int DIALOG_ID_CREATE_USER = 0;
	private String FILE_NAME_PREFS; // If I final it and assign its value here, it will crash
	private final String USER_NAME_EMPTY = "";
//	private final String DIR_RELATIVE_NAME_USERS = "users";
//	private String DIR_NAME_USERS;
	private String DIR_NAME_USERS = "users";
	private final String USER_FILE_NAME_PREFIX = "user_";
	private final String USER_FILE_NAME_SUFFIX = "";
	private final String USER_FILE_NAME_EXTENSION = "";
	
	private final String LOG_TAG = "SN.Login";
	
	private String mUserName = USER_NAME_EMPTY;
	private ArrayAdapter<CharSequence> mAdapter;
	
	ApplicationSocialNetwork application = null;
	public static ActivityLogin instance = null;
//	private DialogInterface.OnDismissListener mOnDismissListener;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

Log.d(LOG_TAG, "D Where will this be printed ??");

    	application = (ApplicationSocialNetwork)getApplication();
    	ActivityLogin.instance = this;
        FILE_NAME_PREFS = getResources().getString(R.string.file_name_prefs);
//        DIR_NAME_USERS = getApplicationContext().getFilesDir() + File.separator + DIR_RELATIVE_NAME_USERS;

        // Get the last logged-in user name (and put it in mUserName)
		SharedPreferences settings = getSharedPreferences(FILE_NAME_PREFS, MODE_PRIVATE);
    	String prefNameLastLoggedInUserName = getResources().getString(R.string.pref_name_last_logged_in_user_name);
    	mUserName = settings.getString(prefNameLastLoggedInUserName, USER_NAME_EMPTY);
    	
        // TODO : Check if the user checked to always login with a certain user.
        //        If so, don't display this screen. Simply login with that user
//        loginAutomaticallyIfNeeded();
        
        // Else, if it wasn't set to login automatically, continue (show the activity)
        
        // Set all the listeners (e.g for buttons, dialogs) and adapters
        setListenersAndAdapters();
        
        // TODO : Get the list of users on the current machine and populate the spinner
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
    
	// Implement the OnClickListener callback
	public void onClick(View view)
	{
    	switch (view.getId())
    	{
    		case R.id.ButtonCreateUser :
    		{
    			// Show the user creation dialog
    			showDialog(DIALOG_ID_CREATE_USER);

    			break;
    		}
    		
    		case R.id.ButtonDeleteUser :
    		{
				Spinner spinnerUserNames = (Spinner) findViewById(R.id.SpinnerUserName);
				String userNameToDelete = (String) spinnerUserNames.getSelectedItem();
				// TODO : Ask the user if he is sure he wants to delete the selected user
				
				// Delete the user file
//				File userFile = new File(DIR_NAME_USERS + File.separator + userNameToDelete);
//				userFile.delete();
				getApplicationContext().deleteFile(getUserFileName(userNameToDelete));
				
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

	private String getUserFileName(String userName)
	{
//		String extension = "";
//		
//		if (USER_FILE_NAME_EXTENSION.equals("") == false)
//		{
//			extension = "." + USER_FILE_NAME_EXTENSION;
//		}
		
		return USER_FILE_NAME_PREFIX + userName + USER_FILE_NAME_SUFFIX + USER_FILE_NAME_EXTENSION;
	}

	private String getUserNameFromUserFileName(String userFileName)
	{
		return userFileName.substring(USER_FILE_NAME_PREFIX.length(),
									  userFileName.length() - (USER_FILE_NAME_SUFFIX.length() + USER_FILE_NAME_EXTENSION.length())); 
	}

//	private File getUesrsDir()
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}


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
			application.loadMyDetails(getUserFileName(userNameToLogin)); //, userNameToLogin);
			
			// Open the opening screen's activity
//			Intent intent = new Intent(this, ActivityConnect.class);
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
//		File dirUsers = getApplicationContext().getDir(DIR_NAME_USERS, MODE_PRIVATE);
		File dirUsers = getApplicationContext().getFilesDir();
		String[] userFilesNames = dirUsers.list(new FilenameFilter() {
			public boolean accept(File dir, String fileName)
			{
				boolean shouldAccecpt = false;
				
				// TODO : Check the fileName. It probably contains an extension and it
				//        needs to be dealt with
				if (fileName.startsWith(USER_FILE_NAME_PREFIX) &&
					fileName.endsWith(USER_FILE_NAME_SUFFIX + USER_FILE_NAME_EXTENSION))
				{
					shouldAccecpt = true;
				}

				return shouldAccecpt;
			}
		});
		
		// Add all the users to the spinner
		for (String currUserFileName : userFilesNames)
		{
			mAdapter.add(getUserNameFromUserFileName(currUserFileName));
		}
		
		// TODO : The keyboard didn't show on one of the devices and I got stuck without any users, so I added these.
		//        Delete this code when the keyboard decides it wants to work again
//		mAdapter.add("NoKeyboard1");
//		mAdapter.add("NoKeyboard2");
//		mAdapter.add("NoKeyboard3");
		
		
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

	protected Dialog onCreateDialog(int id)
	{
	    Dialog dialog = null;
	    
	    switch(id)
	    {
		    case DIALOG_ID_CREATE_USER:
		    {
		    	mUserName = "";
		    	
		    	dialog = new Dialog(this);
    			
    			dialog.setContentView(R.layout.dlg_layout_create_user);
    			dialog.setTitle("Create A New User");
    			
    			Button buttonCreate = (Button) dialog.findViewById(R.id.ButtonCreate);
    			buttonCreate.setOnClickListener(new Button.OnClickListener() {
					public void onClick(View view)
					{
						Spinner spinnerUserNames = (Spinner) findViewById(R.id.SpinnerUserName);
						EditText txtUserName = (EditText)((View)( view.getParent().getParent()) ).findViewById(R.id.EditTextCreateUserUserName);
						String createdUserName = txtUserName.getText().toString();
				
						if (createdUserName.equals(USER_NAME_EMPTY))
						{
							Toast.makeText(ActivityLogin.this, "User name cannot be empty", Toast.LENGTH_LONG).show();
						}
						else
						{
							// Check if the spinner already contains the created user
							if (mAdapter.getPosition(createdUserName) >= 0)
							{
								Toast.makeText(ActivityLogin.this, "User already exists", Toast.LENGTH_LONG).show();
							}
							else
							{
								// Add the new user name to the spinner and sort the values
								mAdapter.add(createdUserName);
								mAdapter.sort(null);
														
								// Set the new user name in the spinner
								spinnerUserNames.setSelection(mAdapter.getPosition(createdUserName));
								
								mUserName = createdUserName;
								
								// Create a file for the user
//								File dirUsers = getApplicationContext().getDir(DIR_NAME_USERS, MODE_PRIVATE);
//								dirUsers.c

								String userFileName = getUserFileName(mUserName);
								FileOutputStream fos = null;
								OutputStreamWriter osw = null;
								try {
//									 fos = new FileOutputStream(getApplicationContext().getFilesDir() + File.separator + DIR_NAME_USERS + File.separator + mUserName);
									 fos = openFileOutput(userFileName, MODE_PRIVATE);
									 osw = new OutputStreamWriter(fos); 
//									 osw.write(mUserName);
								} catch (FileNotFoundException e) {
									e.printStackTrace();
//								} catch (IOException e) {
//									e.printStackTrace();
								} finally {
									try {
										osw.flush();
										fos.close();
										osw.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
								
								RadioButton rdoMale = (RadioButton)((View)( view.getParent().getParent()) ).findViewById(R.id.RadioButtonCreateUserMale);
								String sex = (rdoMale.isChecked() ? User.Sex.MALE.toString() : User.Sex.FEMALE.toString());
								DatePicker dateBirth = (DatePicker)((View)( view.getParent().getParent()) ).findViewById(R.id.DatePickerCreateUserBirth);
								
								application.writePropertyToFile(userFileName, "Username", mUserName);
								application.writePropertyToFile(userFileName, "Sex", sex);
								application.writePropertyToFile(userFileName, "Date of Birth", dateBirth.getYear() + " " + dateBirth.getMonth() + " " + dateBirth.getDayOfMonth());
							}
							
							dismissDialog(DIALOG_ID_CREATE_USER);
						}
					}
				});
    			
    			Button buttonCancel = (Button) dialog.findViewById(R.id.ButtonCancel);
    			buttonCancel.setOnClickListener(new Button.OnClickListener() {
					public void onClick(View view) {
						dismissDialog(DIALOG_ID_CREATE_USER);
					}
				});

    			EditText txtNewUserName = (EditText) dialog.findViewById(R.id.EditTextCreateUserUserName);
    			txtNewUserName.setText("keyboard broken");
    			
		        break;
		    }
		    
		    default:
		    {
		        dialog = null;
		    }
	    }
	    
	    return dialog;
	}
	
	public void onPrepareDialog(int id, Dialog dialog)
	{
		switch (id)
		{
			case DIALOG_ID_CREATE_USER :
			{
				EditText editTextNewUserName = (EditText) dialog.findViewById(R.id.EditTextCreateUserUserName);
				
				editTextNewUserName.setText("");
				
				break;
			}
		}
	}
	private Handler mHandler = new Handler() {
		public String getUserName()
		{
			return mUserName;
				}
	};

	public Handler getUpdateHandler()
	{
		return mHandler;
	}
	
	public String getUserName()
	{
		return mUserName;
			}
}