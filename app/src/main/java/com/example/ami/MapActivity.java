package com.example.ami;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

public class MapActivity extends AppCompatActivity implements MapboxFragment.OnTurningListener{




Context context;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);



    context = this;

    initializeNavigationViewFragment(savedInstanceState);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
          getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
        this.finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void initializeNavigationViewFragment(@Nullable Bundle savedInstanceState) {
    FragmentManager fragmentManager = getSupportFragmentManager();
    if (savedInstanceState == null) {

      Bundle bundle = new Bundle();
      bundle.putDouble("latStart", Double.parseDouble(getIntent().getStringExtra("latStart")));
      bundle.putDouble("lngStart", Double.parseDouble(getIntent().getStringExtra("lngStart")));
      bundle.putDouble("latEnd", Double.parseDouble(getIntent().getStringExtra("latEnd")));
      bundle.putDouble("lngEnd", Double.parseDouble(getIntent().getStringExtra("lngEnd")));
      bundle.putBoolean("isMock",  getIntent().getBooleanExtra("isMock",true));


      FragmentTransaction transaction = fragmentManager.beginTransaction();
      transaction.disallowAddToBackStack();
      MapboxFragment fragInfo = new MapboxFragment();
      fragInfo.setArguments(bundle);
      fragInfo.setOnTurningListener(this);
      transaction.add(R.id.navigation_fragment_frame, fragInfo).commit();
    }
  }

  public void showPlaceholderFragment() {
    replaceFragment(new PlaceholderFragment());
  }

  private void replaceFragment(Fragment newFragment) {


    String tag = String.valueOf(newFragment.getId());
    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    transaction.disallowAddToBackStack();
   // transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
   // int fadeInAnimId = R.anim.abc_fade_in;
  //  int fadeOutAnimId = R.anim.abc_fade_out;
   // transaction.setCustomAnimations(fadeInAnimId, fadeOutAnimId, fadeInAnimId, fadeOutAnimId);
    transaction.replace(R.id.navigation_fragment_frame, newFragment, tag).commit();

  }



  @Override
  public void OnTurning(int turning) {


    Intent intent = new Intent();
    intent.setAction(MainActivity.MyBroadcastReceiver.ACTION);
    intent.putExtra("turning", turning);
    context.sendBroadcast(intent);
  }
}
