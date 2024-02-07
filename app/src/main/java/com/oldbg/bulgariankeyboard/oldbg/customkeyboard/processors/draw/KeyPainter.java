package com.oldbg.bulgariankeyboard.oldbg.customkeyboard.processors.draw;

import android.content.Context;
import android.content.ContextWrapper;
import android.inputmethodservice.Keyboard;
import android.support.v4.content.ContextCompat;

/**
 * Created by rado_sz on 18.06.16.
 */
public class KeyPainter extends ContextWrapper{


    public KeyPainter(Context base) {
        super(base);
    }

    public void redrawIcon(Keyboard.Key key, int rIcon){
        key.icon = ContextCompat.getDrawable(getBaseContext(),rIcon);
    }

    public void changeLabel(Keyboard.Key key, String label){
        key.label = label;
    }

}
