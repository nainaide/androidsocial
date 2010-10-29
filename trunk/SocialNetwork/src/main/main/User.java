package main.main;

import java.util.Calendar;
import java.util.GregorianCalendar;


public class User
{
	public enum Sex
	{
		MALE
		{
			public String toString()
			{
				return "Male";
			}
		},
		FEMALE
		{
			public String toString()
			{
				return "Female";
			}
		}
	};

	private final int INDEX_FIRST_NAME = 0;
	private final int INDEX_LAST_NAME = 1;

	private String mFirstName = "";
	private String mLastName = "";
	private Sex mSex;
	private GregorianCalendar mDateBirth;

	// TODO : Add a picture to the user
	
	private String mHobbies = "";
	private String mFavoriteMusic = "";

	private long mLastPongTime = 0;
	private String mIPaddress;


	//	public String statusMsg;

	public User(String newFirstName, String newLastName, Sex newSex, GregorianCalendar newDateBirth, String newIPAddress)
	{
		setFirstName(newFirstName);
		setLastName(newLastName);
		setSex(newSex);
		setDateBirth(newDateBirth);
		setIPAddress(newIPAddress);
		
		setLastPongTime(System.currentTimeMillis());
	}

	public User(String newFirstName, String newLastName, Sex newSex, int newBirthYear, int newBirthMonth, int newBirthDay, String newIPAddress)
	{
		this(newFirstName, newLastName, newSex, new GregorianCalendar(newBirthYear, newBirthMonth, newBirthDay), newIPAddress);
	}

//	public User(String userFileName, String userName)
//	{
//		// TODO : Implement
//		// TODO : When extraction of username from the file (or file name) will be done, remove the additional parameter - userName
//		setFullName(userName);
//		setSex(Sex.FEMALE);
//		setDateBirth(new GregorianCalendar(1982, 8, 23));
//		
//		
//		// If the user's file exists
//		
//			// Read the details, if exist
//
//	}
	
	public User(Messages.MessageNewUser msgNewUser)
	{
		// Structure : NewUser IPAddress username birthDate Sex Picture
		setIPAddress(msgNewUser.getIPAddress());
		setFullName(msgNewUser.getUsername());
		setDateBirth(new GregorianCalendar(Integer.parseInt(msgNewUser.getDateYear()), Integer.parseInt(msgNewUser.getDateMonth()), Integer.parseInt(msgNewUser.getDateDay())));
		setSex(Sex.valueOf(msgNewUser.getSex().toUpperCase()));
		
		setLastPongTime(System.currentTimeMillis());
		
		// TODO : Deal with the picture
	}
	
	
	public String getFullName()
	{
		return mFirstName + ((mLastName != null && mLastName.equals("") == false) ? " " + mLastName : "");
	}

	public String getSex()
	{
		return mSex.toString();
	}

	public GregorianCalendar getDateBirth()
	{
		return mDateBirth;
	}

	public int getAge()
	{
		Calendar cal = Calendar.getInstance();
		int age;

		// Calculate the difference in years between Now and the user's birth date
		age = cal.get(Calendar.YEAR) - mDateBirth.get(GregorianCalendar.YEAR);

		// Decrease the age by 1 if Now's month is earlier than the birth date's or if
		// they have the same month, but the Now's day is earlier
		if (cal.get(Calendar.MONTH) < mDateBirth.get(GregorianCalendar.MONTH) ||
			(cal.get(Calendar.MONTH) == mDateBirth.get(GregorianCalendar.MONTH) &&
			 cal.get(Calendar.DAY_OF_MONTH) < mDateBirth.get(GregorianCalendar.DAY_OF_MONTH)))
		{
			--age;
		}

		age = (age < 0) ? 0 : age;

		return age;
	}

	public String getIPAddress()
	{
		return mIPaddress;
	}

	public long getLastPongTime()
	{
		return mLastPongTime;
	}
	
	public String getHobbies()
	{
		return mHobbies;
	}
	
	public String getFavoriteMusic()
	{
		return mFavoriteMusic;
	}
	
	
	public void setFullName(String newFullName)
	{
		String[] nameParts = newFullName.split(" ");

		// TODO : Check the validity of the name
		if (nameParts.length >= 2)
		{
			setFirstName(nameParts[INDEX_FIRST_NAME]);
			setLastName(nameParts[INDEX_LAST_NAME]);
		}
		else
		{
			if (INDEX_FIRST_NAME < INDEX_LAST_NAME)
			{
				setFirstName(nameParts[INDEX_FIRST_NAME]);
			}
			else
			{
				setLastName(nameParts[INDEX_LAST_NAME]);
			}
		}
	}

	public void setFirstName(String newFirstName)
	{
		mFirstName = newFirstName;
	}

	public void setLastName(String newLastString)
	{
		mLastName = newLastString;
	}

	public void setSex(Sex newSex)
	{
		mSex = newSex;
	}

	public void setDateBirth(GregorianCalendar newDateBirth)
	{
		mDateBirth = newDateBirth;
	}

	public void setIPAddress(String newIPAddress)
	{
		mIPaddress = newIPAddress;
	}

	public void setLastPongTime(long newLastPongTime)
	{
		mLastPongTime = newLastPongTime;
	}
	
	public void setHobbies(String newHobbies)
	{
		mHobbies = newHobbies;
	}
	
	public void setFavoriteMusic(String newFavoriteMusic)
	{
		mFavoriteMusic = newFavoriteMusic;
	}
	
	
	public boolean equals(Object objOther)
	{
		User userOther = (User) objOther;

		return (getFullName().equals(userOther.getFullName()) && mIPaddress.equals(userOther.getIPAddress()));
	}
}
