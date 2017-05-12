package com.roplabs.bard.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.roplabs.bard.ClientApp;
import com.roplabs.bard.R;
import com.roplabs.bard.adapters.ContactCursorAdapter;
import com.roplabs.bard.models.Setting;
import com.roplabs.bard.util.Analytics;
import com.roplabs.bard.util.BardLogger;
import com.roplabs.bard.util.Helper;

public class ContactsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener {

    /*
     * Defines an array that contains column names to move from
     * the Cursor to the ListView.
     */
    @SuppressLint("InlinedApi")
    private final static String[] FROM_COLUMNS = {
            Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.HONEYCOMB ?
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
                    ContactsContract.Contacts.DISPLAY_NAME
    };

    @SuppressLint("InlinedApi")
    private static final String[] PROJECTION =
            {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    Build.VERSION.SDK_INT
                            >= Build.VERSION_CODES.HONEYCOMB ?
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
                            ContactsContract.Contacts.DISPLAY_NAME

            };

    private String mSearchString = "";

    // Define a ListView object
    ListView mContactsList;
    private ContactCursorAdapter mCursorAdapter;

    // Empty public constructor, required by the system
    public ContactsFragment() {}

    // A UI Fragment must inflate its View
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the fragment layout
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);

        // Gets the ListView from the View list of the parent activity
        mContactsList =
                (ListView) view.findViewById(R.id.contacts_list);
        // Gets a CursorAdapter
//        mCursorAdapter = new SimpleCursorAdapter(
//                getActivity(),
//                R.layout.contacts_list_item,
//                null,
//                FROM_COLUMNS, TO_IDS,
//                0);
        mCursorAdapter = new ContactCursorAdapter(getActivity());

        // Sets the adapter for the ListView
        mContactsList.setAdapter(mCursorAdapter);
        mContactsList.setDivider(null);


        mContactsList.setOnItemClickListener(this);
        getLoaderManager().initLoader(0, null, this);

        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {
        Uri contentUri;

        if (mCursorAdapter.getSearchTerm().isEmpty()) {
            // Since there's no search string, use the content URI that searches the entire
            // Contacts table
            contentUri = ContactCursorAdapter.ContactsQuery.CONTENT_URI;
        } else {
            // Since there's a search string, use the special content Uri that searches the
            // Contacts table. The URI consists of a base Uri and the search string.
            contentUri =
                    Uri.withAppendedPath(ContactCursorAdapter.ContactsQuery.FILTER_URI, Uri.encode(mCursorAdapter.getSearchTerm()));
        }

        return new CursorLoader(getActivity(),
                contentUri,
                ContactCursorAdapter.ContactsQuery.PROJECTION,
                ContactCursorAdapter.ContactsQuery.SELECTION,
                null,
                ContactCursorAdapter.ContactsQuery.SORT_ORDER);

    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Put the result Cursor in the adapter for the ListView
        mCursorAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Delete the reference to the existing Cursor
        mCursorAdapter.swapCursor(null);

    }


    @Override
    public void onItemClick(
            AdapterView<?> parent, View item, int position, long rowID) {
        // Get the Cursor
        Cursor cursor = mCursorAdapter.getCursor();
        // Move to the selected contact
        cursor.moveToPosition(position);

//        final Uri uri = ContactsContract.Contacts.getLookupUri(
//                cursor.getLong(ContactCursorAdapter.ContactsQuery.ID),
//                cursor.getString(ContactCursorAdapter.ContactsQuery.LOOKUP_KEY));

//        Long phoneNumber = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));

        Long contactId = cursor.getLong(ContactCursorAdapter.ContactsQuery.ID);

//        String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
        Helper.sendSMSInvite(getActivity(), contactId);


        // Get the _ID value
        /*
         * You can use mContactUri as the content URI for retrieving
         * the details for a contact.
         */
    }



}

