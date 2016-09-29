package com.roplabs.bard.models;

import android.content.Context;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.roplabs.bard.config.Configuration;

/**
 * Created by reg on 2016-09-29.
 */
public class AmazonCognito {
    public static CognitoCachingCredentialsProvider credentialsProvider;

    public static void init(Context context) {
        credentialsProvider = new CognitoCachingCredentialsProvider(
                context,    /* get the context for the application */
                Configuration.cognitoIdentityPool(),    /* Identity Pool ID */
                Regions.US_WEST_2           /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );
    }
}
