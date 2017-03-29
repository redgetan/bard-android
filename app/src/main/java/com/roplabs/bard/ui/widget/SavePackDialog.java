package com.roplabs.bard.ui.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;

/**
 * Created by reg on 2017-03-28.
 */
public class SavePackDialog {
    private String packName;
    private AlertDialog.Builder builder;

    private OnPackDialogEvent listener;

    // Define the events that the fragment will use to communicate
    public interface OnPackDialogEvent  {
        // This can be any number of events to be sent to the activity
        public void onSavePackConfirm(String packName);
    }


    public SavePackDialog(Context context) {

        if (context instanceof OnPackDialogEvent) {
            listener = (OnPackDialogEvent) context;
        } else {
            throw new ClassCastException(context.toString()
                    + " must implement SavePackDialog.OnPackDialogEvent");
        }

        builder = new AlertDialog.Builder(context);
        builder.setTitle("Name your pack");

// Set up the input
        final EditText input = new EditText(context);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                packName = input.getText().toString();
                listener.onSavePackConfirm(packName);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
    }

    public void show() {
        builder.show();
    }

    public String getPackName() {
        return packName;
    }
}
