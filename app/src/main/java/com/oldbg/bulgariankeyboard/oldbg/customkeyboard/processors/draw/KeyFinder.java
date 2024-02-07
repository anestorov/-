package com.oldbg.bulgariankeyboard.oldbg.customkeyboard.processors.draw;

import android.inputmethodservice.Keyboard;
import android.support.annotation.Nullable;

/**
 * Created by rado_sz on 19.06.16.
 */
public class KeyFinder {

    @Nullable
    public static Keyboard.Key findKey(Keyboard keyboard, int keyCode){
        for(Keyboard.Key key : keyboard.getKeys()){
            if(key.codes[0]==keyCode){
                return key;
            }
        }
        return null;
    }
}
