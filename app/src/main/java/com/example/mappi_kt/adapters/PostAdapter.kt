package com.example.mappi_kt.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mappi_kt.R
import com.example.mappi_kt.entity.Post

class PostAdapter(
    private val context: Context
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {
    private var postsList: List<Post> = emptyList()
    private var onItemClickListener: OnItemClickListener? = null


    interface OnItemClickListener {
        fun onItemClick(post: Post)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(posts: List<Post>) {
        postsList = posts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.photo_item, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postsList[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int {
        return postsList.size
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postImageView: ImageView = itemView.findViewById(R.id.postImageView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = postsList[position]
                    // Handle item click event here
                    onItemClickListener?.onItemClick(post)
                }
            }
        }

        fun bind(post: Post) {
            val imageUrl = post.urlImage
            Glide.with(context)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(postImageView)
        }
    }
}
