package com.example.ami;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import java.util.List;
import retrofit2.Call;
import retrofit2.Response;


public class MapboxFragment extends Fragment implements OnNavigationReadyCallback,
    NavigationListener,
    ProgressChangeListener  {

  OnTurningListener callback;



  public void setOnTurningListener(OnTurningListener callback){
    this.callback = callback;
  }


  public interface  OnTurningListener {
     void OnTurning(int turning);
  }




  private static final double ORIGIN_LONGITUDE = 7.662242;
  private static final double ORIGIN_LATITUDE = 45.062442;
  private static final double DESTINATION_LONGITUDE = 7.678462;
  private static final double DESTINATION_LATITUDE = 45.062247;
  private NavigationView navigationView;
  private DirectionsRoute directionsRoute;

  double latStart;
  double lngStart;
  double latEnd;
  double lngEnd;
  boolean isMock;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    Mapbox.getInstance(getContext(), getString(R.string.access_token));
    try
    {
      latStart = getArguments().getDouble("latStart", ORIGIN_LATITUDE);
      lngStart = getArguments().getDouble("lngStart",ORIGIN_LONGITUDE);
      latEnd = getArguments().getDouble("latEnd",DESTINATION_LONGITUDE);
      lngEnd = getArguments().getDouble("lngEnd",DESTINATION_LATITUDE);
      isMock = getArguments().getBoolean("isMock",false);

      Log.e("latStart>>",latStart+"");
      Log.e("lngStart>>",lngStart+"");
      Log.e("latEnd>>",latEnd+"");
      Log.e("lngEnd>>",lngEnd+"");
      Log.e("isMock>>",isMock+"");
    }catch(Exception e) {
      e.printStackTrace();
    }




    return inflater.inflate(R.layout.fragment_mapbox, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    updateNightMode();
    navigationView = view.findViewById(R.id.navigation_view_fragment);
    navigationView.onCreate(savedInstanceState);
    navigationView.initialize(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    navigationView.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    navigationView.onResume();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    navigationView.onSaveInstanceState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState != null) {
      navigationView.onRestoreInstanceState(savedInstanceState);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    navigationView.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
    navigationView.onStop();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    navigationView.onLowMemory();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    navigationView.onDestroy();
  }

  @Override
  public void onNavigationReady(boolean isRunning) {


    Point origin = Point.fromLngLat(lngStart, latStart);
    Point destination = Point.fromLngLat(lngEnd, latEnd);
    fetchRoute(origin, destination);
  }

  @Override
  public void onCancelNavigation() {
    navigationView.stopNavigation();
    stopNavigation();


  }

  @Override
  public void onNavigationFinished() {
    // no-op

  }

  @Override
  public void onNavigationRunning() {
    // no-op
  }


  String modifier = "";
  double distance = 0 ;
  int lastTurning = -1;
  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    //Log.d(">leg remaining distance",String.format("%f", routeProgress.currentLegProgress().distanceRemaining()));
    // Log.d(">>>>>>>>>current banner", routeProgress.currentLegProgress().currentStep().bannerInstructions().get(0).primary().modifier());
    // Log.d(">currentStepProgress", routeProgress.currentLegProgress().currentStepProgress().toString());
    Log.d(">step distanceRemaining", String.format("%f",
        routeProgress.distanceRemaining()));

   if(routeProgress.distanceRemaining() <= 3 ) // 2.788354 测试的时候，到这就不走了，可能是有误差
    {
      if(lastTurning == 2)
        return;
      callback.OnTurning(2);
      lastTurning = 2;
      navigationView.stopNavigation();
      stopNavigation();
      return;
    }

    distance =  routeProgress.currentLegProgress().currentStepProgress().distanceRemaining();
    if ( distance< 15 && routeProgress.distanceRemaining()>0) {
      if (routeProgress.distanceRemaining()>0
          && routeProgress.currentLegProgress().upComingStep() != null
      && routeProgress.currentLegProgress().upComingStep().maneuver()!=null
      && routeProgress.currentLegProgress().upComingStep().maneuver().modifier()!=null) {

        Log.d(">>>>step turning:",
            routeProgress.currentLegProgress().upComingStep().maneuver().modifier());
        modifier = routeProgress.currentLegProgress().upComingStep().maneuver().modifier();
        if(modifier.contains("left") && lastTurning != 1){
          callback.OnTurning(1);
          lastTurning = 1;
        }
        else if(modifier.contains("right") && lastTurning != 0){
          callback.OnTurning(0);
          lastTurning = 0;
        }

      }
    }
    boolean isInTunnel = routeProgress.inTunnel();
    boolean wasInTunnel = wasInTunnel();
    if (isInTunnel) {
      if (!wasInTunnel) {
        updateWasInTunnel(true);
        updateCurrentNightMode(AppCompatDelegate.MODE_NIGHT_YES);
      }
    } else {
      if (wasInTunnel) {
        updateWasInTunnel(false);
        updateCurrentNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
      }
    }
  }

  private void updateNightMode() {
    if (wasNavigationStopped()) {
      updateWasNavigationStopped(false);
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
      getActivity().recreate();
    }
  }

  private void fetchRoute(Point origin, Point destination) {
    NavigationRoute.builder(getContext())
        .accessToken(Mapbox.getAccessToken())
        .origin(origin)
        .destination(destination)
        .profile("cycling")
        .build()
        .getRoute(new SimplifiedCallback() {
          @Override
          public void onResponse(Call<DirectionsResponse> call,
              Response<DirectionsResponse> response) {
            directionsRoute = response.body().routes().get(0);
            startNavigation();
          }
        });
  }

  private void startNavigation() {
    if (directionsRoute == null) {
      return;
    }
    NavigationViewOptions options = NavigationViewOptions.builder()
        .directionsRoute(directionsRoute)
        .shouldSimulateRoute(isMock)
        .navigationListener(MapboxFragment.this)
        .progressChangeListener(this)
        .build();
    navigationView.startNavigation(options);
  }

  private void stopNavigation() {
    FragmentActivity activity = getActivity();
    if (activity != null && activity instanceof MapActivity) {
      MapActivity fragmentNavigationActivity = (MapActivity) activity;
      fragmentNavigationActivity.showPlaceholderFragment();

//      fragmentNavigationActivity.showNavigationFab();
      updateWasNavigationStopped(true);
      updateWasInTunnel(false);
    }
  }

  private boolean wasInTunnel() {
    Context context = getActivity();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    return preferences.getBoolean(context.getString(R.string.was_in_tunnel), false);
  }

  private void updateWasInTunnel(boolean wasInTunnel) {
    Context context = getActivity();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(context.getString(R.string.was_in_tunnel), wasInTunnel);
    editor.apply();
  }

  private void updateCurrentNightMode(int nightMode) {
    AppCompatDelegate.setDefaultNightMode(nightMode);
    getActivity().recreate();
  }

  private boolean wasNavigationStopped() {
    Context context = getActivity();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    return preferences.getBoolean(getString(R.string.was_navigation_stopped), false);
  }

  public void updateWasNavigationStopped(boolean wasNavigationStopped) {
    Context context = getActivity();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(getString(R.string.was_navigation_stopped), wasNavigationStopped);
    editor.apply();
  }


}
