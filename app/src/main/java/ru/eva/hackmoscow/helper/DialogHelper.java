package ru.eva.hackmoscow.helper;

import android.app.Activity;
import android.app.AlertDialog;

import ru.eva.hackmoscow.R;

public class DialogHelper {
    private static DialogHelper instance;

    public static DialogHelper getInstance() {
        if (instance == null) {
            instance = new DialogHelper();
        }
        return instance;
    }

    public void showAlertDialog(Activity activity, String message) {
        System.out.println("ERROR: Cannot initialize Map Fragment" + message);
        new AlertDialog.Builder(activity)
                .setMessage(message)
                .setTitle(R.string.engine_init_error)
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> activity.finish()).create().show();
    }
}
