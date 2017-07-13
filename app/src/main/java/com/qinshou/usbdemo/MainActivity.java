package com.qinshou.usbdemo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView mUSBState;
    private TextView mUSBState2;
    private TextView mUSBState3;
    private TextView mTvResult;
    private Button mBtnSendData;
    private Button mBtnStopSendData;
    private ScrollView mSv;

    private UsbManager mUsbManager;
    public static final String ACTION_DEVICE_PERMISSION = "com.linc.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
    private LoopThread mLoopThread;
    private int mResult;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        mUSBState = (TextView) findViewById(R.id.tvUSBState);
        mUSBState2 = (TextView) findViewById(R.id.tvUSBState2);
        mUSBState3 = (TextView) findViewById(R.id.tvUSBState3);
        mTvResult = (TextView) findViewById(R.id.result);
        mBtnSendData = (Button) findViewById(R.id.btnSendData);
        mBtnStopSendData = (Button) findViewById(R.id.btnStopSendData);
        mSv = (ScrollView) findViewById(R.id.sv);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        initListener();
    }

    private void initListener() {

        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, usbFilter);


        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_DEVICE_PERMISSION), 0);
        IntentFilter permissionFilter = new IntentFilter(ACTION_DEVICE_PERMISSION);
        registerReceiver(mUsbPermissionReceiver, permissionFilter);

        mBtnStopSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoopThread != null && !mLoopThread.mExit) {
                    mLoopThread.exitLoop();
                    Toast.makeText(MainActivity.this, "已停止发送！", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoopThread = new LoopThread() {
                    @Override
                    public void run() {
                        int count = 0;
                        while (!mExit) {
                            //需要在另一个线程中进行
                            try {
                                Log.d(TAG, "run: begin send data..........");
                                String data = "hello world ! " + count;
                                if (mUsbDeviceConnection == null) {
                                    Log.e(TAG, "run: mUsbDeviceConnection is null!!!!!!!");
                                    return;
                                }
                                mResult = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, data.getBytes(), data.length(), 1000);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mTvResult.append("result: count--" + mResult + "\n");
                                    }
                                });
                                Log.d(TAG, "run: mResult --------" + mResult);
                                count++;
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                Log.e(TAG, "run: Exception happened:  -------" + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                    }
                };
                mLoopThread.start();
                Toast.makeText(MainActivity.this, "begin sending data....", Toast.LENGTH_SHORT).show();
            }
        });
    }

    class LoopThread extends Thread {
        public boolean mExit = false;

        public void exitLoop() {
            mExit = true;
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbPermissionReceiver);
        if (mLoopThread != null && !mLoopThread.mExit) {
            mLoopThread.exitLoop();
            Toast.makeText(MainActivity.this, "已停止发送！", Toast.LENGTH_SHORT).show();
        }
        super.onDestroy();
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: mUsbReceiver");
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                mUSBState.setText("ACTION_USB_DEVICE_ATTACHED");

                HashMap<String, UsbDevice> deviceHashMap = mUsbManager.getDeviceList();
                Iterator<UsbDevice> iterator = deviceHashMap.values().iterator();
                while (iterator.hasNext()) {
                    UsbDevice device = iterator.next();
                    mUSBState2.setText("\ndevice name: " + device.getDeviceName() +
                            "\ndevice product name:" + device.getProductName() +
                            "\ndevice product id:" + device.getProductId() +
                            "\nvendor id:" + device.getVendorId() +
                            "\ndevice serial: " + device.getSerialNumber());
                    Log.d(TAG, "onReceive: " + "" +
                            "\ndevice name: " + device.getDeviceName() +
                            "\ndevice product name:" + device.getProductName() +
                            "\ndevice product id:" + device.getProductId() +
                            "\nvendor id:" + device.getVendorId() +
                            "\ndevice serial: " + device.getSerialNumber());

                    // 判断device是否已经获得权限
//                    if (device.getProductId() == 13824 && device.getVendorId() == 5118) {
                    if (device.getProductId() == 0xa4a0 && device.getVendorId() == 0x054c) {
                        if (mUsbManager.hasPermission(device)) {
                            Log.d(TAG, " hasPermission...");
                            Toast.makeText(context, "This device has Permission...", Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(context, "This device has no Permission,begin  request ...", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, " NoPermission!!! Begin request ...");
                            mUsbManager.requestPermission(device, mPermissionIntent);
                        }
                    }

                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                mUSBState.setText("ACTION_USB_DEVICE_DETACHED");
            }
        }
    };

    private final BroadcastReceiver mUsbPermissionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: mUsbPermissionReceiver.........");
            String action = intent.getAction();
            if (ACTION_DEVICE_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            mUSBState2.setText("usb EXTRA_PERMISSION_GRANTED");
                            Log.d(TAG, "onReceive: usb EXTRA_PERMISSION_GRANTED...........");
                            initCommunication(device);
                        }
                    } else {
                        Log.d(TAG, "onReceive: usb EXTRA_PERMISSION_GRANTED  null!!!...........");
                        mUSBState2.setText("usb EXTRA_PERMISSION_GRANTED null!!!");
                    }
                }
            }
        }
    };

    private UsbEndpoint mUsbEndpointIn;
    private UsbEndpoint mUsbEndpointOut;
    private UsbInterface mUsbInterface;
    private UsbDeviceConnection mUsbDeviceConnection;


    private void initCommunication(UsbDevice device) {
        Log.d(TAG, "initCommunication: ......");
        mUSBState3.append("initCommunication in\n");
//        if (1234 == device.getVendorId() && 5678 == device.getProductId()) {
        if (0x054c == device.getVendorId() && 0xa4a0 == device.getProductId()) {
            mUSBState3.append("initCommunication in right device\n");
            Log.d(TAG, "initCommunication in right device: ......");
            int interfaceCount = device.getInterfaceCount();
            for (int interfaceIndex = 0; interfaceIndex < interfaceCount; interfaceIndex++) {
                UsbInterface usbInterface = device.getInterface(interfaceIndex);
                if ((255 != usbInterface.getInterfaceClass()) && (240 != usbInterface.getInterfaceSubclass())) {
                    continue;
                }
                mUSBState3.append("initCommunication: usbInterface.getEndpointCount()-----" + usbInterface.getEndpointCount());
                Log.d(TAG, "initCommunication: usbInterface.getEndpointCount()-----" + usbInterface.getEndpointCount());
                for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                    UsbEndpoint ep = usbInterface.getEndpoint(i);
                    mUSBState3.append("ep.getType------" + ep.getType() + ", ep.getDirection()-------" + ep.getDirection());
                    Log.d(TAG, "initCommunication: ep.getType------" + ep.getType() + ", ep.getDirection()-------" + ep.getDirection());
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.getDirection() != UsbConstants.USB_DIR_OUT) {
                            mUsbEndpointIn = ep;
                        } else {
                            mUsbEndpointOut = ep;
                        }
                    }
                }

                if ((null == mUsbEndpointIn) || (null == mUsbEndpointOut)) {
                    mUSBState3.append("endpoint is null\n");
                    Log.d(TAG, "initCommunication: endpoint is null  !!!");
                    mUsbEndpointIn = null;
                    mUsbEndpointOut = null;
                    mUsbInterface = null;
                } else {
                    Log.d(TAG, "" + "\nendpoint out: " + mUsbEndpointOut + ",endpoint in: " +
                            mUsbEndpointIn.getAddress() + "\n");

                    mUSBState3.append("\nendpoint out: " + mUsbEndpointOut + ",endpoint in: " +
                            mUsbEndpointIn.getAddress() + "\n");
                    mUsbInterface = usbInterface;
                    mUsbDeviceConnection = mUsbManager.openDevice(device);

                    if (mUsbDeviceConnection == null) {
                        Toast.makeText(this, "mUsbDeviceConnection is null!!!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (mUsbDeviceConnection.claimInterface(mUsbInterface, true)) {
                        if (mUsbDeviceConnection != null) {
                            // 到此你的android设备已经连上zigbee设备
                            Toast.makeText(this, "open设备成功", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "initCommunication: open设备成功");
                        }

                    } else {
                        Toast.makeText(this, "无法打开连接通道", Toast.LENGTH_SHORT).show();
                        mUsbDeviceConnection.close();
                    }
                    break;
                }
            }
        }
    }
}
