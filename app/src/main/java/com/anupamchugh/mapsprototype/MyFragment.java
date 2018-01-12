package com.anupamchugh.mapsprototype;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.anupamchugh.mylibrary.AfterPermissionGranted;
import com.anupamchugh.mylibrary.AppSettingsDialog;
import com.anupamchugh.mylibrary.EasyPermissions;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.warkiz.widget.IndicatorSeekBar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/**
 * Created by anupamchugh on 10/01/18.
 */
public class MyFragment extends Fragment implements EasyPermissions.PermissionCallbacks, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener, GoogleMap.OnMyLocationChangeListener {

    String title = "";
    boolean notDraggable = false;
    boolean showPlaces = false;
    TextView txtCurrentLocation;
    Location mOldLocation;
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private long UPDATE_INTERVAL = 60000;  /* 60 secs */
    private long FASTEST_INTERVAL = 45000; /* 45 secs */
    Location mLocation;
    FloatingActionButton fabLocation, fabFilter;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final int REQUEST_PERMISSIONS = 20;
    Activity mActivity;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private RelativeLayout clickRl;
    CardView cardView;
    Location mLastLocation;
    LatLng center;
    boolean m_bFirstMoved = false;
    public String currentUser;
    DatabaseReference mFirebaseDatabase;
    GeoFire geoFire;
    Button btnNearby;
    GeoLocation myGeoLocation;
    HashMap<String, GeoLocation> geoLocationHashMap;
    ImageView centerMarker;
    double mRadius = 0.0f;

    public MyFragment() {
        // Required empty public constructor
    }

