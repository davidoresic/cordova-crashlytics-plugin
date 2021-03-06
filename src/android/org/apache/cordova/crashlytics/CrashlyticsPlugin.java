package org.apache.cordova.crashlytics;

import android.content.Context;
import com.crashlytics.android.Crashlytics;
import org.apache.cordova.*;
import org.json.JSONException;

import javax.security.auth.callback.Callback;

public class CrashlyticsPlugin extends CordovaPlugin {

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Crashlytics.start((Context) cordova.getActivity());
    }

    private static enum BridgedMethods {
        logError(1){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.log(args.getString(0));
            }
        },
        // Kept for backward compatibility only ...
        throwError(1){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.logException(new RuntimeException(args.getString(0)));
            }
        },
        logException(1){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.logException(new RuntimeException(args.getString(0)));
            }
        },
        log(1){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                if (argsLengthValid(3, args)) {
                    Crashlytics.log(args.getInt(0), args.getString(1), args.getString(2));
                } else {
                    Crashlytics.log(args.getString(0));
                }
            }
        },
        setApplicationInstallationIdentifier(1){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.setApplicationInstallationIdentifier(args.getString(0));
            }
        },
        setBool(2){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.setBool(args.getString(0), args.getBoolean(1));
            }
        },
        setDouble(2){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.setDouble(args.getString(0), args.getDouble(1));
            }
        },
        setFloat(2){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.setFloat(args.getString(0), Float.valueOf(args.getString(1)));
            }
        },
        setInt(2){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.setInt(args.getString(0), args.getInt(1));
            }
        },
        setLong(2){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.setLong(args.getString(0), args.getLong(1));
            }
        },
        setString(2){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.setString(args.getString(0), args.getString(1));
            }
        },
        setUserEmail(1){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.setUserEmail(args.getString(0));
            }
        },
        setUserIdentifier(1){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.setUserIdentifier(args.getString(0));
            }
        },
        setUserName(1){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                Crashlytics.setUserName(args.getString(0));
            }
        },
        simulateCrash(0, false){
            @Override
            public void call(CordovaArgs args) throws JSONException {
                String message = args.getString(0) == null ? "This is a crash":args.getString(0);
                throw new RuntimeException(message);
            }
        };

        int minExpectedArgsLength;
        boolean isAsync = true;
        BridgedMethods(int minExpectedArgsLength) {
            this(minExpectedArgsLength, true);
        }
        BridgedMethods(int minExpectedArgsLength, boolean isAsync) {
            this.minExpectedArgsLength = minExpectedArgsLength;
            this.isAsync = isAsync;
        }

        public abstract void call(CordovaArgs args) throws JSONException;

        public static boolean argsLengthValid(int minExpectedArgsLenght, CordovaArgs args) throws JSONException {
            return (args != null
                    // Doesn't seem to have any api (better than this...) to retrieve args' length ...
                    && args.getString(minExpectedArgsLenght -1)!=null
                    && args.getString(minExpectedArgsLenght -1).length()!=0);
        }

        public Runnable runnableFrom(final CallbackContext callbackContext, final CordovaArgs args) {
            final BridgedMethods method = this;
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        method.call(args);
                        callbackContext.success();
                    } catch (Throwable t) {
                        callbackContext.error(t.getMessage());
                    }
                }
            };
        }
    }

    @Override
    public boolean execute(String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        try {
            final BridgedMethods bridgedMethods = BridgedMethods.valueOf(action);
            if (bridgedMethods != null) {
                if (!BridgedMethods.argsLengthValid(bridgedMethods.minExpectedArgsLength, args)) {
                    callbackContext.error(String.format("Unsatisfied min args length (expected=%s)", bridgedMethods.minExpectedArgsLength));
                    return true;
                }

                Runnable runnable = bridgedMethods.runnableFrom(callbackContext, args);
                if(bridgedMethods.isAsync) {
                    cordova.getThreadPool().execute(runnable);
                } else {
                    runnable.run();
                }
                
                return true;
            }
        }catch(IllegalArgumentException e) { // Didn't found any enum value corresponding to requested action
            return false;
        }

        return false;
    }
}