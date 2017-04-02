package com.roplabs.bard.ui.widget.timeline;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import com.roplabs.bard.R;
import com.roplabs.bard.models.Post;
import com.roplabs.bard.models.Repo;

public class TimelineItem {

    @NonNull private final FbUser author;
    @NonNull private final String itemContent;
    @NonNull private final EmbedItem embedItem;
    private Post post;

    public TimelineItem(Context context, Post post) {
        author = new FbUser();
        itemContent = "hello world yoshi";
        embedItem = Factory.newItem(context, post);
    }

    @NonNull public FbUser getAuthor() {
        return author;
    }

    @NonNull public String getItemContent() {
        return itemContent;
    }

    @NonNull public EmbedItem getEmbedItem() {
        return embedItem;
    }

    public static class PhotoItem implements EmbedItem {

        @DrawableRes private final int photoUrl;

        public PhotoItem() {
            photoUrl = R.drawable.ic_account_circle_black_24dp;
        }

        @DrawableRes public int getPhotoUrl() {
            return photoUrl;
        }

        @Override public String getClassName() {
            return getClass().getSimpleName();
        }
    }

    public static class VideoItem implements Parcelable, EmbedItem {

        private final String videoUrl;

        public VideoItem() {
            videoUrl = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4";
        }

        public VideoItem(Post post) {
            this.videoUrl = post.getRepoSourceUrl();
        }

        protected VideoItem(Parcel in) {
            videoUrl = in.readString();
        }

        @Override public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(videoUrl);
        }

        @Override public int describeContents() {
            return 0;
        }

        public static final Creator<VideoItem> CREATOR = new Creator<VideoItem>() {
            @Override public VideoItem createFromParcel(Parcel in) {
                return new VideoItem(in);
            }

            @Override public VideoItem[] newArray(int size) {
                return new VideoItem[size];
            }
        };

        public String getVideoUrl() {
            return videoUrl;
        }

        @Override public String getClassName() {
            return getClass().getSimpleName();
        }
    }

    interface EmbedItem {

        @NonNull String getClassName();
    }

    static class Factory {

        static EmbedItem newItem(Context context, Post post) {
            return new VideoItem(post);
        }
    }
}

