package csci201.classy.MainActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import csci201.classy.Adapters.WatcherAdapter;
import csci201.classy.Background.NotificationService;
import csci201.classy.LoginActivity.LoginActivity;
import csci201.classy.R;
import csci201.classy.SettingsActivity;
import csci201.classy.Tabs.SelectorFragment;
import csci201.classy.Tabs.WatcherFragment;


public class MainActivity extends AppCompatActivity implements SelectorFragment.OnFragmentInteractionListener, WatcherFragment.OnWatcherFragmentInteractionListener, SearchView.OnQueryTextListener {
    private static final String TAG = "MainActivity";
    NotificationService mService;
    boolean mBound = false;
    private  FirebaseUser user;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private boolean selectorViewDone = false;
    private boolean watcherViewDone = false;
    private boolean selectorViewLoaded = false;
    private boolean watcherViewLoaded = false;
    private DataSnapshot dataSnapshot = null;
    private DataSnapshot watcherDataSnapshot = null;
    private DatabaseReference ref;
    private DatabaseReference userRef;
    private ValueEventListener userRefListener;
    private boolean guest = true;
    private String guest_email = "";
    private int addedSections = 0;
    private ViewPager viewPager;
    private PagerAdapter adapter;
    private HashMap<String, DatabaseReference> sectionReferences = new HashMap<>(); //section id, database reference
    private HashMap<DatabaseReference, ValueEventListener> sectionListeners = new HashMap<>(); //section id, listener
    private ServiceConnection mConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        Intent i = getIntent();
        guest = i.getBooleanExtra("GUEST", false);
        guest_email = i.getStringExtra("GUEST_EMAIL");
        Log.d(TAG, "guest: " + guest + " email:" + guest_email);

