package at.searles.fractview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by searles on 10.06.17.
 */

public class DialogHelper {

    public static interface DialogFunction {
        void apply(DialogInterface d);
    }

    public static void error(Context context, String message) {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();

        Log.e(elements[elements.length - 2].toString(), message);

        Toast.makeText(context, "ERROR: " + message, Toast.LENGTH_LONG).show();
    }

    public static void info(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void inputText(Context context, String title, String defaultInput, Commons.KeyAction positiveAction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        final EditText input = new EditText(context);

        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredText = input.getText().toString();

                positiveAction.apply(enteredText);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void showConfirmDialog(Context context, String message, Runnable action) {
        AlertDialog.Builder yesNoBuilder = new AlertDialog.Builder(context);
        yesNoBuilder.setIcon(android.R.drawable.ic_delete);
        yesNoBuilder.setTitle(message);

        yesNoBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                action.run();
                dialogInterface.dismiss();
            }
        });

        yesNoBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        });

        yesNoBuilder.show();
    }

    public static void inputCustom(Context context, String title, int layoutId, DialogFunction initView, DialogFunction acceptView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        builder.setView(layoutId);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                acceptView.apply(dialog);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.show();

        initView.apply(dialog);
    }
}
