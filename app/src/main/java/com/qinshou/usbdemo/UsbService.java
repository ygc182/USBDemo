package com.qinshou.usbdemo;

import android.app.PendingIntent;
import android.app.Service;
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
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;

import static com.qinshou.usbdemo.MainActivity.ACTION_DEVICE_PERMISSION;

/**
 * Description:
 * Created on 2017/7/13
 */

public class UsbService extends Service {

    public static final String TAG = "UsbService";
    private UsbManager mUsbManager;
    private Context mContext;
    private PendingIntent mPermissionIntent;

    public static final int VENDOR_ID = 0x054c;
    public static final int PRODUCT_ID = 0xa4a0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "UsbService onCreate...");
        mContext = getApplicationContext();
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

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "UsbService onStartCommand...");

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        enumerateDevice(mUsbManager);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "UsbService onDestroy...");

        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbPermissionReceiver);

        super.onDestroy();
    }

    /**
     * 枚举设备
     *
     * @param mUsbManager
     */
    private void enumerateDevice(UsbManager mUsbManager) {
        if (mUsbManager == null) {
            Log.e(TAG, "enumerateDevice: mUsbManager is null !!!");
            return;
        }
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if (!(deviceList.isEmpty())) {
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();
                Log.d(TAG, "onReceive: " + "" +
                        "\ndevice name: " + device.getDeviceName() +
                        "\ndevice product name:" + device.getProductName() +
                        "\ndevice product id:" + device.getProductId() +
                        "\nvendor id:" + device.getVendorId() +
                        "\ndevice serial: " + device.getSerialNumber());

                // 判断device是否已经获得权限
                if (device.getVendorId() == VENDOR_ID && device.getProductId() == PRODUCT_ID) {
                    if (mUsbManager.hasPermission(device)) {
                        Log.d(TAG, "This device has Permission...");
                        initCommunication(device);
                    } else {
                        Log.d(TAG, "This device has no Permission!!! Begin request ...");
                        mUsbManager.requestPermission(device, mPermissionIntent);
                    }
                }
            }
        } else {
            Log.e(TAG, "enumerateDevice: 请连接USB");
        }

    }

    private UsbEndpoint mUsbEndpointIn;
    private UsbEndpoint mUsbEndpointOut;
    private UsbInterface mUsbInterface;
    private UsbDeviceConnection mUsbDeviceConnection;

    /**
     * 初始化设备
     *
     * @param device
     */
    private void initCommunication(UsbDevice device) {
        Log.d(TAG, "initCommunication: ......");
        if (0x054c == device.getVendorId() && 0xa4a0 == device.getProductId()) {
            Log.d(TAG, "initCommunication in right device: ......");
            int interfaceCount = device.getInterfaceCount();
            Log.d(TAG, "initCommunication: interfaceCount  " + interfaceCount);
            for (int interfaceIndex = 0; interfaceIndex < interfaceCount; interfaceIndex++) {
                UsbInterface usbInterface = device.getInterface(interfaceIndex);
                if ((255 != usbInterface.getInterfaceClass()) && (240 != usbInterface.getInterfaceSubclass())) {
                    continue;
                }
                Log.d(TAG, "initCommunication: usbInterface.getEndpointCount()-----" + usbInterface.getEndpointCount());
                for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                    UsbEndpoint ep = usbInterface.getEndpoint(i);
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
                    Log.d(TAG, "initCommunication: endpoint is null  !!!");
                    mUsbEndpointIn = null;
                    mUsbEndpointOut = null;
                    mUsbInterface = null;
                } else {
                    Log.d(TAG, "endpoint out: " + mUsbEndpointOut + ",endpoint in: " + mUsbEndpointIn.getAddress());

                    mUsbInterface = usbInterface;
                    mUsbDeviceConnection = mUsbManager.openDevice(device);

                    if (mUsbDeviceConnection == null) {
                        Toast.makeText(this, "mUsbDeviceConnection is null !!!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (mUsbDeviceConnection.claimInterface(mUsbInterface, true)) {
                        Log.d(TAG, "initCommunication: open设备成功");
                        Toast.makeText(mContext, "open设备成功", Toast.LENGTH_SHORT).show();

                    } else {
                        Log.e(TAG, "initCommunication: 无法打开连接通道");
                        Toast.makeText(mContext, "无法打开连接通道", Toast.LENGTH_SHORT).show();
                        mUsbDeviceConnection.close();
                    }
                    break;
                }
            }
        }
    }

    /**
     * 发送数据
     *
     * @param buffer
     */
    private void sendMessageToPoint(byte[] buffer, int timeout) {
        // bulkOut传输
        int sendResult = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, buffer, buffer.length, timeout);
    }

    /**
     * 从设备接收数据bulkIn
     *
     * @return
     */
    private byte[] receiveMessageFromPoint(int timeout) {
        byte[] buffer = new byte[15];
        int receiveResult = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, buffer, buffer.length, timeout);
        return buffer;
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: mUsbReceiver---action: " + action);
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Log.d(TAG, "onReceive: USB设备插入");
                enumerateDevice(mUsbManager);
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Log.d(TAG, "onReceive: USB设备拔出");
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
                            Log.d(TAG, "onReceive: usb EXTRA_PERMISSION_GRANTED...........");
                            initCommunication(device);
                        }
                    } else {
                        Log.d(TAG, "onReceive: usb EXTRA_PERMISSION_GRANTED  null!!!...........");
                    }
                }
            }
        }
    };
}
