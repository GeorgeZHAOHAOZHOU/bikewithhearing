package com.example.ami;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import com.example.ami.okhttp.OkHttpEngine;
import com.example.ami.okhttp.ResultCallBack;
import com.example.ami.utils.Utils;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.android.gestures.RotateGestureDetector;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMap.OnCameraMoveListener;
import com.mapbox.mapboxsdk.maps.MapboxMap.OnRotateListener;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import okhttp3.Request;
import org.angmarch.views.NiceSpinner;
import org.angmarch.views.OnSpinnerItemSelectedListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements PermissionsListener,
    OnMapReadyCallback, MapboxMap.OnMapClickListener{
  static String FINSIH = "2";
  static String LEFT = "1";
  static String RIGHT = "0";
  //Google api key

  private PermissionsManager permissionsManager;


  // variables for adding location layer
  private MapView mapView;
  private MapboxMap mapboxMap;
  private LocationComponent locationComponent;
  // variables for calculating and drawing a route
  private DirectionsRoute currentRoute;
  private static final String TAG = "DirectionsActivity";
  private NavigationMapRoute navigationMapRoute;

  //手指点到地图
  private boolean isHandPoint = false;



  private double startLat;
  private double startLng;
  private double destLat;
  private double destLng;


  //广播发送蓝牙信号
  private MyBroadcastReceiver receiver;
  public class MyBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.example.ACTION_SOMETHING";
    @Override
    public void onReceive(Context context, Intent intent) {
      int turning  = intent.getIntExtra("turning", 99);

      Log.e("Main turning:", turning + "");

      if (bluetooth.isBluetoothEnabled()){

        switch (turning){
          case 1:
            bluetooth.send(LEFT, true);break;
          case 0:
            bluetooth.send(RIGHT,true);break;
          case 2:
            bluetooth.send(FINSIH,true);break;
            default:
        }
      }

    }
  }

  BluetoothSPP bluetooth;
  boolean isBluetoothConnected = false;
  Button connect;
  EditText inputOrigin;
  EditText inputDesination;
  ProgressBar progressBar;

  Button btnTest;
  boolean isMock = false;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Mapbox.getInstance(this, getString(R.string.access_token));
    setContentView(R.layout.activity_main);

    //初始化mapView
    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);


    inputOrigin = findViewById(R.id.inputOrigin);
    inputDesination = findViewById(R.id.inputDestination);
    progressBar = findViewById(R.id.progressBar);


    btnTest = findViewById(R.id.btnTest);
    //导航按钮 GO
    btnTest.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        if (PermissionsManager.areLocationPermissionsGranted(MainActivity.this)) {
          TestGoogleAPI(savedInstanceState);
        } else {
          permissionsManager = new PermissionsManager(MainActivity.this);
          permissionsManager.requestLocationPermissions(MainActivity.this);
        }
      }
    });





    receiver = new MyBroadcastReceiver();
    this.registerReceiver(receiver, new IntentFilter(MyBroadcastReceiver.ACTION));

    //--------------------------------------------------------------------------bluetooth ok
    bluetooth = new BluetoothSPP(this);
    connect = (Button) findViewById(R.id.connect);
