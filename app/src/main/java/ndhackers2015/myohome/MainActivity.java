package ndhackers2015.myohome;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ActionBar;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Text;

import java.util.Vector;
import java.util.concurrent.TimeUnit;
import ndhackers2015.myohome.DigitalLifeController;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

public class MainActivity extends AppCompatActivity  {

    public boolean isLightOn = false, isDoorLocked, isSmartPlugOn = false, isNight = false;
    public String Dguid;
    public DigitalLifeController dlc;
    public ImageView I1, I2, I3, I4, I5, I6, I7, I8, I9;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionbar = getActionBar();
        if (actionbar != null) {
            actionbar.setTitle("");
        }

        I1 = (ImageView) findViewById(R.id.imageView1);
        I2 = (ImageView) findViewById(R.id.imageView2);
        I3 = (ImageView) findViewById(R.id.imageView3);
        I4 = (ImageView) findViewById(R.id.imageView4);
        I5 = (ImageView) findViewById(R.id.imageView5);
        I6 = (ImageView) findViewById(R.id.imageView6);
        I7 = (ImageView) findViewById(R.id.imageView7);
        I8 = (ImageView) findViewById(R.id.imageView8);
        I9 = (ImageView) findViewById(R.id.imageView9);

        // Set Myo Hub and Listeners
        Hub hub = Hub.getInstance();
        if (!hub.init(this)) {
            Log.e(String.valueOf(this), "Could not initialize the Hub.");
            finish();
            return;
        }
        Hub.getInstance().addListener(mListener);

        I5.setBackgroundResource(R.drawable.myo5);
        I6.setBackgroundResource(R.drawable.myo4);
        I7.setBackgroundResource(R.drawable.myo1);
        I8.setBackgroundResource(R.drawable.myo2);
        I9.setBackgroundResource(R.drawable.myo3);

        // Initialize DL
        try {
            dlc = DigitalLifeController.getInstance();
            dlc.init("EE_E424920D0D768DAF_1", "https://systest.digitallife.att.com");
            dlc.login( "553474454", "NO-PASSWD");
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Login Failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }

