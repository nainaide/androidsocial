package main.main;

import java.util.Calendar;
import java.util.GregorianCalendar;


/**
 * Represents a user in the program.
 */
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

	private String mUesrname;
	private Sex mSex;
	private GregorianCalendar mDateBirth;

	private String mHobbies = "";
	private String mFavoriteMusic = "";

	private long mLastPongTime = 0;
	private String mIPaddress;


	public User(String newUsername, Sex newSex, GregorianCalendar newDateBirth)
	{
		setUsername(newUsername);
		setSex(newSex);
		setDateBirth(newDateBirth);
		
		setIPAddress("");
		
		setLastPongTime(System.currentTimeMillis());
	}

	public User(String newUsername, Sex newSex, int newBirthYear, int newBirthMonth, int newBirthDay)
	{
		this(newUsername, newSex, new GregorianCalendar(newBirthYear, newBirthMonth, newBirthDay));
	}

	public User(Messages.MessageNewUser msgNewUser)
	{
		// Structure of MessageNewUser : NewUser IPAddress username birthDate Sex Picture
		this(msgNewUser.getUsername(),
			 Sex.valueOf(msgNewUser.getSex().toUpperCase()),
			 new GregorianCalendar(Integer.parseInt(msgNewUser.getDateYear()), Integer.parseInt(msgNewUser.getDateMonth()), Integer.parseInt(msgNewUser.getDateDay())));
		
		setIPAddress(msgNewUser.getIPAddress());
	}
	
	
	public boolean equals(Object objOther)
	{
		User userOther = (User) objOther;
		
		return (getUsername().equals(userOther.getUsername()) && mIPaddress.equals(userOther.getIPAddress()));
	}

	public String getUsername()
	{
		return mUesrname;
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
		Calendar calendar = Calendar.getInstance();
		int age;

		// Calculate the difference in years between Now and the user's birth date
		age = calendar.get(Calendar.YEAR) - mDateBirth.get(GregorianCalendar.YEAR);

		// Decrease the age by 1 if Now's month is earlier than the birth date's or if
		// they have the same month, but the Now's day is earlier
		if (calendar.get(Calendar.MONTH) < mDateBirth.get(GregorianCalendar.MONTH) ||
			(calendar.get(Calendar.MONTH) == mDateBirth.get(GregorianCalendar.MONTH) &&
			 calendar.get(Calendar.DAY_OF_MONTH) < mDateBirth.get(GregorianCalendar.DAY_OF_MONTH)))
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
	
	public void setUsername(String newUsername)
	{
		mUesrname = newUsername;
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
}
