package com.jktuxiyang.simpleparkingsolution;

import com.parse.Parse;

/**
 * Created by jktu on 4/10/2015.
 */
public class Application extends android.app.Application {
    public Application(){

    }

    @Override
    public void onCreate(){
        super.onCreate();
        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "s9II0BJAWC1zKfTyn5lzUHgvRMNrm690g0LBc0z0", "SU4NDjjRZFLM0jvplb7a3EOg3nqFGTACu4sebLVi");
    }
}
