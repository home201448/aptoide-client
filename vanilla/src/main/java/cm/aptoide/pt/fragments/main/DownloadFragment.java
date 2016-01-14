package cm.aptoide.pt.fragments.main;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.ProgressBar;

import com.aptoide.amethyst.LinearRecyclerFragment;
import com.aptoide.amethyst.downloadmanager.adapter.DownloadRow;
import com.aptoide.amethyst.events.BusProvider;
import com.aptoide.amethyst.events.OttoEvents;
import com.aptoide.amethyst.events.OttoEvents.DownloadServiceConnected;
import com.aptoide.amethyst.utils.Logger;
import com.aptoide.models.Displayable;
import com.aptoide.models.HeaderRow;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import butterknife.Bind;
import cm.aptoide.pt.MainActivity;
import cm.aptoide.pt.R;
import cm.aptoide.pt.adapter.main.DownloadTabAdapter;
import cm.aptoide.pt.services.DownloadService;

/**
 * Created by hsousa on 09-07-2015.
 */
public class DownloadFragment extends LinearRecyclerFragment {
    @Bind(R.id.swipe_container)       SwipeRefreshLayout swipeContainer;
    @Bind(R.id.progress_bar)          ProgressBar progressBar;

    private DownloadService service;
    private ArrayList<Displayable> displayableList = new ArrayList<>();

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.d("AptoideDownloadFragment", "onViewCreated");
        DownloadTabAdapter adapter = new DownloadTabAdapter(displayableList);
        getRecyclerView().setAdapter(adapter);
        swipeContainer.setEnabled(false);
        progressBar.setVisibility(View.GONE);
        // TODO: empty donwload message
    }

    @Subscribe
    public void refreshUi(DownloadServiceConnected event) {
        Logger.i("AptoideDownloadFragment", "refreshUi");
        service = ((MainActivity)getActivity()).getDownloadService();
        if (service != null) {
//            ongoingList.clear();
//            notOngoingList.clear();
//            ongoingList.addAll(service.getAllActiveDownloads());
//            notOngoingList.addAll(service.getAllNonActiveDownloads());
//            sectionAdapter.notifyDataSetChanged();

            displayableList.clear();
            ArrayList<Displayable> onGoing = service.getAllActiveDownloads();
            ArrayList<Displayable> notOnGoing = service.getAllNonActiveDownloads();

            if (onGoing != null && onGoing.size() > 0) {
                displayableList.add(new HeaderRow(getString(R.string.active), false, BUCKET_SIZE));
                displayableList.addAll(onGoing);
            }

            if (notOnGoing != null && notOnGoing.size() > 0) {
                displayableList.add(new HeaderRow(getString(R.string.completed), false, BUCKET_SIZE));
                displayableList.addAll(notOnGoing);
            }

            getRecyclerView().getAdapter().notifyDataSetChanged();
            getActivity().supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        BusProvider.getInstance().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onResume() {
        Logger.d("AptoideDownloadFragment", "onResume");
        super.onResume();
        refreshUi(null);
    }

    @Subscribe
    public synchronized void onDownload(OttoEvents.DownloadEvent event) {
        Logger.d("AptoideDownloadFragment", "onDownload" + event.getId());
        refreshUi(null);
    }


    @Subscribe
    public synchronized void onDownloadUpdate(OttoEvents.DownloadInProgress event) {
        if (event != null && event.getDownload() != null) {
            Logger.d("AptoideDownloadFragment", "onDownloadUpdate " + event.getDownload().getId());
            for (Displayable displayable : displayableList) {
                if (displayable instanceof DownloadRow) {
                    DownloadRow row = (DownloadRow) displayable;
                    if (event.getDownload().equals(row.download)) {
                        getRecyclerView().getAdapter().notifyItemChanged(displayableList.indexOf(displayable));
                    }
                }
            }
        } else {
            Logger.d("AptoideDownloadFragment", "onDownloadUpdate ERROR! event || download == null");
        }

//        try {
//            int start = getListView().getFirstVisiblePosition();
//            for (int i = start, j = getListView().getLastVisiblePosition(); i <= j; i++) {
//                if (download.equals((getListView().getItemAtPosition(i)))) {
//                    View view = getListView().getChildAt(i - start);
//                    getListView().getAdapter().getView(i, view, getListView());
//                    break;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    public static DownloadFragment newInstance(){
        return new DownloadFragment();
    }

}