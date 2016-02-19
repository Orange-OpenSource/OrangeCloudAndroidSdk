package com.orange.labs.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.orange.labs.sdk.OrangeCloudAPI;
import com.orange.labs.sdk.OrangeListener;
import com.orange.labs.sdk.exception.OrangeAPIException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by emorvill on 18/02/2016.
 */
public class CloudContextMenu extends AlertDialog {

    public CloudContextMenu(final Context ctx, OrangeCloudAPI.Entry entry, OrangeCloudAPI api) {
        super(ctx);
        //LayoutInflater factory = LayoutInflater.from(ctx);

        ListView listView = new ListView(ctx);
        MenuAdapter adapter = new MenuAdapter(ctx);

        List<String> menu = new ArrayList<>();
        menu.add("Copy");
        menu.add("Move");
        menu.add("Rename");
        menu.add("Delete");

        adapter.setMenu(menu);
        listView.setAdapter(adapter);
        setView(listView);

        setTitle(entry.name);

        if (entry.type != OrangeCloudAPI.Entry.Type.DIRECTORY) {
            // Change default icon:
            switch (entry.type) {
                case FILE:
                    setIcon(R.mipmap.file);
                    break;
                case IMAGE:
                    setIcon(R.mipmap.picture);
                    break;
                case VIDEO:
                    setIcon(R.mipmap.video);
                    break;
                case MUSIC:
                    setIcon(R.mipmap.music);
                    break;
            }

            if (entry.thumbnailURL != null) {
                api.thumbnail(entry, new OrangeListener.Success<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        Drawable d = new BitmapDrawable(ctx.getResources(), response);
                        setIcon(d);
                    }
                }, new OrangeListener.Error() {
                    @Override
                    public void onErrorResponse(OrangeAPIException error) {
                        // do nothing !
                    }
                });
            }
        } else {
            setIcon(R.mipmap.folder);
        }
    }

    public class MenuAdapter extends BaseAdapter {

        Context context;

        List<String> mMenu = new ArrayList<>();

        public MenuAdapter(Context context) {
            this.context = context;
            notifyDataSetChanged();
        }

        public List<String> getMenu() {
            return mMenu;
        }

        public void setMenu(List<String> menu) {
            if (mMenu != null) {
                mMenu.clear();
            }
            addMenu(menu);
        }

        public void addMenu(List<String> menu) {
            mMenu.addAll(menu);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mMenu.size();
        }

        @Override
        public String getItem(int position) {
            return mMenu.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mMenu.indexOf(getItem(position));
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final MenuItemHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.context_menu, parent, false);
                viewHolder = new MenuItemHolder();
                viewHolder.iconView = (ImageView) convertView.findViewById(R.id.icon);
                viewHolder.textView = (TextView) convertView.findViewById(R.id.text);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (MenuItemHolder)convertView.getTag();
            }
            String menuItemId = getItem(position);

            viewHolder.iconView.setImageResource(R.mipmap.add);
            viewHolder.textView.setText(menuItemId);

            return convertView;
        }
    }

    private static class MenuItemHolder {
        ImageView iconView;
        TextView textView;
    }
}
