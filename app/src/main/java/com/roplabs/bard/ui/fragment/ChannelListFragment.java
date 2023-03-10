package com.roplabs.bard.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.firebase.database.*;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ChannelListAdapter;
import com.roplabs.bard.api.BardClient;
import com.roplabs.bard.api.GsonUTCDateAdapter;
import com.roplabs.bard.config.Configuration;
import com.roplabs.bard.models.Channel;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.ui.activity.ChannelActivity;
import com.roplabs.bard.ui.widget.ItemOffsetDecoration;
import com.roplabs.bard.util.BardLogger;
import com.roplabs.bard.util.Helper;
import io.realm.Realm;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.*;

import static com.roplabs.bard.util.Helper.CHANNEL_REQUEST_CODE;

/*
"channels": {
    "one": {
        "title": "Historical Tech Pioneers",
        "lastMessage": "ghopper: Relay malfunction found. Cause: moth.",
        "timestamp": 1459361875666,
        users: {
            uid_1: true,
            uid_3: true
        }
    },
    "two": { ... },

"users": {
    "rsenov": {
      "channels": {
        "one": true,
        "two": true
      }
    }
  }
*/
public class ChannelListFragment extends Fragment {

    private String defaultChannelToken;
    private RecyclerView recyclerView;
    private List<Channel> channelList;
    private Map<String, Map<DatabaseReference,  ValueEventListener>> channelListenerMap;

    private FrameLayout emptyStateContainer;
    private TextView emptyStateTitle;
    private TextView emptyStateDescription;

    public static ChannelListFragment newInstance() {
        Bundle args = new Bundle();
        ChannelListFragment fragment = new ChannelListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        defaultChannelToken = Configuration.mainChannelToken();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_channel_list, container, false);


        recyclerView = (RecyclerView) view.findViewById(R.id.channel_list);
        initEmptyState(view);
        initChannelList(view);
        fetchChannelList();

        return view;
    }

    private void initChannelList(View view) {
        final Context self = getActivity();
        channelList = new ArrayList<Channel>();
        channelListenerMap = new HashMap<String, Map<DatabaseReference, ValueEventListener>>();

        recyclerView = (RecyclerView) view.findViewById(R.id.channel_list);
        ChannelListAdapter adapter = new ChannelListAdapter(self, channelList);
        adapter.setOnItemClickListener(new ChannelListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position, Channel channel) {
                Intent intent = new Intent(self, ChannelActivity.class);
                intent.putExtra("channel", channel);
                BardLogger.trace("[view channel] " + channel.getToken());
                startActivityForResult(intent, CHANNEL_REQUEST_CODE);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(self));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(self, R.dimen.character_item_offset);
        recyclerView.addItemDecoration(itemDecoration);
    }

    private Channel getChannelFromToken(String channelToken) {
        Channel targetChannel = null;

        for (Channel channel : channelList) {
            if (channel.getToken().equals(channelToken)) {
                targetChannel = channel;
                break;
            }
        }

        return targetChannel;
    }

    public void updateChannels(String channelToken, HashMap<String, Object> channelResult) {
        Channel channel = getChannelFromToken(channelToken);

        if (channel == null) {
            // create channel
            channel = Channel.createFromFirebase(channelToken, channelResult);
            channelList.add(channel);
        } else {
            channel.updateFromFirebase(channelResult);
        }
    }

    private void listenToNewChannelMessage(final String channelToken) {

        DatabaseReference channelRef = FirebaseDatabase.getInstance().getReference("channels/" + channelToken + "");
        ValueEventListener valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                HashMap<String, Object> channelResult = (HashMap<String, Object>) dataSnapshot.getValue();
                updateChannels(channelToken, channelResult);
                Collections.sort(channelList);
                recyclerView.getAdapter().notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        Map<DatabaseReference, ValueEventListener> map = new HashMap<DatabaseReference, ValueEventListener>();
        map.put(channelRef, valueEventListener);

        channelListenerMap.put(channelToken, map);
        channelRef.addValueEventListener(valueEventListener);

    }

    public void fetchChannelList() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userChannelsRef = database.getReference("users/" + Setting.getUsername(ClientApp.getContext()) + "/channels");
        userChannelsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                // there is data, remove empty state
                emptyStateContainer.setVisibility(View.GONE);

                String channelToken = dataSnapshot.getKey();
                listenToNewChannelMessage(channelToken);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String channelToken = dataSnapshot.getKey();

                for (Channel channel : channelList) {
                    if (channel.getToken().equals(channelToken)) {
                        Map<DatabaseReference, ValueEventListener> referenceMap = channelListenerMap.get(channelToken);
                        if (referenceMap != null) {
                            for (DatabaseReference dbRef : referenceMap.keySet()) {
                                ValueEventListener valueEventListener = referenceMap.get(dbRef);
                                dbRef.removeEventListener(valueEventListener);
                            }

                        }
                        channelList.remove(channel);
                        break;
                    }
                }

                recyclerView.getAdapter().notifyDataSetChanged();

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    @Override
    public void onResume() {
//        if (Setting.isLogined(ClientApp.getContext())) {
//            fetchChannelList();
//        }
        super.onResume();
    }

    private void initEmptyState(View view) {
        emptyStateContainer = (FrameLayout) view.findViewById(R.id.empty_state_no_internet_container);
        emptyStateTitle = (TextView) view.findViewById(R.id.empty_state_title);
        emptyStateDescription = (TextView) view.findViewById(R.id.empty_state_description);

        if (!Helper.isConnectedToInternet()) {
            emptyStateTitle.setText(R.string.no_network_connection);
        } else {
            emptyStateTitle.setText("Party Time");
            emptyStateDescription.setText("Create funny videos with friends or other users ");
        }

        emptyStateContainer.setVisibility(View.VISIBLE);
        emptyStateContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchChannelList();
            }
        });
    }

}
