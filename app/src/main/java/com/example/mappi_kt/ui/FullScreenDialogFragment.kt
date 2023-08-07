package com.example.mappi_kt.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mappi_kt.R
import com.google.firebase.auth.FirebaseAuth

class FullScreenDialogFragment : DialogFragment() {

    private lateinit var fullScreenImageView: ImageView
    private lateinit var deleteButton: Button
    private var imageUrl: String? = null
    private var userId: String? = null
    private var onDeleteClickListener: OnDeleteClickListener? = null

    companion object {
        private const val ARG_IMAGE_URL = "image_url"
        private const val ARG_USER_ID = "user_id"

        fun newInstance(imageUrl: String, userId: String): FullScreenDialogFragment {
            val fragment = FullScreenDialogFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE_URL, imageUrl)
            args.putString(ARG_USER_ID, userId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imageUrl = it.getString(ARG_IMAGE_URL)
            userId = it.getString(ARG_USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_full_screen_post, container, false)
        fullScreenImageView = view.findViewById(R.id.fullScreenImageView)
        deleteButton = view.findViewById(R.id.deletePostButton)


        if (userId != FirebaseAuth.getInstance().currentUser?.uid) {
            deleteButton.visibility = View.GONE
            deleteButton.isEnabled = false
        }


        // Load the image using Glide or any other image loading library
        Glide.with(requireContext())
            .load(imageUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(fullScreenImageView)

        deleteButton.setOnClickListener {
            onDeleteClickListener?.onDeleteClicked()
            dismiss()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    interface OnDeleteClickListener {
        fun onDeleteClicked()
    }

    fun setOnDeleteClickListener(listener: OnDeleteClickListener) {
        onDeleteClickListener = listener
    }


}