//        on = (Button) findViewById(R.id.on);
//        off = (Button) findViewById(R.id.off);

    if (!bluetooth.isBluetoothAvailable()) {
      Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_SHORT).show();
      finish();
    }

    bluetooth.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {

      public void onDeviceConnected(String name, String address) {
        progressBar.setVisibility(View.GONE);
        connect.setText("Connected to " + name);
        isBluetoothConnected = true;
        connect.setVisibility(View.GONE);
        Utils.createAlert("Connection","Connected to " + name, "OK","",null,MainActivity.this);
      }

      public void onDeviceDisconnected() {
        progressBar.setVisibility(View.GONE);
        connect.setText("Bluetooth connection lost");
        isBluetoothConnected = false;
        connect.setVisibility(View.VISIBLE);
        Utils.createAlert("Connection","Bluetooth connection lost", "OK","",null,MainActivity.this);
      }

      public void onDeviceConnectionFailed() {
        progressBar.setVisibility(View.GONE);
        connect.setText("Unable to connect bluetooth device");
        isBluetoothConnected = false;
        connect.setVisibility(View.VISIBLE);
        Utils.createAlert("Connection","Unable to connect bluetooth device", "OK","",null,MainActivity.this);
      }
    });


    //启动时，尝试连接蓝牙
    if (bluetooth.getServiceState() == BluetoothState.STATE_CONNECTED) {
      bluetooth.disconnect();
    } else {
      progressBar.setVisibility(View.VISIBLE);
      Intent intent = new Intent(getApplicationContext(), DeviceList.class);
      startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
    }
    connect.setVisibility(View.GONE);
    connect.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (bluetooth.getServiceState() == BluetoothState.STATE_CONNECTED) {
          bluetooth.disconnect();
        } else {
          Intent intent = new Intent(getApplicationContext(), DeviceList.class);
          startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }
      }
    });



  }
  public void onStart() {
    super.onStart();
    mapView.onStart();
    if (!bluetooth.isBluetoothEnabled()) {
      bluetooth.enable();
    } else {
      if (!bluetooth.isServiceAvailable()) {
        bluetooth.setupService();
        bluetooth.startService(BluetoothState.DEVICE_OTHER);
      }
    }
  }
  @Override
  protected void onResume() {
    super.onResume();
    mapView.onResume();
  }
  @Override
  protected void onPause() {
    super.onPause();
    mapView.onPause();
  }
  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }


  public void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
    bluetooth.stopService();
    this.unregisterReceiver(receiver);
  }
  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
      if (resultCode == Activity.RESULT_OK)
        bluetooth.connect(data);
    } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
      if (resultCode == Activity.RESULT_OK) {
        bluetooth.setupService();
      } else {
        Toast.makeText(getApplicationContext()
            , "Bluetooth was not enabled."
            , Toast.LENGTH_SHORT).show();
        finish();
      }
    }
  }

  public void TestGoogleAPI(@Nullable Bundle savedInstanceState){
      if ( ! isBluetoothConnected){
        Toast.makeText(getApplicationContext()
            , "Please connect to the bluetooth first!"
            , Toast.LENGTH_SHORT).show();
        return;
      }



      progressBar.setVisibility(View.VISIBLE);


      if( ! inputDesination.getText().toString().isEmpty() & ! inputDesination.getText().toString().contains("lat")){
        MapboxGeocoding mapboxGeocoding = MapboxGeocoding.builder()
            .accessToken(Mapbox.getAccessToken())
            .query(inputDesination.getText().toString())
            .build();


        mapboxGeocoding.enqueueCall(new Callback<GeocodingResponse>() {
          @Override
          public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {

            List<CarmenFeature> results = response.body().features();

            if (results.size() > 0) {

              try{
                // Log the first results Point.
                Point firstResultPoint = results.get(0).center();
                Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
                    locationComponent.getLastKnownLocation().getLatitude());
                String latStart = originPoint.latitude() + "";
                String lngStart = originPoint.longitude() +"";
                String latEnd = firstResultPoint.latitude() + "";
                String lngEnd = firstResultPoint.longitude() + "";


                Intent myIntent = new Intent(MainActivity.this, MapActivity.class);
                myIntent.putExtra("latStart", latStart); //Optional parameters
                myIntent.putExtra("lngStart", lngStart); //Optional parameters
                myIntent.putExtra("latEnd", latEnd); //Optional parameters
                myIntent.putExtra("lngEnd", lngEnd); //Optional parameters
                myIntent.putExtra("isMock", false);
                MainActivity.this.startActivity(myIntent);
                progressBar.setVisibility(View.GONE);
              }catch (Exception ex){
                Utils.displayToastLong(ex.getMessage(),MainActivity.this);
                progressBar.setVisibility(View.GONE);
              }



            } else {
              Utils.displayToastLong("onResponse: No result found",MainActivity.this);
              progressBar.setVisibility(View.GONE);
              // No result for your request were found.

            }
          }

          @Override
          public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
            Utils.displayToastLong("Failure",MainActivity.this);
            progressBar.setVisibility(View.GONE);
          }
        });
      }
      else{
        Log.e("TAG>>>", startLat+"");
        Log.e("TAG>>>", startLng+"");
        Log.e("TAG>>>", destLat+"");
        Log.e("TAG>>>", destLng+"");


        Intent myIntent = new Intent(MainActivity.this, MapActivity.class);
        myIntent.putExtra("latStart", startLat+""); //Optional parameters
        myIntent.putExtra("lngStart", startLng+""); //Optional parameters
        myIntent.putExtra("latEnd", destLat+""); //Optional parameters
        myIntent.putExtra("lngEnd", destLng+""); //Optional parameters
        myIntent.putExtra("isMock", false);
        MainActivity.this.startActivity(myIntent);
        progressBar.setVisibility(View.GONE);
      }


  }


  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public void onExplanationNeeded(List<String> permissionsToExplain) {
    Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
  }

  @Override
  public void onPermissionResult(boolean granted) {
    if (granted) {

    } else {
      Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
      finish();
    }
  }



