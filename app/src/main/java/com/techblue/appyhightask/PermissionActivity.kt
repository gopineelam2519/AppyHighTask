package com.techblue.appyhightask

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionActivity : AppCompatActivity() {

    private var permissionArray: Array<String>? = null
    private var permissionMessage = "Please make sure permissions are granted."

    companion object {
        const val PERMISSIONS_REQUEST_CODE = 1994
        const val PERMISSION_ARRAY_NAME = "PERMISSION_ARRAY"
        const val PERMISSION_REQUEST_MESSAGE = "PERMISSION_REQUEST_MESSAGE"
        const val TAG = "PermissionActivity"
        var permissionRequestListener: PermissionRequestListener? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        permissionArray = intent.getStringArrayExtra(PERMISSION_ARRAY_NAME)
        permissionMessage = if (intent.hasExtra(PERMISSION_REQUEST_MESSAGE)) {
            intent.getStringExtra(PERMISSION_REQUEST_MESSAGE)
        } else {
            "Please make sure permissions are granted."
        }

        if (permissionArray != null && permissionRequestListener != null) {
            if (checkPermission()) {
                permissionRequestListener?.onPermissionGranted(granted = true)
                finish()
            } else {
                requestPermission()
            }
        } else {
            Log.e(TAG, "No Permissions")
            permissionRequestListener?.onPermissionGranted(granted = false)
            finish()
        }
    }

    private fun checkPermission(): Boolean {
        var allPermissionGranted = true
        for (permission in permissionArray!!) {
            if (allPermissionGranted) allPermissionGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        return allPermissionGranted
    }

    private fun checkPermissionAreGrantedOrNot(grantResults: IntArray): Boolean {
        var allPermissionGranted = true
        for (permission in grantResults) {
            if (allPermissionGranted) allPermissionGranted = permission == PackageManager.PERMISSION_GRANTED
        }
        return allPermissionGranted
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissionArray!!, PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> if (grantResults.isNotEmpty()) {

                if (checkPermissionAreGrantedOrNot(grantResults)) {

                    permissionRequestListener?.onPermissionGranted(granted = true)
                    finish()

                } else {
                    //first check for shouldShowRequestPermissionRationale if yes show dialog with purpose

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        showMessageOKCancel(permissionMessage, DialogInterface.OnClickListener { dialog, which ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(permissionArray!!, PERMISSIONS_REQUEST_CODE)
                            }
                        }, DialogInterface.OnClickListener { dialog, which ->
                            permissionRequestListener?.onPermissionGranted(granted = false)
                            finish()
                        })
                        return
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    showMessageOKCancel(permissionMessage, DialogInterface.OnClickListener { dialog, which ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(permissionArray!!, PERMISSIONS_REQUEST_CODE)
                        }
                    }, DialogInterface.OnClickListener { dialog, which ->
                        permissionRequestListener?.onPermissionGranted(granted = false)
                        finish()
                    })
                    return
                }
            }
            else -> {
                permissionRequestListener?.onPermissionGranted(granted = false)
                finish()
            }
        }
    }

    private fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener, onClickListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this).setMessage(message).setPositiveButton("Ok", okListener).setNegativeButton("Cancel", onClickListener).create().show()
    }

    fun setPermissionsRequestListener(permissionRequestListener: PermissionRequestListener?) {
        PermissionActivity.permissionRequestListener = permissionRequestListener
    }

    override fun onDestroy() {
        super.onDestroy()
        permissionRequestListener = null
    }

    interface PermissionRequestListener {
        fun onPermissionGranted(granted: Boolean = false)
    }
}