        /**
         * Start the notification thread
         */
        Intent intent = new Intent(this, NotificationService.class);
        intent.putExtra("GUEST", guest);
        intent.putExtra("GUEST_EMAIL", guest_email);
        startService(intent);
        //bind to service
        //load service
        mConnection = new ServiceConnection() {

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
        intent = new Intent(this, NotificationService.class);
        intent.putExtra("GUEST", guest);
        intent.putExtra("GUEST_EMAIL", guest_email);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        if (Build.VERSION.SDK_INT > 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setBackgroundColor(Color.parseColor("#1a75ff"));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //adding tabs with titles as strings
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Class Watcher"));
        tabLayout.addTab(tabLayout.newTab().setText("Class Selector"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        //this allows us to add fragments to our tabs
        viewPager = (ViewPager) findViewById(R.id.pager);
        adapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            //when a tab is pressed, display that tab
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.setOffscreenPageLimit(2);


        if(!guest)
            loadUser();
        loadFirebase();
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("GUEST", guest);
            intent.putExtra("GUEST_EMAIL", guest_email);
            startActivity(intent);
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    /**
     * user has selected a section form the selector fragment
     * add the section into the user's profile on firebase
     * then start watching through the service thread
     * @param sectionMap
     */
    @Override
    public void onSelectorListItemClicked(final Map<String, Object> sectionMap) {
        addedSections++;
        Log.d(TAG, "selectorListItemClicked");
        if(guest && addedSections > 3){
            Log.d(TAG, "guest added over 3");
            Toast.makeText(MainActivity.this,"Guests can only add 3 sections", Toast.LENGTH_LONG).show();
            return;
        }
        DatabaseReference users = database.getReference("users");
        Fragment fragment = (Fragment) viewPager.getAdapter().instantiateItem(viewPager, 0);
        if (fragment instanceof WatcherFragment) {
            WatcherFragment watcherFragment = (WatcherFragment) fragment;
            watcherFragment.addSection(sectionMap);

            //update user in database
            if(!guest) {
                final DatabaseReference userRef = users.child(this.user.getUid());

                userRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        List<Object> sections = (List) dataSnapshot.getValue();
                        if (sections == null) {
                            ArrayList a = new ArrayList();
                            a.add(sectionMap);
                            userRef.setValue(a);
                            Log.d(TAG, "user had no sections in db");

                        } else {
                            Log.d(TAG, "user had sections in db");

                            sections.add(sectionMap);
                            userRef.setValue(sections);
                        }
                        userRef.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            if (mBound) {
                Log.d("MainActivity", "mService adding sectionMap");
                mService.addSection(sectionMap);
            }
        }

    }



    //load the user in
    public void loadUser() {
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if(user != null) {
            database = FirebaseDatabase.getInstance();
            String uid = user.getUid();
            userRef = database.getReference("users").child(uid);
            if(userRef != null) {
                Log.d(TAG, "loadUser found: " + uid);
                userRefListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        watcherDataSnapshot = dataSnapshot;
                        if(mBound) {
                            //if user has sections. if not, don't do anything
                            List<Object> sections = (List<Object>) dataSnapshot.getValue();
                            if(sections != null && watcherViewDone) {
                                Log.d(TAG, "loadFirebase sections:" + sections.toString());
                                mService.loadAllSections(sections);
                                //TODO update watcher view
                                WatcherFragment watcherFragment = getWatcherFragment();
                                watcherFragment.populateListView(sections);
                                userRef.removeEventListener(this);
                                //load once and then we're done.
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                };
                userRef.addListenerForSingleValueEvent(userRefListener);
            }
        } else { //guest log in

        }


    }
    public void loadFirebase() {
        database = FirebaseDatabase.getInstance();
        ref = database.getReference("schools");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                MainActivity.this.dataSnapshot = dataSnapshot;
                //if selectorview's fragment's onviewcreated is already called
                if (selectorViewDone) {
                    Log.d("MainActivity", "loadFirebase selectorViewDone");
                    SelectorFragment selector = getSelectorFragment();
                    selector.populateListView(dataSnapshot);
                    selectorViewLoaded = true;
                }
                //else just do nothing and wait for oncreateviewdone
                ref.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    @Override
    public void onCreateWatcherViewDone(View view) {
        watcherViewDone = true;
        Log.d(TAG, "onCreateWatcherViewDone");
        //if oncreateview is done but data is not loaded
        if(!watcherViewLoaded && watcherDataSnapshot != null && watcherDataSnapshot.getValue() != null) {
            WatcherFragment watcher = getWatcherFragment();
            watcher.populateListView((List)(watcherDataSnapshot.getValue()));
            watcherViewLoaded = true;
        }
    }
    @Override
    public void onCreateSelectorViewDone(View view) {
        selectorViewDone = true;
        Log.d(TAG, "onCreateSelectorViewDone");
        //if oncreateview is done but data is not loaded
        if (!selectorViewLoaded && dataSnapshot != null) {
            Log.d("MainActivity", "database loaded before view was created");
            SelectorFragment selector = getSelectorFragment();
            selector.populateListView(dataSnapshot);
            selectorViewLoaded = true;
        }

    }

    /**
     * remove section from service
     * remove section from account
     * @param section
     */
    @Override
    public void onSectionRemoved(final Map section) {
        addedSections--;
        if (!guest) {
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Object> sections = (List)dataSnapshot.getValue();
                    sections.remove(section);
                    userRef.setValue(sections);
                    userRef.removeEventListener(this);
                    Log.d(TAG, "onSectionRemoved: " + section.get("coursename"));
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        if (mBound) {
            mService.removeSection(section);
        }

    }

    @Override
    public void onConnectWatcherViewToFirebase(final WatcherAdapter adapter, Map section, final int position) {
        Log.d(TAG, "onConnectWatcherViewToFirebase");
        String sectionID = (String) section.get("id");
        String query = (String) section.get("query");
        DatabaseReference ref;
        if (sectionReferences.containsKey(sectionID)) {
            ref = sectionReferences.get(sectionID);
        } else {
            ref = database.getReference(query);
            sectionReferences.put(sectionID, ref);
        }
        ValueEventListener v = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                    Map<String, Object> previousItem = (Map) adapter.getItem(position);
                    String query = (String) previousItem.get("query");
                    String coursename = (String) previousItem.get("coursename");
                    Map<String, Object> newItem = (Map) dataSnapshot.getValue();
                    newItem.put("query", query);
                    newItem.put("coursename", coursename);
                    adapter.replaceObject(position, newItem);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        //if previous listener, remove and add the new one
        if (sectionListeners.containsKey(ref)) {
            ref.removeEventListener(sectionListeners.get(ref));
        }
        ref.addValueEventListener(v);
        sectionListeners.put(ref, v);
    }

    @Override
    public void onBackPressed() {
        /**
         * if currently on the class selector
         */
        if (viewPager.getCurrentItem() == 1) {
            SelectorFragment selector = getSelectorFragment();
            int back = selector.goToPreviousList();
            Log.d("onBackPressed", back + "");
            if (back == SelectorFragment.SCHOOL_MODE) {
                dialogForLogOut();
            }
        } else {
            dialogForLogOut();
        }
    }

    public void dialogForLogOut(){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Sign Out");
        alertDialog.setMessage("Are you sure you want to back? You will be logged out!");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Sign Out",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mAuth = FirebaseAuth.getInstance();
                        user = mAuth.getCurrentUser();
                        mAuth.signOut();
                        Toast.makeText(MainActivity.this, "Logged Out",
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        //remove all listeners
        for(DatabaseReference ref : sectionListeners.keySet()) {
            ValueEventListener v = sectionListeners.get(ref);
            ref.removeEventListener(v);
        }

        if(userRef != null && userRefListener != null) {
            userRef.removeEventListener(userRefListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, NotificationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private SelectorFragment getSelectorFragment() {
        Fragment viewPagerFrag = (Fragment) viewPager.getAdapter().instantiateItem(viewPager, 1);
        SelectorFragment selector = (SelectorFragment) viewPagerFrag;
        return selector;
    }
    private WatcherFragment getWatcherFragment(){
        Fragment viewPagerFrag = (Fragment) viewPager.getAdapter().instantiateItem(viewPager, 0);
        WatcherFragment watcher = (WatcherFragment) viewPagerFrag;
        return watcher;
    }
}
