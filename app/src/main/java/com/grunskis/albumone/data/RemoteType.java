package com.grunskis.albumone.data;

import java.util.HashMap;
import java.util.Map;

public enum RemoteType {
    GOOGLE_PHOTOS(1),
    UNSPLASH(2);

    private static final Map<Integer, RemoteType> typesByValue = new HashMap<>();

    static {
        for (RemoteType type : RemoteType.values()) {
            typesByValue.put(type.value, type);
        }
    }

    private int value;

    RemoteType(int value) {
        this.value = value;
    }

    public static RemoteType forValue(int value) {
        return typesByValue.get(value);
    }

    public int getValue() {
        return value;
    }
}
