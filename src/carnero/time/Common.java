package carnero.time;

import android.content.SharedPreferences;

public class Common {

	// preferences
	public static final String PREFS_NAME = "carnero.time.prefs";
	public static final String PREFS_HRS = "hours";
	public static final String PREFS_MINS = "mins";
	// default values
	public static final int DEFAULT_HRS = 22;
	public static final int DEFAULT_MINS = 00;
	// useful stuff
	public static final String TAG = "carnero.time";
	public static final String SQL_TAUTOLOGY = "1=1";

	public static int getHours(SharedPreferences prefs) {
		if (prefs.contains(PREFS_HRS)) {
			return prefs.getInt(PREFS_HRS, DEFAULT_HRS);
		} else {
			return DEFAULT_HRS;
		}
	}

	public static int getMinutes(SharedPreferences prefs) {
		if (prefs.contains(PREFS_MINS)) {
			return prefs.getInt(PREFS_MINS, DEFAULT_MINS);
		} else {
			return DEFAULT_MINS;
		}
	}

	public static boolean setTime(SharedPreferences prefs, int hours, int minutes) {
		SharedPreferences.Editor edit = prefs.edit();
		edit.putInt(PREFS_HRS, hours);
		edit.putInt(PREFS_MINS, minutes);
		return edit.commit();
	}
}
