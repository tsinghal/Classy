package csci201.classy.Adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import csci201.classy.R;

/**
 * Created by edward on 11/16/16.
 */

public class WatcherAdapter extends ArrayAdapter {
    Context context;
    int resource;
    List<Object> objects;
    private WatcherAdapterCallback mCallback;
    private HashSet<String> limiter = new HashSet<>();
    public WatcherAdapter(Context context, int resource, List<Object> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
    }
    public void replaceObject(int position, Object object) {
        objects.set(position,object);
        notifyDataSetChanged();
    }
    public void setListener(WatcherAdapterCallback callback) {
        mCallback = callback;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(resource, parent, false);
        }
        final Map<String, Object> dataItem = (Map<String, Object>) objects.get(position);
        //section name
        String name = (String) dataItem.get("coursename");
        TextView nameTextView = (TextView) convertView.findViewById(R.id.classname);
        nameTextView.setText(name);
        //section type
        String type = (String) dataItem.get("type");
        TextView typeTextView = (TextView) convertView.findViewById(R.id.type);
        typeTextView.setText(type);
        //time
        Object daysObject = dataItem.get("day");
        String days = "";
        if(daysObject instanceof String) {
           days = (String) dataItem.get("day");
        } else if (daysObject instanceof ArrayList) {
            for(Object day : (ArrayList)daysObject) {
                days+=day;
            }
        }
        String time = days + " " + dataItem.get("start_time") + "-" + dataItem.get("end_time");
        TextView timeTextView = (TextView) convertView.findViewById(R.id.day_and_time);
        timeTextView.setText(time);
        //spots available
        String spots = dataItem.get("number_registered") + "/" + dataItem.get("spaces_available");
        TextView spacesTextView = (TextView) convertView.findViewById(R.id.fraction);
        spacesTextView.setText(spots);
        //remove button
        Button deleteButton = (Button) convertView.findViewById(R.id.removeButton);
        deleteButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                mCallback.deleteButtonClicked(dataItem);
                objects.remove(position);
                notifyDataSetChanged();
            }
        });
        if(!limiter.contains(dataItem.get("coursename"))) {
            mCallback.connectWatcherViewToFirebase(this, dataItem, position);
            limiter.add((String) dataItem.get("coursename"));
        }
        return convertView;
    }

    public void resetLimiter() {
        limiter = new HashSet<>();
    }

    public interface WatcherAdapterCallback {
        void deleteButtonClicked(Map section);
        void connectWatcherViewToFirebase(WatcherAdapter adapter, Map section, int position);
    }
}
