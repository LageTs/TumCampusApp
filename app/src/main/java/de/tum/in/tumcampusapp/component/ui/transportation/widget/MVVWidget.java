package de.tum.in.tumcampusapp.component.ui.transportation.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Timer;
import java.util.TimerTask;

import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.component.ui.transportation.TransportController;
import de.tum.in.tumcampusapp.component.ui.transportation.model.efa.WidgetDepartures;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link MVVWidgetConfigureActivity MVVWidgetConfigureActivity}
 */
public class MVVWidget extends AppWidgetProvider {

    private static final String BROADCAST_RELOAD_ALL_ALARM = "de.tum.in.newtumcampus.intent.action.BROADCAST_MVV_WIDGET_RELOAD_ALL_ALARM";
    private static final String BROADCAST_RELOAD_ALL = "de.tum.in.newtumcampus.intent.action.BROADCAST_MVV_WIDGET_RELOAD_ALL";
    static final String MVV_WIDGET_FORCE_RELOAD = "de.tum.in.newtumcampus.intent.action.MVV_WIDGET_FORCE_RELOAD";
    private static Timer timer;
    private static TransportController transportManager;

    public static final int UPDATE_ALARM_DELAY = 60 * 1000;
    public static final int UPDATE_TRIGGER_DELAY = 20 * 1000;
    public static final int DOWNLOAD_DELAY = 5 * 60 * 1000;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (transportManager == null) {
            transportManager = new TransportController(context);
        }
        updateAppWidgets(context, appWidgetManager, appWidgetIds);
        setAlarm(context);
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        if (transportManager == null) {
            transportManager = new TransportController(context);
        }
        // When the user deletes the widget, delete the associated setting from the database.
        for (int appWidgetId : appWidgetIds) {
            transportManager.deleteWidget(appWidgetId);
        }
        transportManager = null;
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        if (transportManager == null) {
            transportManager = new TransportController(context);
        }
        // Enter relevant functionality for when the first widget is created
        setAlarm(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Cancel alarm as the last widget has been removed
        setAlarm(context);
        super.onDisabled(context);
    }

    /**
     * If no alarm is running yet a new alarm is started which repeats every minute
     */
    public static void setAlarm(Context context) {
        boolean autoReload = false;
        for (int appWidgetId : getActiveWidgetIds(context)) {
            WidgetDepartures widgetDepartures = transportManager.getWidget(appWidgetId);
            if (widgetDepartures != null && widgetDepartures.getAutoReload()) {
                autoReload = true;
                break;
            }
        }
        Intent intent = new Intent(context, MVVWidget.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(sender);
        if (autoReload) {
            intent.setAction(MVVWidget.BROADCAST_RELOAD_ALL_ALARM);
            am.setRepeating(AlarmManager.RTC, 5000, UPDATE_ALARM_DELAY, sender);
        }
    }

    /**
     * Plans updates the widgets after 30s and 60s
     */
    static void planUpdates(final Context context) {
        if (MVVWidget.timer == null) {
            MVVWidget.timer = new Timer();
        }
        for (int i = 1; i <= 3; i++) {
            MVVWidget.timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Intent reloadIntent = new Intent(context, MVVWidget.class);
                    reloadIntent.setAction(MVVWidget.BROADCAST_RELOAD_ALL);
                    context.sendBroadcast(reloadIntent);
                }
            }, UPDATE_TRIGGER_DELAY * i);
        }
    }

    /**
     * Updates the content of multiple widgets
     *
     * @param appWidgetIds the array of widget ids to update
     */
    static void updateAppWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            MVVWidget.updateAppWidget(context, appWidgetManager, widgetId, false);
        }
    }

    /**
     * Updates the content of the widget
     *
     * @param appWidgetId the id of the widget to update
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, boolean forceLoadData) {
        // Get the settingsPrefix for this widget from the database
        WidgetDepartures widgetDepartures = transportManager.getWidget(appWidgetId);

        // Instantiate the RemoteViews object for the app widget layout.
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.mvv_widget);
        rv.setTextViewText(R.id.mvv_widget_station, widgetDepartures.getStation());

        // Set up offline symbol (may be shown one update delayed)
        rv.setViewVisibility(R.id.mvv_widget_offline, widgetDepartures.isOffline() ? View.VISIBLE : View.INVISIBLE);

        // Set up the configuration activity listeners
        Intent configIntent = new Intent(context, MVVWidgetConfigureActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.mvv_widget_header, pendingIntent);

        // Set up the reload functionality
        Intent reloadIntent = new Intent(context, MVVWidget.class);
        reloadIntent.setAction(MVVWidget.MVV_WIDGET_FORCE_RELOAD);
        reloadIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingReloadIntent = PendingIntent.getBroadcast(context, appWidgetId, reloadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.mvv_widget_reload, pendingReloadIntent);
        rv.setViewVisibility(R.id.mvv_widget_reload, widgetDepartures.getAutoReload() ? View.GONE : View.VISIBLE);

        // Set up the intent that starts the MVVWidgetService, which will
        // provide the departure times for this station
        Intent intent = new Intent(context, MVVWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra(MVVWidget.MVV_WIDGET_FORCE_RELOAD, forceLoadData);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        rv.setRemoteAdapter(R.id.mvv_widget_listview, intent);

        // The empty view is displayed when the collection has no items.
        // It should be in the same layout used to instantiate the RemoteViews
        // object above.
        rv.setEmptyView(R.id.mvv_widget_listview, R.id.empty_list_item);

        // Instruct the widget manager to update the widget
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.mvv_widget_listview);
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (transportManager == null) {
            transportManager = new TransportController(context);
        }

        String action = intent.getAction();
        if (action == null || action.equals(MVVWidget.BROADCAST_RELOAD_ALL)) {
            updateAppWidgets(context, AppWidgetManager.getInstance(context), getActiveWidgetIds(context));
        } else if (action.equals(MVVWidget.BROADCAST_RELOAD_ALL_ALARM)) {
            planUpdates(context);
            updateAppWidgets(context, AppWidgetManager.getInstance(context), getActiveWidgetIds(context));
        } else if (action.equals(MVVWidget.MVV_WIDGET_FORCE_RELOAD)) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId >= 0) {
                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId, true);
            }
        }
        super.onReceive(context, intent);
    }

    private static int[] getActiveWidgetIds(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, MVVWidget.class);
        return appWidgetManager.getAppWidgetIds(thisWidget);
    }
}
