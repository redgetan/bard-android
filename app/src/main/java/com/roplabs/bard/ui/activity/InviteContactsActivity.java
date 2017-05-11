package com.roplabs.bard.ui.activity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.roplabs.bard.R;
import com.roplabs.bard.util.BardLogger;
import com.roplabs.bard.util.Helper;

import static com.roplabs.bard.util.Helper.MY_PERMISSIONS_REQUEST_READ_CONTACTS;

public class InviteContactsActivity extends BaseActivity {

    private SimpleCursorAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BardLogger.log("Invite Contacts onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_contacts);

        TextView title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        title.setText("Contacts");
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // click on 'up' button in the action bar, handle it here
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
