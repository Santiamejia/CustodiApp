package com.santiagoM.custodiapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TrackedContactsAdapter extends RecyclerView.Adapter<TrackedContactsAdapter.ContactViewHolder> {

    private List<ReportesActivity.TrackedContact> contacts;
    private OnContactRemoveListener removeListener;

    public interface OnContactRemoveListener {
        void onRemoveContact(ReportesActivity.TrackedContact contact);
    }

    public TrackedContactsAdapter(List<ReportesActivity.TrackedContact> contacts, OnContactRemoveListener removeListener) {
        this.contacts = contacts;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tracked_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ReportesActivity.TrackedContact contact = contacts.get(position);
        holder.bind(contact, removeListener);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private TextView tvContactName;
        private ImageView ivRemoveContact;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContactName = itemView.findViewById(R.id.tv_contact_name);
            ivRemoveContact = itemView.findViewById(R.id.iv_remove_contact);
        }

        public void bind(ReportesActivity.TrackedContact contact, OnContactRemoveListener removeListener) {
            tvContactName.setText(contact.getUserName());
            ivRemoveContact.setOnClickListener(v -> removeListener.onRemoveContact(contact));
        }
    }
}