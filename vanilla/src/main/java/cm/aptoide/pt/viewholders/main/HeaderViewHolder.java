package cm.aptoide.pt.viewholders.main;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aptoide.amethyst.models.EnumStoreTheme;
import com.aptoide.models.Displayable;
import com.aptoide.models.HeaderRow;

import butterknife.Bind;
import butterknife.ButterKnife;
import cm.aptoide.pt.R;
import cm.aptoide.pt.adapter.BaseAdapter;
import cm.aptoide.pt.viewholders.BaseViewHolder;

/**
 * Created by rmateus on 02/06/15.
 */
public class HeaderViewHolder extends BaseViewHolder {

    private final EnumStoreTheme theme;
    private final String storeName;
    private final long storeId;

    @Bind(R.id.title)         public TextView title;
    @Bind(R.id.more)          public Button more;
    @Bind(R.id.more_layout)   public RelativeLayout moreLayout;

    public HeaderViewHolder(View itemView, int viewType, EnumStoreTheme theme) {
        super(itemView, viewType);
        this.theme = theme;
        ButterKnife.bind(this, itemView);
        storeName = null;
        storeId = 0;
    }

    public HeaderViewHolder(View itemView, int viewType, EnumStoreTheme theme, String storeName, long storeId) {
        super(itemView, viewType);
        this.theme = theme;
        ButterKnife.bind(this, itemView);
        this.storeName = storeName;
        this.storeId = storeId;
    }

    @Override
    public void populateView(Displayable displayable) {
        HeaderRow row = (HeaderRow) displayable;
        title.setText(row.getLabel());
        if (row.isHasMore()) {
            more.setVisibility(View.VISIBLE);
            if (storeName == null || TextUtils.isEmpty(storeName)) {
                if (storeId == 0) {
                    more.setOnClickListener(new BaseAdapter.IHasMoreOnClickListener(row, theme));
                    moreLayout.setOnClickListener(new BaseAdapter.IHasMoreOnClickListener(row, theme));
                } else {
                    more.setOnClickListener(new BaseAdapter.IHasMoreOnClickListener(row, theme,storeId));
                    moreLayout.setOnClickListener(new BaseAdapter.IHasMoreOnClickListener(row, theme,storeId));
                }
            } else {
                if (storeId == 0) {
                    more.setOnClickListener(new BaseAdapter.IHasMoreOnClickListener(row, theme, storeName));
                    moreLayout.setOnClickListener(new BaseAdapter.IHasMoreOnClickListener(row, theme, storeName));
                } else {
                    more.setOnClickListener(new BaseAdapter.IHasMoreOnClickListener(row, theme, storeName,storeId));
                    moreLayout.setOnClickListener(new BaseAdapter.IHasMoreOnClickListener(row, theme, storeName,storeId));
                }
            }
        } else {
            more.setVisibility(View.GONE);
            moreLayout.setClickable(false);
            moreLayout.setFocusable(false);
        }

        if (theme != null) {
            @ColorInt int color = itemView.getContext().getResources().getColor(theme.getStoreHeader());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                more.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            } else {

                // put this in Utils when you need to tint a Button background
                Drawable wrapDrawable = DrawableCompat.wrap(more.getBackground());
                DrawableCompat.setTint(wrapDrawable, itemView.getContext().getResources().getColor(theme.getStoreHeader()));
                more.setBackgroundDrawable(DrawableCompat.unwrap(wrapDrawable));
            }
        }
    }

}