package com.qinshou.usbdemo;

import java.util.Arrays;

/**
 * Created by shuozhe on 17/5/11 23:39
 * email shuozhe_cn@163.com
 */
public class VideoData {

    public byte[] v_data = new byte[1024 * 1024]; //接收视频流数据的容器
    public byte[] v_type = new byte[8];  //接收视频流类型的容器
    public int length;

    public VideoData(byte[] v_data, int length) {
        this.v_data = v_data;
        this.length = length;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VideoData{");
        sb.append(", v_type=").append(Arrays.toString(v_type));
        sb.append(", length=").append(length);
        sb.append('}');
        return sb.toString();
    }
}
