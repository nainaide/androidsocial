package main.main;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;

public class ActivityConnect extends Activity
{
	private ProgressDialog mProgDialog = null;
	
	private ApplicationSocialNetwork application = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.connect);

    	application = (ApplicationSocialNetwork)getApplication();

        // ----- IS CODE CORRECT - BEGIN ---------------------
//        ConnectivityManager connectivityManager = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
//		NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
//		
//		if (netInfo != null &&
//			netInfo.getState() == NetworkInfo.State.CONNECTED &&
//			netInfo.isAvailable())
//		{
//	        if (netInfo.getTypeName().equalsIgnoreCase("WIFI") == false)
//	        {
//	        	// TODO : Something
//	             // WIFI is not enabled on this device
//	        }
//		}
        // ----- IS CODE CORRECT - END -----------------------
        
//        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        
        application.disableWifi();
//        application.enableWifi();
        
//    	if (mWifiManager.isWifiEnabled() == false)
//        {
//        	// TODO : Instead of Toasting that Wifi will be enabled, show a dialog and ask
//        	//        the user whether he would to us to enable for him
//    		// TODO : Decide what now - Exit ? Try again later ?
//        	application.showToast("Wifi is disabled. Enabling Wifi...");
//        	
//        	// TODO : Check if setWifiEnabled blocks or not
//        	if (mWifiManager.setWifiEnabled(true) == false)
//        	{
//        		// TODO
//        		// Notify the user that WiFi could not be enabled at the moment
//        		
//        	}
//        	
//        	application.showToast("Wifi enabled");
//        }
    	
    	mProgDialog = ProgressDialog.show(this, "", "Searching for an existing network. Please wait...", true);
                
        
        
		// Search for a network
		Runnable runnableSearchForNetwork = new Runnable(){
			@Override
			public void run()
			{
				boolean isClientEnabled = application.enableAdhocClient();
				
				mProgDialog.dismiss();
				
				if (isClientEnabled == false)
				{
					application.disableAdhocClient();
					Looper.prepare(); // Apparently it's needed for showing a dialog (next line) in a thread
					application.showToast("No network could be found. Creating a new one...");
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
		@Override
		public void run()
		{
			// Start the application's threads of sending and receiving messages
			application.startService();
			
			// Launching the application, giving it the network
			Intent intent = new Intent(ActivityConnect.this, ActivityUsersList.class);
			startActivity(intent);
		}
	};
}
