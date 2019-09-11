package com.example.ami.utils;

/**
 * Created by congrui on 2016/11/11.
 */

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Author : Rajanikant
 * Date : 16 Jan 2016
 * Time : 13:08
 */
public  class Utils {
    // 要上传的文件路径，理论上可以传输任何文件，实际使用时根据需要处理

    public static String getKey()
    {
        return "3jf939iuf9f00f93,.!=+";
    }


    public static void writeToFile(String data, Context context) {


        try {
          //  Log.e("TAGWrite:", DBManager.getInstance(context,DBManager.dbNameTest).querySettingsList().get(0).getMCode()+".txt");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(Utils.getSharedPreference("NodeID",context) + "C" + ".json", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
//json覆盖写入的
    public static void writeToFile(String data, Context context, String filename) {

        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(filename, Context.MODE_APPEND));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public static void setSharedPreference(String key, String value, Context mActivity){
        SharedPreferences sharedPref =mActivity.getSharedPreferences("my_prefs", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value); //putString
        editor.commit();
    }
    public static String getSharedPreference(String key, Context mActivity){
        SharedPreferences sharedPref = mActivity.getSharedPreferences("my_prefs", Activity.MODE_PRIVATE);
        return sharedPref.getString(key, "");
    }


    public static void displayToastSHORT(final String message, final Context context){
      Handler handler = new Handler();
      handler.post(new Runnable() {
          @Override
          public void run() {
              Toast toast;
              toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
              toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 120);
              toast.show();
          }
      });
    }

    public static void displayToastLong(final String message, final Context context){
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast;
                toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 120);
                toast.show();
            }
        });
    }

    public static  void createAlert( String title, String message, final String positive, final String negative, final Runnable func, final Context mActivity)
    {

        new AlertDialog.Builder(mActivity)
                .setTitle(title)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton(positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(func != null){
                            func.run();
                        }

                    }
                })
                .setNegativeButton(negative, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    //键盘弹出后，失去焦点则消失
    public static void hideKeyboard(View view, Context context) {
        InputMethodManager inputMethodManager =(InputMethodManager)context.getSystemService(
            Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    public static void showUpKeyboard(View view, Context context){
        InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
    }



    public static String getVersionName(Context context) {
        PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo != null) {
            return packageInfo.versionName;
        }
        return "";
    }
    public static PackageInfo getPackageInfo(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    //检查是哦福友网络
    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    // MD5加码。32位
    public static String md5(String inStr) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            // Debug.out(e.toString());
            e.printStackTrace();
            return "";
        }
        char[] charArray = inStr.toCharArray();
        byte[] byteArray = new byte[charArray.length];

        for (int i = 0; i < charArray.length; i++)
            byteArray[i] = (byte) charArray[i];

        byte[] md5Bytes = md5.digest(byteArray);

        StringBuffer hexValue = new StringBuffer();

        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16)
                hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }

        return hexValue.toString();
    }

    public static String getMACAddress(final Context mContext){
        return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getIntDate()
    {
        Date dNow = new Date( );
        SimpleDateFormat ft =
            new SimpleDateFormat("yyyyMMdd", Locale.ITALY);
        return ft.format(dNow);
    }

    public static String getTimeStamp()
    {
        return String.valueOf(System.currentTimeMillis() / 1000L);
    }


    public static String convertStringSpecial(String str)
    {
        return str.replace("'","''").replace("\"","").replace("\\","\\\\");
    }

}