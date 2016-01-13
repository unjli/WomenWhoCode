package com.example.womenwhocode.womenwhocode.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.womenwhocode.womenwhocode.R;
import com.example.womenwhocode.womenwhocode.fragments.AddPostDialogFragment;
import com.example.womenwhocode.womenwhocode.fragments.EventPostsFragment;
import com.example.womenwhocode.womenwhocode.models.Event;
import com.example.womenwhocode.womenwhocode.models.Post;
import com.example.womenwhocode.womenwhocode.models.Subscribe;
import com.example.womenwhocode.womenwhocode.utils.LocalDataStore;
import com.example.womenwhocode.womenwhocode.utils.NetworkConnectivityReceiver;
import com.example.womenwhocode.womenwhocode.utils.ThemeUtils;
import com.example.womenwhocode.womenwhocode.widgets.CustomTabStrip;
import com.example.womenwhocode.womenwhocode.widgets.CustomViewPager;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class EventDetailsActivity extends AppCompatActivity implements AddPostDialogFragment.OnSubmitPostListener {

    private static final String SUBSCRIBED_TEXT = "unfollow";
    private static final String SUBSCRIBE_TEXT = "follow";
    private static final String SUBSCRIBERS_TEXT = " followers";
    private TextView tvEventTitle;
    private TextView tvSubscribeCount;
    private Button btnSubscribeIcon;
    private Event event;
    private RelativeLayout rlEvents;
    private String event_id;
    private ParseUser currentUser;
    private Subscribe subscribe;
    private int subscribeCount;
    private Toolbar toolbar;
    private TextView tvToolbarTitle;
    private ImageView ivEventImage;
    private CustomViewPager vpPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // MUST BE SET BEFORE setContentView
        ThemeUtils.onActivityCreateSetTheme(this);
        setContentView(R.layout.activity_event_details);

        // for up button
        // set tool bar to replace actionbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false); // hide the action bar title to only so toolbar title

        // get event from intent
        event_id = getIntent().getStringExtra("event_id");
        currentUser = ParseUser.getCurrentUser();

        setUpView();
        setUpViewPager();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Nullable
    @Override
    public Intent getSupportParentActivityIntent() {
        // get fragment position from parent class
        int fragmentPosition = getIntent().getIntExtra(TimelineActivity.SELECTED_TAB_EXTRA_KEY, 0);
        // send position back to parent
        Intent newIntent = new Intent(this, TimelineActivity.class);
        newIntent.putExtra(TimelineActivity.SELECTED_TAB_EXTRA_KEY, fragmentPosition);
        // Return the created intent as the "up" activity
        return newIntent;

    }

    private void setUpView() {
        // hide scroll view so the progress bar is the center of attention
        rlEvents = (RelativeLayout) findViewById(R.id.rlEvents);
//        rlEvents.setVisibility(ScrollView.INVISIBLE);

        // get title on tool bar
        tvToolbarTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);

        // look up views
        tvEventTitle = (TextView) findViewById(R.id.tvEventTopicTitle);
        tvSubscribeCount = (TextView) findViewById(R.id.tvSubscribeCount);
        btnSubscribeIcon = (Button) findViewById(R.id.btnSubscribeIcon);
        ivEventImage = (ImageView) findViewById(R.id.ivEventImage);

        // query parse
        ParseQuery<Event> query = ParseQuery.getQuery(Event.class);

        if (!NetworkConnectivityReceiver.isNetworkAvailable(this)) {
            query.fromPin(LocalDataStore.EVENT_PIN);
        }

        // Execute the query to find the object with ID
        query.getInBackground(event_id, new GetCallback<Event>() {
            public void done(Event parseEvent, ParseException e) {
                if (e == null) {
                    if (parseEvent != null) {
                        event = parseEvent;
                        setEventData();

                        // set up count
                        subscribeCount = event.getSubscribeCount();
                        tvSubscribeCount.setText(String.valueOf(subscribeCount + SUBSCRIBERS_TEXT));

                        ParseQuery<Subscribe> subscribeParseQuery = ParseQuery.getQuery(Subscribe.class);
                        subscribeParseQuery.whereEqualTo(Subscribe.EVENT_KEY, event);
                        subscribeParseQuery.whereEqualTo(Subscribe.USER_KEY, currentUser);
                        subscribeParseQuery.getFirstInBackground(new GetCallback<Subscribe>() {
                            @Override
                            public void done(Subscribe sub, ParseException e) {
                                if (sub != null) {
                                    subscribe = sub;
                                    if (sub.getSubscribed()) {
                                        btnSubscribeIcon.setText(SUBSCRIBED_TEXT);
                                        ivEventImage.setImageResource(R.drawable.ic_calendar_check);
                                    } else {
                                        btnSubscribeIcon.setText(SUBSCRIBE_TEXT);
                                        ivEventImage.setImageResource(R.drawable.ic_calendar_plus);
                                    }
                                } else {
                                    btnSubscribeIcon.setText(SUBSCRIBE_TEXT);
                                    ivEventImage.setImageResource(R.drawable.ic_calendar_plus);
                                }

                                // hide the progress bar, show the main view
//                                pb.setVisibility(ProgressBar.GONE);
//                                rlEvents.setVisibility(RelativeLayout.VISIBLE);
                            }
                        });
                    } else {
                        Log.d("EVENT_PS_NO_DATA", e.toString());
                    }
                } else {
                    Log.d("EVENT_PS_ERROR", e.toString());
                }
            }
        });
    }

    private void setEventData() {
        // setup views
        tvToolbarTitle.setText(event.getTitle());
        tvEventTitle.setText(event.getTitle());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_event_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onSubscribe(View view) {
        if (subscribe != null) {
            if (subscribe.getSubscribed()) { // maybe just check against icon value
                subscribe.setSubscribed(false);

                // decrement counter
                subscribeCount = event.getSubscribeCount() - 1;
                event.setSubscribeCount(subscribeCount);
                event.saveInBackground();
                tvSubscribeCount.setText(String.valueOf(subscribeCount + SUBSCRIBERS_TEXT));
                ivEventImage.setImageResource(R.drawable.ic_calendar_plus);

                // update subscription
                subscribe.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        btnSubscribeIcon.setText(SUBSCRIBE_TEXT);
                    }
                });
            } else {
                subscribe.setSubscribed(true);

                // increment counter
                subscribeCount = event.getSubscribeCount() + 1;
                event.setSubscribeCount(subscribeCount);
                event.saveInBackground();
                tvSubscribeCount.setText(String.valueOf(subscribeCount + SUBSCRIBERS_TEXT));
                ivEventImage.setImageResource(R.drawable.ic_calendar_check);

                // update subscription
                subscribe.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        btnSubscribeIcon.setText(SUBSCRIBED_TEXT);
                    }
                });
            }
        } else {
            // create subscription - stays the same
            subscribe = new Subscribe();
            subscribe.setSubscribed(true);
            subscribe.setUser(currentUser);
            subscribe.setEvent(event);

            // increment counter
            subscribeCount = event.getSubscribeCount() + 1;
            event.setSubscribeCount(subscribeCount);
            event.saveInBackground();
            tvSubscribeCount.setText(String.valueOf(subscribeCount + SUBSCRIBERS_TEXT));
            ivEventImage.setImageResource(R.drawable.ic_calendar_check);

            subscribe.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    btnSubscribeIcon.setText(SUBSCRIBED_TEXT);
                }
            });
        }
    }

    public void onEventInfo(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.getUrl()));
        startActivity(browserIntent);
    }

    public void onLaunchAddPostDialog(MenuItem item) {
        FragmentManager fm = getSupportFragmentManager();
        AddPostDialogFragment addPostDialogFragment = AddPostDialogFragment.newInstance();
        addPostDialogFragment.show(fm, "fragment_add_post");
    }

    @Override
    public void onSubmitPostListener(String inputText, Bitmap finalImg) {
        addPost(inputText, finalImg);
        CoordinatorLayout v = (CoordinatorLayout) findViewById(R.id.rlPostLists);
        Snackbar.make(v, R.string.thanks_add_post, Snackbar.LENGTH_SHORT).show();
        // FIXME: make it so you go to the last item position when this is final so the user can see their post was submitted
        // FIXME: add post to bottom of the list!
    }

    private void addPost(String postBody, Bitmap finalImg) {
        final Post post = new Post();
        post.setDescription(postBody);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (finalImg != null) {
            finalImg.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            // get byte array here
            byte[] bytearray = stream.toByteArray();
            ParseFile imgFile = new ParseFile("profileImg.png", bytearray);
            imgFile.saveInBackground();
            post.setPostPicFile(imgFile);
        }
        post.setUser(currentUser);
        post.setEvent(event);
        post.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                EventPostsFragment pf = (EventPostsFragment) getSupportFragmentManager().getFragments().get(0); // make sure it will aways be that 0! posts are zero in view pager
                setTab();
                if (null != pf) {
                    pf.setReceivedPost(post);
                }
            }
        });

