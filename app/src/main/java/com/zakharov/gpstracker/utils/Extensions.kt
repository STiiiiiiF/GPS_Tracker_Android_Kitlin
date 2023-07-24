package com.zakharov.gpstracker.utils

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.zakharov.gpstracker.R

fun Fragment.openFragment(f: Fragment) {
    (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        .replace(R.id.place_holder, f).commit()
}

fun AppCompatActivity.openFragment(f: Fragment) {
    if (supportFragmentManager.fragments.isNotEmpty()) {
        if (supportFragmentManager.fragments[0].javaClass == f.javaClass) return
    }
    supportFragmentManager.beginTransaction()
        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        .replace(R.id.place_holder, f).commit()
}

fun Fragment.checkPermission(p: String): Boolean {
    return when (PackageManager.PERMISSION_GRANTED) {
        ContextCompat.checkSelfPermission(activity as AppCompatActivity, p) -> true
        else -> false
    }

}