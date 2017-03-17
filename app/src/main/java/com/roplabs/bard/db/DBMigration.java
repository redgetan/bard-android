package com.roplabs.bard.db;

import com.roplabs.bard.util.BardLogger;
import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

import java.util.Date;

public class DBMigration implements RealmMigration {
    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {

        // DynamicRealm exposes an editable schema
        RealmSchema schema = realm.getSchema();

        if (oldVersion == 0) {
            schema.create("Character")
                    .addField("name", String.class)
                    .addField("token", String.class)
                    .addField("description", String.class)
                    .addField("isBundleDownloaded", Boolean.class)
                    .addField("createdAt", Date.class);

            schema.create("Scene")
                    .addField("name", String.class)
                    .addField("token", String.class)
                    .addField("characterToken", String.class)
                    .addField("wordList", String.class)
                    .addField("thumbnailUrl", String.class)
                    .addField("createdAt", Date.class);

            schema.get("Repo")
                    .addField("isPublished", Boolean.class)
                    .addField("username", String.class)
                    .addField("characterToken", String.class)
                    .addField("sceneToken", String.class);

            oldVersion++;
        }

        if (oldVersion == 1) {
            schema.get("Character")
                    .addPrimaryKey("token")
                    .renameField("description","details");

            schema.get("Scene")
                    .addPrimaryKey("token");

            oldVersion++;
        }

        if (oldVersion == 2) {
            oldVersion++;
        }

        if (oldVersion == 3) {
            oldVersion++;
        }

        if (oldVersion == 4) {
            if (!schema.contains("Favorite")) {
                schema.create("Favorite")
                        .addField("sceneToken", String.class)
                        .addField("username", String.class)
                        .addField("createdAt", Date.class);
            }

            schema.get("Character")
                    .addField("thumbnailUrl", String.class);

            oldVersion++;
        }

        // migrate to version 6
        if (oldVersion == 5) {

            if (!schema.contains("UserPack")) {
                schema.create("UserPack")
                        .addField("packToken", String.class)
                        .addField("username", String.class)
                        .addField("createdAt", Date.class);
            }

            oldVersion++;
        }

        // migrate to version 7
        if (oldVersion == 6) {
            schema.get("Character")
                    .addField("wordListByScene", String.class)
                    .addField("timestamp", Integer.class);


            oldVersion++;
        }

    }
}

