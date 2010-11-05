package main.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ActivityFileBrowser extends ListActivity {

	public static final String EXTRA_KEY_FILENAME = "fileName";
	
	public static final String PREFIX_SMALL_PIC_FILE_NAME = "small_";
	
	
//	private enum DISPLAYMODE {
//		ABSOLUTE, RELATIVE;
//	}
//
//	private final DISPLAYMODE displayMode = DISPLAYMODE.ABSOLUTE;
	private List<String> directoryEntries = new ArrayList<String>();
	private File currentDirectory = new File("/");

	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// setContentView() gets called within the next line,
		// so we do not need it here.
		browseToRoot();
	}

	/**
	 * This function browses to the root-directory of the file-system.
	 */
	private void browseToRoot() {
		browseTo(new File("/sdcard/"));
	}

	/**
	 * This function browses up one level according to the field:
	 * currentDirectory
	 */
	private void upOneLevel() {
		if (this.currentDirectory.getParent() != null)
			this.browseTo(this.currentDirectory.getParentFile());
	}

	private void browseTo(final File aDirectory) {
		if (aDirectory.isDirectory()) {
			this.currentDirectory = aDirectory;
			fill(aDirectory.listFiles());
		} else {
			// First copy the selected picture to a smaller file, making the picture not bigger than the size of image view that
			// is shown in ActivityUserDetails
			String filePath = aDirectory.getAbsolutePath();
//			BitmapFactory.Options options = new BitmapFactory.Options();
			Bitmap bitmapOriginal = BitmapFactory.decodeFile(filePath);
//			Bitmap bitmapSmall = BitmapFactory.decodeFile(filePath, options);
			int maxDimensionSmallPic = (int)getResources().getDimension(R.dimen.image_size); 
			int widthOriginal = bitmapOriginal.getWidth();
			int heightOriginal = bitmapOriginal.getHeight();
			int maxDimensionOriginal = Math.max(widthOriginal, heightOriginal);
			int widthSmall = (int)((double) widthOriginal / (double) maxDimensionOriginal * maxDimensionSmallPic);
			int heightSmall = (int)((double) heightOriginal / (double) maxDimensionOriginal * maxDimensionSmallPic);
			Bitmap bitmapSmall = Bitmap.createScaledBitmap(bitmapOriginal, widthSmall, heightSmall, true);
			String fileName = aDirectory.getName();
			String fileSmallName = PREFIX_SMALL_PIC_FILE_NAME + fileName;
			String fileSmallPath = aDirectory.getAbsolutePath().replace(fileName, fileSmallName);
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(fileSmallPath);
				bitmapSmall.compress(CompressFormat.JPEG, 85, fos);
			} catch (FileNotFoundException e) {
			} finally {
				try {
					fos.close();
				} catch (Exception e) {
				}
			}
			
			
			Intent data = new Intent( );
//			data.putExtra(EXTRA_KEY_FILENAME, aDirectory.getAbsolutePath());
			data.putExtra(EXTRA_KEY_FILENAME, fileSmallPath);
			setResult(RESULT_OK, data);
			finish( );	
		}
	}

	private void fill(File[] files) {
		this.directoryEntries.clear();

		// Add the "." and the ".." == 'Up one level'
		try {
			Thread.sleep(10);
		} catch (InterruptedException e1) {
//			e1.printStackTrace();
		}
		this.directoryEntries.add(".");

		if (this.currentDirectory.getParent() != null)
			this.directoryEntries.add("..");

//		switch (this.displayMode) {
//			case ABSOLUTE:
		int lastIndexOfDot;
		
				for (File file : files) {
					lastIndexOfDot = file.getName().lastIndexOf(".");
					String extensionCurrFile = (lastIndexOfDot > -1) ? file.getName().substring(lastIndexOfDot + 1) : ""; 
					if (file.isDirectory() || extensionCurrFile.toLowerCase().equals("jpg") || extensionCurrFile.toLowerCase().equals("png"))
					{
						this.directoryEntries.add(file.getPath());
					}
				}
//				break;
//				
//			case RELATIVE: // On relative Mode, we have to add the current-path to the beginning
//				int currentPathStringLenght = this.currentDirectory.getAbsolutePath().length();
//				for (File file : files) {
//					this.directoryEntries.add(file.getAbsolutePath().substring(currentPathStringLenght));
//				}
//				break;
//		}

		ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this, R.layout.filebrowser, this.directoryEntries);
		this.setListAdapter(directoryList);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		int selectionRowID = (int) this.getSelectedItemId();
		String selectedFileString = this.directoryEntries.get(selectionRowID);
		
		if (selectedFileString.equals(".")) {
			// Refresh
			this.browseTo(this.currentDirectory);
		} else if (selectedFileString.equals("..")) {
			this.upOneLevel();
		} else {
			File clickedFile = null;
//			switch (this.displayMode) {
//				case RELATIVE:
//					clickedFile = new File(this.currentDirectory.getAbsolutePath() + this.directoryEntries.get(selectionRowID));
//					break;
//				case ABSOLUTE:
					clickedFile = new File(this.directoryEntries.get(selectionRowID));
//					break;
//			}
			if (clickedFile != null)
				this.browseTo(clickedFile);
		}
	}
}
