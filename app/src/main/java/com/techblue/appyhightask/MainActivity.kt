package com.techblue.appyhightask

import android.Manifest
import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.ads.nativetemplates.NativeTemplateStyle
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.database.*
import com.sinch.android.rtc.*
import com.sinch.android.rtc.calling.Call
import com.sinch.android.rtc.calling.CallClient
import com.sinch.android.rtc.calling.CallClientListener
import com.sinch.android.rtc.video.VideoCallListener
import com.techblue.appyhightask.model.User
import java.util.*


class MainActivity : AppCompatActivity(), SinchClientListener, CallClientListener {

    companion object {
        const val PREF_NAME = "AppyHighTask"
        const val USER_ID = "User_Id"
        const val TAG = "MainActivity"
    }

    private lateinit var sharedPreferences: SharedPreferences
    private var mDataBase: DatabaseReference? = null
    private var userId: String? = null
    private var sinchClient: SinchClient? = null
    private var call: Call? = null

    private var isClientThere: Boolean = false
    private var isInComingCall: Boolean = false

    private lateinit var getStartedBtn: Button
    private lateinit var statusTxt: MaterialTextView
    private lateinit var localView: FrameLayout
    private lateinit var remoteView: FrameLayout
    private lateinit var acceptFab: FloatingActionButton
    private lateinit var rejectFab: FloatingActionButton
    private lateinit var adTemplateView: TemplateView

    private var handler: Handler? = null