    public static MyFragment newInstance(String title, boolean notDraggable, boolean showPlaces, String uid) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        bundle.putBoolean("notDraggable", notDraggable);
        bundle.putBoolean("showPlaces", showPlaces);
        bundle.putString("currentUser", uid);
        MyFragment fragment = new MyFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    private void readBundle(Bundle bundle) {
        if (bundle != null) {
            title = bundle.getString("title");
            notDraggable = bundle.getBoolean("notDraggable");
            showPlaces = bundle.getBoolean("showPlaces");
            currentUser = bundle.getString("currentUser");

        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calls, container, false);
        readBundle(getArguments());

        mapFragment = ((SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map));
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;

                if (notDraggable)
                    mMap.getUiSettings().setScrollGesturesEnabled(false);

                locationPermissions();
            }
        });
        Log.d("API123", "google Maps");


        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mFirebaseDatabase = FirebaseDatabase.getInstance().getReference("user_locations");
        geoFire = new GeoFire(mFirebaseDatabase);


        connectClient();
        btnNearby = view.findViewById(R.id.btnNearby);
        btnNearby.setOnClickListener(this);
        clickRl = view.findViewById(R.id.click_rl);
        clickRl.setOnClickListener(this);
        cardView = view.findViewById(R.id.cardView);
        centerMarker = view.findViewById(R.id.centerMarker);
        if (!showPlaces)
            cardView.setVisibility(View.GONE);

        fabLocation = view.findViewById(R.id.fabLocation);
        txtCurrentLocation = view.findViewById(R.id.txtCurrentLocation);

        fabLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mLocation != null) {

                    mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                        @Override
                        public void onCameraIdle() {
                            currentLocationMarker();
                        }
                    });

                    LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16);
                    mMap.animateCamera(cameraUpdate);
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "Unable to fetch your location. Please try again", Toast.LENGTH_SHORT).show();
                }
            }
        });

        fabFilter = view.findViewById(R.id.fabFilter);
        fabFilter.setOnClickListener(this);

    }

    protected void connectClient() {

        if (mGoogleApiClient == null) {

            mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build();
            mGoogleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            builder.setAlwaysShow(true); //this is the key ingredient

            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    final LocationSettingsStates state = result.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All mOldLocation settings are satisfied. The client can initialize mOldLocation
                            // requests here.
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the user
                            // a dialog.
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the string_result in onActivityResult().
                                status.startResolutionForResult(
                                        mActivity, 1000);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            });
        }


        if (checkPlayServices() && mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(mActivity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(mActivity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                mActivity.finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Decide what to do based on the original request code
        switch (requestCode) {

            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                switch (resultCode) {
                    case RESULT_OK:
                        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected())
                            mGoogleApiClient.connect();
                        break;


                }

            case AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE:
                locationPermissions();
                break;

            case PLACE_AUTOCOMPLETE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    Place place = PlaceAutocomplete.getPlace(mActivity, data);
                    Log.d("API123", "Place Address: " + place.getAddress() + "Name :  " + place.getName() + " " + place.getLatLng() + " place id " + place.getId());
                    String place_name = "";
                    if (!place.getAddress().toString().contains(place.getName())) {
                        place_name = place.getName().toString();
                        txtCurrentLocation.setText(place.getName() + ", " + place.getAddress());
                    } else {
                        place_name = "";
                    }

                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 16);
                    mMap.animateCamera(cameraUpdate);


                } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                    Toast.makeText(mActivity, "Error in retrieving place info", Toast.LENGTH_SHORT).show();

                }
                break;
        }


    }

    @AfterPermissionGranted(REQUEST_PERMISSIONS)
    public void locationPermissions() {
        String[] perms = {android.Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions(mActivity, perms)) {
            // Have permissions, do the thing!
            loadMap(mMap);
        } else {

            if (!EasyPermissions.somePermissionPermanentlyDenied(this, Arrays.asList(perms))) {
                showDialogForPermanentDenied();
            } else {
                EasyPermissions.requestPermissions(this, "Permission Denied",
                        REQUEST_PERMISSIONS, perms);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @Override
    public void onPermissionsGranted(int i, List<String> list) {

    }

    @Override
    public void onPermissionsDenied(int i, final List<String> list) {

        if (EasyPermissions.somePermissionPermanentlyDenied(this, list)) {
            showDialogForPermanentDenied();
        } else {

            final String desc = "Looks like the mOldLocation permission has been denied. Please allow access to it in order to use the feature";


            new MaterialDialog.Builder(mActivity)
                    .title("PERMISSIONS DENIED")
                    .content("Are you sure you want to exit the application?")
                    .positiveText("RETRY")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            EasyPermissions.requestPermissions(mActivity, desc,
                                    REQUEST_PERMISSIONS, list.toArray(new String[list.size()]));
                        }
                    })
                    .negativeText("CANCEL")
                    .show();

        }

    }

    public void showDialogForPermanentDenied() {

        String permissions = "PERMISSIONS REVOKED";
        String desc = "Looks like the mOldLocation permission has been permanently denied. Please enable the permission from the settings app in order to use the feature";


        new MaterialDialog.Builder(mActivity)
                .title("PERMISSIONS DENIED")
                .content(permissions)
                .positiveText("SETTINGS")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Intent intent = new Intent();
                        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", mActivity.getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE);
                    }
                })
                .negativeText("CANCEL")
                .show();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            mActivity = (Activity) context;
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mActivity != null)
            mActivity = null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLocation != null) {
            LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 16);
            mMap.animateCamera(cameraUpdate);

        }

    }

    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    protected void loadMap(GoogleMap googleMap) {
        mMap = googleMap;


        mMap.getUiSettings().setMyLocationButtonEnabled(false);


        if (mMap != null) {
            if (ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(mActivity.getApplicationContext(), "Enable Permissions", Toast.LENGTH_LONG).show();
            }

            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                if (mLocation != null) {
                    LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                }
            }

            mMap.setMyLocationEnabled(true);
            View locationButton = mapFragment.getView().findViewById(Integer.parseInt("2"));
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            rlp.setMargins(0, 0, 30, 30);
            mMap.setPadding(0, 200, 0, 50);


            if (!mGoogleApiClient.isConnected()) {
                mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this).build();

                connectClient();

            }


            mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                    currentLocationMarker();
                }
            });

        } else {
            Toast.makeText(mActivity, "Error - Map was null!!", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location1) {

        if (mOldLocation == null)
            mOldLocation = location1;

        else if (location1.distanceTo(mOldLocation) > 10) {

            LatLng latLng = new LatLng(location1.getLatitude(), location1.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        }

        mOldLocation = location1;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.click_rl:
                try {

                    AutocompleteFilter autocompleteFilter = new AutocompleteFilter.Builder()
                            .setTypeFilter(Place.TYPE_COUNTRY)
                            .setCountry("IND")
                            .build();

                    Intent intent =
                            new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                                    .setFilter(autocompleteFilter)
                                    .build(mActivity);
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.btnNearby:
                getNearbyFriends();
                break;

            case R.id.fabFilter:
                openSeekBarDialog(view);
                break;
        }
    }

    @Override
    public void onMyLocationChange(Location location) {
        Log.d("API123", "OnMyLocationChanged");
        mLastLocation = location;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 16));
        if (!m_bFirstMoved) {
            m_bFirstMoved = true;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 16));
        }
    }


    private void currentLocationMarker() {


        center = mMap.getCameraPosition().target;
        getMyLocationAddress(center.latitude, center.longitude);

    }

    public void getMyLocationAddress(double latitude, double longitude) {

        Geocoder geocoder = new Geocoder(mActivity, Locale.ENGLISH);

        Log.d("API123", "getMyLocationAddress");
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses.size() > 0) {
                Address fetchedAddress = addresses.get(0);
                StringBuilder strAddress = new StringBuilder();
                for (int i = 0; i < fetchedAddress.getMaxAddressLineIndex(); i++) {
                    strAddress.append(fetchedAddress.getAddressLine(i)).append(" ");
                }

                /*DatabaseReference mFirebaseDatabase = FirebaseDatabase.getInstance().getReference("users");
                DataModel model = new DataModel(latitude, longitude);
                mFirebaseDatabase.child(currentUser).setValue(model);*/
                myGeoLocation = new GeoLocation(latitude, longitude);
                geoFire.setLocation(currentUser, myGeoLocation);

                bestAddress(fetchedAddress);

            } else {
                txtCurrentLocation.setText("Locating...");
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(mActivity.getApplicationContext(), "Could not get address..!", Toast.LENGTH_LONG).show();
        }
    }


    public void bestAddress(Address fetchedAddress) {
        String thoroughFare = fetchedAddress.getThoroughfare();
        String subThoroughFare = fetchedAddress.getSubThoroughfare();
        String locality = fetchedAddress.getLocality();
        String subLocality = fetchedAddress.getSubLocality();
        if (thoroughFare == null && locality == null) {
            txtCurrentLocation.setText("Searching...");
        } else {
            if (thoroughFare != null) {
                if (subThoroughFare != null) {
                    txtCurrentLocation.setText(subThoroughFare + " " + thoroughFare);
                } else
                    txtCurrentLocation.setText(thoroughFare);
            } else {

                if (locality != null) {
                    if (subLocality != null) {
                        txtCurrentLocation.setText(subLocality + " " + locality);
                    } else
                        txtCurrentLocation.setText(locality);
                } else {
                    txtCurrentLocation.setText("Searching...");
                }
            }
        }
    }

    private void getNearbyFriends() {
        Log.d("API123", "Get Nearby Friends");


        if (myGeoLocation != null) {
            geoLocationHashMap = new HashMap<>();
            geoLocationHashMap.put(currentUser, myGeoLocation);

            GeoQuery geoQuery = geoFire.queryAtLocation(myGeoLocation, mRadius);
            geoQuery.setCenter(myGeoLocation);
            Log.d("API123", "currentGeoLocation " + myGeoLocation);
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    Log.d("API123", "onKeyEntered " + location);
                    geoLocationHashMap.put(key, location);
                }

                @Override
                public void onKeyExited(String key) {
                    Log.d("API123", "onKeyExited");
                    geoLocationHashMap.remove(key);
                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {
                    Log.d("API123", "onKeyMoved");
                    geoLocationHashMap.put(key, location);
                }

                @Override
                public void onGeoQueryReady() {

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();

                    if (geoLocationHashMap.size() > 1) {
                        mMap.clear();
                        centerMarker.setVisibility(View.GONE);
                        for (Map.Entry<String, GeoLocation> entry : geoLocationHashMap.entrySet()) {
                            builder.include(new LatLng(entry.getValue().latitude, entry.getValue().longitude));
                            if (entry.getKey().equals(currentUser)) {
                                mMap.addMarker(new MarkerOptions().position(new LatLng(entry.getValue().latitude, entry.getValue().longitude)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                            } else
                                mMap.addMarker(new MarkerOptions().position(new LatLng(entry.getValue().latitude, entry.getValue().longitude)));
                        }

                        LatLngBounds bounds = builder.build();
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 150);
                        mMap.animateCamera(cu);

                    } else {
                        centerMarker.setVisibility(View.VISIBLE);
                        Toast.makeText(mActivity.getApplicationContext(), "Not found", Toast.LENGTH_LONG).show();
                    }

                }

                @Override
                public void onGeoQueryError(DatabaseError error) {
                    Log.d("API123", "onGeoQueryError");
                    Toast.makeText(getActivity().getApplicationContext(), "Unable to get nearby locations.", Toast.LENGTH_LONG).show();

                }
            });
        } else {
            Toast.makeText(getActivity().getApplicationContext(), "Unable to get nearby locations.", Toast.LENGTH_LONG).show();
        }
    }

    private void openSeekBarDialog(View view) {
        final Dialog yourDialog = new Dialog(mActivity);
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.seekbar_dialog, (ViewGroup) view.findViewById(R.id.your_dialog_root_element));
        yourDialog.setContentView(layout);

        Button btnOk = layout.findViewById(R.id.btnOk);
        final IndicatorSeekBar indicatorSeekBar = layout.findViewById(R.id.seekBar);

        indicatorSeekBar.setOnSeekChangeListener(new IndicatorSeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(IndicatorSeekBar seekBar, int progress, float progressFloat, boolean fromUserTouch) {

                mRadius = Double.valueOf(Float.valueOf(progressFloat).toString()).doubleValue();

            }

            @Override
            public void onSectionChanged(IndicatorSeekBar seekBar, int thumbPosOnTick, String textBelowTick, boolean fromUserTouch) {
                //only callback on discrete series SeekBar type.
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar, int thumbPosOnTick) {
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                yourDialog.dismiss();

            }
        });


        yourDialog.show();


    }
}

