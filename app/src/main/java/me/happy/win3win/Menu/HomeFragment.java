package me.happy.win3win.Menu;

import android.app.Dialog;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.ArrayList;

import me.happy.win3win.Custom.EndlessRecyclerViewScrollListener;
import me.happy.win3win.R;
import me.happy.win3win.Recyclerview.HomeAdapter;
import me.happy.win3win.Recyclerview.RecAdapter;
import me.happy.win3win.Server.Item;
import me.happy.win3win.Server.ReqItems;
import me.happy.win3win.Server.RetroInterface;
import me.happy.win3win.Server.ServerGenerator;
import me.happy.win3win.UserDB.UserDBManager;
import me.happy.win3win.databinding.HomeBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Created by JY on 2017-04-11.
 */

public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private  static final String KEY = "";

    private ArrayList<Item> dataSet;
    private ArrayList<Item> dataRec; //추천용 뷰
    private HomeAdapter adapterSet;
    private RecAdapter adapterRec;

    private StaggeredGridLayoutManager mLayoutManager;
    private LinearLayoutManager mLayoutManager2;
    private String token;
    private String name;
    private int mPage=1;

    private EndlessRecyclerViewScrollListener endlessScrollListener;

    private UserDBManager mDBManager;
    private FragmentManager fgManager;
    private HomeBinding binding;

    private Dialog dialog;

    private String[] url;

    public HomeFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.home, container, false);
        View view = binding.getRoot();
        binding.setHome(this);

        binding.swipeLayout.setColorSchemeResources(R.color.orange_a);
        binding.swipeLayout.setOnRefreshListener(this);

        initRecyclerview(binding.rvHome);
        initRecyclerview(binding.rvRec);

        callAPI(ServerGenerator.getAllService(), mPage);

