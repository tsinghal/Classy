package csci201.classy.Tabs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import csci201.classy.Adapters.CourseAdapter;
import csci201.classy.Adapters.DepartmentAdapter;
import csci201.classy.Adapters.SchoolAdapter;
import csci201.classy.Adapters.SectionAdapter;
import csci201.classy.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SelectorFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SelectorFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SelectorFragment extends Fragment implements AdapterView.OnItemClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String ARG_PARAM1 = "param1";
    public static final String ARG_PARAM2 = "param2";
    public static final int SCHOOL_MODE = 0;
    public static final int DEPARTMENT_MODE = 1;
    public static final int COURSE_MODE = 2;
    public static final int SECTION_MODE = 3;
    private static final String TAG = "SelectorFragment";
    final ArrayList schools = new ArrayList();
    DepartmentAdapter departmentAdapter;
    CourseAdapter courseAdapter;
    SectionAdapter sectionAdapter;
    SchoolAdapter schoolAdapter;
    Map<String, Object> courseData = null;
    private boolean weird = false;
    private FirebaseDatabase database;
    private DatabaseReference ref;
    private ListView listView;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mode = SCHOOL_MODE;
    private OnFragmentInteractionListener mListener;
    private String currentCourse;
    private List<String> queryStack = new ArrayList<String>();
    public SelectorFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SelectorFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SelectorFragment newInstance(String param1, String param2) {
        SelectorFragment fragment = new SelectorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_selector, container, false);
        listView = (ListView) view.findViewById(R.id.class_selector_listview);
        listView.setOnItemClickListener(this);
        mListener.onCreateSelectorViewDone(view);
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        switch (mode) {
            case SCHOOL_MODE:
                queryStack.clear();
                queryStack.add("schools/" + i + "/");
                Map<String, Object> schoolMap = (Map<String, Object>) schoolAdapter.getItem(i);
                if (schoolMap.containsKey("department")) {
                    List<Object> departments = (List<Object>) schoolMap.get("department");
                    departmentAdapter = new DepartmentAdapter(getContext(), R.layout.department_list_item, departments.toArray());
                    listView.setAdapter(departmentAdapter);
                    weird = false;
                    mode = DEPARTMENT_MODE;
                } else if (schoolMap.containsKey("courses")) {
                    List<Object> courses = (List<Object>) schoolMap.get("courses");
                    courseAdapter = new CourseAdapter(getContext(), R.layout.course_list_item, courses.toArray());
                    listView.setAdapter(courseAdapter);
                    weird = true;
                    mode = COURSE_MODE;
                }
                break;
            case DEPARTMENT_MODE:
                queryStack.add("department/" + i + "/");
                List<Object> courses = (List<Object>) ((Map<String, Object>) (departmentAdapter.getItem(i))).get("courses");
                courseAdapter = new CourseAdapter(getContext(), R.layout.course_list_item, courses.toArray());
                listView.setAdapter(courseAdapter);
                mode = COURSE_MODE;
                break;
            case COURSE_MODE:
                queryStack.add("courses/" + i + "/CourseData/");
                courseData = (Map<String, Object>) ((Map<String, Object>) (courseAdapter.getItem(i))).get("CourseData");
                Object obj = courseData.get("SectionData");
                currentCourse = courseData.get("prefix") + "-" + courseData.get("number");
                if (obj instanceof List) {
                    queryStack.add("list");
                    List sections = (List) obj;
                    sectionAdapter = new SectionAdapter(getContext(), R.layout.section_list_item, sections.toArray());
                } else if (obj instanceof HashMap) {
                    queryStack.add("map");
                    Object[] sections = new Object[]{obj};
                    sectionAdapter = new SectionAdapter(getContext(), R.layout.section_list_item, sections);
                }
                listView.setAdapter(sectionAdapter);
                mode = SECTION_MODE;
                break;
            case SECTION_MODE:
                if(queryStack.get(queryStack.size()-1).equals("list")) {
                    queryStack.remove(queryStack.size()-1);
                    queryStack.add("SectionData/" + i + "/" );
                } else if(queryStack.get(queryStack.size()-1).equals("map")){
                    queryStack.remove(queryStack.size()-1);
                    queryStack.add("SectionData/");
                } else {
                    Log.d(TAG, "section mode 3rd case");
                    queryStack.remove(queryStack.size()-1);
                   if(courseData != null) {
                       Object sectionData = courseData.get("SectionData");
                       if(sectionData instanceof List) {
                           queryStack.add("SectionData/" + i + "/" );
                       } else if (sectionData instanceof Map) {
                           queryStack.add("SectionData/");
                       }
                   }
                }
                printQuery();
                final Map<String, Object> section = (Map<String, Object>)(sectionAdapter.getItem(i));
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View v= vi.inflate(R.layout.section_dialog_content, null);
                TextView course = (TextView) v.findViewById(R.id.sectionDialogCourseCodeTextView);
                course.setText(currentCourse);
                TextView coursename = (TextView) v.findViewById(R.id.sectionDialogCourseNameTextView);
                coursename.setText((String)section.get("title"));
                //instructors
                Object instructors = section.get("instructor");
                String instructor = "";
                if (instructors instanceof Map) {
                    instructor = ((Map) instructors).get("first_name") + " " + ((Map) instructors).get("last_name");
                } else if (instructors instanceof List) {
                    List<Map> instructorsList = (List<Map>) instructors;
                    for (Map ij : instructorsList) {
                        instructor += ij.get("first_name") + " " + ij.get("last_name") + ", ";
                    }
                    //chop off the last space and comma
                    instructor = instructor.substring(0, instructor.length() - 2);
                }
                TextView instructorTextView = (TextView) v.findViewById(R.id.sectionDialogInstructors);
                instructorTextView.setText("Instructor: " + instructor);
                TextView sectionID = (TextView) v.findViewById(R.id.sectionDialogSectionID);
                sectionID.setText((String)section.get("id"));
                TextView type = (TextView) v.findViewById(R.id.sectionDialogSectionType);
                type.setText("Type: " + (String)section.get("type"));
                TextView time = (TextView) v.findViewById(R.id.sectionDialogSectionTime);
                String timeString = section.get("start_time") + "-" + section.get("end_time");
                time.setText("Time: " + timeString);
                TextView units = (TextView) v.findViewById(R.id.sectionDialogSectionUnits);
                units.setText("Units: " + (String) section.get("units"));
                TextView registered = (TextView) v.findViewById(R.id.sectionDialogFraction);
                String fraction = section.get("number_registered") + "/" + section.get("spaces_available");
                registered.setText("Spaces: " + fraction);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder
                        .setView(v)
                        .setPositiveButton("Subscribe", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                section.put("coursename", currentCourse);
                                section.put("query", printQuery());
                                mListener.onSelectorListItemClicked(section);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                break;
        }
    }

    public int goToPreviousList() {
        switch (mode) {
            case SCHOOL_MODE:
                queryStack.clear();
                return SCHOOL_MODE;
            case DEPARTMENT_MODE:
                queryStack.remove(queryStack.size()-1);
                printQuery();
                listView.setAdapter(schoolAdapter);
                mode = SCHOOL_MODE;
                return DEPARTMENT_MODE;
            case COURSE_MODE:
                queryStack.remove(queryStack.size()-1);
                printQuery();
                if(weird){
                    listView.setAdapter(schoolAdapter);
                    mode = SCHOOL_MODE;
                    return COURSE_MODE;
                }
                else {
                    listView.setAdapter(departmentAdapter);
                    mode = DEPARTMENT_MODE;
                    return COURSE_MODE;
                }
            case SECTION_MODE:
                queryStack.remove(queryStack.size()-1);
                queryStack.remove(queryStack.size()-1);
                printQuery();
                listView.setAdapter(courseAdapter);
                mode = COURSE_MODE;
                return SECTION_MODE;
            default:
                return 999;
        }
    }
    private String printQuery(){
        String query = "";
        for (int i1 = 0; i1 < queryStack.size(); i1++) {
            query+=queryStack.get(i1);
        }
       // Log.d("queryStack", query);
        return query;
    }

    public void populateListView(DataSnapshot dataSnapshot) {
        for (DataSnapshot school : dataSnapshot.getChildren()) {
            Map<String, Object> schoolMap = (Map<String, Object>) school.getValue();
            schools.add(schoolMap);
        }
        if(getActivity() != null) {
            schoolAdapter = new SchoolAdapter(getContext(), R.layout.school_list_item, schools.toArray());
            listView.setAdapter(schoolAdapter);
        }
    }
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onSelectorListItemClicked(Map<String, Object> sectionMap);
        void onCreateSelectorViewDone(View view);
    }
}
