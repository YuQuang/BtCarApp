package com.aimma.gitexample

import android.util.Log
import android.view.MenuItem
import com.google.android.material.navigation.NavigationView

class NavViewListener: NavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.about -> {
                Log.d("Nav", "about")
            }
            R.id.website -> {
                Log.d("Nav", "website")
            }
            R.id.setting -> {
                Log.d("Nav", "setting")
            }
        }
        return true
    }
}