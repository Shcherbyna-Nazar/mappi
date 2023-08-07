package com.example.mappi_kt

import SearchFriendsFragment
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.mappi_kt.fragments.FriendRequestsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class SearchActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager
    private lateinit var bottomNavigation: BottomNavigationView
    private var prevMenuItem: MenuItem? = null

    @SuppressLint("NonConstantResourceId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_activity)

        viewPager = findViewById(R.id.viewPager)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Create an instance of the PagerAdapter
        val pagerAdapter = PagerAdapter(supportFragmentManager)

        // Set the adapter on the ViewPager
        viewPager.adapter = pagerAdapter

        // Set a listener for BottomNavigationView item selection
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_search_friends -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.menu_friend_requests -> {
                    viewPager.currentItem = 1
                    true
                }
                else -> false
            }
        }

        // Set a listener for ViewPager page changes
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                prevMenuItem?.isChecked = false
                bottomNavigation.menu.getItem(position).isChecked = true
                prevMenuItem = bottomNavigation.menu.getItem(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private inner class PagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private val NUM_PAGES = 2 // Number of fragments

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> SearchFriendsFragment()
                1 -> FriendRequestsFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }

        override fun getCount(): Int {
            return NUM_PAGES
        }
    }
}
