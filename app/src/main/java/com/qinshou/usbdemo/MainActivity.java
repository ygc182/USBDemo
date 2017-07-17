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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private TextView mUSBState;
    private TextView mUSBState2;
    private TextView mUSBState3;
    private Button mBtnSendData;
    private SurfaceView mSurfaceView;
    private VideoDecoder mVideoDecoder;

    private UsbManager mUsbManager;
    public static final String ACTION_DEVICE_PERMISSION = "com.linc.USB_PERMISSION";
    private PendingIntent mPermissionIntent;
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
        mBtnSendData = (Button) findViewById(R.id.btnSendData);
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mVideoDecoder = new VideoDecoder();
        toShowVideo();

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

        mBtnSendData.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSendData:
                startSendData();
                break;
        }
    }

    private void toShowVideo() {
        Log.d(TAG, "toShowVideo: ...........");
        if (mVideoDecoder == null) {
            return;
        }
        mSurfaceView.getHolder().removeCallback(surfaceCB);
        mSurfaceView.getHolder().addCallback(surfaceCB);
    }

    SurfaceHolder.Callback surfaceCB = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated: .................");

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed: .................");

        }
    };

//    private void startReceiveData() {
//
//        toShowVideo();
//
//        mReceiveDataThread = new LoopThread() {
//
//            byte[] buffer = new byte[512];
//
//            @Override
//            public void run() {
//                for (; ; ) {
//                    Log.d(TAG, "run: begin....................");
//                    int videoDataLength = 0;
//                    int total = 0;
//                    while (true) {
//                        Log.d(TAG, "run: ");
//                        // c. 解析frame请求，获取类型，长度
//
//                        int receiveResult = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, buffer, buffer.length, 0);
//                        Log.d(TAG, "run: receiveResult......" + receiveResult);  // receiveResult......512
//
//                        if (receiveResult > 0) {
//
//                            total = total + receiveResult;
//                            Log.d(TAG, "run: total....." + total);
//                            if (total == 512) {
////                            CACHETYPE:VIDEO#TYPE:H264#LEN:XXXX#FRAMENUM:xxxx#000000		----发送video
//                                String receiveData = new String(buffer);
//                                Log.d(TAG, "run: receiveData----" + receiveData);
//                                String[] spilts = receiveData.split("#");
//                                String lenStr = (spilts[2].split(":"))[1];
//                                Log.d(TAG, "run: lenStr......" + lenStr);
//                                videoDataLength = Integer.parseInt(lenStr);  // 768000
//
//                                break;
//                            }
//
//                        } else {
//                            Log.d(TAG, "run: receiveResult---" + receiveResult);
//                            break;
//                        }
//                    }
//
//                    int readedLen = 0;
//                    Log.d(TAG, "run: videoDataLength..." + videoDataLength);  // run: videoDataLength...768000
//                    int readPerLength = 16 * 1024;        //  每次读到 16k
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    while (true) {
//                        byte[] bytes;
//                        int unreadLength = videoDataLength - readedLen;
//                        if (unreadLength > readPerLength) {
//                            bytes = new byte[readPerLength];
//                        } else {
//                            bytes = new byte[unreadLength];
//                        }
//                        int currLength = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, bytes, bytes.length, 0);
//
//                        if (currLength > 0) {
//                            Log.d(TAG, "loopToGetData...1.readedLen:" + readedLen + ",currLength" + currLength);
//                            baos.write(bytes, 0, currLength);
//                            readedLen = readedLen + currLength;
//                            Log.d(TAG, "loopToGetData...2.readedLen:" + readedLen + ",currLength" + currLength);
//                        }
//
//                        if (currLength <= 0 || readedLen >= videoDataLength) {
//                            Log.d(TAG, "loopToGetData...complete...currLength:" + currLength + ",readedLen:" + readedLen + ",videoDataLength" + videoDataLength);
//                            byte[] finalBytes = baos.toByteArray();
//                            try {
//                                baos.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                            // TODO: 2017/7/15  SurfaceView显示数据
//
//                            VideoData.VideoData videoData = new VideoData.VideoData(finalBytes,finalBytes.length);
//
//                            Log.d(TAG, "loopToGetData...finalGetData:" + finalBytes.length);
//                            break;
//                        }
//
//                    }
//                }
//
//            }
//        };
//        mReceiveDataThread.start();
//
////        // 开启轮询接收数据
////        mReceiveDataThread = new LoopThread() {
////            @Override
////            public void run() {
////                while (!mExit) {
////
////                    // b. 发送frame请求
//////                    CACHETYPE:VIDEO#CMD:REQUEST FRAME#		----请求video
////                    String requstFrame = "CACHETYPE:VIDEO#CMD:REQUEST FRAME#";
////                    ByteBuffer byteBuffer = ByteBuffer.allocate(512);
////                    byteBuffer.put(requstFrame.getBytes(), 0, requstFrame.getBytes().length);
////                    Log.d(TAG, "requstFrame: byteBuffer.remaining()----" + byteBuffer.remaining());
////                    byte[] b = new byte[byteBuffer.remaining()];
////                    byteBuffer.get(b, 0, b.length);
////                    Log.d(TAG, "requstFrame: b.length---" + b.length);
////                    int frameRequestResult = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, b, b.length, 0);
////                    Log.d(TAG, "requstFrame: frameRequestResult---------" + frameRequestResult);
////
////                    if (frameRequestResult > 0) {
////                        // c. 解析frame请求，获取类型，长度
////                        byte[] buffer = new byte[512 * 1024];
////                        int receiveResult = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, buffer, buffer.length, 0);
////                        Log.d(TAG, "run: receiveResult---" + receiveResult);
////                        if (receiveResult > 0) {
//////                        CACHETYPE:VIDEO#TYPE:H264#LEN:XXXX#FRAMENUM:xxxx#		----发送video
////                            String receiveData = new String(buffer);
////                            Log.d(TAG, "run: receiveData----" + receiveData);
////                            String[] spilts = receiveData.split("#");
////                            for (String sp : spilts) {
////                                Log.d(TAG, "run: sp-----" + sp);
////                            }
////                            String lenStr = (spilts[2].split(":"))[1];
////                            int lenth = Integer.getInteger(lenStr);
////
////                            // d. 请求具体数据
////                            byte[] videBuffer = new byte[lenth];
////                            int vedioDataLength = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, videBuffer, lenth, 0);
////                            Log.d(TAG, "run: vedioDataLength-----" + vedioDataLength);
////                        }
////                    }
////
////
////                }
////            }
////        };
////        mReceiveDataThread.start();
//    }

    private void startSendData() {
        Log.d(TAG, "startSendData: ");
        if (mUsbDeviceConnection == null) {
            Log.e(TAG, "run: mUsbDeviceConnection is null!!!!!!!");
            return;
        }
        // a. 预备通信
        String startHead = "CACHETYPE:VIDEO#CMD:START#";
        ByteBuffer byteBuffer = ByteBuffer.allocate(512);
        byteBuffer.put(startHead.getBytes(), 0, startHead.getBytes().length);
        Log.d(TAG, "startSendData: byteBuffer.remaining()----" + byteBuffer.remaining());
        byte[] b = new byte[byteBuffer.remaining()];
        byteBuffer.get(b, 0, b.length);
        Log.d(TAG, "startSendData: b.length---" + b.length);
        mResult = mUsbDeviceConnection.bulkTransfer(mUsbEndpointOut, b, b.length, 0);
        Log.d(TAG, "startSendData: mResult---------" + mResult);

        if (mResult > 0) {
//            startReceiveData();
        }
        Toast.makeText(MainActivity.this, "begin sending data....", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbPermissionReceiver);
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

                    // 判断device是否已经获得权限
//                    if (device.getProductId() == 13824 && device.getVendorId() == 5118) {
//                    if (device.getProductId() == 0xa4a0 && device.getVendorId() == 0x054c) {
                    if (0x0525 == device.getVendorId() && 0xa4a5 == device.getProductId()) {

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
//        if (1234 == device.getVendorId() && 5678 == device.getProductId()) {
//        if (0x054c == device.getVendorId() && 0xa4a0 == device.getProductId()) {
        if (0x0525 == device.getVendorId() && 0xa4a5 == device.getProductId()) {
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
                            Toast.makeText(this, "open设备成功", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "initCommunication: open设备成功");
                            // TODO: 2017/7/17  开始接收数据并显示

                            mVideoDecoder.setUsbDeviceConnection(mUsbDeviceConnection);
                            mVideoDecoder.setUsbEndpoint(mUsbEndpointIn);
                            mVideoDecoder.startDecode(mSurfaceView.getHolder());

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
