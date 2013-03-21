package carnero.time;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.util.Log;
import android.util.Pair;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.util.*;

public class Extension extends DashClockExtension {

	private SharedPreferences mPrefs;

	protected void onUpdateData(int reason) {
		if (mPrefs == null) {
			mPrefs = getSharedPreferences(Common.PREFS_NAME, MODE_PRIVATE);
		}

		// time to the end of the day
		Calendar now = Calendar.getInstance(Locale.getDefault());
		int endHrs = Common.getHours(mPrefs);
		int endMins = Common.getMinutes(mPrefs);

		int hrs = endHrs - now.get(Calendar.HOUR_OF_DAY);
		int mins = (60 + endMins) - now.get(Calendar.MINUTE);
		if (mins > 0 && mins < 60)  {
			hrs --;
		}
		if (hrs < 0) {
			hrs = 24 + hrs;
		}

		String hrsS = String.valueOf(hrs);
		if (hrs < 10) {
			hrsS = "0" + hrsS;
		}
		String minsS = String.valueOf(mins);
		if (mins < 10) {
			minsS = "0" + minsS;
		}

		String time = hrsS + ":" + minsS;

		// planned time
		float plannedTime = 0f; // used time, millis
		long current = getCurrentTimestamp(); // current time
		long end = current + (hrs * 60 * 60 * 1000) + (mins * 60 * 1000); // end of the day
		long eventStart;
		long eventEnd;
		long eventLength;

		final Cursor cursor = tryOpenEventsCursor(hrs);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				eventStart = cursor.getLong(EventsQuery.BEGIN);
				if (eventStart < current) {
					eventStart = current;
				}

				eventEnd = cursor.getLong(EventsQuery.END);
				if (eventEnd > end) {
					eventEnd = end;
				}

				eventLength = eventEnd - eventStart;

				plannedTime += eventLength;
			}

			cursor.close();
		}

		int toEnd = (hrs * 60) + mins; // minutes
		int planned = (int) (plannedTime / (60 * 1000)); // minutes
		int remaining;
		if (planned > toEnd) {
			remaining = 0;
		} else {
			remaining = toEnd - planned;
		}

		int hrsP = (int) Math.floor(remaining / 60);
		int minsP = remaining - (hrsP * 60);

		String hrsPS = String.valueOf(hrsP);
		if (hrsP < 10) {
			hrsPS = "0" + hrsPS;
		}
		String minsPS = String.valueOf(minsP);
		if (minsP < 10) {
			minsPS = "0" + minsPS;
		}

		String remainingS = hrsPS + ":" + minsPS;
		String expanded;
		if (remaining >= (toEnd - 1)) {
			expanded = getString(R.string.content_expanded_free);
		} else {
			expanded = getString(R.string.content_expanded_planed, remainingS);
		}

		// display
		publishUpdate(new ExtensionData()
				.visible(true)
				.icon(R.drawable.ic_time)
				.status(time)
				.expandedTitle(time)
				.expandedBody(expanded)
				.clickIntent(null)
		);
	}

	/*
	 * Following code is mostly copy&paste from DashClock
	 * source: https://code.google.com/p/dashclock/source/browse/main/src/com/google/android/apps/dashclock/calendar/CalendarExtension.java
	 * author: Roman Nurik
	 */
	private Cursor tryOpenEventsCursor(int hours) {
		String calendarSelection = generateCalendarSelection();
		Set<String> calendarSet = getSelectedCalendars();
		String[] calendarsSelectionArgs = calendarSet.toArray(new String[calendarSet.size()]);

		StringBuilder where = new StringBuilder();
		// visible events
		where.append(CalendarContract.Instances.VISIBLE).append(" != 0");
		where.append(" and (");
		// i'm busy or tentative
		where.append(CalendarContract.Instances.AVAILABILITY).append(" == ").append(CalendarContract.Instances.AVAILABILITY_BUSY);
		where.append(" or ");
		where.append(CalendarContract.Instances.AVAILABILITY).append(" == ").append(CalendarContract.Instances.AVAILABILITY_TENTATIVE);
		where.append(") and ");
		// not declined events
		where.append(CalendarContract.Instances.SELF_ATTENDEE_STATUS).append(" != ").append(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED);
		where.append(" and ");
		where.append("ifnull(").append(CalendarContract.Instances.STATUS).append(", 0) != ").append(CalendarContract.Instances.STATUS_CANCELED);
		where.append(" and (");
		// from visible calendars
		where.append(calendarSelection);
		where.append(")");

		long now = getCurrentTimestamp();

		try {
			return getContentResolver().query(
					CalendarContract.Instances.CONTENT_URI.buildUpon()
							.appendPath(Long.toString(now - (10 * 60 * 1000))) // events from 10 minutes ago
							.appendPath(Long.toString(now + ((hours + 1) * 60 * 60 * 1000))) // events to end of the day + 1 hour
							.build(),
					EventsQuery.PROJECTION,
					where.toString(),
					calendarsSelectionArgs,
					CalendarContract.Instances.BEGIN
			);
		} catch (Exception e) {
			Log.e(Common.TAG, "Failed to query calendar API");
			return null;
		}
	}

	static List<Pair<String, Boolean>> getAllCalendars(Context context) {
		// Only return calendars that are marked as synced to device.
		// (This is different from the display flag)
		List<Pair<String, Boolean>> calendars = new ArrayList<Pair<String, Boolean>>();

		try {
			Cursor cursor = context.getContentResolver().query(
					CalendarContract.Calendars.CONTENT_URI,
					CalendarsQuery.PROJECTION,
					CalendarContract.Calendars.SYNC_EVENTS + "=1",
					null,
					null
			);

			if (cursor != null) {
				while (cursor.moveToNext()) {
					calendars.add(new Pair<String, Boolean>(
							cursor.getString(CalendarsQuery.ID),
							cursor.getInt(CalendarsQuery.VISIBLE) == 1)
					);
				}

				cursor.close();
			}
		} catch (SecurityException e) {
			return null;
		}

		return calendars;
	}

	private Set<String> getSelectedCalendars() {
		final List<Pair<String, Boolean>> allCalendars = getAllCalendars(this);
		final Set<String> selectedCalendars = new HashSet<String>();

		for (Pair<String, Boolean> pair : allCalendars) {
			if (pair.second) {
				selectedCalendars.add(pair.first);
			}
		}

		return selectedCalendars;
	}

	private String generateCalendarSelection() {
		Set<String> calendars = getSelectedCalendars();
		int count = calendars.size();

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < count; i++) {
			if (i != 0) {
				sb.append(" or ");
			}

			sb.append(CalendarContract.Events.CALENDAR_ID);
			sb.append(" = ?");
		}

		if (sb.length() == 0) {
			sb.append(Common.SQL_TAUTOLOGY); // constant expression to prevent returning NULL
		}

		return sb.toString();
	}

	private static long getCurrentTimestamp() {
		return Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
	}

	private interface EventsQuery {
		String[] PROJECTION = {
				CalendarContract.Instances.EVENT_ID,
				CalendarContract.Instances.BEGIN,
				CalendarContract.Instances.END,
		};

		// int EVENT_ID = 0;
		int BEGIN = 1;
		int END = 2;
	}

	private interface CalendarsQuery {
		String[] PROJECTION = {
				CalendarContract.Calendars._ID,
				CalendarContract.Calendars.VISIBLE,
		};

		int ID = 0;
		int VISIBLE = 1;
	}
}