    val timer = object : CountDownTimer(15000, 1000) {
        override fun onFinish() {
            if (call != null) {
                call!!.hangup()
            }
            getStartedBtn.isEnabled = true
            setUserState(false)
            Toast.makeText(applicationContext, "Please try again after sometime...", Toast.LENGTH_SHORT).show()
        }

        override fun onTick(p0: Long) {
            statusTxt.text = "${(p0 / 1000) + 1}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getAllViews()

        //initialize the Firebase DB
        mDataBase = FirebaseDatabase.getInstance().reference
        handler = Handler(Looper.getMainLooper())

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        userId = sharedPreferences.getString(USER_ID, null)
        if (userId == null) {
            //it occurs only for the first time only
            //add this new created user to Firebase DB
            val uuid = UUID.randomUUID().toString()
            sharedPreferences.edit().apply {
                putString(USER_ID, uuid)
            }.apply()
            val user = User(uuid, true)

            if (mDataBase != null)
                mDataBase!!.child("users").child(uuid).setValue(user).addOnSuccessListener {
                    Toast.makeText(
                        applicationContext,
                        "user has added successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    //setting new randomId to userId
                    userId = uuid
                    startSinchClient()
                }.addOnFailureListener {
                    Toast.makeText(
                        applicationContext,
                        "Error while adding user--->${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            //start the sinch client
            startSinchClient()
        }

        loadNativeAd()
    }

    private fun loadNativeAd() {
        val adLoader: AdLoader = AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
            .forUnifiedNativeAd { unifiedNativeAd ->
                adTemplateView.visibility = View.VISIBLE

                val styles = NativeTemplateStyle.Builder().build()
                adTemplateView.setStyles(styles)
                adTemplateView.setNativeAd(unifiedNativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(p0: Int) {
                    super.onAdFailedToLoad(p0)
                    adTemplateView.visibility = View.GONE
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun getAllViews() {
        getStartedBtn = findViewById(R.id.getStartedBtn)
        statusTxt = findViewById(R.id.statusTxt)
        localView = findViewById(R.id.localView)
        remoteView = findViewById(R.id.remoteView)
        acceptFab = findViewById(R.id.floating_action_button_accept)
        rejectFab = findViewById(R.id.floating_action_button_end)
        adTemplateView = findViewById(R.id.adTemplate)

        getStartedBtn.setOnClickListener {
            getRequiredPermission()
        }

        acceptFab.setOnClickListener {
            if (call != null) {
                call!!.answer()
            }
        }

        rejectFab.setOnClickListener {
            if (call != null) {
                call!!.hangup()
                setUserState(false)
                timer.cancel()
                it.isEnabled = true
            }
        }
    }

    fun onGetStartedBtnClick() {
        getStartedBtn.isEnabled = false
        timer.start()
        setUserState(true)
        fetchAllUserFromFireBaseDB()
    }

    private fun getRequiredPermission() {
        val permissionActivity = PermissionActivity()
        permissionActivity.setPermissionsRequestListener(null)
        permissionActivity.setPermissionsRequestListener(object : PermissionActivity.PermissionRequestListener {
            override fun onPermissionGranted(granted: Boolean) {
                //Here you will get the permissions are accepted or not
                if (granted) {
                    onGetStartedBtnClick()
                } else {
                    Toast.makeText(applicationContext, "Couldn't proceed further", Toast.LENGTH_SHORT).show()
                }
            }
        })
        val permissionIntent = Intent(this, permissionActivity.javaClass)
        //start of pass required permissions as Array
        permissionIntent.putExtra(PermissionActivity.PERMISSION_ARRAY_NAME, arrayOf<String>(CAMERA, Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECORD_AUDIO))
        //end of pass required permissions as Array
        startActivity(permissionIntent)
    }

    private fun fetchAllUserFromFireBaseDB() {
        if (mDataBase != null)
            mDataBase!!.child("users").orderByChild("active").equalTo(true)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error: ${error.message} ")
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userInOnline = mutableListOf<User>()
                        for (singleSnapshot in snapshot.children) {
                            val user = singleSnapshot.getValue(User::class.java)
                            if (user != null && user.userId != userId)
                                userInOnline.add(user)
                        }
                        if (userInOnline.size > 0) {
                            timer.cancel()
                            callRandomUser(userInOnline)
                        }
                    }
                })
    }

    private fun callRandomUser(userInOnline: MutableList<User>) {
        val randomUser = userInOnline[Random().nextInt(userInOnline.size)]
        Log.d(TAG, "RandomUser---->$randomUser")

        if (sinchClient != null) {
            val callClient = sinchClient!!.callClient
            val call: Call = callClient.callUserVideo(randomUser.userId)
            call.addCallListener(SinchVideoCallListener())
        }
    }

    private fun startSinchClient() {
        if (userId != null) {
            sinchClient = Sinch.getSinchClientBuilder().context(applicationContext)
                .applicationKey(resources.getString(R.string.key))
                .applicationSecret(resources.getString(R.string.secret))
                .environmentHost(resources.getString(R.string.host_name))
                .userId(userId)
                .build()
            sinchClient?.apply {
                setSupportCalling(true)
                startListeningOnActiveConnection()
                setSupportManagedPush(true)
                addSinchClientListener(this@MainActivity)
                callClient.addCallClientListener(this@MainActivity)
                start()
            }
        }
    }

    fun setStatusMsg(status: String) {
        handler?.post {
            statusTxt.text = status
        }
    }

    private fun terminateSinchClient() {

        if (call != null) {
            call!!.hangup()
            call = null
        }

        sinchClient?.stopListeningOnActiveConnection()
        sinchClient?.terminate()
    }

    private fun setUserState(state: Boolean) {
        if (userId != null && mDataBase != null)
            mDataBase!!.child("users").child(userId!!).child("active").setValue(state)
    }

    override fun onDestroy() {
        setUserState(false)
        terminateSinchClient()
        handler = null
        super.onDestroy()
    }

    //sinchClient listener
    override fun onClientStarted(p0: SinchClient?) {
        Log.d(TAG, "client has started")
        isClientThere = true
        setStatusMsg("Client Connected")
    }

    //sinchClient listener
    override fun onClientStopped(p0: SinchClient?) {
        Log.d(TAG, "client has stopped")
        isClientThere = false
        setStatusMsg("Client DisConnected")
    }

    //sinchClient listener
    override fun onRegistrationCredentialsRequired(p0: SinchClient?, p1: ClientRegistration?) {
    }

    //sinchClient listener
    override fun onLogMessage(p0: Int, p1: String?, p2: String?) {
        Log.e(TAG, "Log Msg--->${p1}--->$p2")
        // setStatusMsg("$p1--->$p2")
    }

    //sinchClient listener
    override fun onClientFailed(p0: SinchClient?, p1: SinchError?) {
        Log.e(TAG, "client failed--->${p1?.message ?: ""}")
        isClientThere = false
        setStatusMsg("Client DisConnected")
    }

    //call client listener
    override fun onIncomingCall(p0: CallClient?, p1: Call?) {

        isInComingCall = true
        timer.cancel()

        Log.d(TAG, "onIncomingCall")
        setStatusMsg("InComingCall--->${p1?.callId}")

        this@MainActivity.call = p1

        p1?.addCallListener(SinchVideoCallListener())

        enableIncomingCallViews()
        acceptFab.performClick()
    }

    private fun enableIncomingCallViews() {
        getStartedBtn.visibility = View.GONE
        adTemplateView.visibility = View.GONE

        localView.visibility = View.VISIBLE
        remoteView.visibility = View.VISIBLE

        acceptFab.visibility = View.VISIBLE
        val acceptParams = acceptFab.layoutParams as ConstraintLayout.LayoutParams
        acceptParams.horizontalBias = 0.3F
        acceptFab.layoutParams = acceptParams

        rejectFab.visibility = View.VISIBLE
    }

    fun enableOutGoingCallViews() {
        getStartedBtn.visibility = View.GONE
        adTemplateView.visibility = View.GONE

        localView.visibility = View.VISIBLE
        remoteView.visibility = View.VISIBLE

        acceptFab.visibility = View.GONE
        val acceptParams = acceptFab.layoutParams as ConstraintLayout.LayoutParams
        acceptParams.horizontalBias = 0F
        acceptFab.layoutParams = acceptParams

        rejectFab.visibility = View.VISIBLE
    }

    fun defaultViews() {
        getStartedBtn.visibility = View.VISIBLE
        getStartedBtn.isEnabled = true
        adTemplateView.visibility = View.VISIBLE

        localView.visibility = View.GONE
        remoteView.visibility = View.GONE

        acceptFab.visibility = View.GONE
        rejectFab.visibility = View.GONE

        isInComingCall = false
        this.call = null
    }

    fun addVideoViews() {
        if (this.sinchClient != null) {
            val videoController = sinchClient!!.videoController

            if (videoController != null) {
                val localView = videoController.localView
                val remoteView = videoController.remoteView

                this.localView.addView(localView)
                this.remoteView.addView(remoteView)
            }
        }
    }

    fun removeVideoViews() {
        if (this.sinchClient != null) {
            val videoController = sinchClient!!.videoController

            if (videoController != null) {
                val localView = videoController.localView
                val remoteView = videoController.remoteView

                this.localView.removeView(localView)
                this.remoteView.removeView(remoteView)
            }
        }
    }

    private inner class SinchVideoCallListener : VideoCallListener {
        override fun onCallEnded(call: Call) {
            Log.d(TAG, "onCallEnded")
            setStatusMsg("Call ended")

            this@MainActivity.call = call

            defaultViews()
            isInComingCall = false

            removeVideoViews()
            setUserState(false)
        }

        override fun onCallEstablished(call: Call) {
            Log.d(TAG, "Call established")
            setStatusMsg("Connected.")
            this@MainActivity.call = call

            if (isInComingCall) {
                acceptFab.visibility = View.GONE
                val acceptParams = acceptFab.layoutParams as ConstraintLayout.LayoutParams
                acceptParams.horizontalBias = 0.0F
                acceptFab.layoutParams = acceptParams
            }
        }

        override fun onVideoTrackResumed(p0: Call?) {
            Log.d(TAG, "onVideoTrackResumed")
            this@MainActivity.call = p0
        }

        override fun onCallProgressing(call: Call) {
            Log.d(TAG, "onCallProgressing")
            setStatusMsg("Ringing...")

            this@MainActivity.call = call

            enableOutGoingCallViews()
        }

        override fun onShouldSendPushNotification(p0: Call?, p1: MutableList<PushPair>?) {
        }

        override fun onVideoTrackAdded(call: Call) {
            // Display some kind of icon showing it's a video call
            Log.d(TAG, "onVideoTrackAdded")
            this@MainActivity.call = call

            addVideoViews()
        }

        override fun onVideoTrackPaused(p0: Call?) {
            Log.d(TAG, "onVideoTrackPaused")
            this@MainActivity.call = p0
        }
    }
}
