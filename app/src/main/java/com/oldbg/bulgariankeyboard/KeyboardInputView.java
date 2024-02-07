/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oldbg.bulgariankeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.List;

//Pop up keyboard + BulgarianKeybord in one View
public class KeyboardInputView extends KeyboardView {

    static final int KEYCODE_OPTIONS = -100;
    static final int KEYCODE_LANGUAGE_SWITCH = -101;
    private InputMethodManager mgr;
    private char mComposingChar;

    public boolean isLongPress = false;
    private InputConnection ic;
    private String word;

    private int width;
    private int height;

    private String keyboard_layout;

    public KeyboardInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mgr = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        Object keyboard_layout = PreferenceManager.getDefaultSharedPreferences(context).getAll().get("keyboard_layout");
        if(keyboard_layout!=null){
            this.keyboard_layout = keyboard_layout.toString();
        }else{
            this.keyboard_layout = "phonetic";
        }

    }

    public void setLongPress(boolean longPress) {
        isLongPress = longPress;
    }

    public char getmComposingChar() {
        return mComposingChar;
    }

    public void setInputConnectionAndWord(InputConnection ic,String word) {
        this.ic = ic;
        this.word=word;
    }

    public void setIc(InputConnection ic){
        this.ic = ic;
    }

    public KeyboardInputView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;
        Object keyboard_layout = PreferenceManager.getDefaultSharedPreferences(context).getAll().get("keyboard_layout");
        if(keyboard_layout!=null){
            this.keyboard_layout = keyboard_layout.toString();
        }else{
            this.keyboard_layout = "phonetic";
        }
    }



    //long  press logic
    protected boolean onLongPress(Key key) {
        CharSequence cs = key.popupCharacters;

        isLongPress = false;

        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;

        }else if(key.codes[0] == 48){
            try {
                if (key.codes[1] == 43) {
                    isLongPress = true;
                    BulgarianRetroKeyboard.mComposing.append((char) key.codes[1]);
                    ic.commitText(BulgarianRetroKeyboard.mComposing, 1);
                    return true;
                }
            }catch (ArrayIndexOutOfBoundsException e){
                //key.codes[1] in not exists
            }
        }
        else  if(key.codes[0] == 32){
            if (mgr != null) {
                mgr.showInputMethodPicker();
            }
            return true;
        }else if(null != cs && cs.length()>=2) {
            return super.onLongPress(key);
        }
        else if(cs != null && cs.length()>=1){
            if(super.isShifted()){
                mComposingChar = String.valueOf(cs.charAt(0)).toUpperCase().charAt(0);
            }else{
                mComposingChar = cs.charAt(0);
            }

            BulgarianRetroKeyboard.mComposing.append(mComposingChar);
            String text = BulgarianRetroKeyboard.mComposing.toString();
            ic.setComposingText(text,text.length());
            isLongPress = true;
            this.word ="";
            return true;

        }else if(key.label != null){
            if(!super.isShifted()){
                mComposingChar = String.valueOf(key.label).toUpperCase().charAt(0);
            }else{
                mComposingChar = key.label.charAt(0);
            }

            BulgarianRetroKeyboard.mComposing.append(mComposingChar);
            String text = BulgarianRetroKeyboard.mComposing.toString();
            ic.setComposingText(text,text.length());
            isLongPress = true;
            this.word ="";
            return true;

        }
        isLongPress = false;
        return false;
    }

    void setSubtypeOnSpaceKey(final InputMethodSubtype subtype) {
        final XMLtoKeyboard keyboard = (XMLtoKeyboard)getKeyboard();
        //keyboard.setSpaceIcon(getResources().getDrawable(subtype.getIconResId()));
        invalidateAllKeys();
    }


    @Override
    public void onDraw(Canvas canvas) {
        //Draw original  qwerty keyboard
        super.onDraw(canvas);
        Paint spaceColor = new Paint();
        spaceColor.setColor(ContextCompat.getColor(getContext(),R.color.space_button));

        List<Key> keys = getKeyboard().getKeys();
        for(Key key: keys) {
            if(key.label != null) {
                drawSpecialKeys(canvas,key);
            }else if(key.codes[0]==32 && key.icon == null){
                int right = key.x+key.width;
                canvas.drawRect(key.x+5,key.y+30,right,key.height+key.y-30,spaceColor);
            }
        }
    }

    public void drawSpecialKeys(Canvas canvas,Key key){

        int w;
        int h;
        int size;

        if (width <650 && height < 1300) {
            w = 15;
            h = 30;
            size = 20;
        }else{
            w = 25;
            h = 40;
            size = 28;
        }

        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(size);
        paint.setTypeface(Typeface.SANS_SERIF);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(false);
        paint.setColor(0xFF4F4F4F);

        //w=25 h=40

        //h ->  +40 up +70 down w -> +25 right -40 left
        if (key.label.equals("ѣ")){
            canvas.drawText("ѧ", key.x + (key.width - w), key.y + h, paint);
        }else if (key.label.equals("ѫ")){
            canvas.drawText("ѭ", key.x + (key.width - w), key.y + h, paint);
        }else if (key.label.equals("й")){
            canvas.drawText("ѝ", key.x + (key.width - w), key.y + h, paint);
//        }else if (key.label.equals("а")){
//            canvas.drawText("ꙗ", key.x + (key.width - w), key.y + h, paint);
//        }else if (key.label.equals("и")){
//            canvas.drawText("ꙑ", key.x + (key.width - w), key.y + h, paint);
        }
        if(this.keyboard_layout.equals("phonetic")){
            if (key.label.equals("я")) {
                canvas.drawText("1", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("в")) {
                canvas.drawText("2", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("е")) {
                canvas.drawText("3", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("р")) {
                canvas.drawText("4", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("т")) {
                canvas.drawText("5", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("ъ")) {
                canvas.drawText("6", key.x + (key.width - w), key.y + h, paint);
            }
            else if (key.label.equals("у")) {
                canvas.drawText("7", key.x + (key.width - w), key.y + h, paint);
            }
            else if (key.label.equals("и")) {
                canvas.drawText("8", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("о")) {
                canvas.drawText("9", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("п")) {
                canvas.drawText("0", key.x + (key.width - w), key.y + h, paint);
            }
        }else{ //else bds
            if (key.label.equals("у")) {
                canvas.drawText("1", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("е")) {
                canvas.drawText("2", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("и")) {
                canvas.drawText("3", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("ш")) {
                canvas.drawText("4", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("щ")) {
                canvas.drawText("5", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("к")) {
                canvas.drawText("6", key.x + (key.width - w), key.y + h, paint);
            }
            else if (key.label.equals("с")) {
                canvas.drawText("7", key.x + (key.width - w), key.y + h, paint);
            }
            else if (key.label.equals("д")) {
                canvas.drawText("8", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("з")) {
                canvas.drawText("9", key.x + (key.width - w), key.y + h, paint);
            } else if (key.label.equals("ц")) {
                canvas.drawText("0", key.x + (key.width - w), key.y + h, paint);
            }
        }
    }
}
