package com.dropbox.sample.datastoretask;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxException;

public class DatastoreTaskActivity extends Activity {
    private static final String APP_KEY = "lbznawovo76y2uo";
    private static final String APP_SECRET = "fndy639jct5bm3p";
    private static final String TAG = "DatastoreTaskActivity";

    private static final int REQUEST_LINK_TO_DBX = 0;

    private DbxAccountManager mDbxAcctMgr;
    private DbxDatastore mDatastore;
    private TaskTable mTaskTable;

    private DbxDatastore.SyncStatusListener mDatastoreListener = new DbxDatastore.SyncStatusListener() {
        @Override
        public void onDatastoreStatusChange(DbxDatastore ds) {
            Log.d(TAG, "SYNC STATUS: " + ds.getSyncStatus().toString());
            if (ds.getSyncStatus().hasIncoming) {
                try {
                    mDatastore.sync();
                } catch (DbxException e) {
                    handleException(e);
                }
            }
            updateList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);
        setContentView(R.layout.activity_datastore_task);

        Button linkButton = (Button) findViewById(R.id.link_button);
        linkButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDbxAcctMgr.startLink((Activity)DatastoreTaskActivity.this, REQUEST_LINK_TO_DBX);
            }
        });

        Button newTaskButton = (Button) findViewById(R.id.new_task_button);
        newTaskButton.setOnClickListener(new OnClickListener(){
            public void onClick(View v) {
                EditText nameInput = (EditText) findViewById(R.id.new_task_name);
                String taskName = nameInput.getText().toString();
                nameInput.setText("");

                try {
                    if (taskName.length() > 0) {
                        mTaskTable.createTask(taskName);
                    }
                } catch (DbxException e) {
                    handleException(e);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDbxAcctMgr.hasLinkedAccount()) {
            // already have an account linked
            showTasksView();
        } else {
            // Hide the add-task UI and show the link button
            findViewById(R.id.link_button).setVisibility(View.VISIBLE);
            findViewById(R.id.new_task_ui).setVisibility(View.GONE);
            ((ViewGroup) findViewById(R.id.task_table)).removeAllViews();
        }
    }

    protected void onPause() {
        super.onPause();
        if (mDatastore != null) {
            mDatastore.removeSyncStatusListener(mDatastoreListener);
            mDatastore.close();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == RESULT_OK) {
                showTasksView();
            } else {
                // ... Link failed or was cancelled by the user.
                Toast.makeText(this, "Link to Dropbox failed.", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showTasksView() {
        try {
            if (null == mDatastore) {
                mDatastore = DbxDatastore.openDefault(mDbxAcctMgr.getLinkedAccount());
            }
            mTaskTable = new TaskTable(mDatastore);

            // Hide the Link button and show the add-task UI
            findViewById(R.id.link_button).setVisibility(View.GONE);
            findViewById(R.id.new_task_ui).setVisibility(View.VISIBLE);

            mDatastore.addSyncStatusListener(mDatastoreListener);
            mDatastore.sync();
            updateList();

            mDatastore.sync();
        } catch (DbxException e) {
            handleException(e);
        }

    }

    private void updateList() {
        List<TaskTable.Task> tasks;
        try {
            tasks = mTaskTable.getTasksSorted();
        } catch (DbxException e) {
            handleException(e);
            return;
        }

        TableLayout tableView = (TableLayout) findViewById(R.id.task_table);
        tableView.removeAllViews();

        for (final TaskTable.Task task: tasks) {
            TableRow row = (TableRow)getLayoutInflater().inflate(R.layout.task_row, null);
            tableView.addView(row);
            Button delete = (Button)row.findViewById(R.id.delete_button);
            delete.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    try {
                        task.delete();
                    } catch (DbxException e) {
                        handleException(e);
                    }
                }
            });

            CheckBox toggleComplete = (CheckBox)row.findViewById(R.id.completed_checkbox);
            toggleComplete.setChecked(task.isCompleted());
            toggleComplete.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    try {
                        task.toggleCompleted();
                    } catch (DbxException e) {
                        handleException(e);
                    }
                }
            });

            TextView text = (TextView)row.findViewById(R.id.task_text);
            text.setText(task.getName());
            if (task.isCompleted()) {
                text.setPaintFlags(text.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }

    private void handleException(DbxException e) {
        e.printStackTrace();
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
