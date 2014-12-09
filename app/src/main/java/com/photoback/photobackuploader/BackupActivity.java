package com.photoback.photobackuploader;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BackupActivity extends ActionBarActivity {

    TextView mText;
    UsbManager mUsbManager;
    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
    String mMessage;
    String mMessage2;
    String TAG = "DebugPy";


    Runnable mUpdateUI = new Runnable() {
        @Override
        public void run() {
            mText.append(mMessage);
        }
    };

    Runnable mUpdateUI2 = new Runnable() {
        @Override
        public void run() {
            mText.append(mMessage2);
        }
    };

    Runnable mListenerTask = new Runnable() {
        @Override
        public void run() {

            byte[] buffer = new byte[5];
            int ret;

            try {
                mMessage = ">>> ";
                ret = mInputStream.read(buffer);
                if (ret == 5) {
                    String msg = new String(buffer);
                    mMessage += msg;
                } else {
                    mMessage += "Read error";
                }

            } catch (IOException e) {
                e.printStackTrace();
                mMessage += "Read error";
            }

            mMessage += System.getProperty("line.separator");
            mText.post(mUpdateUI);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            new Thread(this).start();
        }
    };

    /** Called when the user clicks the Decrease/Increase button */
    public void sendMessage(View view) {

        char direction = 0;

        switch (view.getId()) {
            case R.id.actuator_decrease:
                direction = '1';
                break;
            case R.id.actuator_increase:
                direction = '0';
                break;
        }

        byte[] buffer = new byte[5];
        buffer[0] = (byte) 'A';
        for (int i = 1; i < 5; i++) {
            buffer[i] = (byte) direction;
        }

        mMessage2 = "<<< ";
        try {
            mOutputStream.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            mMessage2 += "Send error";
        } catch (NullPointerException e) {
            e.printStackTrace();
            mMessage += "Could not write to device. Probably no device connected";
        }
        String msg = new String(buffer);
        mMessage2 += msg + System.getProperty("line.separator");
        mText.post(mUpdateUI2);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        mText = (TextView) findViewById(R.id.display_area);
        mText.setMovementMethod(new ScrollingMovementMethod());

        Intent intent = getIntent();
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

        if (mAccessory == null) {
            mText.append("Not started by the Accessory directly" + System.getProperty("line.separator"));
            return;
        }

        Log.v(TAG, mAccessory.toString());
        mFileDescriptor = mUsbManager.openAccessory(mAccessory);
        if (mFileDescriptor != null) {
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
        }
        Log.v(TAG, mFileDescriptor != null ? mFileDescriptor.toString() : null);
        new Thread(mListenerTask).start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_backup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
