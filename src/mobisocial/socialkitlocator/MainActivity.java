package mobisocial.socialkitlocator;

import java.util.List;
//import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.example.socialkit_locator.R;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String TAG = "Locator";
	
	private static final String ACTION_CREATE_FEED = "musubi.intent.action.CREATE_FEED";
	private static final int REQUEST_CREATE_FEED = 1;
	
	private TextView locationPresenter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		locationPresenter = (TextView) findViewById(R.id.locationPresenter);
		beginListeningForLocation();
	}
	
	private static int updated = 0;
	
	private void beginListeningForLocation() {
		// Acquire a reference to the system Location Manager
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		
		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				locationPresenter.setText(updated ++ + ": " + location.getLatitude() + ", " + location.getLongitude());
			}
			
			@Override
			public void onProviderDisabled(String provider) {
				
			}

			@Override
			public void onProviderEnabled(String provider) {
				
			}

			@Override
			public void onStatusChanged(String provider, int status, Bundle extras) {
				
			}
		};
		
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.create_feed:
			if (!Musubi.isMusubiInstalled(this)) {
				Log.d(TAG, "Musubi is not installed.");
				return super.onOptionsItemSelected(item);
			}
			Intent intent = new Intent(ACTION_CREATE_FEED);
			startActivityForResult(intent, REQUEST_CREATE_FEED);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CREATE_FEED && resultCode == RESULT_OK) {
			if (data == null || data.getData() == null) {
				return;
			}
			
			Uri feedUri = data.getData();
			Log.d(TAG, "feedUri: " + feedUri.toString());
			
			Musubi musubi = Musubi.getInstance(this);
			
			DbFeed feed = musubi.getFeed(feedUri);
			if (feed == null) {
				Log.d(TAG, "feed is null?!?");
				return;
			}
			
			List<DbIdentity> members = feed.getMembers();
			for (DbIdentity member: members) {
				Log.d(TAG, "member: " + member.getName() + ", " + member.getId());
			}
			
			JSONObject json = new JSONObject();
			try {
				json.put("can't see this", "invisible");
				json.put(Obj.FIELD_HTML, "hi");
			} catch (JSONException e) {
				Log.e(TAG, "json error", e);
				return;
			}
			feed.postObj(new MemObj("socialkit-locator", json));
			Log.d(TAG, "json obj posted: " + json.toString());
		}
	}
}
