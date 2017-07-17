package com.qinshou.usbdemo;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class VideoDecoder {
    private static final String TAG = "VideoDecoder";

    private static final int EMPTY_VIDEO_FRAME_WAIT_MS = 1;
    MediaCodec codec;
    DecodeThread mDecodeThread;
    MediaCodec.BufferInfo mBufferInfo;

    public void startDecode(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "startDecode: ...........");
        if (surfaceHolder == null) {
            throw new RuntimeException("surfaceHolder is null...");
        }

        try {
            initMediaCodec(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mDecodeThread = new DecodeThread();
        mDecodeThread.start();

    }

    public void stop() {
        if (mDecodeThread != null) {
            mDecodeThread.exitLoop();
        }
    }

    private void initMediaCodec(SurfaceHolder surfaceHolder) throws IOException {
        Log.d(TAG, "initMediaCodec: .............");
        mBufferInfo = new MediaCodec.BufferInfo();

        int mWidth = 800;
        int mHeight = 480;

        // 编码器那边会先发sps和pps来，头一帧就由sps和pps组成
        MediaFormat format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, mHeight * mWidth);
        format.setInteger(MediaFormat.KEY_MAX_HEIGHT, mHeight);
        format.setInteger(MediaFormat.KEY_MAX_WIDTH, mWidth);

        codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        codec.configure(format, surfaceHolder.getSurface(), null, 0);
    }


    int timeoutUs = 10000;

    class DecodeThread extends Thread {
        private boolean mExit;

        public void exitLoop() {
            mExit = true;
        }

        byte[] buffer = new byte[512];

        @Override
        public void run() {
            Log.i(TAG, "DecodeThread begin run...");
            codec.start();
            int inputBufferId = -1;
            boolean lastHasData = true;
            ByteBuffer[] inputBuffers = codec.getInputBuffers();
            VideoData videoData = null;
            while (!mExit) {
                if (lastHasData) {
                    inputBufferId = codec.dequeueInputBuffer(timeoutUs);
                }
                Log.i(TAG, "inputBufferId:" + inputBufferId);
                if (inputBufferId >= 0) {
                    // TODO: 2017/7/17  同步接收videoData
                    int videoDataLength = 0;
                    int total = 0;
                    while (true) {
                        Log.d(TAG, "run .....receiving videodata........... ");
                        // c. 解析frame请求，获取类型，长度

                        int receiveResult = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, buffer, buffer.length, 0);
                        Log.d(TAG, "run: receiveResult......" + receiveResult);  // receiveResult......512

                        if (receiveResult > 0) {

                            total = total + receiveResult;
                            Log.d(TAG, "run: total....." + total);
                            if (total == 512) {
//                            CACHETYPE:VIDEO#TYPE:H264#LEN:XXXX#FRAMENUM:xxxx#000000		----发送video
                                String receiveData = new String(buffer);
                                Log.d(TAG, "run: receiveData----" + receiveData);
                                String[] spilts = receiveData.split("#");
                                String lenStr = (spilts[2].split(":"))[1];
                                Log.d(TAG, "run: lenStr......" + lenStr);
                                videoDataLength = Integer.parseInt(lenStr);  // 768000

                                break;
                            }

                        } else {
                            Log.d(TAG, "run: receiveResult---" + receiveResult);
                            break;
                        }
                    }

                    int readedLen = 0;
                    Log.d(TAG, "run: videoDataLength..." + videoDataLength);  // run: videoDataLength...768000
                    int readPerLength = 16 * 1024;        //  每次读到 16k
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while (true) {
                        byte[] bytes;
                        int unreadLength = videoDataLength - readedLen;
                        Log.d(TAG, "loopToGetData...unreadLength " + unreadLength);
                        if (unreadLength > readPerLength) {
                            bytes = new byte[readPerLength];
                        } else {
                            bytes = new byte[unreadLength];
                        }
                        int currLength = mUsbDeviceConnection.bulkTransfer(mUsbEndpointIn, bytes, bytes.length, 0);

                        if (currLength > 0) {
                            Log.d(TAG, "loopToGetData...1.readedLen:" + readedLen + ",currLength" + currLength);
                            baos.write(bytes, 0, currLength);
                            readedLen = readedLen + currLength;
                            Log.d(TAG, "loopToGetData...2.readedLen:" + readedLen + ",currLength" + currLength);
                        }

                        if (currLength <= 0) {
                            Log.d(TAG, "loopToGetData...complete...currLength:" + currLength + ",readedLen:" + readedLen + ",videoDataLength" + videoDataLength);
                            break;
                        }

                        if (readedLen == videoDataLength) {
                            Log.d(TAG, "loopToGetData...complete...currLength:" + currLength + ",readedLen:" + readedLen + ",videoDataLength" + videoDataLength);
                            byte[] finalBytes = baos.toByteArray();
                            try {
                                baos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            // TODO: 2017/7/15  SurfaceView显示数据
                            videoData = new VideoData(finalBytes, finalBytes.length);

                            Log.d(TAG, "loopToGetData...finalGetData:" + finalBytes.length);
                            break;
                        }

                    }


                    if (videoData.length > 0) {
                        ByteBuffer inputBuffer = getBuffer(inputBuffers, inputBufferId);
                        inputBuffer.clear();
                        inputBuffer.put(videoData.v_data, 0, videoData.length);
                        inputBuffer.clear();
                        inputBuffer.limit(videoData.length);
                        codec.queueInputBuffer(inputBufferId, 0, videoData.length, 0, MediaCodec.BUFFER_FLAG_KEY_FRAME);
                        lastHasData = true;
                    }
                    if (videoData.length < 0) {
                        codec.queueInputBuffer(inputBufferId, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        mExit = true;
                        break;
                    }
                    if (videoData.length == 0) {
                        lastHasData = false;
                        try {
                            synchronized (this) {
                                wait(EMPTY_VIDEO_FRAME_WAIT_MS);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                }

                // 释放资源
                int outIndex = codec.dequeueOutputBuffer(mBufferInfo, timeoutUs);
                try {
                    while (outIndex >= 0 && !mExit) {
                        //退出surfaceView 解码数据 IllegalStateException
                        codec.releaseOutputBuffer(outIndex, true);
                        // 再次获取数据，如果没有数据输出则outIndex=-1
                        outIndex = codec.dequeueOutputBuffer(mBufferInfo, timeoutUs);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "release codec..");
            codec.stop();
            codec.release();
            Log.i(TAG, "release codec..end");
        }

        private ByteBuffer getBuffer(ByteBuffer[] inputBuffers, int inputBufferId) {
            ByteBuffer inputBuffer;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                inputBuffer = codec.getInputBuffer(inputBufferId);
            } else {
                inputBuffer = inputBuffers[inputBufferId];
            }
            return inputBuffer;
        }

    }

    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbEndpoint mUsbEndpointIn;


    public void setUsbDeviceConnection(UsbDeviceConnection usbDeviceConnection) {
        this.mUsbDeviceConnection = usbDeviceConnection;
    }

    public void setUsbEndpoint(UsbEndpoint usbEndpointIn) {
        this.mUsbEndpointIn = usbEndpointIn;
    }

}
