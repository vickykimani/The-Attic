package com.vicky.theattic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private DatabaseReference likesRef;
    private FirebaseAuth mAuth;

    private FirebaseAuth.AuthStateListener mAuthListener;
    Boolean likeChecker = false;
    private FirebaseRecyclerAdapter adapter;
    String currentUserID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        linearLayoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent loginIntent = new Intent(MainActivity.this, RegisterActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            updateUI(currentUser);
            adapter.startListening();
        }
    }

    private void updateUI(final FirebaseUser currentUser) {
        Query query = FirebaseDatabase.getInstance().getReference().child("Posts");
        FirebaseRecyclerOptions<Attic> options = new FirebaseRecyclerOptions.Builder<Attic>().setQuery(query, new SnapshotParser<Attic>() {
            @NonNull
            @Override
            public Attic parseSnapshot(@NonNull DataSnapshot snapshot) {
                return new Attic(snapshot.child("title").getValue().toString(),
                        snapshot.child("desc").getValue().toString(),
                        snapshot.child("postImage").getValue().toString(),
                        snapshot.child("displayName").getValue().toString(),
                        snapshot.child("profilePhoto").getValue().toString(),
                        snapshot.child("time").getValue().toString(),
                        snapshot.child("date").getValue().toString());
            }
        })
        .build();

        adapter = new FirebaseRecyclerAdapter<Attic, AtticViewHolder>(options) {
            @NonNull
            @Override
            public AtticViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_items, parent, false);
                return new AtticViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull AtticViewHolder holder, int positon, @NonNull Attic model) {
                final String post_key = getRef(position).getKey();

                holder.setTitle(model.getTitle());
                holder.setDesc(model.getDesc());
                holder.setPostImage(getApplicationContext(), model.getPostImage());
                holder.setUserName(model.getDisplayName());
                holder.setProfilePhoto(getApplicationContext(), model.getProfilePhoto());
                holder.setTime(model.getTime());
                holder.setDate(model.getDate());

                holder.setLikeButtonStatus(post_key);

                holder.post_layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        likeChecker = true;

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if(user != null){
                            currentUserID=user.getUid();
                        } else {
                            Toast.makeText(MainActivity.this, "please login",Toast.LENGTH_SHORT).show();
                        }

                        likesRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (likeChecker.equals(true)) {
                                    if (snapshot.child(post_key).hasChild(currentUserID)) {
                                        likesRef.child(post_key).child(currentUserID).removeValue();
                                        likeChecker = false;
                                    } else {
                                        likesRef.child(post_key).child(currentUserID).setValue(true);
                                        likeChecker = false;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }
                });
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            adapter.stopListening();
        }
    }

    public class AtticViewHolder extends RecyclerView.ViewHolder{

        public TextView post_title;
        public TextView post_desc;
        public ImageView post_image;
        public TextView postUserName;
        public ImageView user_image;
        public TextView postTime;
        public TextView postDate;
        public LinearLayout post_layout;
        public ImageButton likePostButton, commentPostButton;
        public TextView displayLikes;

        int countLikes;

        String currentUserID;

        FirebaseAuth mAuth;

        DatabaseReference likesRef;

        public AtticViewHolder(@NonNull View itemView) {
            super(itemView);

            post_title = itemView.findViewById(R.id.post_title_txtview);
            post_desc = itemView.findViewById(R.id.post_desc_txtview);
            post_image = itemView.findViewById(R.id.post_image);
            postUserName = itemView.findViewById(R.id.post_user);
            user_image = itemView.findViewById(R.id.userImage);
            postTime = itemView.findViewById(R.id.time);
            postDate = itemView.findViewById(R.id.date);
            post_layout = itemView.findViewById(R.id.linear_layout_post);
            likePostButton = itemView.findViewById(R.id.likes_display);

            likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        }

        public void setTitle(String title){
            post_title.setText(title);
        }
        public void setDesc(String desc){
            post_desc.setText(desc);
        }
        public void setPostImage(Context ctx, String postImage){
            Picasso.get().load(postImage).into(post_image);
        }
        public void setUserName(String userName){
            postUserName.setText(userName);
        }
        public void setProfilePhoto(Context context, String profilePhoto){
            Picasso.get().load(profilePhoto).into(user_image);
        }
        public void setTime(String time){
            postTime.setText(time);
        }
        public void setDate(String date){
            postDate.setText(date);
        }
        public void setLikeButtonStatus(final String post_key){
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                currentUserID = user.getUid();
            } else {
                Toast.makeText(MainActivity.this, "please login", Toast.LENGTH_SHORT).show();
            }

            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if(snapshot.child(post_key).hasChild(currentUserID)){
                        countLikes=(int) snapshot.child(post_key).getChildrenCount();
                        likePostButton.setImageResource(R.drawable.like);
                        displayLikes.setText(Integer.toString(countLikes));
                    } else {
                        countLikes=(int) snapshot.child(post_key).getChildrenCount();
                        likePostButton.setImageResource(R.drawable.dislike);
                        displayLikes.setText(Integer.toString(countLikes));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id == R.id.action_add) {
            Intent postIntent=new Intent(this,PostActivity.class);
            startActivity(postIntent);
        }
        else if(id == R.id.logout) {
            mAuth.signOut();
            Intent logoutIntent = new Intent(MainActivity.this, RegisterActivity.class);
            logoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(logoutIntent);
        }
        return super.onOptionsItemSelected(item);
    }



}




















































































































