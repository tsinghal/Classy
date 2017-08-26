package csci201.classy.Adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import csci201.classy.R;

/**
 * Created by edward on 11/16/16.
 */

public class SectionAdapter extends ArrayAdapter {
    Context context;
    int resource;
    Object[] objects;

    public SectionAdapter(Context context, int resource, Object[] objects) {
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
        //instructor
//        Log.d("Instructor",  dataItem.toString());
//        Map<String, Object> instructorMap = (Map<String, Object>) dataItem.get("instructor");
        Object instructors = dataItem.get("instructor");
        String instructor = "";
        if (instructors instanceof Map) {
            instructor = ((Map) instructors).get("first_name") + " " + ((Map) instructors).get("last_name");
        } else if (instructors instanceof List) {
            List<Map> instructorsList = (List<Map>) instructors;
            for (Map i : instructorsList) {
                instructor += i.get("first_name") + " " + i.get("last_name") + ", ";
            }
            //chop off the last space and comma
            instructor = instructor.substring(0, instructor.length() - 2);
        }
        TextView instructorTextView = (TextView) convertView.findViewById(R.id.instructorTextView);
        instructorTextView.setText(instructor);
        //section type
        String type = (String) dataItem.get("type");
        TextView typeTextView = (TextView) convertView.findViewById(R.id.sectionTypeTextView);
        typeTextView.setText(type);
        //time
        String time = dataItem.get("start_time") + "-" + dataItem.get("end_time");
        TextView timeTextView = (TextView) convertView.findViewById(R.id.sectionTimeTextView);
        timeTextView.setText(time);
        //days
        Object dayObject = dataItem.get("day");
        String days = "";
        if (dayObject instanceof List) {
            for(String day : (List<String>) dayObject) {
                days +=day;
            }
        } else if (dayObject instanceof String) {
             days = (String) dataItem.get("day");
        }
        dataItem.put("day", days);
        TextView dayTextView = (TextView) convertView.findViewById(R.id.sectionDayTextView);
        dayTextView.setText(days);
        //spots available
        String spots = dataItem.get("number_registered") + "/" + dataItem.get("spaces_available");
        TextView spacesTextView = (TextView) convertView.findViewById(R.id.spacesAvailableTextView);
        spacesTextView.setText(spots);
        return convertView;
    }
}
