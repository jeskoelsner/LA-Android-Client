package eu.guardiansystems.livesapp.android.adapter.objects;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.Expose;

public class ParcelableNewsObject implements Parcelable {
		@Expose
		private String dispatcher_id;
		@Expose
		private String id;
		@Expose
		private String title;
		@Expose
		private String description;
		@Expose
		private String post;
		@Expose
		private String image_url;
		@Expose
		private String thumbnail;
		@Expose
		private int views;
		@Expose
		private double created;
		@Expose
		private double edited;

		/*
		 * Native constructor
		 */
		public ParcelableNewsObject(String dispatcher_id, String id, String title, String description, String post, String image_url, String thumbnail, int views, int created, int edited) {
				this.dispatcher_id = dispatcher_id;
				this.id = id;
				this.title = title;
				this.description = description;
				this.post = post;
				this.image_url = image_url;
				this.thumbnail = thumbnail;
				this.views = views;
				this.created = created;
				this.edited = edited;
		}

		public double getCreated() {
				return created;
		}

		public void setCreated(double created) {
				this.created = created;
		}

		public String getDescription() {
				return description;
		}

		public void setDescription(String description) {
				this.description = description;
		}

		public String getDispatcher_id() {
				return dispatcher_id;
		}

		public void setDispatcher_id(String dispatcher_id) {
				this.dispatcher_id = dispatcher_id;
		}

		public double getEdited() {
				return edited;
		}

		public void setEdited(double edited) {
				this.edited = edited;
		}

		public String getId() {
				return id;
		}

		public void setId(String id) {
				this.id = id;
		}

		public String getImage_url() {
				return image_url;
		}

		public void setImage_url(String image_url) {
				this.image_url = image_url;
		}

		public String getPost() {
				return post;
		}

		public void setPost(String post) {
				this.post = post;
		}

		public String getThumbnail() {
				return thumbnail;
		}

		public void setThumbnail(String thumbnail) {
				this.thumbnail = thumbnail;
		}

		public String getTitle() {
				return title;
		}

		public void setTitle(String title) {
				this.title = title;
		}

		public int getViews() {
				return views;
		}

		public void setViews(int views) {
				this.views = views;
		}

		//implementation of a parcelable
		public int describeContents() {
				return 0;
		}

		private void readFromParcel(Parcel in) {
				setDispatcher_id(in.readString());
				setId(in.readString());
				setTitle(in.readString());
				setDescription(in.readString());
				setPost(in.readString());
				setImage_url(in.readString());
				setThumbnail(in.readString());
				setViews(in.readInt());
				setCreated(in.readDouble());
				setEdited(in.readDouble());
		}

		public void writeToParcel(Parcel dest, int flags) {
				dest.writeString(getDispatcher_id());
				dest.writeString(getId());
				dest.writeString(getTitle());
				dest.writeString(getDescription());
				dest.writeString(getPost());
				dest.writeString(getImage_url());
				dest.writeString(getThumbnail());
				dest.writeInt(getViews());
				dest.writeDouble(getCreated());
				dest.writeDouble(getEdited());
		}

		//reading from a parcel
		public ParcelableNewsObject(Parcel in) {
				readFromParcel(in);
		}

		//CREATOR instance
		public static final Creator CREATOR = new Creator() {
				public ParcelableNewsObject createFromParcel(Parcel in) {
						return new ParcelableNewsObject(in);
				}

				public ParcelableNewsObject[] newArray(int size) {
						return new ParcelableNewsObject[size];
				}

		};
}
