package com.example.mappi_kt

import ExampleFragment
import com.example.mappi_kt.fragments.ProfileFragment
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mappi_kt.fragments.ChatFragment
import com.example.mappi_kt.fragments.MapFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_activity)

        viewPager = findViewById(R.id.viewPager)

        val fragmentList = mutableListOf(MapFragment(), ChatFragment(), ProfileFragment())

        val fragmentAdapter = FragmentAdapter(supportFragmentManager, fragmentList)
        viewPager.adapter = fragmentAdapter

        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val customTabView = LayoutInflater.from(this).inflate(R.layout.tab_item, null)
            val tabIcon = customTabView.findViewById<ImageView>(R.id.tab_icon)

            when (position) {
                0 -> tabIcon.setImageResource(R.drawable.map)
                1 -> tabIcon.setImageResource(R.drawable.chat)
                2 -> tabIcon.setImageResource(R.drawable.ic_person_foreground)
            }

            tab.customView = customTabView
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewPager.isUserInputEnabled = position != 0
            }
        })

        if (intent.getBooleanExtra("openProfileFragment", false)) {
            val friendUserId = intent.getStringExtra("userId")
            friendUserId?.let {
                val profileFragment = ProfileFragment()
                val bundle = Bundle()
                bundle.putString("userId", friendUserId) // Set the user ID value
                profileFragment.arguments = bundle

                // Replace the specific fragment within the fragment list
                fragmentList[2] = profileFragment
            }
            viewPager.currentItem = 2
        }

    }

    private inner class FragmentAdapter(
        fragmentManager: FragmentManager,
        private val fragmentList: List<Fragment>
    ) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int {
            return fragmentList.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragmentList[position]
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
