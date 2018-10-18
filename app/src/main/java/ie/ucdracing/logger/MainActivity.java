package ie.ucdracing.logger;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity implements LocationListener, SensorEventListener {
    private DatagramSocket mSocket = new DatagramSocket();
    private InetAddress mServerAddress = InetAddress.getByName("192.168.0.59");

    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;

    private ByteBuffer mBuffer = ByteBuffer.allocate(256);
    private boolean mSending = false;

    public MainActivity() throws SocketException, UnknownHostException {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        ImageView imageView = findViewById(R.id.kuba);
        RotateAnimation rotateAnimation = new RotateAnimation(0, 720, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        rotateAnimation.setRepeatMode(Animation.REVERSE);
        rotateAnimation.setDuration(2000);
        imageView.startAnimation(rotateAnimation);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mSending) return;

        mBuffer.putInt(0, event.sensor.getType());
        for (int i = 0; i < event.values.length; i++) {
            mBuffer.putFloat(4 + i * 32, event.values[i]);
        }

        new SendValueTask().execute();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mSending) return;

        mBuffer.putInt(0, 255);
        mBuffer.putDouble(4, location.getLatitude());
        mBuffer.putDouble(68, location.getLongitude());

        new SendValueTask().execute();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class SendValueTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... param) {
            mSending = true;
            byte[] array = mBuffer.array();
            DatagramPacket packet = new DatagramPacket(array, array.length, mServerAddress, 4445);
            try {
                mSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mSending = false;

            return null;
        }
    }
}
