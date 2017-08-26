package csci201.classy.Adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Map;

import csci201.classy.R;

/**
 * Created by edward on 11/16/16.
 */

public class SchoolAdapter extends ArrayAdapter {
    Context context;
    int resource;
    Object[] objects;

    public SchoolAdapter(Context context, int resource, Object[] objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(resource, parent, false);
        }
        Map<String, Object> dataItem = (Map<String, Object>) objects[position];
        //set all of convertview
        TextView schoolNameTextView = (TextView) convertView.findViewById(R.id.schoolNameTextView);
        schoolNameTextView.setText((String) dataItem.get("name"));

        TextView schoolCodeTextView = (TextView) convertView.findViewById(R.id.schoolCodeTextView);
        schoolCodeTextView.setText((String) dataItem.get("code"));
        return convertView;
    }
}
