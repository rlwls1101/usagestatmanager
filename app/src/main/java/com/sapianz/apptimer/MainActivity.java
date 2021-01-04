package com.sapianz.apptimer;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.txusballesteros.widgets.FitChart;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private AppTimerDbHelper dbHelper;
    UsageStatsManager usageStatsManager;
    Context context;
    ArrayList<HashMap> topAppsList = new ArrayList<>();
    ConstraintLayout loader;
    AppListAdapter appListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (!hasUsageStatsPermission(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permissions Needed")
                    .setMessage("AppTimer requires extra permissions to view your app usage data. Grant these permissions on the next screen.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), 1);
                        }
                    });
            builder.show();
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permissions Needed")
                    .setMessage("AppTimer requires extra permissions to show an app usage HUD on your screen. Grant these permissions on the next screen.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION), 2);
                        }
                    });
            builder.show();
        } else {
            dbHelper = new AppTimerDbHelper(this);
            usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            loader = findViewById(R.id.dataLoader);
            context = this;

            recyclerView = findViewById(R.id.topAppsRecyclerView);
            recyclerView.setHasFixedSize(true);
            recyclerView.addItemDecoration(new DividerItemDecoration(this,
                    DividerItemDecoration.VERTICAL));

            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);

            refreshData();

         //   MobileAds.initialize(this, "ca-app-pub-sample.app.id");

        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (!hasUsageStatsPermission(this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permissions Needed")
                        .setMessage("AppTimer has not been granted permissions and will now exit.")
                        .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finishAndRemoveTask();
                            }
                        });
                builder.show();
            } else {
                recreate();
            }
        }

        if (requestCode == 2) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permissions Needed")
                        .setMessage("AppTimer has not been granted permissions and will now exit.")
                        .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finishAndRemoveTask();
                            }
                        });
                builder.show();
            } else {
                recreate();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }



    private void refreshData() {
        new RefreshDatabaseTask().execute();
    }

    private class RefreshDatabaseTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            loader.setVisibility(View.VISIBLE);

            DataUtils dataUtils = new DataUtils(context);
            dataUtils.refreshDatabase();

            topAppsList = dataUtils.getTopApps();

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            appListAdapter = new AppListAdapter(topAppsList);
            appListAdapter.notifyDataSetChanged();
            recyclerView.setAdapter(appListAdapter);
        }
    }
}
