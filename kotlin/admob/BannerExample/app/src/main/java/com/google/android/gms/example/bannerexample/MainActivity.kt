package com.google.android.gms.example.bannerexample

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.example.bannerexample.databinding.ActivityMainBinding
import com.google.android.ump.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding
  private lateinit var consentInformation: ConsentInformation
  private lateinit var consentForm: ConsentForm
  private var alreadyInitializedSDK = false

  private fun initializeGoogleMobileAdsSDK() {
    if (!alreadyInitializedSDK) {
      // Initialize the Mobile Ads SDK with an AdMob App ID.
      MobileAds.initialize(this) {}
      alreadyInitializedSDK = true
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // Log the Mobile Ads SDK version.
    Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())

    // Set your test devices. Check your logcat output for the hashed device ID to
    // get test ads on a physical device. e.g.
    // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
    // to get test ads on this device."
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder().setTestDeviceIds(listOf("ABCDEF012345")).build()
    )

    // The Google Mobile Ads SDK provides the User Messaging Platform (Google's
    // IAB Certified consent management platform) as one solution to capture
    // consent for users in GDPR impacted countries. This is an example and
    // you can choose another consent management platform to capture consent.

    consentInformation = UserMessagingPlatform.getConsentInformation(this)
    if (
      consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED ||
        consentInformation.consentStatus == ConsentInformation.ConsentStatus.NOT_REQUIRED
    ) {
      // Initialize Google Mobile Ads SDK
      initializeGoogleMobileAdsSDK()
    }

    // For testing purposes, you can force a DebugGeography of EEA or NOT_EEA.
    // val debugSettings = ConsentDebugSettings.Builder(this)
    // .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
    // .build()

    val params =
      ConsentRequestParameters.Builder()
        // .setConsentDebugSettings(debugSettings)
        .build()

    consentInformation.requestConsentInfoUpdate(
      this,
      params,
      {
        // The consent information state was updated.
        // You are now ready to check if a form is available.
        if (consentInformation.isConsentFormAvailable) {
          loadAndPresentFormIfNecessary(true)
        } else {
          // Initialize Google Mobile Ads SDK
          initializeGoogleMobileAdsSDK()

          // Consent not needed before loading ads.
          loadAd()
        }
      },
      {
        // Handle the error.
        Toast.makeText(
            this,
            "Failed to request consent information. Will not attempt to load ads.",
            Toast.LENGTH_SHORT
          )
          .show()
      }
    )
  }

  // Called when leaving the activity
  public override fun onPause() {
    binding.adView.pause()
    super.onPause()
  }

  // Called when returning to the activity
  public override fun onResume() {
    super.onResume()
    binding.adView.resume()
  }

  // Called before the activity is destroyed
  public override fun onDestroy() {
    binding.adView.destroy()
    super.onDestroy()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.action_menu, menu)
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val menuItemView = findViewById<View>(item.itemId)
    val popup = PopupMenu(this, menuItemView)
    popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)
    popup.show()
    popup.setOnMenuItemClickListener {
      loadAndPresentFormIfNecessary(false)
      true
    }
    return super.onOptionsItemSelected(item)
  }

  private fun loadAndPresentFormIfNecessary(shouldLoadAds: Boolean) {
    // Loads a consent form. Must be called on the main thread.
    UserMessagingPlatform.loadConsentForm(
      this,
      {
        consentForm = it
        if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
          consentForm.show(this) {

            // Initialize Google Mobile Ads SDK
            initializeGoogleMobileAdsSDK()

            // App can start requesting ads.
            if (shouldLoadAds) {
              loadAd()
            }

            // Your app needs to allow the user to change their consent
            // status at any time. Load another form and store it to allow
            // the user to change their consent status.
            loadAndPresentFormIfNecessary(false)
          }
        } else {
          // Keep the form available for changes to user consent.
          consentForm = it

          // Initialize Google Mobile Ads SDK
          initializeGoogleMobileAdsSDK()

          // App can request ads.
          if (shouldLoadAds) {
            loadAd()
          }
        }
      },
      {
        // Handle the error.
        Toast.makeText(
            this,
            "Failed to dismiss the form. Will not attempt to load ads.",
            Toast.LENGTH_SHORT
          )
          .show()
      }
    )
  }

  private fun loadAd() {
    // Create an ad request
    val adRequest = AdRequest.Builder().build()
    binding.adView.loadAd(adRequest)
  }
}
