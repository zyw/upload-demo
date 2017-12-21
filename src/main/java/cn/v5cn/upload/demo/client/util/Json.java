package cn.v5cn.upload.demo.client.util;

import cn.v5cn.upload.demo.client.StringMap;
import com.alibaba.fastjson.JSON;

import java.util.Map;

public final class Json {
    private Json() {
    }

    public static String encode(StringMap map) {
        return JSON.toJSONString(map.map());
    }

    public static String encode(Object obj) {
        return JSON.toJSONString(obj);
    }

    public static <T> T decode(String json, Class<T> classOfT) {
        return JSON.parseObject(json,classOfT);
    }

    public static StringMap decode(String json) {
        Map<String, Object> x = JSON.parseObject(json,Map.class);
        return new StringMap(x);
    }
}
