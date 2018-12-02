package com.example.robert.ph_prototype;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentSchedulerActivity extends AppCompatActivity {
    private static final String TAG = "lol";

    private static String REFERENCE = "ActivityCollection";
    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
    DatabaseReference mRootReference = firebaseDatabase.getReference(REFERENCE);

    HashMap<String, HashMap<String, String>> allActivities;
    HashMap<String, String> activityIds = new HashMap<>();

    private ScheduleItemCardArrayAdapter scheduleItemCardArrayAdapter;
    private ListView listView;

    private ScheduleItemCard.Type filter;
    private int userId;
    private String activities;
    private String userEmail;
    private boolean signupEnabled;

    private Set<String> allRooms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_scheduler);

        Intent i = getIntent();
        userId = i.getIntExtra("user_id", -1);
        activities = i.getStringExtra("activities");
        userEmail = i.getStringExtra("user_email");
        signupEnabled = i.getBooleanExtra("signup_enabled", true);
        filter = ScheduleItemCard.Type.valueOf(i.getStringExtra("FILTER"));
        Log.d("nshinn", filter.toString());

        allRooms = new HashSet<>();
        mRootReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                allActivities =
                        (HashMap<String, HashMap<String, String>>) dataSnapshot.getValue();
                parseActivityData(scheduleItemCardArrayAdapter);
                Log.d(TAG, "List of activities: " + allActivities);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

        initScheduleListView();
        initDropdownFilters();
        initFAB();
    }

    private void initScheduleListView() {

        listView = (ListView) findViewById(R.id.card_listView);

        listView.addHeaderView(new View(this));
        listView.addFooterView(new View(this));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ScheduleItemCard selectedItem = (ScheduleItemCard) parent.getItemAtPosition(position);
                Intent intent = new Intent(StudentSchedulerActivity.this, StudentSignupActivity.class);
                intent.putExtra("parcelable_item", (Parcelable) selectedItem);
                intent.putExtra("activity_id", activityIds.get(selectedItem.getName()));
                intent.putExtra("activities", activities);
                intent.putExtra("user_id", userId);
                intent.putExtra("user_email", userEmail);
                intent.putExtra("signup_enabled", signupEnabled);
                startActivityForResult(intent, 1);
            }
        });

        scheduleItemCardArrayAdapter = new ScheduleItemCardArrayAdapter(getApplicationContext(), R.layout.schedule_item_card);
        listView.setAdapter(scheduleItemCardArrayAdapter);
    }

    //  {Activity1={Type=Club, Students=<Nick Shinn, Austin Le>, Capacity=100, Teacher=Mr. Bboy, Duration=60, Time=11:00, Room=222, Name=BboyClub}}
    private void parseActivityData(ScheduleItemCardArrayAdapter scheduleItemCardArrayAdapter) {
        Log.d(TAG, "Parsing Activity Data");
        if (allActivities == null || scheduleItemCardArrayAdapter == null) {
            Log.d(TAG, "null activities or adapter");
            return;
        } else {
            scheduleItemCardArrayAdapter.reset();
            activityIds.clear();
        }
        if (filter == ScheduleItemCard.Type.MINE) {
            if (activities == null || activities.equals("")) return;
            Set<String> idSet = new HashSet<>();
            idSet.addAll(Arrays.asList(activities.split(" ")));
            for (String activity : idSet) {
                HashMap<String, String> fields = (HashMap<String, String>) allActivities.get(activity);
                Log.d(TAG, fields.toString());

                String name = fields.get("name");
                String type = fields.get("type");
                String room = fields.get("room");
                String teacher = fields.get("teacher");
                String time = fields.get("time");
                String duration = fields.get("duration");
                String students = fields.get("students");
                String capacity = fields.get("capacity");

                ScheduleItemCard newActivity =
                        new ScheduleItemCard(name, type, room, teacher, time, duration, students, capacity);
                scheduleItemCardArrayAdapter.add(newActivity);
                allRooms.add(room);
                activityIds.put(name, activity);
            }
        } else {
            for (String activity : allActivities.keySet()) {
                HashMap<String, String> fields = (HashMap<String, String>) allActivities.get(activity);
                Log.d(TAG, fields.toString());

                String name = fields.get("name");
                String type = fields.get("type");
                String room = fields.get("room");
                String teacher = fields.get("teacher");
                String time = fields.get("time");
                String duration = fields.get("duration");
                String students = fields.get("students");
                String capacity = fields.get("capacity");

                if (filter == ScheduleItemCard.Type.valueOf(type)) {
                    ScheduleItemCard newActivity =
                            new ScheduleItemCard(name, type, room, teacher, time, duration, students, capacity);
                    scheduleItemCardArrayAdapter.add(newActivity);
                }
                allRooms.add(room);
                activityIds.put(name, activity);
            }
        }
        // Reinitialize room filter to populate with all rooms
        initRoomFilter();
    }

    private void initFAB() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private void initDropdownFilters() {
        initTimeFilter();
        initRoomFilter();
    }

    private void initRoomFilter() {
        // Set up room filter
        Spinner roomFilter = findViewById(R.id.room_filter);
        roomFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);

                if (selectedItem.equals("All Rooms")) {
                    listView.setAdapter(scheduleItemCardArrayAdapter);
                    return;
                }

                ArrayList<ScheduleItemCard> filteredList = new ArrayList<>();
                for (int i = 0; i < scheduleItemCardArrayAdapter.getCount(); i++) {
                    ScheduleItemCard current = scheduleItemCardArrayAdapter.getItem(i);
                    Log.d("lol", current.getRoom()+" and "+selectedItem);
                    if (current.getRoom().equals(selectedItem)) {
                        filteredList.add(current);
                    }
                }
                Log.d("lol", String.valueOf(scheduleItemCardArrayAdapter.getCount()));
                ScheduleItemCardArrayAdapter filteredAdapter =
                        new ScheduleItemCardArrayAdapter(getApplicationContext(),
                                R.layout.schedule_item_card, filteredList);
                listView.setAdapter(filteredAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // lol
            }
        });
        List<String> rooms = new ArrayList<>();
        rooms.add("All Rooms");
        rooms.addAll(allRooms);
        ArrayAdapter<String> roomFilterAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rooms);
        roomFilter.setAdapter(roomFilterAdapter);
    }

    private void initTimeFilter() {
        // Set up time filter
        Spinner timeFilter = findViewById(R.id.time_filter);
        timeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // lol
            }
        });
        String[] timeSlots = new String[]{"10:25 AM", "10:40 AM", "10:55 AM", "11:10 AM", "11:25 AM"};
        ArrayList<String> times = new ArrayList<>();
        times.add("All Time Slots");
        times.addAll(Arrays.asList(timeSlots));
        ArrayAdapter<String> timeFilterAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, times);
        timeFilter.setAdapter(timeFilterAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                onBackPressed();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

}
