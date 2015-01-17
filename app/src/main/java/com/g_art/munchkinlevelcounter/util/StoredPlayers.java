package com.g_art.munchkinlevelcounter.util;

import android.content.SharedPreferences;

import com.g_art.munchkinlevelcounter.bean.Player;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * MunchinLevelCounter
 * Created by G_Art on 17/1/2015.
 */
public class StoredPlayers {
    private final String PREFS_PLAYERS_KEY = "players";

    private static StoredPlayers instance;

    private SharedPreferences mPrefs;
    private boolean result = false;
    private ArrayList<Player> playersList;
    private String json;

    private Gson gson;


    public static StoredPlayers getInstance(SharedPreferences sharedPreferences) {
        if (instance == null) {
            instance = new StoredPlayers(sharedPreferences);
        } else {
            instance.setSharedPreferences(sharedPreferences);
        }
        return instance;
    }

    private StoredPlayers(SharedPreferences sharedPreferences) {
        this.gson = new Gson();
        this.mPrefs = sharedPreferences;
    }

    /**
     * Saving players stats to shared prefs in json format, using Gson from Google(deprecated)
     *
     * @return boolean. True for success, false for failure.
     */
    public boolean savePlayers(List<Player> playersList) {
        json = gson.toJson(playersList);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString(PREFS_PLAYERS_KEY, json);
        try {
            prefsEditor.apply();
            result = true;
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    /**
     * Getting players stats from shared prefs.
     *
     * @return ArrayList<Player>
     */
    public ArrayList<Player> loadPlayers(String noData) {
        playersList = new ArrayList<Player>();

        if (isPlayersStored()) {
            json = mPrefs.getString(PREFS_PLAYERS_KEY, noData);
            if (!json.equals(noData)) {
                Type type = new TypeToken<ArrayList<Player>>() {
                }.getType();
                try {
                    playersList = gson.fromJson(json, type);
                    result = true;
                } catch (JsonSyntaxException jsonSyntaxEx) {
                    result = false;
                } catch (IllegalStateException illegalStateEx) {
                    result = false;
                }
            } else {
                result = false;
            }
        }
        return playersList;
    }

    /**
     * Removing previous results of statistic in shared prefs before storing new one
     *
     * @return boolean. True for success, false for failure.
     */
    public boolean clearStoredPlayers() {
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        if (isPlayersStored()) {
            prefsEditor.remove(PREFS_PLAYERS_KEY);
            try {
                prefsEditor.apply();
                result = true;
            } catch (Exception ex) {
                result = false;
            }
        }
        return result;

    }

    public boolean isPlayersStored() {
        return mPrefs.contains(PREFS_PLAYERS_KEY);
    }

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.mPrefs = sharedPreferences;
    }
}