//-------------------------------------------------------------------MapView


  @Override
  public void onMapReady(@NonNull final MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        enableLocationComponent(style);

        addDestinationIconSymbolLayer(style);

        mapboxMap.addOnMapClickListener(MainActivity.this);

      }
    });
  }



  //设定起步点
  @SuppressWarnings( {"MissingPermission"})
  private void enableLocationComponent(@NonNull Style loadedMapStyle) {
// Check if permissions are enabled and if not request
    if (PermissionsManager.areLocationPermissionsGranted(this)) {
// Activate the MapboxMap LocationComponent to show user location
// Adding in LocationComponentOptions is also an optional parameter
      locationComponent = mapboxMap.getLocationComponent();
      locationComponent.activateLocationComponent(this, loadedMapStyle);
      locationComponent.setLocationComponentEnabled(true);
// Set the component's camera mode
      locationComponent.setCameraMode(CameraMode.TRACKING);
      //起始点坐标
      Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
          locationComponent.getLastKnownLocation().getLatitude());
      inputOrigin.setText("My Location");
      startLng = originPoint.longitude();
      startLat= originPoint.latitude();

    } else {
      permissionsManager = new PermissionsManager(this);
      permissionsManager.requestLocationPermissions(this);
    }
  }


  //添加地点标志
  private void addDestinationIconSymbolLayer(@NonNull Style loadedMapStyle) {
    loadedMapStyle.addImage("destination-icon-id",
        BitmapFactory.decodeResource(this.getResources(), R.drawable.mapbox_marker_icon_default));
    GeoJsonSource geoJsonSource = new GeoJsonSource("destination-source-id");
    loadedMapStyle.addSource(geoJsonSource);
    SymbolLayer destinationSymbolLayer = new SymbolLayer("destination-symbol-layer-id", "destination-source-id");
    destinationSymbolLayer.withProperties(
        iconImage("destination-icon-id"),
        iconAllowOverlap(true),
        iconIgnorePlacement(true)
    );
    loadedMapStyle.addLayer(destinationSymbolLayer);
  }



  //在地图上点击一个位置，作为结束点
  @SuppressWarnings( {"MissingPermission"})
  @Override
  public boolean onMapClick(@NonNull LatLng point) {

    Point destinationPoint = Point.fromLngLat(point.getLongitude(), point.getLatitude());
    Point originPoint = Point.fromLngLat(locationComponent.getLastKnownLocation().getLongitude(),
        locationComponent.getLastKnownLocation().getLatitude());

    //点击的结束点坐标
    inputDesination.setText("lng:" + destinationPoint.longitude()+ "\n" +"lat:" +destinationPoint.latitude());

    destLng = destinationPoint.longitude();
    destLat = destinationPoint.latitude();

    GeoJsonSource source = mapboxMap.getStyle().getSourceAs("destination-source-id");
    if (source != null) {
      source.setGeoJson(Feature.fromGeometry(destinationPoint));
    }
    getRoute(originPoint, destinationPoint);
    isHandPoint = true;

    return true;
  }



  //获取起始点到结束点的路径
  private void getRoute(Point origin, Point destination) {
    NavigationRoute.builder(this)
        .accessToken(Mapbox.getAccessToken())
        .origin(origin)
        .profile("cycling")
        .destination(destination)
        .build()
        .getRoute(new Callback<DirectionsResponse>() {
          @Override
          public void onResponse(
              Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
// You can get the generic HTTP info about the response
            Log.d(TAG, "Response code: " + response.code());
            if (response.body() == null) {
              Log.e(TAG, "No routes found, make sure you set the right user and access token.");
              return;
            } else if (response.body().routes().size() < 1) {
              Log.e(TAG, "No routes found");
              return;
            }

            currentRoute = response.body().routes().get(0);

// Draw the route on the map
            if (navigationMapRoute != null) {
              navigationMapRoute.removeRoute();
            } else {
              navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
            }
            navigationMapRoute.addRoute(currentRoute);
          }

          @Override
          public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
            Log.e(TAG, "Error: " + throwable.getMessage());
          }
        });
  }
}





