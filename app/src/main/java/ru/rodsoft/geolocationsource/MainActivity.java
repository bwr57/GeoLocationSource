package ru.rodsoft.geolocationsource;

import java.util.Locale;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements LocationListener {

    public class StartStopButtonListener implements ToggleButton.OnCheckedChangeListener
    {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                                     boolean isChecked)
        {
            if(isChecked) {
                boolean streaming = switchGps(true);
                if (!isConnectionActive) {
                    switchGps(false);
                }
            }
            else {
//               if (isConnectionActive || toActivateConnection) {
                    switchGps(false);
                }

            toActivateConnection = isConnectionActive;
        }

    }



    protected static double latitude = 0;

    public double getLatitude() {
        return latitude;
    }

    protected static double longitude = 0;
    protected static double altitude = 0;

    public double getAltitude() {
        return altitude;
    }

    public double getLongitude() {
        return longitude;
    }

    protected static double speed;

    public double getSpeed() {
        return speed;
    }

    protected static double bearing;

    public double getBearing() {
        return bearing;
    }

    protected static long mGPS_UCT_Time;

    public long getmGPS_UCT_Time() {
        return mGPS_UCT_Time;
    }

    protected static boolean isGpsEnabled = false;

    private static boolean isConnectionActive = false;

    public static DatagramSocket socket = null;
    public static DatagramPacket connection = null;

    public boolean toActivateConnection = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        textViewLatitude = (TextView) findViewById(R.id.Lat_GPS);
        textViewLongitude = (TextView) findViewById(R.id.Long_GPS);
        textViewAltitude = (TextView) findViewById(R.id.Alt_GPS);
        textViewSpeed = (TextView) findViewById(R.id.Speed_GPS);
        textViewBearing = (TextView) findViewById(R.id.Bearing_GPS);
        textViewGpsStatus = (TextView) findViewById(R.id.Status_GPS);

        editTextTargetIP = (EditText) findViewById(R.id.EditTextTargetIP);
        editTextTargetPort = (EditText) findViewById(R.id.EditTextTargetPort);
        toggleButtonStartStop = (ToggleButton) findViewById(R.id.ToggleButtonStartStop);
        toggleButtonStartStop.setOnCheckedChangeListener(new StartStopButtonListener());

        toActivateConnection = loadConfiguration();

        locationmanager = (LocationManager) getSystemService(LOCATION_SERVICE);
        switchGps(toActivateConnection);
    }

    protected boolean loadConfiguration()
    {
        SharedPreferences configuration =
                this.getPreferences(Context.MODE_PRIVATE);
        editTextTargetIP.setText(configuration.getString(getString(R.string.saved_target_ip_key),
                getString(R.string.DefaultTargetIP)));
        editTextTargetPort.setText(configuration.getString(getString(R.string.saved_target_port_key),
                getString(R.string.DefaultTargetPort)));

        return configuration.getInt(getString(R.string.saved_activated_key), 0) != 0;
    }

    protected void saveConfiguration(boolean toActivate)
    {
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.saved_target_ip_key),
                editTextTargetIP.getText().toString());
        editor.putString(getString(R.string.saved_target_port_key),
                editTextTargetPort.getText().toString());
        editor.putInt(getString(R.string.saved_activated_key), toActivate ? 1 : 0);
        editor.commit();
    }

    protected void createConnection()
    {
        String address = editTextTargetIP.getText().toString(); // "10.5.1.148";
        String portStr = editTextTargetPort.getText().toString();
        int port = 0;
        InetAddress clientAddress = null;
        try {
            clientAddress = InetAddress.getByName(address);
            port = Integer.parseInt(portStr);
        } catch (UnknownHostException e) {
            toActivateConnection = false;
            showDialog(R.string.error_invalidaddr);
            return;
        }

        try {
            socket = new DatagramSocket();
            socket.setReuseAddress(true);
        } catch (SocketException e) {
            socket = null;
            toActivateConnection = false;
            showDialog(R.string.error_neterror);
            return;
        }

        byte[] buf = new byte[256];
        try {
 //           port = Integer.parseInt(port.getText().toString());
            connection = new DatagramPacket(buf, buf.length, clientAddress, port);
        } catch (Exception e) {
            socket.close();
            socket = null;
            toActivateConnection = false;
            showDialog(R.string.error_neterror);
            return;
        }
        isConnectionActive = true;
    }

    protected void stopConnection()
    {
        if(isConnectionActive && socket != null)
        {
            try
            {
                socket.close();
                socket = null;
            }
            catch (Exception ex)
            { }
        }
        isConnectionActive = false;
    }

    protected void sendLocationData()
    {
        if(isConnectionActive && connection != null) {

            try {
                String messageStr = String.format(Locale.ENGLISH, "%10.6f,%10.6f,%6.1f,%6.1f,%3.0f",
                        getLatitude(), getLongitude(), getAltitude(), getSpeed(), getBearing());
                byte[] message = messageStr.getBytes("UTF-8");
                connection.setData(message);
                connection.setLength(message.length);
                socket.send(connection);
            } catch (Exception ex) {
            }
        }
    }

    public boolean switchGps(boolean toActivate) {
        if (toActivate) {
            boolean GPS_enabled = locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!GPS_enabled) {
                Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                setGpsStatus(false);
                textViewGpsStatus.setText(R.string.PermissionError);
                return false;
            }
            locationmanager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                    0, this);
            setGpsStatus(GPS_enabled);
            createConnection();
            if(isConnectionActive)
            {
                editTextTargetIP.setEnabled(false);
                editTextTargetPort.setEnabled(false);
            }
            else
            {
                editTextTargetIP.setEnabled(true);
                editTextTargetPort.setEnabled(true);
            }
            toggleButtonStartStop.setChecked(GPS_enabled);
            return GPS_enabled;
        }
        else
        {
            setGpsStatus(false);
            stopConnection();
            locationmanager.removeUpdates(this);
            editTextTargetIP.setEnabled(true);
            editTextTargetPort.setEnabled(true);
        }
        toggleButtonStartStop.setChecked(false);
        return false;
    }

    protected void setGpsStatus(boolean isGpsEnabled)
    {
        this.isGpsEnabled = isGpsEnabled;
        textViewGpsStatus.setText(isGpsEnabled ? R.string.Active : R.string.NotActive);
    }

    public static LocationManager locationmanager;


    private TextView textViewLatitude;
    private TextView textViewLongitude;
    private TextView textViewAltitude;
    private TextView textViewSpeed;
    private TextView textViewBearing;
    private TextView textViewGpsStatus;
    private EditText editTextTargetIP;
    private EditText editTextTargetPort;
    private ToggleButton toggleButtonStartStop;

    protected boolean gpsAvailable = false;

    public void onLocationChanged(Location location) {

        if(isGpsEnabled)
        {
            gpsAvailable = false;

            latitude= location.getLatitude();
            longitude= location.getLongitude();
            altitude= location.getAltitude();

            speed = location.getSpeed() * 4;
            bearing = location.getBearing();

            mGPS_UCT_Time = location.getTime();


            gpsAvailable = true;

            textViewLatitude.setText(String.format("%.5f", getLatitude()));
            textViewLongitude.setText(String.format("%.5f", getLongitude()));
            textViewAltitude.setText(String.format("%.5f", getAltitude()));
            textViewSpeed.setText(String.format("%.5f", getSpeed()));
            textViewBearing.setText(String.format("%.5f", getBearing()));

            sendLocationData();
            textViewGpsStatus.setText(R.string.Active);
        }
        //mGPS_counter++;

    }


    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
        if (provider.equals("gps")) {
            if (locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                setGpsStatus(false);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        if (provider.equals("gps"))
        {
            setGpsStatus(true);
        }
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

        // TODO Auto-generated method stub
        if (provider.equals("gps") && status == 0)
        {
            setGpsStatus(false);
        }
    }

    @Override
    protected void onDestroy()
    {
        saveConfiguration(isConnectionActive);
        switchGps(false);
        super.onDestroy();
    }

}
