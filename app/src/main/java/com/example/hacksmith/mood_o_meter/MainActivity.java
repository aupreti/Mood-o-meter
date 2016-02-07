package com.example.hacksmith.mood_o_meter;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandPendingResult;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.UUID;
import com.microsoft.band.tiles.BandIcon;
import com.microsoft.band.tiles.BandTile;


public class MainActivity extends ActionBarActivity {


    public BandClient client=null;
    private Button btnStart, btnConsent;
    private TextView txtStatus;
    private int mood=-5;


    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            int newMood=-5;
            try {
            if (event != null) {
                newMood = event.getHeartRate();
            }
            }catch(Exception e){

            }
            if ((event != null && (newMood-mood)> 5) || (event != null && (mood!=-5))) {
                mood = event.getHeartRate();
                String moodString="";
                if (mood>= 100){
                    moodString= "Angry/Anxious";
                }else if( 85 <= mood & mood <100){
                    moodString= "Happy";
                }else if( 60 <= mood & mood <85){
                    moodString= "Calm";
                }else{
                    moodString= "Sad";
                }
                appendToUI((String.format("Your Mood = %s",moodString )));
                //appendToUI(String.format("Heart Rate = %d beats per minute\n"
                        //+ "Quality = %s\n", event.getHeartRate(), event.getQuality()));
                try {
// determine the number of available tile slots on the Band
                    int tileCapacity =
                            client.getTileManager().getRemainingTileCapacity().await();
                            //appendToUI((String.format("%d",tileCapacity)));
                    if (tileCapacity>= 1){
                        // create the small and tile icons from writable bitmaps
// small icons are 24x24 pixels
                        Bitmap smallIconBitmap = Bitmap.createBitmap(24, 24, null);
                        BandIcon smallIcon = BandIcon.toBandIcon(smallIconBitmap);
// tile icons are 46x46 pixels
                        Bitmap tileIconBitmap = Bitmap.createBitmap(46, 46, null);
                        BandIcon tileIcon = BandIcon.toBandIcon(tileIconBitmap);
// create a new UUID for the tile
                        UUID tileUuid = UUID.randomUUID();
// create a new BandTile using the builder
                        BandTile tile = new BandTile.Builder(tileUuid,"Mood",tileIcon)
                                .setTileSmallIcon(smallIcon).build();
                        try {
                            if(client.getTileManager().addTile((Activity) getBaseContext(),
                                    tile).await()) {
                             // do work if the tile was successfully created
                                try {
// send a dialog to the Band for one of our tiles
                                    client.getNotificationManager().showDialog(tileUuid,"Your Mood", moodString).await();
                                } catch (BandException e) {
// handle BandException
                                } catch (InterruptedException e) {
// handle InterruptedException
                                }

                            }
                        } catch (BandException e) {
// handle BandException
                        } catch (InterruptedException e) {
// handle InterruptedException
                        }
                    }
                } catch (BandException e) {
// handle BandException
                } catch (InterruptedException e) {
// handle InterruptedException
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set heart rate
        txtStatus = (TextView) findViewById(R.id.txtStatus);

        //set consent
        btnConsent = (Button) findViewById(R.id.btnConsent);

        //set start heart rate
        btnStart = (Button) findViewById(R.id.btnStart);

        final WeakReference<Activity> reference = new WeakReference<Activity>(this);

        btnConsent.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onClick(View v) {
                new HeartRateConsentTask().execute(reference);
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtStatus.setText("");
                new HeartRateSubscriptionTask().execute();
            }
        });



    }

    //Kick off the heart rate reading
    private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                    } else {
                        appendToUI("You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n");
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }



    //Need to get user consent
    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {
                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                            }
                        });
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }


    //Get connection to band
    private boolean getConnectedBandClient() throws InterruptedException, BandException {

        if (client == null) {
            //Find paired bands
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                //No bands found...message to user
                return false;
            }
            //need to set client if there are devices
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if(ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        //need to return connected status
        return ConnectionState.CONNECTED == client.connect().await();
    }

    @Override
    protected void onResume() {
        super.onResume();
        txtStatus.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (client != null) {
            try {
                client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
            } catch (BandIOException e) {
                appendToUI(e.getMessage());
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(string);
            }
        });
    }


}
