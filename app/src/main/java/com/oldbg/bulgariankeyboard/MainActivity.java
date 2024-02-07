package com.oldbg.bulgariankeyboard;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button openSettings = (Button) findViewById(R.id.open_settings);
        openSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivityForResult(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS), 0);
            }
        });

        final Button selectKeyboard = (Button) findViewById(R.id.select_keyboard);
        selectKeyboard.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                InputMethodManager mgr =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (mgr != null) {
                    mgr.showInputMethodPicker();
                }
            }
        });


        final Button keyboardSettings = (Button) findViewById(R.id.keyboard_settings);
        keyboardSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ImePreferences.class));
            }
        });

        final Button moreInfo = (Button) findViewById(R.id.more_info);
        moreInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(getOpenFacebookIntent(getApplicationContext()));
            }
        });

    }


    public static Intent getOpenFacebookIntent(Context context) {

        try {
            context.getPackageManager()
                    .getPackageInfo("com.facebook.katana", 0); //Checks if FB is even installed.
            return new Intent(Intent.ACTION_VIEW,
                    Uri.parse("fb://facewebmodal/f?href=https://www.facebook.com/starpravopis/")); //Trys to make intent with FB's URI
        } catch (Exception e) {
            return new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.facebook.com/starpravopis")); //catches and opens a url to the desired page
        }
    }


}
