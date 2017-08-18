package ru.neosvet.wallpaper.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import ru.neosvet.wallpaper.R;

/**
 * Created by NeoSvet on 18.08.2017.
 */

public class CustomImageView extends LinearLayout {
    private SubsamplingScaleImageView zooming_view;
    private ImageView simple_view;

    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CustomImageView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        View rootView = inflate(context, R.layout.custom_image_view, this);
        zooming_view = (SubsamplingScaleImageView) rootView.findViewById(R.id.zooming_view);
        simple_view = (ImageView) rootView.findViewById(R.id.simple_view);
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
//        super.setOnTouchListener(l);
        if (zooming_view != null)
            zooming_view.setOnTouchListener(l);
        else
            simple_view.setOnTouchListener(l);
    }

    public void selectSimpleView(boolean isSimple) {
        if (isSimple) {
            zooming_view.setVisibility(GONE);
            zooming_view = null;
        } else {
            simple_view.setVisibility(GONE);
            simple_view = null;
        }
    }

    public boolean isSimpleView() {
        return zooming_view == null;
    }

    public float getScale() {
        if (zooming_view != null)
            return zooming_view.getScale();
        return 1f;
    }

    public void setImage(int resource) {
        if (zooming_view != null)
            zooming_view.setImage(ImageSource.resource(resource));
        else
            simple_view.setImageResource(resource);
    }

    public void setImage(Bitmap bitmap, String path) {
        if (zooming_view != null)
            zooming_view.setImage(ImageSource.bitmap(bitmap));
        else
            simple_view.setImageURI(android.net.Uri.parse(path));
    }
}
