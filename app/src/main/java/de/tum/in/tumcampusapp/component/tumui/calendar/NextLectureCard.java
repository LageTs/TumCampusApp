package de.tum.in.tumcampusapp.component.tumui.calendar;

import android.app.Notification;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.component.tumui.roomfinder.RoomFinderActivity;
import de.tum.in.tumcampusapp.component.ui.overview.CardManager;
import de.tum.in.tumcampusapp.component.ui.overview.card.CardViewHolder;
import de.tum.in.tumcampusapp.component.ui.overview.card.NotificationAwareCard;
import de.tum.in.tumcampusapp.utils.DateTimeUtils;

public class NextLectureCard extends NotificationAwareCard {

    private static final String NEXT_LECTURE_DATE = "next_date";
    private final static int[] IDS = {
            R.id.lecture_1,
            R.id.lecture_2,
            R.id.lecture_3,
            R.id.lecture_4
    };
    private TextView mLocation;
    private final List<CalendarItem> lectures = new ArrayList<>();
    private TextView mTimeView;
    private int mSelected;
    private TextView mEvent;

    public NextLectureCard(Context context) {
        super(CardManager.CARD_NEXT_LECTURE, context, "card_next_lecture");
    }

    public static CardViewHolder inflateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.card_next_lecture_item, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public String getTitle() {
        return lectures.get(mSelected).title;
    }

    @Override
    public void updateViewHolder(RecyclerView.ViewHolder viewHolder) {
        super.updateViewHolder(viewHolder);
        setMCard(viewHolder.itemView);
        setMLinearLayout(getMCard().findViewById(R.id.card_view));
        setMTitleView(getMCard().findViewById(R.id.card_title));
        mTimeView = getMCard().findViewById(R.id.card_time);
        mLocation = getMCard().findViewById(R.id.card_location_action);
        mEvent = getMCard().findViewById(R.id.card_event_action);

        showItem(0);

        int i = 0;
        if (lectures.size() > 1) {
            for (; i < lectures.size(); i++) {
                final int j = i;
                Button text = getMCard().findViewById(IDS[i]);
                text.setOnClickListener(view -> showItem(j));
            }
        }
        for (; i < 4; i++) {
            View text = getMCard().findViewById(IDS[i]);
            text.setVisibility(View.GONE);
        }
    }

    private void showItem(int sel) {
        // Set selection on the buttons
        mSelected = sel;
        for (int i = 0; i < 4; i++) {
            getMCard().findViewById(IDS[i])
                      .setSelected(i == sel);
        }

        final CalendarItem item = lectures.get(sel);

        // Set current title
        getMTitleView().setText(getTitle());

        //Add content
        mTimeView.setText(DateTimeUtils.INSTANCE.formatFutureTime(item.start, getContext()));

        //Add location with link to room finder
        if (item.location == null || item.location.isEmpty()) {
            mLocation.setVisibility(View.GONE);
        } else {
            mLocation.setText(item.location);
            mLocation.setOnClickListener(v -> {
                Intent i = new Intent(getContext(), RoomFinderActivity.class);
                i.putExtra(SearchManager.QUERY, item.locationForSearch);
                getContext().startActivity(i);
            });
        }

        DateTimeFormatter dayOfWeek = DateTimeFormat.forPattern("EEEE, ").withLocale(Locale.getDefault());
        DateTimeFormatter time = DateTimeFormat.shortTime();
        mEvent.setText(String.format("%s%s - %s", dayOfWeek.print(item.start), time.print(item.start), time.print(item.end)));
        mEvent.setOnClickListener(view -> {
            Intent i = new Intent(getContext(), CalendarActivity.class);
            CalendarItem item1 = lectures.get(mSelected);
            i.putExtra(CalendarActivity.EVENT_TIME, item1.start.getMillis());
            getContext().startActivity(i);
        });
    }

    @Override
    protected void discard(Editor editor) {
        CalendarItem item = lectures.get(lectures.size() - 1);
        editor.putLong(NEXT_LECTURE_DATE, item.start.getMillis());
    }

    @Override
    protected boolean shouldShow(SharedPreferences prefs) {
        CalendarItem item = lectures.get(0);
        long prevTime = prefs.getLong(NEXT_LECTURE_DATE, 0);
        return item.start.getMillis() > prevTime;
    }

    @Override
    public Intent getIntent() {
        return null;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    protected Notification fillNotification(NotificationCompat.Builder notificationBuilder) {
        CalendarItem item = lectures.get(0);
        final String time = DateTimeUtils.INSTANCE.formatFutureTime(item.start, getContext());
        notificationBuilder.setContentText(item.title + '\n' + time);
        notificationBuilder.setSmallIcon(R.drawable.ic_notification);
        Bitmap bm = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.wear_next_lecture);
        notificationBuilder.extend(new NotificationCompat.WearableExtender().setBackground(bm));
        return notificationBuilder.build();
    }

    public void setLectures(List<de.tum.in.tumcampusapp.component.tumui.calendar.model.CalendarItem> calendarItems) {
        for (de.tum.in.tumcampusapp.component.tumui.calendar.model.CalendarItem calendarItem : calendarItems) {
            CalendarItem item = new CalendarItem();
            item.start = calendarItem.getDtstart();
            item.end = calendarItem.getDtend();

            // Extract course title
            item.title = calendarItem.getFormattedTitle();

            // Handle location
            item.location = calendarItem.getEventLocation();
            // This is the location in a format which is useful for searches:
            item.locationForSearch = calendarItem.getLocation();

            lectures.add(item);
        }
    }

    private static class CalendarItem {
        String title;
        DateTime start;
        DateTime end;
        String location;
        String locationForSearch;
    }

    @Override
    public RemoteViews getRemoteViews(@NonNull Context context, int appWidgetId) {
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.cards_widget_card);
        remoteViews.setTextViewText(R.id.widgetCardTextView, this.getTitle());
        remoteViews.setImageViewResource(R.id.widgetCardImageView, R.drawable.ic_my_lectures);
        return remoteViews;
    }
}
