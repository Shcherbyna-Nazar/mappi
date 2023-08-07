package com.example.mappi_kt.fragments

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mappi_kt.FriendsActivity
import com.example.mappi_kt.LoginActivity
import com.example.mappi_kt.R
import com.example.mappi_kt.SearchActivity
import com.example.mappi_kt.adapters.PostAdapter
import com.example.mappi_kt.entity.Post
import com.example.mappi_kt.entity.User
import com.example.mappi_kt.ui.FullScreenDialogFragment
import com.example.mappi_kt.utils.ImageUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_IMAGE_PICK = 2
        private var isPost: Boolean = false
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var usernameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var userImage: ImageView

    private lateinit var postRecyclerView: RecyclerView
    private lateinit var addPostButton: Button
    private lateinit var addFriendsIcon: ImageView

    private lateinit var friendsAmount: TextView
    private lateinit var markersAmount: TextView
    private lateinit var postsAmount: TextView

    private lateinit var database: DatabaseReference
    private lateinit var postAdapter: PostAdapter

    private lateinit var menuIcon: ImageView
    private lateinit var friends: LinearLayout

    private lateinit var userId: String
    private lateinit var authUserId: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        menuIcon = view.findViewById(R.id.menuIcon)

        postRecyclerView = view.findViewById(R.id.postRecyclerView)
        addPostButton = view.findViewById(R.id.addPostButton)
        addFriendsIcon = view.findViewById(R.id.addFriendsIcon)

        friendsAmount = view.findViewById(R.id.friendsAmount)
        markersAmount = view.findViewById(R.id.markersAmount)
        postsAmount = view.findViewById(R.id.postsAmount)

        usernameTextView = view.findViewById(R.id.usernameTextView)
        emailTextView = view.findViewById(R.id.emailTextView)
        userImage = view.findViewById(R.id.userImage)
        userImage.setOnClickListener {
            showImagePickerDialog()
        }

        database = FirebaseDatabase.getInstance().reference
        postAdapter = PostAdapter(requireContext())

        authUserId = FirebaseAuth.getInstance().currentUser?.uid.toString()


        userId = arguments?.getString("userId")
            ?: authUserId


        if (userId != authUserId) {
            addPostButton.visibility = View.GONE
            addPostButton.isEnabled = false
        }

        userId.let { uid ->
            val userRef = database.child("users").child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        usernameTextView.text = it.userName
                        emailTextView.text = it.email
                        Glide.with(requireContext())
                            .load(it.imageUrl)
                            .placeholder(R.drawable.ic_person_foreground)
                            .error(R.drawable.ic_person_foreground)
                            .into(userImage)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle any database error if needed
                }
            })
        }

        setupRecyclerView()

        addPostButton.setOnClickListener {
            isPost = true
            checkCameraPermission()
        }

        menuIcon.setOnClickListener { v ->
            val popupMenu = PopupMenu(requireContext(), menuIcon)
            popupMenu.menuInflater.inflate(R.menu.home_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_sign_out -> {
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        startActivity(intent)
                        activity?.finish()
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }

        addFriendsIcon = view.findViewById(R.id.addFriendsIcon)
        addFriendsIcon.setOnClickListener {
            val newIntent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(newIntent)
        }

        friends = view.findViewById(R.id.friends)
        friends.setOnClickListener {
            val newIntent = Intent(requireContext(), FriendsActivity::class.java)
            startActivity(newIntent)
        }



        return view
    }

    private fun setupRecyclerView() {
        postRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        postRecyclerView.adapter = postAdapter

        userId.let { uid ->
            val postsRef = database.child("posts").child(uid)
            postsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val posts = mutableListOf<Post>()
                    for (postSnapshot in snapshot.children) {
                        val post = postSnapshot.getValue(Post::class.java)
                        post?.let {
                            posts.add(it)
                        }
                    }
                    posts.reverse()
                    postAdapter.setData(posts)
                    postsAmount.text =
                        posts.size.toString() // Set the postsAmount TextView with the number of posts

                    markersAmount.text = MapFragment.getInstance().getMarkersSize().toString()

                    var friendsCount: Int
                    val userRef = database.child("users").child(userId)
                    userRef.child("friends").addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            friendsCount = snapshot.childrenCount.toInt()
                            friendsAmount.text = friendsCount.toString()
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    }
                    )

                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle any database error if needed
                }
            })
        }
        postAdapter.setOnItemClickListener(object : PostAdapter.OnItemClickListener {
            override fun onItemClick(post: Post) {
                openFullScreenDialog(post.urlImage, post.timestamp)
            }
        })
    }

    private fun openFullScreenDialog(imageUrl: String, postId: String) {

        val fragment = FullScreenDialogFragment.newInstance(imageUrl, userId)
        fragment.setOnDeleteClickListener(object : FullScreenDialogFragment.OnDeleteClickListener {
            override fun onDeleteClicked() {
                deletePostFromFirebase(userId, postId)
            }
        })
        fragment.show(parentFragmentManager, "fullScreenDialog")
    }

    private fun deletePostFromFirebase(userId: String?, postId: String) {
        userId?.let {
            val postRef = database.child("posts").child(userId).child(postId)
            postRef.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    deletePostImageFromFirebaseStorage(userId, postId)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to delete post",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun deletePostImageFromFirebaseStorage(userId: String?, postId: String) {
        userId?.let {
            val storageRef =
                FirebaseStorage.getInstance().reference.child("posts").child(userId)
                    .child("$postId.jpg")
            storageRef.delete().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Post deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to delete post image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Option")
        builder.setItems(options) { _, item ->
            when (item) {
                0 -> checkCameraPermission()
                1 -> chooseFromGallery()
                2 -> return@setItems
            }
        }
        builder.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            takePhoto()
        }
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            // Create a file to store the image
            val imageFile = createImageFile()
            val imageUri = imageFile?.let {
                FileProvider.getUriForFile(
                    requireContext(),
                    "com.example.mappi_kt.fileprovider",
                    it
                )
            }

            // Pass the file URI to the camera intent
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(requireContext(), "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private var currentPhotoPath: String? = null

    private fun createImageFile(): File? {
        // Create an image file name
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )

        // Save a file path for use with ACTION_IMAGE_CAPTURE intent
        currentPhotoPath = imageFile.absolutePath

        return imageFile
    }


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun chooseFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    if (!isPost) {
                        val imageFile = currentPhotoPath?.let { File(it) }
                        val imageBitmap =
                            imageFile?.let { BitmapFactory.decodeFile(it.absolutePath) }
                        val rotatedBitmap = imageBitmap?.let {
                            ImageUtils.rotateImageIfRequired(
                                requireContext(),
                                it,
                                Uri.fromFile(imageFile)
                            )
                        }

                        Glide.with(requireContext())
                            .load(rotatedBitmap)
                            .placeholder(R.drawable.ic_person_foreground)
                            .error(R.drawable.ic_person_foreground)
                            .into(userImage)
                        rotatedBitmap?.let { uploadImageToFirebaseStorage(it) }
                    } else {
                        val imageFile = currentPhotoPath?.let { File(it) }
                        val imageBitmap =
                            imageFile?.let { BitmapFactory.decodeFile(it.absolutePath) }
                        val rotatedBitmap = imageBitmap?.let {
                            ImageUtils.rotateImageIfRequired(
                                requireContext(),
                                it,
                                Uri.fromFile(imageFile)
                            )
                        }
                        val post = Post()
                        post.timestamp =
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

                        userId.let {
                            if (imageBitmap != null && rotatedBitmap != null) {
                                val location =
                                    if (ActivityCompat.checkSelfPermission(
                                            requireContext(),
                                            Manifest.permission.ACCESS_FINE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        try {
                                            val locationManager =
                                                requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                        } catch (e: SecurityException) {
                                            e.printStackTrace()
                                            null
                                        }
                                    } else {
                                        null
                                    }
                                post.location.latitude = location?.latitude!!
                                post.location.longitude = location.longitude
                                uploadPostToFirebaseDatabase(it, post, rotatedBitmap)
                            }
                        }
                    }
                }
                REQUEST_IMAGE_PICK -> {
                    val imageUri = data?.data as Uri
                    val imageBitmap = ImageUtils.getBitmapFromUri(requireContext(), imageUri)
                    val rotatedBitmap =
                        ImageUtils.rotateImageIfRequired(requireContext(), imageBitmap, imageUri)
                    Glide.with(requireContext())
                        .load(rotatedBitmap)
                        .placeholder(R.drawable.ic_person_foreground)
                        .error(R.drawable.ic_person_foreground)
                        .into(userImage)
                    uploadImageToFirebaseStorage(rotatedBitmap)
                }
            }
        }
    }


    private fun uploadPostToFirebaseDatabase(userId: String, post: Post, imageBitmap: Bitmap) {
        val postId = post.timestamp

        val userPosts = database.child("posts").child(userId)
        val postRef = userPosts.child(postId)

        postRef.setValue(post).addOnCompleteListener { postUploadTask ->
            if (postUploadTask.isSuccessful) {
                // Upload the image to Firebase Storage
                val storageRef = FirebaseStorage.getInstance().reference
                val imageRef = storageRef.child("posts").child(userId).child("$postId.jpg")
                val imageData: ByteArray = ImageUtils.convertBitmapToByteArray(imageBitmap)
                val uploadTask = imageRef.putBytes(imageData)
                uploadTask.addOnCompleteListener { imageUploadTask ->
                    if (imageUploadTask.isSuccessful) {
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            val imageUrl = uri.toString()

                            // Update the post's imageUrl property
                            postRef.child("urlImage").setValue(imageUrl)
                                .addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Post uploaded successfully",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Failed to update post's imageUrl",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Image upload failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Failed to upload post",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun uploadImageToFirebaseStorage(imageBitmap: Bitmap) {
        userId.let { uid ->
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("userImages").child("$uid.jpg")

            val imageData: ByteArray = ImageUtils.convertBitmapToByteArray(imageBitmap)

            val uploadTask = imageRef.putBytes(imageData)
            uploadTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        updateImageUrlInFirebaseDatabase(uid, imageUrl)
                    }
                } else {
                    Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


    private fun updateImageUrlInFirebaseDatabase(userId: String, imageUrl: String) {
        val userRef = database.child("users").child(userId)
        userRef.child("imageUrl").setValue(imageUrl)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Image uploaded successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to update image URL",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


}
