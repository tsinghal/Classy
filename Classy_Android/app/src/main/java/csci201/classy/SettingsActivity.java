package csci201.classy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashSet;
import java.util.Set;

import csci201.classy.Background.NotificationService;
import csci201.classy.LoginActivity.LoginActivity;

public class SettingsActivity extends AppCompatActivity {
    public static final String E_OPEN = "e_open";
    public static final String E_CLOSED= "e_closed";
    public static final String N_OPEN = "n_open";
    public static final String N_CLOSED = "n_closed";
    NotificationService mService;
    boolean mBound = false;
    boolean guest;
    String guest_email = "";
    //get switches
    Switch filledSwitch;
    Switch openedSwitch;
    Switch e_filledSwitch;
    Switch e_openedSwitch;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    private Button logout;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NotificationService.LocalBinder binder = (NotificationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (Build.VERSION.SDK_INT > 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setBackgroundColor(Color.parseColor("#1a75ff"));
        Switch inAppFilledClassSwitch = (Switch) findViewById(R.id.inAppFilledClassSwitch);
        inAppFilledClassSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });

        mAuth = FirebaseAuth.getInstance();

        logout = (Button)findViewById(R.id.logoutButton);
        logout.setOnClickListener(new Button.OnClickListener(){

            @Override
            public void onClick(View view) {
                FirebaseUser user = mAuth.getCurrentUser();
                mAuth.signOut();
                Toast.makeText(SettingsActivity.this, "Logged Out",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        Intent intent = getIntent();
        guest = intent.getBooleanExtra("GUEST", true);
        guest_email = intent.getStringExtra("GUEST_EMAIL");
        Intent serviceIntent = new Intent(this, NotificationService.class);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = preferences.edit();

        filledSwitch = (Switch) findViewById(R.id.inAppFilledClassSwitch);
        openedSwitch = (Switch) findViewById(R.id.inAppOpenSpotSwitch);
        e_filledSwitch = (Switch) findViewById(R.id.emailFilledClassSwitch);
        e_openedSwitch = (Switch) findViewById(R.id.emailOpenSpotSwitch);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if(guest) {
            e_filledSwitch.setEnabled(false);
            e_openedSwitch.setEnabled(false);
        }
        Log.d("Settings Activity", preferences.getAll().toString());
        loadSettings();
        filledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                //if guest, commit using guest's email
                if(guest){
                    Set<String> settings = preferences.getStringSet(guest_email,new HashSet<String>());
                    Set<String> temp = new HashSet<String>();
                    for(String setting : settings) {
                        temp.add(setting);
                    }

                    if(b) {
                        temp.add(N_CLOSED);
                    } else {
                        if(temp.contains(N_CLOSED))
                            temp.remove(N_CLOSED);
                    }
                    editor.putStringSet(guest_email, temp);
                    mService.setSettings(guest_email, temp);
                    editor.apply();
                } else {
                    Set<String> settings = preferences.getStringSet(user.getUid(),new HashSet<String>());
                    Set<String> temp = new HashSet<String>();
                    for(String setting : settings) {
                        temp.add(setting);
                    }
                    if(b) {
                        temp.add(N_CLOSED);
                    } else {
                        if(temp.contains(N_CLOSED))
                            temp.remove(N_CLOSED);
                    }
                    editor.putStringSet(user.getUid(), temp);
                    mService.setSettings(user.getUid(), temp);
                    editor.apply();
                }
            }
        });
        openedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                //if guest, commit using guest's email
                if(guest){
                    Set<String> settings = preferences.getStringSet(guest_email,new HashSet<String>());
                    Set<String> temp = new HashSet<String>();
                    for(String setting : settings) {
                        temp.add(setting);
                    }

                    if(b) {
                        temp.add(N_OPEN);
                    } else {
                        if(temp.contains(N_OPEN))
                            temp.remove(N_OPEN);
                    }
                    editor.putStringSet(guest_email, temp);
                    mService.setSettings(guest_email, temp);
                    editor.apply();
                } else {
                    Set<String> settings = preferences.getStringSet(user.getUid(),new HashSet<String>());
                    Set<String> temp = new HashSet<String>();
                    for(String setting : settings) {
                        temp.add(setting);
                    }
                    if(b) {
                        temp.add(N_OPEN);
                    } else {
                        if(temp.contains(N_OPEN))
                            temp.remove(N_OPEN);
                    }
                    editor.putStringSet(user.getUid(), temp);
                    mService.setSettings(user.getUid(), temp);
                    editor.apply();
                }

            }
        });
        e_openedSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                //if guest, commit using guest's email
                if(guest){
                    Set<String> settings = preferences.getStringSet(guest_email,new HashSet<String>());
                    Set<String> temp = new HashSet<String>();
                    for(String setting : settings) {
                        temp.add(setting);
                    }

                    if(b) {
                        temp.add(E_OPEN);
                    } else {
                        if(temp.contains(E_OPEN))
                            temp.remove(E_OPEN);
                    }
                    editor.putStringSet(guest_email, temp);
                    mService.setSettings(guest_email, temp);
                    editor.apply();
                } else {
                    Set<String> settings = preferences.getStringSet(user.getUid(),new HashSet<String>());
                    Set<String> temp = new HashSet<String>();
                    for(String setting : settings) {
                        temp.add(setting);
                    }
                    if(b) {
                        temp.add(E_OPEN);
                    } else {
                        if(temp.contains(E_OPEN))
                            temp.remove(E_OPEN);
                    }
                    editor.putStringSet(user.getUid(), temp);
                    mService.setSettings(user.getUid(), temp);
                    editor.apply();
                }

            }
        });
        filledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                //if guest, commit using guest's email
                if(guest){
                    Set<String> settings = preferences.getStringSet(guest_email,new HashSet<String>());
                    Set<String> temp = new HashSet<String>();
                    for(String setting : settings) {
                        temp.add(setting);
                    }

                    if(b) {
                        temp.add(N_CLOSED);
                    } else {
                        if(temp.contains(N_CLOSED))
                            temp.remove(N_CLOSED);
                    }
                    editor.putStringSet(guest_email, temp);
                    mService.setSettings(guest_email, temp);
                    editor.apply();
                } else {
                    Set<String> settings = preferences.getStringSet(user.getUid(),new HashSet<String>());
                    Set<String> temp = new HashSet<String>();
                    for(String setting : settings) {
                        temp.add(setting);
                    }
                    if(b) {
                        temp.add(N_CLOSED);
                    } else {
                        if(temp.contains(N_CLOSED))
                            temp.remove(N_CLOSED);
                    }
                    editor.putStringSet(user.getUid(), temp);
                    mService.setSettings(user.getUid(), temp);
                    editor.apply();
                }
            }
        });

        e_filledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                //if guest, commit using guest's email
                if(guest){
                    Set<String> settings = preferences.getStringSet(guest_email,new HashSet<String>());
                    Set<String> temp = new HashSet<String>();
                    for(String setting : settings) {
                        temp.add(setting);
                    }

                    if(b) {
                        temp.add(E_CLOSED);
                    } else {
                        if(temp.contains(E_CLOSED))
                            temp.remove(E_CLOSED);
                    }
                    editor.putStringSet(guest_email, temp);
                    mService.setSettings(guest_email, temp);
                    editor.apply();
                } else {
                    Set<String> settings = preferences.getStringSet(user.getUid(),new HashSet<String>());
                    Set<String> temp = new HashSet<String>();
                    for(String setting : settings) {
                        temp.add(setting);
                    }
                    if(b) {
                        temp.add(E_CLOSED);
                    } else {
                        if(temp.contains(E_CLOSED))
                            temp.remove(E_CLOSED);
                    }
                    editor.putStringSet(user.getUid(), temp);
                    mService.setSettings(user.getUid(), temp);
                    editor.apply();
                }
            }
        });
    }

    
    private void loadSettings(){
        if(guest) {
            Set settings = preferences.getStringSet(guest_email,null);
            if(settings != null) {
                filledSwitch.setChecked(false);
                openedSwitch.setChecked(false);
                if(settings.contains(N_CLOSED)) {
                    filledSwitch.setChecked(true);
                }
                if(settings.contains(N_OPEN)) {
                    openedSwitch.setChecked(true);
                }
            } else {
                filledSwitch.setChecked(true);
                openedSwitch.setChecked(true);
            }
        } else {
            Set settings = preferences.getStringSet(user.getUid(),null);
            if(settings != null) {
                e_openedSwitch.setChecked(false);
                e_filledSwitch.setChecked(false);
                openedSwitch.setChecked(false);
                filledSwitch.setChecked(false);
                if(settings.contains(N_CLOSED)) {
                    filledSwitch.setChecked(true);
                }
                if(settings.contains(N_OPEN)) {
                    openedSwitch.setChecked(true);
                }
                if(settings.contains(E_CLOSED)) {
                    e_filledSwitch.setChecked(true);
                }
                if(settings.contains(E_OPEN)) {
                    e_openedSwitch.setChecked(true);
                }
            } else {
                e_openedSwitch.setChecked(true);
                e_filledSwitch.setChecked(true);
                openedSwitch.setChecked(true);
                filledSwitch.setChecked(true);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}
