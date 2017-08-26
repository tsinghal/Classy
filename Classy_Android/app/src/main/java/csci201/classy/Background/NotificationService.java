package csci201.classy.Background;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sendgrid.SendGrid;
import com.sendgrid.SendGridException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import csci201.classy.MainActivity.MainActivity;
import csci201.classy.R;

import static csci201.classy.SettingsActivity.E_CLOSED;
import static csci201.classy.SettingsActivity.E_OPEN;
import static csci201.classy.SettingsActivity.N_CLOSED;
import static csci201.classy.SettingsActivity.N_OPEN;

public class NotificationService extends Service {
    private static final String TAG = "NotificationService";
    private final IBinder mBinder = new LocalBinder();
    public boolean guest = false;
    public String guest_email = "";
    DatabaseReference ref;
    SharedPreferences preferences;
    private FirebaseDatabase database;
    private Hashtable<String, DatabaseReference> refs;
    private Hashtable<DatabaseReference, ValueEventListener> listeners;
    private FirebaseUser user;
    private boolean e_open = true;
    private boolean e_close = true;
    private boolean n_close = true;
    private boolean n_open = true;

    public NotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        database = FirebaseDatabase.getInstance();
        listeners = new Hashtable<>();
        refs = new Hashtable<>();
        if (intent != null) {
            guest = intent.getBooleanExtra("GUEST", false);
            guest_email = intent.getStringExtra("GUEST_EMAIL");
        }
        //get settings
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        user = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "guest boolean:" + guest);
        if(user != null) {

            Set<String> settings = preferences.getStringSet(user.getUid(), new HashSet<String>());
            for (String setting : settings) {
                switch (setting) {
                    case N_CLOSED:
                        n_close = true;
                        break;
                    case N_OPEN:
                        n_open = true;
                        break;
                    case E_CLOSED:
                        e_close = true;
                        break;
                    case E_OPEN:
                        e_open = true;
                        break;
                }
            }
        } else if (guest) {
            Log.d(TAG, "GUEST service on create");
            Set<String> settings = preferences.getStringSet(guest_email, new HashSet<String>());
            e_close = false;
            e_open = false;
            for (String setting : settings) {
                switch (setting) {
                    case N_CLOSED:
                        n_close = true;
                        break;
                    case N_OPEN:
                        n_open = true;
                        break;
                }
            }
        }

        Log.d(TAG, preferences.getAll().toString());
        return START_STICKY;
    }

    public void setSettings(String userorguest, Set<String> settings) {
        Log.d(TAG, "setSettings: " + settings.toString());
        e_open = true;
        e_close = true;
        n_close = false;
        n_open = false;
        for (String setting : settings) {
            switch (setting) {
                case N_CLOSED:
                    n_close = true;
                    break;
                case N_OPEN:
                    n_open = true;
                    break;
                case E_CLOSED:
                    e_close = true;
                    break;
                case E_OPEN:
                    e_open = true;
                    break;
            }
        }
    }

    public void addSection(Map<String, Object> section) {
        String query = (String) section.get("query");
        final String coursename = (String) section.get("coursename");
        //make sure there is only one reference for one query
        if (refs.containsKey(query)) {
            ref = refs.get(query);
        } else {
            ref = database.getReference(query);
            refs.put(query, ref);
        }
        Log.d(TAG, "addSection:  " + query);

        ValueEventListener v = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Object> section = (Map<String, Object>) dataSnapshot.getValue();
                String limit = (String) section.get("spaces_available");
                String current = (String) section.get("number_registered");
                int limitNum = Integer.parseInt(limit);
                int currentNum = Integer.parseInt(current);
                if (currentNum < limitNum) {
                    if (n_open)
                        NotificationService.this.notify("A section opened in " + coursename);
                    //if user has an email
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    user = auth.getCurrentUser();
                    if (user != null && e_open) {
                        Log.d(TAG, "sent email out for spot opened" + user.getEmail());
                        SendEmailASyncTask task = new SendEmailASyncTask(NotificationService.this,user.getEmail(), user.getEmail(),
                                coursename + " has a section opened!", currentNum + "/" + limitNum + " spots available.", null, null);
                        task.execute();
                    }

                } else if (currentNum == limitNum) {
                    if (n_close)
                        NotificationService.this.notify(coursename + " has closed.");
                    //if user has an email
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    user = auth.getCurrentUser();
                    if (user != null && e_close) {
                        SendEmailASyncTask task = new SendEmailASyncTask(NotificationService.this,user.getEmail(), user.getEmail(),
                                coursename + " has a section closed.", currentNum + "/" + limitNum + " spots available.", null, null);
                        task.execute();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        ref.addValueEventListener(v);
        listeners.put(ref, v);
    }

    public void removeSection(Map section) {
        String query = (String) section.get("query");
        if (refs.containsKey(query)) {
            DatabaseReference r = refs.get(query);
            if (listeners.containsKey(r)) {
                //remove listener from dbref
                ValueEventListener v = listeners.get(r);
                r.removeEventListener(v);
                //remove ref / listener from hashmaps
                listeners.remove(r);
                refs.remove(query);
                Log.d("NotificationService", "removed " + query);
            }
        }
    }

    /**
     * removes all sections thread is listening for
     */
    private void removeAllSections() {
        for (String query : refs.keySet()) {
            refs.remove(query);
        }
        for (DatabaseReference ref : listeners.keySet()) {
            ValueEventListener v = listeners.get(ref);
            ref.removeEventListener(v);
            listeners.remove(ref);
        }
    }

    /**
     * loads in a list of the current user's choices for listening
     *
     * @param userChoices
     */
    public void loadAllSections(List<Object> userChoices) {
        Log.d(TAG, "loadAllSections");
        removeAllSections();
        for (Object section : userChoices) {
            Map sectionMap = (Map) section;
            addSection(sectionMap);
        }
    }

    private void notify(String coursename) {
        //send notification in-app
        //image
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notify)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setContentTitle("Classy")
                        .setContentText(coursename);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(1, mBuilder.build());
    }

    /**
     * ASyncTask that composes and sends email
     */
    private static class SendEmailASyncTask extends AsyncTask<Void, Void, Void> {

        private Context mAppContext;
        private String mMsgResponse;

        private String mTo;
        private String mFrom;
        private String mSubject;
        private String mText;
        private Uri mUri;
        private String mAttachmentName;

        public SendEmailASyncTask(Context context, String mTo, String mFrom, String mSubject,
                                  String mText, Uri mUri, String mAttachmentName) {
            this.mAppContext = context.getApplicationContext();
            this.mTo = mTo;
            this.mFrom = mFrom;
            this.mSubject = mSubject;
            this.mText = mText;
            this.mUri = mUri;
            this.mAttachmentName = mAttachmentName;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                SendGrid sendgrid = new SendGrid("edwardhu16","ilove201");

                SendGrid.Email email = new SendGrid.Email();

                // Get values from edit text to compose email
                // TODO: Validate edit texts
                email.addTo(mTo);
                email.setFrom(mFrom);
                email.setSubject(mSubject);
                email.setText(mText);

                // Attach image
                if (mUri != null) {
                    email.addAttachment(mAttachmentName, mAppContext.getContentResolver().openInputStream(mUri));
                }

                // Send email, execute http request
                SendGrid.Response response = sendgrid.send(email);
                mMsgResponse = response.getMessage();

                Log.d("SendAppExample", mMsgResponse);

            } catch (SendGridException | IOException e) {
                Log.e("SendAppExample", e.toString());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    public class LocalBinder extends Binder {
        public NotificationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NotificationService.this;
        }
    }
}


