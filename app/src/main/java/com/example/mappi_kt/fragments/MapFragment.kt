package com.example.mappi_kt.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.example.mappi_kt.R
import com.example.mappi_kt.entity.MyLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class MapFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var currentUser: FirebaseUser
    private lateinit var postsRef: DatabaseReference
    private lateinit var usersRef: DatabaseReference
    private lateinit var postListener: ValueEventListener
    private val markers: MutableMap<String, Marker?> = mutableMapOf()
    private val markerSize = 100
    private val pwLatLng = LatLng(52.221247188227544, 21.00804278307224)

    companion object {
        private lateinit var instance: MapFragment
        fun getInstance(): MapFragment {
            return instance
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        instance = this

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        currentUser = FirebaseAuth.getInstance().currentUser!!
        postsRef = FirebaseDatabase.getInstance().getReference("posts")
        usersRef = FirebaseDatabase.getInstance().getReference("users")

        postListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                updateMarkers(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation
            }
        }
    }

    override fun onStart() {
        super.onStart()
        postsRef.addValueEventListener(postListener)
    }

    override fun onStop() {
        super.onStop()
        postsRef.removeEventListener(postListener)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set the onMarkerClickListener
        mMap.setOnMarkerClickListener(this)

        checkPermissions()
    }


    private fun checkPermissions() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val granted = PackageManager.PERMISSION_GRANTED

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != granted) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), 0)
        } else {
            mMap.isMyLocationEnabled = true
        }
    }

    private fun updateMarkers(snapshot: DataSnapshot) {
        mMap.clear()
        markers.clear()

        var lastLocation: MyLocation? = null

        for (userSnapshot in snapshot.children) {
            val uid = userSnapshot.key

            for (dataSnapshot in userSnapshot.children) {
                val postId = dataSnapshot.key!!
                val location = dataSnapshot.child("location").getValue(MyLocation::class.java)
                val imageUrl = dataSnapshot.child("urlImage").getValue(String::class.java)

                lastLocation = location

                location?.let { loc ->
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    val markerOptions = MarkerOptions().position(latLng)

                    uid?.let { userId ->
                        val userNameRef = usersRef.child(userId).child("userName")
                        userNameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(nameSnapshot: DataSnapshot) {
                                val userName = nameSnapshot.getValue(String::class.java)

                                imageUrl?.let { url ->
                                    Glide.with(requireContext())
                                        .asBitmap()
                                        .load(url)
                                        .placeholder(R.drawable.marker_default_foreground)
                                        .error(R.drawable.marker_default_foreground)
                                        .into(object : CustomTarget<Bitmap>() {
                                            override fun onResourceReady(
                                                resource: Bitmap,
                                                transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                                            ) {
                                                val compactBitmap = Bitmap.createScaledBitmap(
                                                    resource,
                                                    markerSize,
                                                    markerSize,
                                                    false
                                                )
                                                val roundedBitmap = getRoundedBitmap(compactBitmap)
                                                val markerIcon =
                                                    BitmapDescriptorFactory.fromBitmap(roundedBitmap)
                                                markerOptions.icon(markerIcon)
                                                val marker = mMap.addMarker(markerOptions)
                                                marker?.title = userName
                                                markers[postId] = marker
                                            }

                                            override fun onLoadCleared(placeholder: Drawable?) {
                                                // This method is intentionally empty
                                            }
                                        })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle cancellation
                            }
                        })
                    }
                }
            }
        }

        val zoomLevel = 13.0f
        val cameraUpdate =
            if (lastLocation != null && lastLocation.latitude != -1.0 && lastLocation.longitude != -1.0) {
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        lastLocation.latitude,
                        lastLocation.longitude
                    ), zoomLevel
                )
            } else {
                CameraUpdateFactory.newLatLngZoom(pwLatLng, zoomLevel)
            }
        mMap.moveCamera(cameraUpdate)
    }

    private fun getRoundedBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, bitmap.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // Get the marker's position
        val markerPosition = marker.position

        // Show the marker's info window (which includes the title)
        marker.showInfoWindow()

        // Animate the camera movement to the marker with zoom
        val zoomLevel = 15.0f
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerPosition, zoomLevel))

        // Return 'true' to indicate that we have consumed the event
        return true
    }

    fun getMarkersSize(): Int {
        synchronized(markers) {
            return markers.size
        }
    }
}
