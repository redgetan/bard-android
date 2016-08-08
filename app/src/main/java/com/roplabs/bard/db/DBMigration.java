package com.roplabs.bard.db;

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
                    .addPrimaryKey("token");

            schema.get("Scene")
                    .addPrimaryKey("token");
        }

    }
}

