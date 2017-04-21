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

        // migrate to version 8
        if (oldVersion == 7) {
            schema.get("Character")
                    .addField("owner", String.class);


            oldVersion++;
        }

        // migrate to version 9
        if (oldVersion == 8) {
            if (!schema.contains("Channel")) {
                schema.create("Channel")
                        .addField("name", String.class)
                        .addField("description", String.class)
                        .addField("token", String.class)
                        .addField("username", String.class)
                        .addField("createdAt", Date.class);
            }

            if (!schema.get("Repo").hasField("sourceUrl")) {
                schema.get("Repo")
                        .addField("sourceUrl", String.class);
            }

            if (!schema.contains("Post")) {
                schema.create("Post")
                        .addField("id", int.class)
                        .addField("repoTitle", String.class)
                        .addField("repoSourceUrl", String.class)
                        .addField("username", String.class)
                        .addField("createdAt", Date.class);
            }

            oldVersion++;
        }

        // migrate to version 10
        if (oldVersion == 9) {
            if (!schema.contains("Post")) {
                schema.create("Post")
                        .addField("id", int.class)
                        .addField("repoTitle", String.class)
                        .addField("repoSourceUrl", String.class)
                        .addField("username", String.class)
                        .addField("createdAt", Date.class);
            }

            oldVersion++;
        }

        // migrate to version 11
        if (oldVersion == 10) {
            schema.get("Post")
                    .addField("repoWordList", String.class)
                    .addField("repoToken", String.class);

            oldVersion++;
        }

        // migrate to version 12
        if (oldVersion == 11) {
            if (!schema.contains("Like")) {
                schema.create("Like")
                        .addField("repoToken", String.class)
                        .addField("username", String.class)
                        .addField("createdAt", Date.class);
            }

            oldVersion++;
        }

        // migrate to version 13
        if (oldVersion == 12) {
            schema.get("Post")
                    .addField("sceneToken", String.class)
                    .addField("packToken", String.class)
                    .addField("thumbnailUrl", String.class);


            oldVersion++;
        }

        // migrate to version 14
        if (oldVersion == 13) {
            schema.get("Repo")
                    .addField("uuid", String.class);


            oldVersion++;
        }

        // migrate to version 15
        if (oldVersion == 14) {
            schema.get("Scene")
                    .addField("tag_list", String.class)
                    .addField("owner", String.class)
                    .addField("labeler", String.class);
            oldVersion++;
        }

        // migrate to version 16
        if (oldVersion == 15) {
            schema.get("Scene")
                    .addField("tagList", String.class);


            oldVersion++;
        }

        // migrate to version 17
        if (oldVersion == 16) {
            schema.get("Scene")
                    .removeField("tag_list");


            oldVersion++;
        }

        // migrate to version 18
        if (oldVersion == 17) {
            schema.get("Repo")
                    .addField("thumbnailUrl", String.class);


            oldVersion++;
        }

    }
}

