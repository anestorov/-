package com.oldbg.bulgariankeyboard;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.UserDictionary;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextServicesManager;

import com.oldbg.bulgariankeyboard.oldbg.customkeyboard.processors.draw.CandidateViewColorChanger;
import com.oldbg.bulgariankeyboard.oldbg.customkeyboard.processors.draw.KeyFinder;
import com.oldbg.bulgariankeyboard.oldbg.customkeyboard.processors.draw.KeyPainter;
import com.orm.SugarContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


/**
 * Example of writing an input method for a soft keyboard.  This code is
 * focused on simplicity over completeness, so it should in no way be considered
 * to be a complete soft keyboard implementation.  Its purpose is to provide
 * a basic example for how you would get started writing an input method, to
 * be fleshed out as appropriate.
 */
public class BulgarianRetroKeyboard extends InputMethodService
        implements KeyboardView.OnKeyboardActionListener, SpellCheckerSession.SpellCheckerSessionListener {
    static final boolean DEBUG = false;

    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    static final boolean PROCESS_HARD_KEYS = false;

    private InputMethodManager mInputMethodManager;

    private KeyboardInputView mInputView;
    public CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;

    public static StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;


    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;

    private XMLtoKeyboard mSymbolsKeyboard;
    private XMLtoKeyboard mSymbolsShiftedKeyboard;
    private XMLtoKeyboard mQwertyKeyboard;
    private XMLtoKeyboard mNumericKeyboard;
    private XMLtoKeyboard mNumericKeyboardSymbols;

    private XMLtoKeyboard mCurKeyboard;

    private String mWordSeparators;

    private SpellCheckerSession mScs;
    public static List<String> mSuggestions;
    private KeyPainter painter;
    private int shiftPress = 0;
    private InputMethodManager imm;

    //Anti pattern
    public long candidateViewTimer=0;
    private boolean isAndroidDict= false;
    private boolean converter_on;

    private Converter converter;
    private Vibrator vibrator;

    private Locale bgLocale;
    private ContentResolver resolver;

    //колоните от таблицата на речника и запитването за взимане на предложения за думи
    private String[] columns;
    private String condition;

    //колоните от таблицата на речника за взимане на дума с честотата ѝ
    private String[] columnsWithFreq;

    private boolean keyIsReleased;

    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        //Activate Debug
        //android.os.Debug.waitForDebugger();  // this line is keyf
        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
        final TextServicesManager tsm = (TextServicesManager) getSystemService(
                Context.TEXT_SERVICES_MANAGER_SERVICE);
        bgLocale = Locale.getDefault();
        mScs = tsm.newSpellCheckerSession(null, bgLocale, this, false);
        this.painter = new KeyPainter(getApplicationContext());
        imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        converter = new Converter();
        this.converter_on = true;
        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        resolver = getContentResolver();

        //търсене на речника за предложения
        columns = new String[]{UserDictionary.Words._ID, UserDictionary.Words.WORD};
        condition = UserDictionary.Words.WORD  + " LIKE ? ";

        //търсене на речника с дадената честота на всяка дума
        columnsWithFreq = new String[]{UserDictionary.Words._ID, UserDictionary.Words.WORD, UserDictionary.Words.FREQUENCY};

        keyIsReleased = true;
    }

    private String keyboard_layout;
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        Object keyboard_layout = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getAll().get("keyboard_layout");
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth && (keyboard_layout== null || this.keyboard_layout.equals(keyboard_layout.toString()))) return;
            mLastDisplayWidth = displayWidth;
        }

        if(keyboard_layout != null){

            if(keyboard_layout.toString().equals("bds")){
                mQwertyKeyboard = new XMLtoKeyboard(this, R.xml.bds_keyboard);
            }else{
                mQwertyKeyboard = new XMLtoKeyboard(this, R.xml.qwerty);
            }
            this.keyboard_layout = keyboard_layout.toString();
        }else{
            this.keyboard_layout = "phonetic";
            mQwertyKeyboard = new XMLtoKeyboard(this, R.xml.qwerty);
        }

        mSymbolsKeyboard = new XMLtoKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new XMLtoKeyboard(this, R.xml.symbols_shift);
        mNumericKeyboard = new XMLtoKeyboard(this,R.xml.numeric_keyboard);
        mNumericKeyboardSymbols = new XMLtoKeyboard(this,R.xml.numeric_keyboard_symbols);
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
        mInputView = (KeyboardInputView) getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setIc(getCurrentInputConnection());
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setPreviewEnabled(false);
        setLanguageSwitchKey(mQwertyKeyboard);
        return mInputView;
    }

    private void setLanguageSwitchKey(XMLtoKeyboard keyboard) {
        final boolean shouldSupportLanguageSwitchKey =
                mInputMethodManager.shouldOfferSwitchingToNextInputMethod(getToken());
        keyboard.setLanguageSwitchKeyVisibility(shouldSupportLanguageSwitchKey);
    }

    /**
     * Switch keyboard with other keyboard
     */
    private void  changeKeyborad(XMLtoKeyboard keyboard){
        mPredictionOn = false;
        setCandidatesViewShown(mPredictionOn);
        mCurKeyboard = keyboard;

        if(keyboard == mQwertyKeyboard){
            Object word_suggest = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getAll().get("word_suggest");
            if (word_suggest == null ||  Boolean.parseBoolean(word_suggest.toString())) mPredictionOn =true;
            Object converter_on = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getAll().get("converter_on");
            if (converter_on !=null) this.converter_on = Boolean.parseBoolean(converter_on.toString());
        }
        Log.d("keyboard is",String.valueOf(keyboard));
        Log.d("mInput keyboard is",String.valueOf(mInputView));

        if (mInputView != null) {
            mInputView.closing();
            mInputView.setKeyboard(keyboard);
        }


    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        // WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        Log.d("CREATE","CREATE CANDIDATE");
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        Object keyboard_layout = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getAll().get("keyboard_layout");
        if(keyboard_layout != null && this.keyboard_layout!=null && !this.keyboard_layout.equals(keyboard_layout.toString())) {
            onInitializeInterface();
            setInputView(onCreateInputView());
        }

        super.onStartInput(attribute, restarting);

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates();

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                Log.d("mSybolKeyboard is","turn on");
                changeKeyborad(mSymbolsKeyboard);
                break;

            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                changeKeyborad(mNumericKeyboard);
                mPredictionOn=false;
                break;

            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                changeKeyborad(mQwertyKeyboard);

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                changeKeyborad(mQwertyKeyboard);
                updateShiftKeyState(attribute);
        }
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }


    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();

        // Clear current composing text and candidates.
        updateCandidates();
        mComposing.setLength(0);

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

        changeKeyborad(mQwertyKeyboard);
    }


    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        setLanguageSwitchKey(mQwertyKeyboard);
        setLanguageSwitchKey(mSymbolsKeyboard);
        setLanguageSwitchKey(mSymbolsShiftedKeyboard);
        changeKeyborad(mCurKeyboard);
        mInputView.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
        if(!mInputView.isShifted()){
            mInputView.setShifted(true);
        }
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        if(subtype != null && mInputView !=null){
            mInputView.setSubtypeOnSpaceKey(subtype);
        }

    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                            int newSelStart, int newSelEnd,
                                            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        // този if го играе onLongPress event handler...
        if (!keyIsReleased) {
            if(mComposing.length()>0 && mInputView.getKeyboard() == mQwertyKeyboard) {
                getCurrentInputConnection().setComposingText(mComposing, 1);
                updateCandidates();
                updateShiftKeyState(getCurrentInputEditorInfo());
                //mInputView.setShifted(mCapsLock);
            }
            keyIsReleased = true;
        }

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < completions.length; i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            Log.i("stringList ",stringList.toString());
            setSuggestions(stringList, false, false);
        }
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     * Smile icon
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() -1 );
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length()-1);
            }
        }

        onKey(c, null);

        return true;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DEL:

                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;
            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                /*
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState()&KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A);
                            keyDownUp(KeyEvent.KEYCODE_N);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            keyDownUp(KeyEvent.KEYCODE_R);
                            keyDownUp(KeyEvent.KEYCODE_O);
                            keyDownUp(KeyEvent.KEYCODE_I);
                            keyDownUp(KeyEvent.KEYCODE_D);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }*/
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    public void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            //добавяне на думата в речника
            if(mPredictionOn) {
                String word = mComposing.toString().replaceAll("\\s+", "");
                if (word.length() > 1) {
                    if ((".,!?").contains(word.substring(word.length() - 1)))
                        word = word.substring(0, word.length() - 1);
                    updateUserDict(word);
                }
            }
            inputConnection.commitText(mComposing, 1);
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            painter.redrawIcon(mQwertyKeyboard.getmShiftKey(),R.drawable.sym_keyboard_shift_pressed);
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
            if(!mInputView.isShifted()){
                painter.redrawIcon(mQwertyKeyboard.getmShiftKey(),R.drawable.sym_keyboard_shift_normal);
            }
        }
    }

    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {

        Keyboard.Key key = KeyFinder.findKey(mQwertyKeyboard,keyCode);

        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                }else{
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {
        Log.d("Test","KEYCODE: " + primaryCode);
        Keyboard current = mInputView.getKeyboard();

        //SPACE BUTTON IS PRESSED
        if(primaryCode==32){
            mComposing.append(" ");
            commitTyped(getCurrentInputConnection());
            if (mInputView != null && (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard)) {
                setLanguageSwitchKey(mQwertyKeyboard);
                changeKeyborad(mQwertyKeyboard);
                mInputView.setShifted(mCapsLock);
                if(shouldShift(getLastTypedChar())) mInputView.setShifted(true);
            }
            return;
        }

        if(current==mNumericKeyboardSymbols && primaryCode == -123){
            changeKeyborad(mNumericKeyboard);
            return;
        }

//        if (primaryCode == 42) {
//            //Handle numeric keybooard
//            if(current== mNumericKeyboard) {
//                Log.d("Current keyborad is ", "mNumericKeyboard");
//                changeKeyborad(mNumericKeyboardSymbols);
//                return;
//            }
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == KeyboardInputView.KEYCODE_LANGUAGE_SWITCH) {
            handleLanguageSwitch();
            return;
        } else if (primaryCode == KeyboardInputView.KEYCODE_OPTIONS) {
            // Show a menu or something'
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            if (current == mSymbolsKeyboard || current == mSymbolsShiftedKeyboard) {
                setLanguageSwitchKey(mQwertyKeyboard);
                changeKeyborad(mQwertyKeyboard);
                mInputView.setShifted(mCapsLock);
                if(shouldShift(getLastTypedChar())) mInputView.setShifted(true);
            } else {
                setLanguageSwitchKey(mSymbolsKeyboard);
                changeKeyborad(mSymbolsKeyboard);
                mSymbolsKeyboard.setShifted(false);
            }
        }else if (primaryCode == 10){
            keyDownUp(KeyEvent.KEYCODE_ENTER);
        } else {
            handleCharacter(primaryCode);
        }

        if(isWordSeparator(primaryCode) && primaryCode != 10){
            // Handle separator
            if ((mComposing.length() > 0 || mInputView.isLongPress)) {
                commitTyped(getCurrentInputConnection());
                updateShiftKeyState(getCurrentInputEditorInfo());
            }
            if(shouldShift((char)primaryCode)) mInputView.setShifted(true);
        }

    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Трябва ли да е с главна буква след даден знак с
     * @param c
     * @return
     */
    private boolean shouldShift(char c){
      return  ".?!".contains(String.valueOf(c));
    }

    /**
     *
     * @return последния въведен знак, който не е интервал
     */
    private char getLastTypedChar(){
        String lastChars = (String) getCurrentInputConnection().getTextBeforeCursor(2,0);
        if(lastChars != null && lastChars.length()>0) {
            if (lastChars.length() == 2 && lastChars.charAt(1) != ' ') {
                return lastChars.charAt(1);
            } else {
                return lastChars.charAt(0);
            }
        }
        return '0';
    }


    /**
     * Тъй като при някои телефони, обектът mScs (SpellCheckerSession) оставаше null, лентата с предложенията не се показваше
     * Затова този метод съставя списък за лентата с предложения без да използва SpellCheckerSession обекта,
     *      а чрез пряко запитване към личния речник (UserDictionary) на потребителя
     * След това този списък минава през конвертора и така се създава лентата с предложения
     */
    private void tempCandidate(List<String> sb, String input){
        CandidateViewColorChanger changer = CandidateViewColorChanger.getInstance(mCandidateView);

        changer.changeColor(Color.BLACK);
        Log.i("Standart","Dictionary");
        //changer.changeColorTheme();

        // ? in condition will be replaced by `args` in order.
        String[] args = {input +"%"};

        Cursor cursor = resolver.query(UserDictionary.Words.CONTENT_URI, columns, condition, args, null);
        //Cursor cursor = resolver.query(UserDictionary.Words.CONTENT_URI, projection, null, null, null); - get all words from dictionary
        if ( cursor != null ) {
            int index = cursor.getColumnIndex(UserDictionary.Words.WORD);
            //iterate over all words found
            while (cursor.moveToNext()) {
                //gets the value from the column.
                String suggestion = cursor.getString(index);
                //only unique suggestions
                if(!sb.contains(suggestion) && !suggestion.equals(input)) {
                    sb.add(suggestion);
                }
                if(sb.size() >3) break;
            }
            cursor.close();
        }



        //Ако я няма думата в речника
        if(!sb.contains(input)) sb.add(input);

    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        try {
            if (!mCompletionOn && mPredictionOn) {
                if (mComposing.length() > 0) {
                    Log.i("BulgarianRetroKeyboard", "REQUESTING: " + mComposing.toString());
                    //Долният ред е за SpellCheckerSession обекта, който не се използва, тъй като и без това не намира предложения
                    //mScs.getSentenceSuggestions(new TextInfo[]{new TextInfo(mComposing.toString())}, 5);
                    //Следващите редове са за създаване на лента с предложения с конвертирани думи
                    String word = mComposing.toString().replaceAll("\\s+","");
                    final List<String> sb = new ArrayList<String>();
                    tempCandidate(sb,word);
                    List<String> newSb;
                    if(converter_on) {
                        converter.setWords(sb.toArray(new String[sb.size()]));
                        converter.checkWords();
                        newSb = converter.getWords();
                        if(!newSb.contains(word)) newSb.add(word);
                    }else{
                        newSb =  sb;
                    }

                    setSuggestions(newSb,true,true);
                }else{
                   setSuggestions(new ArrayList<String>(Arrays.asList("")), true, true);
                }

                //setSuggestions(new ArrayList<String>(), true, true);
            }
        } catch(NullPointerException e){
        }

    }
    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
           // changeKeyborad(new XMLtoKeyboard(this,R.xml.suggestion_keyboard));

            setCandidatesViewShown(true);
            mCandidateView.requestLayout();

        } else if (isExtractViewShown()) {
            setCandidatesViewShown(false);
        }
        mSuggestions  = suggestions;
        if (mCandidateView != null && suggestions != null) {
            Log.i("suggestions ",suggestions.toString());
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());

    }

    private void handleShift() {

        if (mInputView == null) {
            return;
        }

        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            setLanguageSwitchKey(mSymbolsShiftedKeyboard);
            changeKeyborad(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            setLanguageSwitchKey(mSymbolsKeyboard);
            changeKeyborad(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }

    private void handleCharacter(int primaryCode) {
        String toSend = String.valueOf((char) primaryCode);
        Log.d("TO SEND",toSend);
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                toSend = toSend.toUpperCase();
            }
        }
        if (mPredictionOn || mInputView.getKeyboard() == mQwertyKeyboard) {
            mComposing.append(toSend);

            getCurrentInputConnection().setComposingText(mComposing,1);
            updateCandidates();
            updateShiftKeyState(getCurrentInputEditorInfo());

        } else {
            Log.d("BulgarianRetroKeyboard","Commit text");
            mComposing.append(toSend);
            getCurrentInputConnection().setComposingText(mComposing,1);
           // getCurrentInputConnection().commitText(String.valueOf(toSend), 1);
        }

        String word = "";
        if(mComposing != null && mComposing.toString().length()>1){
            word = mComposing.toString();
        }
        //mInputView.setShift(primaryCode == -1);
        mInputView.setInputConnectionAndWord(getCurrentInputConnection(),word);
    }
    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    private void handleLanguageSwitch() {
        mInputMethodManager.switchToNextInputMethod(getToken(), false /* onlyCurrentIme */);
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mInputView.isShifted()) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }

    }

    private String getWordSeparators() {
        return mWordSeparators;
    }

    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }

    private boolean isExclusionSeparator(int code){
        String separators = getResources().getString(R.string.exclusion_separators);
        return separators.contains(String.valueOf((char)code));
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {

            if (mPredictionOn && mSuggestions != null && index >= 0) {
                mComposing.replace(0, mComposing.length(), mSuggestions.get(index)+" ");
                if(candidateViewTimer >= 500) {
                    candidateViewTimer = 0;
                }
            }

            commitTyped(getCurrentInputConnection());

        }
    }

    public void swipeRight() {
        Log.d("BulgarianRetroKeyboard", "Swipe right");
        if (mCompletionOn || mPredictionOn) {
            pickDefaultCandidate();
        }
    }

    public void swipeLeft() {
        Log.d("BulgarianRetroKeyboard", "Swipe left");
        handleBackspace();
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    //TODO: Change shift  button icon
    public void onPress(final int primaryCode) {
        keyIsReleased = false;
        if(primaryCode == -1 && !mInputView.isShifted()){
            painter.redrawIcon(mQwertyKeyboard.getmShiftKey(),R.drawable.sym_keyboard_shift_pressed);
        }

        if(mInputView.isShifted()) {
            painter.redrawIcon(mQwertyKeyboard.getmShiftKey(), R.drawable.sym_keyboard_shift_normal);
        }


        Object vibrate_on = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getAll().get("vibrate_on");
        if(vibrate_on != null){
            if(Boolean.parseBoolean(vibrate_on.toString())){

                vibrator.vibrate(27);
            }
        }

    }

    public void onRelease(int primaryCode) {
        keyIsReleased = true;
    }

    /**
     * http://www.tutorialspoint.com/android/android_spelling_checker.htm
     * @param results results
     */
    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < results.length; ++i) {
            // Returned suggestions are contained in SuggestionsInfo
            final int len = results[i].getSuggestionsCount();
            sb.append('\n');

            for (int j = 0; j < len; ++j) {
                sb.append("," + results[i].getSuggestionAt(j));
            }

            sb.append(" (" + len + ")");
        }
        Log.d("BulgarianRetroKeyboard", "SUGGESTIONS: " + sb.toString());
    }
    private static final int NOT_A_LENGTH = -1;

    //Add single word in suggestions
    private void dumpSuggestionsInfoInternal(
            final List<String> sb, final SuggestionsInfo si, final int length, final int offset) {


        CandidateViewColorChanger changer = CandidateViewColorChanger.getInstance(mCandidateView);


        Log.i("Standart","Dictionary");
        changer.changeColorTheme();
        for (int i = 0; i < si.getSuggestionsCount(); i++) {
            String word = si.getSuggestionAt(i);

            //only unique suggestions
            if(!sb.contains(word) && !word.equals(mComposing.toString())) {
                sb.add(word);
            }
        }

        //Word not exists in both dictionary
        if(sb.size()==0 && !mComposing.toString().isEmpty()) {
            changer.changeColor(Color.BLUE);
            sb.add(mComposing.toString());
        }

    }


    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        Log.d("BulgarianRetroKeyboard", "onGetSentenceSuggestions");
        final List<String> sb = new ArrayList<String>();
        for (int i = 0; i < results.length; ++i) {

            final SentenceSuggestionsInfo ssi = results[i];
            if (ssi != null) {
                for (int j = 0; j < ssi.getSuggestionsCount(); ++j) {
                    dumpSuggestionsInfoInternal(
                            sb, ssi.getSuggestionsInfoAt(j), ssi.getOffsetAt(j), ssi.getLengthAt(j));
                }
            }
        }

        converter.setWords(sb.toArray(new String[sb.size()]));
        converter.checkWords();
        List<String> newSb = converter.getWords();
        newSb.add(mComposing.toString());
        Log.i("BulgarianRetroKeyboard", "SUGGESTIONS: " + newSb.toString());
         if (newSb.size() > 0){
            setSuggestions(newSb, true, true);
        }else{
            setSuggestions(mSuggestions,true,true);
        }

    }

    @Override
    public void onDestroy() {
        SugarContext.terminate();
        super.onDestroy();
    }

    @Override public void onComputeInsets(InputMethodService.Insets outInsets) {
        super.onComputeInsets(outInsets);
        if (!isFullscreenMode()) {
            outInsets.contentTopInsets = outInsets.visibleTopInsets;
        }
    }

    /**
     * Поставя дадената дума в потребителския речник (UserDictionary) или ако вече е там, ѝ увеличава честотата
     * @param word
     */
    private void updateUserDict(String word){
        int updated = 0;
        // ? in condition will be replaced by `args` in order.
        String[] args = {word};
        Cursor cursor = resolver.query(UserDictionary.Words.CONTENT_URI, columnsWithFreq, UserDictionary.Words.WORD + " = ? ", args, null);
        if (cursor != null) {
            int index = cursor.getColumnIndex(UserDictionary.Words.FREQUENCY);
            //iterate over all words found
            while (cursor.moveToNext()) {
                //gets the value from the column.
                int wordFreq = Integer.parseInt(cursor.getString(index));
                if (wordFreq >= 255) {
                    cursor.close();
                    return;
                }
                ContentValues newFreq = new ContentValues();
                newFreq.put(UserDictionary.Words.FREQUENCY, ++wordFreq);
                updated = resolver.update(UserDictionary.Words.CONTENT_URI, newFreq, condition, args);

                break;
            }
            cursor.close();
        }

        if (updated == 0) {
            UserDictionary.Words.addWord(this, word, 10, null, bgLocale);
        }
    }

}

