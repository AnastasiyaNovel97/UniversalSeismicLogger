package com.example.universalseismiclogger.shared;

import android.text.TextUtils;
import android.util.Log;

import java.util.Objects;

public interface ITraceable extends ITagged{
    default void  trace(String message) { trace(message, new Object[]{});}

    default void  trace(String message, Object... args){
        if(TextUtils.isEmpty(message)){
            throw new NullPointerException("Message cannot be null");
        }
        Log.i(getTag(), String.format(message, args));
    }
}
