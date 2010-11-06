package main.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;

/**
 * This activity enables creating a new user and selecting his basic details : username, sex, date of birth and picture
 */
public class ActivityCreateUser extends Activity implements OnClickListener {
	
	private static final int GET_PICTURE_FILE_NAME = 0x01;
	
	public static final String EXTRA_KEY_USERNAME = "userName";
	public static final String EXTRA_KEY_SEX = "sex";
	public static final String EXTRA_KEY_DATE_BIRTH = "birthday";
	public static final String EXTRA_KEY_PIC_FILENAME = "pictureFileName";
	
	
	private String pictureFileName = "";
	
	
	public void onCreate( Bundle bundle) {
		super.onCreate(bundle);
		setContentView( R.layout.create_user);
		
		RadioButton radioFemale = (RadioButton)findViewById( R.id.RadioButtonCreateUserFemale);
		radioFemale.setOnClickListener( this);
		
		RadioButton radioMale = (RadioButton)findViewById( R.id.RadioButtonCreateUserMale);
		radioMale.setOnClickListener( this);
		
		Button btnCancel = (Button)findViewById( R.id.ButtonCancel);
		btnCancel.setOnClickListener( this);
		
		Button btnCreate = (Button)findViewById( R.id.ButtonCreate);
		btnCreate.setOnClickListener( this);
		
		Button btnFileBrowser = (Button)findViewById( R.id.btnUserPicture);
		btnFileBrowser.setOnClickListener( this);
		
		EditText userNameText = (EditText)findViewById(R.id.EditTextCreateUserUserName);
		userNameText.setText( "");
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	public void onClick(View v) {
		switch ( v.getId()) {
			case R.id.btnUserPicture : {
				Intent fileBrowser = new Intent( this, ActivityFileBrowser.class);
				startActivityForResult( fileBrowser, GET_PICTURE_FILE_NAME);
				break;
			}
			case R.id.RadioButtonCreateUserFemale : {
				RadioButton maleRadio = (RadioButton)findViewById( R.id.RadioButtonCreateUserMale);
				maleRadio.setChecked( false);
				break;
			}
			case R.id.RadioButtonCreateUserMale : {
				RadioButton femaleRadio = (RadioButton)findViewById( R.id.RadioButtonCreateUserFemale);
				femaleRadio.setChecked( false);
				break;
			}
			case R.id.ButtonCreate : {
				EditText txtUserName = (EditText)findViewById(R.id.EditTextCreateUserUserName);
				String userName = txtUserName.getText().toString();
				RadioButton rdoMale = (RadioButton)findViewById(R.id.RadioButtonCreateUserMale);
				String sex = (rdoMale.isChecked() ? User.Sex.MALE.toString() : User.Sex.FEMALE.toString());
				DatePicker dateBirth = (DatePicker)findViewById(R.id.DatePickerCreateUserBirth);
				
				Intent resultData = new Intent( );
				resultData.putExtra(EXTRA_KEY_USERNAME, userName);
				resultData.putExtra(EXTRA_KEY_SEX, sex);
				resultData.putExtra(EXTRA_KEY_DATE_BIRTH, dateBirth.getYear()  + ActivityUserDetails.DATE_ELEMENTS_SEPARATOR +
											 			  dateBirth.getMonth() + ActivityUserDetails.DATE_ELEMENTS_SEPARATOR +
											 			  dateBirth.getDayOfMonth());
				resultData.putExtra(EXTRA_KEY_PIC_FILENAME, pictureFileName);
				setResult( RESULT_OK, resultData);
				finish( );
				break;
			}
			case R.id.ButtonCancel : {
				setResult(RESULT_CANCELED);
				finish( );
				break;
			}
			default: {
				break;
			}
		}
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch( requestCode) {
			case GET_PICTURE_FILE_NAME : {
				if ( resultCode == RESULT_OK) {
					pictureFileName = data.getStringExtra(ActivityFileBrowser.EXTRA_KEY_FILENAME);
					ImageView userPic = (ImageView)findViewById( R.id.userImage);
					userPic.setImageBitmap( BitmapFactory.decodeFile( pictureFileName));
				}
			}
			default:  {
				break;
			}
		}
	}
}