        try {
            isDoorLocked = getStatus(dlc, "door-lock", "lock", isDoorLocked, "lock", "unlock");
            if (isDoorLocked == true) {
                I1.setBackgroundResource(R.drawable.locked);
            } else {
                I1.setBackgroundResource(R.drawable.unlocked);
            }
            isLightOn = getStatus(dlc, "light-control", "switch", isLightOn, "on", "off");
            if (isLightOn == true) {
                I2.setBackgroundResource(R.drawable.light_on);
            } else {
                I2.setBackgroundResource(R.drawable.light_off);
            }
            isSmartPlugOn = getStatus(dlc, "smart-plug", "switch", isSmartPlugOn, "on", "off");
            if (isSmartPlugOn == true) {
                I3.setBackgroundResource(R.drawable.connected);
            } else {
                I3.setBackgroundResource(R.drawable.disconnected);
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Could not load Digital Life", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        I4.setBackgroundResource(R.drawable.night_off);
    }

    public boolean getStatus(DigitalLifeController dlc, String attribute, String label, Boolean in, String t, String f) {
        try {
            JSONArray j_array = dlc.fetchDevices();
            for (int i = 0; i < j_array.size(); i++){
                JSONObject w = (JSONObject)j_array.get(i);
                if(w.get("deviceType")!=null && ((String)w.get("deviceType")).equalsIgnoreCase(attribute)){
                    Dguid = (String) w.get("deviceGuid");
                    JSONArray attributeArray = (JSONArray)w.get("attributes");
                    for(int j = 0; j < attributeArray.size(); j++){
                        JSONObject x = (JSONObject)attributeArray.get(j);
                        System.out.println("label = " + x.get("label") + "value = " + x.get("value"));
                        if (x.get("label").equals(label) && x.get("value").equals(t)) {
                            in = true;
                        }
                        else if (x.get("label").equals(label) && x.get("value").equals(f)){
                            in = false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Could not load Digital Life", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return in;
        }
        return in;
    }

    private void onScanActionSelected() {
        // Launch the ScanActivity to scan for Myos to connect to.
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }

    private DeviceListener mListener = new AbstractDeviceListener() {

        @Override
        public void onConnect(Myo myo, long timestamp) {
            Toast.makeText(MainActivity.this, "Myo Connected!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            Toast.makeText(MainActivity.this, "Myo Disconnected!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPose(Myo myo, long timestamp, Pose pose){
            Toast.makeText(MainActivity.this, "Pose: " + pose, Toast.LENGTH_SHORT).show();

            switch (pose) {
                case REST:
                    break;
                case DOUBLE_TAP:
                    break;
                case FIST:
                    isNight = false;
                    I4.setBackgroundResource(R.drawable.night_off);
                    light();
                    break;
                case WAVE_OUT:
                    isNight = false;
                    I4.setBackgroundResource(R.drawable.night_off);
                    doorLock();
                    break;
                case WAVE_IN:
                    isNight = false;
                    I4.setBackgroundResource(R.drawable.night_off);
                    smartPlug();
                    break;
                case FINGERS_SPREAD:
                    myo.vibrate(Myo.VibrationType.LONG);
                    try { Thread.sleep(1500); } catch(Exception e) { return; }
                    isNight = true;
                    I4.setBackgroundResource(R.drawable.night_on);
                    light();
                    isLightOn = getStatus(dlc, "light-control", "switch", isLightOn, "on", "off");
                    try { Thread.sleep(1500); } catch(Exception e) { return; }
                    doorLock();
                    isDoorLocked = getStatus(dlc, "door-lock", "lock", isDoorLocked, "lock", "unlock");
                    try { Thread.sleep(3000); } catch(Exception e) { return ;}
                    smartPlug();
                    isSmartPlugOn = getStatus(dlc, "smart-plug", "switch", isSmartPlugOn, "on", "off");
                    while (isSmartPlugOn==false);
                    break;
            }
            if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                // Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                // hold the poses without the Myo becoming locked.
                myo.unlock(Myo.UnlockType.HOLD);
                // Notify the Myo that the pose has resulted in an action, in this case changing
                // the text on the screen. The Myo will vibrate.
                myo.notifyUserAction();
            } else {
                // Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                // stay unlocked while poses are being performed, but lock after inactivity.
                myo.unlock(Myo.UnlockType.TIMED);
            }
        }

    };

    public void light() {
        try {
            getStatus(dlc, "light-control", "switch", isLightOn, "on", "off");
            if (isLightOn == true) {
                dlc.updateDevice(Dguid, "switch", "off");
                isLightOn = false;
                I2.setBackgroundResource(R.drawable.light_off);
            } else if (isLightOn == false && isNight == false) {
                dlc.updateDevice(Dguid, "switch", "on");
                isLightOn = true;
                I2.setBackgroundResource(R.drawable.light_on);
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Could not load Digital Life", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }
    }

    public void doorLock() {
        try {
            getStatus(dlc, "door-lock", "lock", isDoorLocked, "lock", "unlock");
            if (isDoorLocked == true && isNight == false) {
                dlc.updateDevice(Dguid, "lock", "unlock");
                isDoorLocked = false;
                I1.setBackgroundResource(R.drawable.unlocked);
            } else if(isDoorLocked == false) {
                dlc.updateDevice(Dguid, "lock", "lock");
                isDoorLocked = true;
                I1.setBackgroundResource(R.drawable.locked);
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Could not load Digital Life", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }
    }

    public void smartPlug() {
        try {
            getStatus(dlc, "smart-plug", "switch", isSmartPlugOn, "on", "off");
            if (isSmartPlugOn == false) {
                dlc.updateDevice(Dguid, "switch", "on");
                isSmartPlugOn = true;
                I3.setBackgroundResource(R.drawable.connected);
            } else if (isSmartPlugOn == true && isNight == false){
                dlc.updateDevice(Dguid, "switch", "off");
                isSmartPlugOn = false;
                I3.setBackgroundResource(R.drawable.disconnected);
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Could not load Digital Life", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDoorLocked = getStatus(dlc, "door-lock", "lock", isDoorLocked, "lock", "unlock");
        if (isDoorLocked == true) {
            I1.setBackgroundResource(R.drawable.locked);
        } else {
            I1.setBackgroundResource(R.drawable.unlocked);
        }
        isLightOn = getStatus(dlc, "light-control", "switch", isLightOn, "on", "off");
        if (isLightOn == true) {
            I2.setBackgroundResource(R.drawable.light_on);
        } else {
            I2.setBackgroundResource(R.drawable.light_off);
        }
        isSmartPlugOn = getStatus(dlc, "smart-plug", "switch", isSmartPlugOn, "on", "off");
        if (isSmartPlugOn == true) {
            I3.setBackgroundResource(R.drawable.connected);
        } else {
            I3.setBackgroundResource(R.drawable.disconnected);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (R.id.action_scan == id) {
            onScanActionSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
