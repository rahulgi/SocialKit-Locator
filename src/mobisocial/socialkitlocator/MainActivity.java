package mobisocial.socialkitlocator;

import java.util.HashMap;
import java.util.List;
//import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import mobisocial.socialkitlocator.R;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends FragmentActivity {

	private static final String TAG = "Locator";
	
	private static final String ACTION_CREATE_FEED = "musubi.intent.action.CREATE_FEED";
	private static final int REQUEST_CREATE_FEED = 1;
	
	private TextView uriPresenter;
	private TextView locationPresenter;
	private GoogleMap map;
	
	private Uri feedUri;
	private static int updated = 0;
	private HashMap<Long, Marker> feedMembers = new HashMap<Long, Marker>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		locationPresenter = (TextView) findViewById(R.id.locationPresenter);
		uriPresenter = (TextView) findViewById(R.id.uriPresenter);
		map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
		beginListeningForLocation();
	}
	
	@Override
	public void onResume() {
		IntentFilter iff = new IntentFilter();
		iff.addAction("mobisocial.intent.action.DATA_RECEIVED");
		this.registerReceiver(this.messageReceiver, iff);
		super.onResume();
	}
	
	@Override
	public void onPause() {
		this.unregisterReceiver(this.messageReceiver);
		super.onPause();
	}
	
	private void beginListeningForLocation() {
		// Acquire a reference to the system Location Manager
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		
		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener() {
			@Override
			public void onLocationChanged(Location location) {
				locationPresenter.setText(updated ++ + ": " + location.getLatitude() + ", " + location.getLongitude());
				broadcastLocation(location);
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
			
			feedUri = data.getData();
			Log.d(TAG, "feedUri: " + feedUri.toString());
			
			Musubi musubi = Musubi.getInstance(this);
			
			DbFeed feed = musubi.getFeed(feedUri);
			uriPresenter.setText(feedUri.toString());
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
	
	private void broadcastLocation (Location location) {
		if (feedUri == null)
			return;
		
		Musubi musubi = Musubi.getInstance(this);
		DbFeed feed = musubi.getFeed(feedUri);
		if (feed == null) {
			Log.d(TAG, "feed is null!");
			return;
		}
		
		JSONObject json = new JSONObject();
		try {
			json.put("latitude", location.getLatitude());
			json.put("longitude", location.getLongitude());
		} catch (JSONException e) {
			Log.e(TAG, "json error", e);
			return;
		}
		feed.postObj(new MemObj("socialkit-locator", json));
		Log.d(TAG, "json obj posted: " + json.toString());
	}
	
	private BroadcastReceiver messageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent == null)
				Log.d(TAG, "no intent");
			
			Log.d(TAG, "message received: " + intent);
			
			Uri objUri = (Uri) intent.getParcelableExtra("objUri");
			if (objUri == null) {
				Log.d(TAG, "no object found");
				return;
			}
			Log.d(TAG, "obj uri: " + objUri.toString());
				
			Musubi musubi = Musubi.forIntent(context, intent);
			DbObj obj = musubi.objForUri(objUri);
			
			if (obj == null) {
				Log.d(TAG, "obj is null?");
				return;
			}
			
			if (feedUri == null) {
				feedUri = obj.getContainingFeed().getUri();
				if (feedUri != null) {
					uriPresenter.setText(feedUri.toString());
				}
			}
			
			JSONObject json = obj.getJson();
			if (json == null) {
				Log.d(TAG, "no json attached to obj");
				return;
			}
			Log.d(TAG, "json: " + json);
			
			if (obj.getSender().isOwned()) {
				// do stuff specific to when message is sent by me
			}
			
			double latitude = 0;
			double longitude = 0; 
			try {
				latitude = json.getDouble("latitude");
				longitude = json.getDouble("longitude");
			} catch (JSONException e) {
				e.printStackTrace();
			}	
			if (feedMembers.containsKey(obj.getSenderId()))
				((Marker) feedMembers.get(obj.getSenderId())).setPosition(new LatLng(latitude, longitude));
			else
				feedMembers.put(obj.getSenderId(), map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title(obj.getSender().getName())));
		}
	};
}
