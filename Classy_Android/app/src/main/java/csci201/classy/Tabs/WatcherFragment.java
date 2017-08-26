package csci201.classy.Tabs;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import csci201.classy.Adapters.WatcherAdapter;
import csci201.classy.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link WatcherFragment interface
 * to handle interaction events.
 * Use the {@link WatcherFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WatcherFragment extends Fragment implements AdapterView.OnItemClickListener, WatcherAdapter.WatcherAdapterCallback {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private ListView listView;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private List<Object> watchedSections;
    private WatcherAdapter watcherAdapter;
    private OnWatcherFragmentInteractionListener mListener;

    public WatcherFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment WatcherFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WatcherFragment newInstance(String param1, String param2) {
        WatcherFragment fragment = new WatcherFragment();
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
        watchedSections = new ArrayList<Object>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_watcher, container, false);
        listView = (ListView) view.findViewById(R.id.watcher_listview);
        listView.setOnItemClickListener(this);
        watcherAdapter = new WatcherAdapter(getContext(), R.layout.watcher_list_item, watchedSections);
        watcherAdapter.setListener(this);
        listView.setAdapter(watcherAdapter);
        mListener.onCreateWatcherViewDone(view);

        //floating action button
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (Object section : watchedSections) {
                    mListener.onSectionRemoved((Map)section);
                }
                watchedSections = new ArrayList<Object>();
                watcherAdapter = new WatcherAdapter(getContext(), R.layout.watcher_list_item, watchedSections);
                watcherAdapter.setListener(WatcherFragment.this);
                listView.setAdapter(watcherAdapter);
                watcherAdapter.notifyDataSetChanged();
            }
        });
        return view;
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnWatcherFragmentInteractionListener) {
            mListener = (OnWatcherFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
    public void populateListView(List<Object> sections) {
        watchedSections = sections;
        if(getActivity() != null) {
            watcherAdapter = new WatcherAdapter(getContext(), R.layout.watcher_list_item, watchedSections);
            watcherAdapter.setListener(this);
            listView.setAdapter(watcherAdapter);
        }
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    public void addSection(Map<String, Object> sectionMap) {
        Log.d("WatcherFragment", "addSection");
        watcherAdapter.add(sectionMap);
        watcherAdapter.notifyDataSetChanged();
    }

    @Override
    public void deleteButtonClicked(Map section) {
        mListener.onSectionRemoved(section);
    }

    @Override
    public void connectWatcherViewToFirebase(WatcherAdapter adapter, Map section, int position) {
        mListener.onConnectWatcherViewToFirebase(adapter, section, position);
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
    public interface OnWatcherFragmentInteractionListener {
        void onSectionRemoved(Map section);
        void onConnectWatcherViewToFirebase(WatcherAdapter adapter, Map section, int position);
        void onCreateWatcherViewDone(View view);
    }
}
