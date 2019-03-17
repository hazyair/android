package io.github.hazyair.util;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import com.michaelflisar.gdprdialog.GDPRConsentState;
import com.michaelflisar.gdprdialog.GDPRLocationCheck;
import com.michaelflisar.gdprdialog.GDPRNetwork;
import com.michaelflisar.gdprdialog.GDPRSetup;
import com.michaelflisar.gdprdialog.helper.GDPRPreperationData;

import io.github.hazyair.R;
import io.github.hazyair.gui.MainActivity;


public class GDPR {
    private static GDPRSetup init(Context context) {
        return new GDPRSetup(
                new GDPRNetwork(
                        context.getString(R.string.privacy_play_name),
                        context.getString(R.string.privacy_play_link),
                        context.getString(R.string.gdpr_type_location), false),
                new GDPRNetwork(
                        context.getString(R.string.privacy_analytics_name),
                        context.getString(R.string.privacy_analytics_link),
                        context.getString(R.string.gdpr_type_analytics), false),
                new GDPRNetwork(
                        context.getString(R.string.privacy_fabric_name),
                        context.getString(R.string.privacy_fabric_link),
                        context.getString(R.string.gdpr_type_crash), false)
                )
                .withCustomDialogTheme(R.style.Consent)
                .withPrivacyPolicy(context.getString(R.string.privacy_link))
                .withAllowNoConsent(false)
                .withCheckRequestLocation(GDPRLocationCheck.DEFAULT_WITH_FALLBACKS)
                .withBottomSheet(true)
                .withForceSelection(true)
                .withShortQuestion(true)
                .withShowPaidOrFreeInfoText(true);
    }

    private static void withdraw(AppCompatActivity activity) {
        if (activity instanceof MainActivity) {
            activity.finish();
        } else {
            Intent intent = new Intent(activity.getApplicationContext(),
                    MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(MainActivity.PARAM_EXIT, true);
            activity.startActivity(intent);
        }
    }

    public static void consent(AppCompatActivity activity) {
        consent(activity, false);
    }

    public static void consent(AppCompatActivity activity, boolean checkIfNeedsToBeShown) {
        com.michaelflisar.gdprdialog.GDPR.getInstance().init(activity);
        GDPRSetup setup = io.github.hazyair.util.GDPR.init(activity);
        com.michaelflisar.gdprdialog.GDPR.getInstance().checkIfNeedsToBeShown(activity, setup,
                new com.michaelflisar.gdprdialog.GDPR.IGDPRCallback() {
                    @Override
                    public void onConsentNeedsToBeRequested(GDPRPreperationData data) {
                        com.michaelflisar.gdprdialog.GDPR.getInstance().showDialog(activity, setup,
                                data.getLocation());
                    }

                    @Override
                    public void onConsentInfoUpdate(GDPRConsentState consentState,
                                                    boolean isNewState) {
                        if (isNewState) {
                            switch (consentState.getConsent()) {
                                case NO_CONSENT:
                                case NON_PERSONAL_CONSENT_ONLY:
                                    io.github.hazyair.util.GDPR.withdraw(
                                            activity);
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            if (checkIfNeedsToBeShown) {
                                switch (consentState.getConsent()) {
                                    case NO_CONSENT:
                                    case NON_PERSONAL_CONSENT_ONLY:
                                        com.michaelflisar.gdprdialog.GDPR.getInstance().showDialog(
                                                activity, setup, consentState.getLocation());
                                        break;
                                    default:
                                        break;
                                }
                            } else {
                                com.michaelflisar.gdprdialog.GDPR.getInstance().showDialog(activity,
                                        setup, consentState.getLocation());
                            }

                        }

                    }
                });
    }
}
