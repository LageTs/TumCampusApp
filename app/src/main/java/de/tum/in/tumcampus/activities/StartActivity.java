package de.tum.in.tumcampus.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import de.tum.in.tumcampus.R;
import de.tum.in.tumcampus.activities.wizzard.WizNavExtrasActivity;
import de.tum.in.tumcampus.activities.wizzard.WizNavStartActivity;
import de.tum.in.tumcampus.adapters.StartSectionsPagerAdapter;
import de.tum.in.tumcampus.auxiliary.Const;
import de.tum.in.tumcampus.auxiliary.PersonalLayoutManager;
import de.tum.in.tumcampus.services.BackgroundService;
import de.tum.in.tumcampus.services.ImportService;
import de.tum.in.tumcampus.services.SilenceService;
import de.tum.in.tumcampus.adapters.SideNavigationAdapter;

/**
 * Main activity displaying the categories and menu items to start each activity (feature)
 * 
 * @author Sascha Moecker
 */
public class StartActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
	public static final int DEFAULT_SECTION = 1;
	public static final String LAST_CHOOSEN_SECTION = "last_choosen_section";
	public static final int REQ_CODE_COLOR_CHANGE = 0;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	StartSectionsPagerAdapter mSectionsPagerAdapter;
	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
    boolean shouldRestartOnResume;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;

	/**
	 * Receiver for Services
	 */
	private final BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ImportService.BROADCAST_NAME)) {
				String message = intent.getStringExtra(Const.MESSAGE_EXTRA);
				String action = intent.getStringExtra(Const.ACTION_EXTRA);

				if (action.length() != 0) {
					Log.i(this.getClass().getSimpleName(), message);
				}
			}
			if (intent.getAction().equals(WizNavExtrasActivity.BROADCAST_NAME)) {
				Log.i(this.getClass().getSimpleName(), "Color has changed");
				StartActivity.this.shouldRestartOnResume = true;
			}
		}
	};
    private ActionBarDrawerToggle mDrawerToggle;


    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Check if there is a result key in an intent
		if (data != null && data.hasExtra(Const.PREFS_HAVE_CHANGED) && data.getBooleanExtra(Const.PREFS_HAVE_CHANGED, false)) {
			// Restart the Activity if prefs have changed
			Intent intent = this.getIntent();
			this.finish();
			this.startActivity(intent);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_start);

        setTitle(getString(R.string.campus_app));

		// Create the adapter that will return a fragment for each of the
		// primary sections of the app.
		this.mSectionsPagerAdapter = new StartSectionsPagerAdapter(this, this.getSupportFragmentManager());

		// Workaround for new API version. There was a security update which
		// disallows applications to execute HTTP request in the GUI main
		// thread.
		if (android.os.Build.VERSION.SDK_INT > 8) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		// Set up the ViewPager with the sections adapter.
		this.mViewPager = (ViewPager) this.findViewById(R.id.pager);
		this.mViewPager.setAdapter(this.mSectionsPagerAdapter);
		this.mViewPager.setCurrentItem(DEFAULT_SECTION);

		// Registers receiver for download and import
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ImportService.BROADCAST_NAME);
		intentFilter.addAction(WizNavExtrasActivity.BROADCAST_NAME);
		this.registerReceiver(this.receiver, intentFilter);

		// Imports default values into database
		Intent service;
		service = new Intent(this, ImportService.class);
		service.putExtra(Const.ACTION_EXTRA, Const.DEFAULTS);
		this.startService(service);

		// Start silence Service (if already started it will just invoke a check)
		service = new Intent(this, SilenceService.class);
		this.startService(service);

		// Start daily Service (same here: if already started it will just invoke a check)
		service = new Intent(this, BackgroundService.class);
		this.startService(service);

		Boolean hideWizzardOnStartup = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Const.HIDE_WIZZARD_ON_STARTUP, false);

		// Check for important news
		// TODO: check if there are any news avaible on the to be implemented webservice - for now hide the icon in the menu_start_activity

		// Setup the navigation drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new SideNavigationAdapter(this));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(this);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

		// Check the flag if user wants the wizzard to open at startup
		if (!hideWizzardOnStartup) {
			Intent intent = new Intent(this, WizNavStartActivity.class);
			this.startActivity(intent);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		this.getMenuInflater().inflate(R.menu.menu_start_activity, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// important to unregister the broadcast receiver
		this.unregisterReceiver(this.receiver);
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
       //TODO menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
		switch (item.getItemId()) {
		case R.id.action_settings:
			// Opens the preferences screen
			Intent intent = new Intent(this, UserPreferencesActivity.class);
			this.startActivityForResult(intent, REQ_CODE_COLOR_CHANGE);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		PersonalLayoutManager.setColorForId(this, R.id.pager_title_strip);
		if (this.shouldRestartOnResume) {
			// finish and restart myself
			this.finish();
			Intent intent = new Intent(this, this.getClass());
			this.startActivity(intent);
		}
	}

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        SideNavigationAdapter.SideNavigationItem sideNavigationItem = (SideNavigationAdapter.SideNavigationItem)adapterView.getAdapter().getItem(position);
        try {
            String a = this.getPackageName() + ".activities." + sideNavigationItem.getActivity();
            Class<?> clazz = Class.forName(a);
            Intent newActivity = new Intent(this.getApplicationContext(), clazz);
            this.startActivity(newActivity);
        } catch (ClassNotFoundException e) {
            Log.w("tca", "ClassNotFound", e);
        }
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }
}