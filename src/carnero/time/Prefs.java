package carnero.time;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;

public class Prefs extends Activity {

	private SharedPreferences mPrefs;
	private EditText mHrsEdit;
	private EditText mMinsEdit;

	@Override
	public void onCreate(Bundle status) {
		super.onCreate(status);

		mPrefs = getSharedPreferences(Common.PREFS_NAME, MODE_PRIVATE);

		getActionBar().setDisplayHomeAsUpEnabled(true);

		setContentView(R.layout.prefs);
		mHrsEdit = (EditText) findViewById(R.id.end_hrs);
		mMinsEdit = (EditText) findViewById(R.id.end_mins);

		String hrs = String.valueOf(Common.getHours(mPrefs));
		if (hrs.length() < 2) {
			hrs = "0" + hrs;
		}
		String mins = String.valueOf(Common.getMinutes(mPrefs));
		if (mins.length() < 2) {
			mins = "0" + mins;
		}

		mHrsEdit.setText(hrs);
		mMinsEdit.setText(mins);
	}

	@Override
	public void onPause() {
		int hrs = Integer.parseInt(mHrsEdit.getText().toString());
		int mins = Integer.parseInt(mMinsEdit.getText().toString());

		if (hrs < 0) {
			hrs = 0;
		} else if (hrs > 23) {
			hrs = 23;
		}

		if (mins < 0) {
			mins = 0;
		} else if (mins > 59) {
			mins = 59;
		}

		Common.setTime(mPrefs, hrs, mins);

		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();

				return true;
		}

		return super.onOptionsItemSelected(item);
	}
}