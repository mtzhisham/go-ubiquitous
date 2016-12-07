package com.example.android.sunshine.app;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by moataz on 06/12/16.
 */
public class WatchService  extends IntentService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private static final String SERVICE_NAME = "WatchService";

    public static final String ACTION_UPDATE_WATCHFACE = "ACTION_UPDATE_WATCHFACE";

    private static final String DATA_PATH = "/weather";
    private static final String ICON_KEY = "ICON_KEY";
    private static final String HIGH_TEMP_KEY = "HIGH_TEMP_KEY";
    private static final String LOW_TEMP_KEY = "LOW_TEMP_KEY";

    private GoogleApiClient mGoogleApiClient;

    public WatchService() {
        super(SERVICE_NAME);

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null
                && intent.getAction() != null
                && intent.getAction().equals(ACTION_UPDATE_WATCHFACE)) {

            mGoogleApiClient = new GoogleApiClient.Builder(WatchService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        //get a cursor and fetch today's data
        //based on the implementation from detailed fragment
        String locationQuery = Utility.getPreferredLocation(this);

        Uri weatherUri = WeatherContract.WeatherEntry
                .buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

        Cursor c = getContentResolver().query(
                weatherUri,
                new String[]{WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
                }, null, null, null);

        if (c.moveToFirst()) {
            int weatherId = c.getInt(c.getColumnIndex(
                    WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
            String highT = Utility.formatTemperature(this, c.getDouble(
                    c.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)));
            String lowT = Utility.formatTemperature(this, c.getDouble(
                    c.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)));
            //pass to the dataAPI
            final PutDataMapRequest mapRequest = PutDataMapRequest.create(DATA_PATH);
            mapRequest.getDataMap().putInt(ICON_KEY, weatherId);
            mapRequest.getDataMap().putString(HIGH_TEMP_KEY, highT);
            mapRequest.getDataMap().putString(LOW_TEMP_KEY, lowT);
            PutDataRequest putDataReq =  mapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);


        }
        c.close();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("WatchService",i + "");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //connection closed before data transferred! moved disconnecting when removing task
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        mGoogleApiClient.disconnect();
    }
}
