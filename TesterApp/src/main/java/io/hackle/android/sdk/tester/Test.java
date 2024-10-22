package io.hackle.android.sdk.tester;

import io.hackle.android.Hackle;
import io.hackle.android.HackleApp;
import io.hackle.sdk.common.HacklePushSubscriptionStatus;

public class Test {
    public void testFunc() {
        HackleApp.getInstance().updatePushSubscriptionStatus(HacklePushSubscriptionStatus.SUBSCRIBED);
    }
}
