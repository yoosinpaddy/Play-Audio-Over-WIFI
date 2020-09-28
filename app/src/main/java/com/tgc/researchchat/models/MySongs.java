package com.tgc.researchchat.models;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class MySongs implements Serializable {
    String displayName="";
    String path="";
    String id="";
    private static final String TAG = "MySongs";

    public MySongs(String displayName, String path, String id) {
        this.displayName = displayName;
        this.path = path;
        this.id = id;
    }
    public MySongs(JSONObject jsonObject) {
        try {
            this.displayName = jsonObject.getString("displayName");
            this.path = jsonObject.getString("path");
            this.id = jsonObject.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getJSONObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("displayName", getDisplayName());
            obj.put("path", getPath());
            obj.put("id", getId());
        } catch (JSONException e) {
            Log.e(TAG, "getJSONObject: DefaultListItem.toString JSONException: "+e.getMessage());
        }
        return obj;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    //broken pipe,connection closed
    //
}