//        // notify fragment
//        EventPostsFragment pf = (EventPostsFragment) getSupportFragmentManager().getFragments().get(0); // make sure it will aways be that 0! posts are zero in view pager
//        setTab();
//        if(null != pf) {
//            pf.setReceivedPost(post);
//        }
    }

    private void setUpViewPager() {
        // Get the viewpager
        vpPager = (CustomViewPager) findViewById(R.id.viewpager);

        // Set the viewpager adapter for the pager
        vpPager.setAdapter(new PagerAdapter(getSupportFragmentManager()));

        // Find the sliding tabstrip
        CustomTabStrip tabStrip = (CustomTabStrip) findViewById(R.id.tabs);
        tabStrip.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Montserrat-Regular.ttf"), Typeface.NORMAL);


        // Attach the tabstrip to the viewpager
        tabStrip.setViewPager(vpPager);

        // Hides header card when the chat view is selected
        tabStrip.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    rlEvents.setVisibility(View.GONE);
                } else {
                    rlEvents.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }
        });
    }

    private void setTab() {
        // Switch to page based on index
        vpPager.setCurrentItem(0);
    }

    public class PagerAdapter extends FragmentPagerAdapter {
        private final String[] tabTitles = {"posts"};

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        // The order and creation fo fragments within the pager
        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return EventPostsFragment.newInstance(event_id);
            } else return null;
        }

        // Return the tab title
        @Override
        public CharSequence getPageTitle(int position) {
            return tabTitles[position];
        }

        // How many fragments there are to swipe between
        @Override
        public int getCount() {
            return tabTitles.length;
        }
    }
}
