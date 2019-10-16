package se.chalmers.cse.dit341.group00.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.chalmers.cse.dit341.group00.R;
import se.chalmers.cse.dit341.group00.activities.MainActivity;
import se.chalmers.cse.dit341.group00.helpers.SharedpreferencesManager;
import se.chalmers.cse.dit341.group00.interfaces.InterfaceMainActivity;
import se.chalmers.cse.dit341.group00.models.Tasklist;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TaskListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the  factory method to
 * create an instance of this fragment.
 */
public class TaskListFragment<newTaskButton> extends Fragment implements View.OnClickListener {

    private static final String TAG = "TaskListFragment";
    private InterfaceMainActivity mInterfaceMainActivity;

    private final ArrayList<Tasklist> list = new ArrayList<>();
    private ListView listView;
    private TextAdapter adapter;
    private Button newTaskButton;
    private Button deleteTaskButton;
    private Button deleteAllTasksButton;
    private MainActivity mMainActivity;
    private String taskListTitle;


    private OnFragmentInteractionListener mListener;

    public TaskListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TaskListFragment.
     */
    // TODO: Rename and change types and number of parameters


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getTaskListsFromBackend();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_task_list, container, false);

        newTaskButton = view.findViewById(R.id.newTaskButton);
        deleteAllTasksButton = view.findViewById(R.id.deleteAllTasks);
        listView = view.findViewById(R.id.listview);


        newTaskButton.setActivated(true);
        newTaskButton.setOnClickListener(this);
        /*listView.setActivated(true);
        listView.setOnClickListener(this);*/


        adapter = new TextAdapter(list);
        listView.setAdapter(adapter);
        System.out.println(listView);

        return view;

    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mInterfaceMainActivity = (InterfaceMainActivity) getActivity();
        mMainActivity =(MainActivity) getActivity();

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.newTaskButton:

                final EditText taskInput = new EditText(getContext());
                taskInput.setSingleLine();
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                dialog.setTitle("add tasklist")
                        .setMessage("What is your taskList title? ")
                        .setView(taskInput)
                        .setPositiveButton("Add Task", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                taskListTitle = taskInput.getText().toString();

                                JSONObject dataToSendToBackend = new JSONObject();

                                try{
                                    dataToSendToBackend.put("title", taskListTitle);

                                }catch(JSONException e){
                                    Log.e(TAG, e.getMessage());
                                }

                                if(dataToSendToBackend.length() > 0 ){
                                    sendDataAsRequest(dataToSendToBackend);
                                }
                                //list.add();
                                //adapter.setData(list);
                            }
                        }).setNegativeButton("cancel", null).create();
                dialog.show();
                break;


            case  R.id.listview:

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                        AlertDialog dialog = new AlertDialog.Builder(getContext())
                                .setTitle("Delete this task")
                                .setPositiveButton("yes", new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String groupId = list.get(position).get_id();
                                        deleteSelectedGroup(groupId);
                                    }
                                }).setNegativeButton("No", null)
                                .create();
                        dialog.show();
                    }
                });
                break;

        }
    }





    private void sendDataAsRequest(JSONObject dataToSendToBackend) {

        RequestQueue requestQueue = Volley.newRequestQueue(mInterfaceMainActivity.getContext());

        String url = mInterfaceMainActivity.getUrl() + "tasklists/" ;

        System.out.println("before request " + dataToSendToBackend);

        ObjectMapper objectMapper = new ObjectMapper();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, dataToSendToBackend, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                System.out.println("inside onresponse " + response);

                try {

                    String JSONResponse = response.toString();
                    JSONObject parsedData = new JSONObject(JSONResponse);
                    System.out.println(parsedData + " parsed data");
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = (JsonObject) parser.parse(parsedData.getString("newTasklist"));
                    System.out.println(jsonObject);
                    String newTasklistString = jsonObject.toString();

                    Tasklist newTasklist = objectMapper.readValue(newTasklistString, Tasklist.class);

                    list.add(newTasklist);
                    adapter.notifyDataSetChanged();
                    System.out.println(list.get(0).title);

                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (JsonParseException e) {
                    e.printStackTrace();
                } catch (JsonMappingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "error line 177 " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("x-auth-token", SharedpreferencesManager.getInstance().getToken());

                return params;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }


    private void getTaskListsFromBackend(){
        RequestQueue requestQueue = Volley.newRequestQueue(mInterfaceMainActivity.getContext());

        String url = mInterfaceMainActivity.getUrl() + "tasklists/";

        final ObjectMapper objectMapper = new ObjectMapper();

        final JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                System.out.print("inside onresponse" + response);

                try {
                    System.out.println(response + "response line 221");

                    for (int i = 0; i < response.length(); i++) {

                        JSONObject jsonObject = response.getJSONObject(i);
                        System.out.println(jsonObject);

                        String newTasklistString = jsonObject.toString();
                        Tasklist newTasklist = objectMapper.readValue(newTasklistString, Tasklist.class);

                        list.add(newTasklist);
                        adapter.notifyDataSetChanged();
                        System.out.println("this message " + list.get(0).title);
                    }

                    System.out.println(list + " array in 252 ");
                } catch (JSONException e) {
                    e.printStackTrace();

                } catch (JsonParseException e) {
                    e.printStackTrace();
                } catch (JsonMappingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, "error" + error.getMessage());
            }
        }) {
            public Map<String,String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("x-auth-token", SharedpreferencesManager.getInstance().getToken());

                return params;
            }
        };
    }

    public void deleteSelectedGroup (String tasklistId) {

        RequestQueue requestQueue = Volley.newRequestQueue(mInterfaceMainActivity.getContext());

        String url = mInterfaceMainActivity.getUrl() + "tasklists/" + tasklistId;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.DELETE, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {

                System.out.println("inside onresponse line " + response);

                try {

                    System.out.println(response + "response line ");

                    String JSONResponse = response.toString();
                    JSONObject parsedData = new JSONObject(JSONResponse);
                    System.out.println(parsedData + " parsed data line");

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i(TAG, error.getMessage());
                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    Toast.makeText(getContext(), "Error with connection to backend", Toast.LENGTH_SHORT).show();

                } else if (error instanceof AuthFailureError) {
                    Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();

                } else if (error instanceof NetworkError) {
                    Toast.makeText(getContext(),
                            "Error with the network",
                            Toast.LENGTH_LONG).show();
                } else if (error instanceof ParseError) {
                    Toast.makeText(getContext(),
                            "Error parsing information",
                            Toast.LENGTH_LONG).show();
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("x-auth-token", SharedpreferencesManager.getInstance().getToken());

                return params;
            }
        };
        requestQueue.add(jsonObjectRequest);
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    class TextAdapter extends BaseAdapter {

        List<Tasklist> adapterlist = new ArrayList<>();

        public TextAdapter (ArrayList<Tasklist> tasklist) {
            this.adapterlist = tasklist;
            adapterlist.addAll(tasklist);
            notifyDataSetChanged();

        }


        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                LayoutInflater inflator = (LayoutInflater)
                        getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflator.inflate(R.layout.tasklist_item, parent, false);
            }

            TextView textView = convertView.findViewById((R.id.task));
            textView.setText( adapterlist.get(position).getTitle());
            return convertView;

        }
    }

}
