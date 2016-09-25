package turtler.voyageur.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import turtler.voyageur.R;
import turtler.voyageur.adapters.TripAdapter;
import turtler.voyageur.models.FriendTripRelation;
import turtler.voyageur.models.Trip;
import turtler.voyageur.models.User;

/**
 * Created by cwong on 9/11/16.
 */
public class ProfileFragment extends Fragment implements CreateTripFragment.CreateTripFragmentListener {
    @BindView(R.id.rvTrips) RecyclerView rvTrips;
    @BindView(R.id.fabAddTrip) FloatingActionButton fabAddTrip;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    ArrayList<Trip> trips;
    TripAdapter tripAdapter;

    public static ProfileFragment newInstance() {
        Bundle args = new Bundle();
        ProfileFragment fragment = new ProfileFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        ButterKnife.bind(this, view);

        mToolbar.setNavigationIcon(R.mipmap.ic_whitelogo);
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        parentActivity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        parentActivity.getSupportActionBar().setDisplayShowTitleEnabled(true);

        trips = new ArrayList<>();
        tripAdapter = new TripAdapter(getContext(), trips);
        rvTrips.setAdapter(tripAdapter);
        rvTrips.setLayoutManager(new LinearLayoutManager(getContext()));

        FloatingActionButton fabAddEvent = (FloatingActionButton) getActivity().findViewById(R.id.fabAddEvent);
        if (fabAddEvent != null) {
            fabAddEvent.setVisibility(View.GONE);
        }

        fabAddTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCreateTripFragment();
            }
        });
        populateTrips();
        return view;
    }

    public void populateTrips() {
        if (getArguments() != null && getArguments().containsKey("email")) {
            //looking at friend's profile
            fabAddTrip.setVisibility(View.GONE);

            String email = getArguments().getString("email");
            ParseQuery<User> pq = new ParseQuery("_User");
            pq.whereEqualTo("email", email);
            pq.findInBackground(new FindCallback<User>() {
                @Override
                public void done(List<User> users, ParseException e) {
                    if (users != null && users.size() > 0) {
                        User user = users.get(0);
                        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
                        parentActivity.getSupportActionBar().setTitle(user.getName());
                        mToolbar.setTitle(user.getName());
                        getUserTrips(user.getObjectId());
                    }
                }
            });
        } else {
            //looking at own profile
            fabAddTrip.setVisibility(View.VISIBLE);
            User currUser = (User) User.getCurrentUser();
            AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
            parentActivity.getSupportActionBar().setTitle(currUser.getName());
            mToolbar.setTitle(currUser.getName());
            getUserTrips(currUser.getObjectId());
        }
    }

    public void getUserTrips(String userId) {
        try {
            ParseQuery<FriendTripRelation> friendTripRelationParseQuery = new ParseQuery<FriendTripRelation>("FriendTripRelation");
            List<FriendTripRelation> friendTripRelations = friendTripRelationParseQuery
                    .whereEqualTo("friendId", userId)
                    .selectKeys(new ArrayList<>(Arrays.asList("tripId")))
                    .find();

            ArrayList<String> tripIds =  new ArrayList<>();
            for (FriendTripRelation rel : friendTripRelations) {
                tripIds.add((String) rel.get("tripId"));
            }
            ParseQuery<Trip> tripParseQuery = new ParseQuery<>("Trip");
            tripParseQuery.whereContainedIn("objectId", tripIds);
            ArrayList<Trip> trips = (ArrayList<Trip>) tripParseQuery.find();
            loadUserTrips(trips);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void loadUserTrips(ArrayList<Trip> userTrips) {
        int curSize = tripAdapter.getItemCount();
        trips.addAll(userTrips);
        tripAdapter.notifyItemRangeInserted(curSize, userTrips.size());
    }

    private void showCreateTripFragment() {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        CreateTripFragment tripFragment = CreateTripFragment.newInstance();
        tripFragment.setTargetFragment(ProfileFragment.this, 300);
        tripFragment.show(fm, "fragment_create_trip");
    }

    @Override
    public void onFinishCreateTripDialog(Trip newTrip) {
        trips.add(0, newTrip);
        tripAdapter.notifyItemInserted(0);
    }
}