//        endlessScrollListener = new EndlessRecyclerViewScrollListener(mLayoutManager, 8) {
//            @Override
//            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
//                callAPI(ServerGenerator.getAllService(), mPage);
//            }
//        };
        /*
        스크롤의 위치가 최상단이아니면 새로고침 불가
         */
        binding.ScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                int scrollY = binding.ScrollView.getScrollY();
                if(scrollY == 0)
                    binding.swipeLayout.setEnabled(true);
                else
                    binding.swipeLayout.setEnabled(false);

            }
        });
        return view;
    }

    /*
    전체항목
    */

    public void callAPI(RetroInterface apiService, int mPage) {
        int i = (int) (Math.random() * 5)+1;

        Call<Chaeyong> call = apiService.getList(16, i, "json", KEY);

        call.enqueue(new Callback<Chaeyong>() {
            @Override
            public void onResponse(Call<Chaeyong> call, Response<Chaeyong> response) {
                callRecommendAPI(ServerGenerator.getRequestService());

                if (response.isSuccessful()) {

                    dataSet.addAll(dataSet.size(), response.body().getResponse().getBody().getItems().getItemList());

                    for (int i = 0; i < dataSet.size(); i++) {
                        dataSet.get(i).setThumbnail(url[i % url.length]);
                    }
                    adapterSet.notifyDataSetChanged(); //데이터가 변경되었음을 알려줌
                    callRecommendAPI(ServerGenerator.getRequestService());
                }
            }
            @Override
            public void onFailure(Call<Chaeyong> call, Throwable t) {
                t.printStackTrace();
                callRecommendAPI(ServerGenerator.getRequestService());
            }
        });
    }

    /*
    추천 데이터
     */
    public void callRecommendAPI(RetroInterface apiService){

        Call<ReqItems> call = apiService.getRecommendList(token);

        dataRec.clear();

        call.enqueue(new Callback<ReqItems>() {
            @Override
            public void onResponse(Call<ReqItems> call, Response<ReqItems> response) {
                if (response.isSuccessful() && response.body().getRequestList() !=null) {

                    dataRec.addAll(dataRec.size(), response.body().getRequestList());

                    for(int i=0; i< dataRec.size(); i++) {
                        int num= (int) (Math.random() * url.length);
                        dataRec.get(i).setThumbnail(url[ num]);
                    }
                    adapterRec.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<ReqItems> call, Throwable t) {
                t.printStackTrace();

            }
        });
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLoadingDialog();

        mDBManager = UserDBManager.getInstance(getActivity());
        getTokenFromDB();

        fgManager = getFragmentManager();

        dataSet = new ArrayList<>();
        dataRec = new ArrayList<>();


        url = new String[]{"http://img.jobkorea.kr/trans/c/200x80/c/o/JK_Co_coset1647.png",
                "http://img.jobkorea.kr/trans/c/200x80/k/n/JK_Co_knlsystem.png",
                "http://img.jobkorea.kr/trans/c/200x80/d/k/JK_Co_dkvascom1.png",
                "http://img.jobkorea.kr/trans/c/200x80/a/c/JK_Co_acegluer.png",
                "http://img.jobkorea.kr/trans/c/200x80/d/k/JK_Co_dkr2011.png",
                "http://img.jobkorea.kr/trans/c/200x80/d/a/JK_Co_daeilfnb420.png",
                "http://img.jobkorea.kr/trans/c/200x80/a/s/JK_Co_asea2010.png",
                "http://img.jobkorea.kr/trans/c/200x80/w/n/JK_Co_wnwpdldostl.png",
                "http://img.jobkorea.kr/trans/c/200x80/g/o/JK_Co_goodwater.png",
                "http://img.jobkorea.kr/trans/c/200x80/q/w/JK_Co_qwedc23.png",
                "http://img.jobkorea.kr/trans/c/200x80/d/a/JK_Co_daeoco.png",
                "http://img.jobkorea.kr/trans/c/200x80/n/i/JK_Co_nit0100.png",
                "http://img.jobkorea.kr/trans/c/200x80/f/e/JK_Co_fe120808.png",
                "http://img.jobkorea.kr/trans/c/200x80/n/a/JK_Co_nava007.png"};
    }
    /*
    로딩 다이얼로그
     */
    private void setLoadingDialog() {
        dialog = new Dialog(getActivity());
        Handler mHandler = new Handler();
        dialog.setContentView(R.layout.loading);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        dialog.create();
        dialog.setCancelable(false);
        dialog.show();

        mHandler.postDelayed(mRunnable, 500);
    }


    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if(dialog != null && dialog.isShowing()){
                dialog.dismiss();
            }
        }
    };

    /*refresh scroll*/
    @Override
    public void onRefresh() {
        setLoadingDialog();
        mPage=1;
        dataSet.clear();
        callAPI(ServerGenerator.getAllService(), mPage);
        binding.swipeLayout.setRefreshing(false);
    }

    /*
 Recyclerview 초기화
  */
    protected void initRecyclerview(RecyclerView rv) {
        rv.setHasFixedSize(true);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setNestedScrollingEnabled(true);


        if (rv.getId() == R.id.rv_home) {
            rv.setNestedScrollingEnabled(false);
            adapterSet = new HomeAdapter(getActivity(), dataSet, R.layout.item_home, rv, fgManager, R.id.frag);
            rv.setAdapter(adapterSet);

            mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
            mLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS); //알아서 잘 조정
            rv.setLayoutManager(mLayoutManager);


        } else if (rv.getId() == R.id.rv_rec) {
            adapterRec = new RecAdapter(getActivity(), dataRec, R.layout.item_rec, rv, fgManager);
            rv.setAdapter(adapterRec);
            mLayoutManager2= new LinearLayoutManager(getActivity());
            mLayoutManager2.setOrientation(LinearLayoutManager.HORIZONTAL);
            rv.setLayoutManager(mLayoutManager2);

        }
    }

    /*
    get token from db
     */
    public void getTokenFromDB() {
        Cursor c = mDBManager.query(new String[]{"token", "name"}, null, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            token = c.getString(0);
            name= c.getString(1);
        }
        c.close();

    }


}
