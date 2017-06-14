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
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.happy.win3win.R;
import me.happy.win3win.Recyclerview.SmartAdapter;
import me.happy.win3win.Server.Item;
import me.happy.win3win.Server.MyResume;
import me.happy.win3win.Server.RetroInterface;
import me.happy.win3win.Server.ServerGenerator;
import me.happy.win3win.UserDB.UserDBManager;
import me.happy.win3win.databinding.OopsBinding;
import me.happy.win3win.databinding.SmartmatchBinding;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by JY on 2017-04-11.
 */

public class SmartMatchFragment extends Fragment {


    private int isResume = 1; //이력서 있음

    private UserDBManager mDBManager;
    private SmartmatchBinding smartmatchBinding;
    private OopsBinding oopsBinding;

    private String token;
    private ArrayList<Item> dataSet;
    private RetroInterface.Resume resume;

    private Dialog dialog;
    private SmartAdapter adapter;
    private StaggeredGridLayoutManager mLayoutManager;

    private FragmentManager fgManager;
    private String[] url;

    public SmartMatchFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        if (isResume == 0) {//이력서 없음
            oopsBinding = DataBindingUtil.inflate(inflater, R.layout.oops, container, false);
            view = oopsBinding.getRoot();
            oopsBinding.setSmartmatch(this);
            ButterKnife.bind(this, view);
            return view;
        }

        smartmatchBinding = DataBindingUtil.inflate(inflater, R.layout.smartmatch, container, false);
        view = smartmatchBinding.getRoot();
        smartmatchBinding.setSmartmatch(this);
        ButterKnife.bind(this, view);

        callSmartMatchAPI(ServerGenerator.getRequestService());

        initRecyclerview(smartmatchBinding.rvSmart);

        return view;
    }

    /*
Recyclerview 초기화
*/
    protected void initRecyclerview(RecyclerView rv) {
        rv.setHasFixedSize(true);
        rv.setItemAnimator(new DefaultItemAnimator());

        adapter = new SmartAdapter(getActivity(), dataSet, R.layout.item_home, rv, fgManager, R.id.frag);
        rv.setAdapter(adapter);
        rv.setNestedScrollingEnabled(false);
        mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mLayoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS); //알아서 잘 조정
        rv.setLayoutManager(mLayoutManager);

    }

    /*
    이력서 작성하기 클릭
     */
    @OnClick(R.id.oops_resume)
    public void goToWriteResume(View v) {
        fgManager
                .beginTransaction()
                .replace(R.id.frag, new MyResumeFragment())
                .commit();
    }

    /*
      매칭 시작 버튼
       */
    @OnClick(R.id.smart_go)
    public void sendResumeGo(View v) {
        setLoadingDialog();
        callSmartMatchAPI(ServerGenerator.getRequestService());
    }

    /*
로딩 다이얼로그
 */
    private void setLoadingDialog() {

        Handler mHandler = new Handler();
        dialog.setContentView(R.layout.matching);
        dialog.setCancelable(false);
        dialog.create();

        ViewGroup.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();

        mHandler.postDelayed(mRunnable, 2000);
    }


    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    };


    /*
    스마트매치 API
     */
    public void callSmartMatchAPI(RetroInterface apiService) {
        Call<JsonObject> call = apiService.getSmartMatch(token);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {

                    if (response.body().get("code").getAsInt() == 200) {
                        Gson gson = new Gson();

                        dataSet = gson.fromJson(response.body().get("result").getAsJsonArray(), new TypeToken<ArrayList<Item>>() {
                        }.getType());


                        for (int i = 0; i < dataSet.size(); i++)
                            dataSet.get(i).setThumbnail(url[i % url.length]);

                        adapter.setGongos(dataSet); //내용 변경됨 알려줌
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {

            }
        });
    }

    public void callGetResumeAPI(RetroInterface apiService) {
        Call<MyResume> call = apiService.getMyResume(token);
    }

    /*
  이력서 보내기
   */
    public void callResumeAPI(RetroInterface apiService) {
        Call<Void> call = apiService.sendResume(token, resume);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful())
                    Log.d("jy", "이력서보내기 성공!");
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fgManager = getFragmentManager();
        dialog = new Dialog(getActivity());
        dataSet = new ArrayList<>();
        String[] columns = new String[]{"token", "wantjob", "edu", "address", "certificate", "etccareer"};

        mDBManager = UserDBManager.getInstance(getActivity()); //디비 객체 생성
        Cursor c = mDBManager.query(columns, null, null, null, null, null);
        resume = new RetroInterface.Resume();

        if (c != null && c.moveToFirst()) {
            token = c.getString(0);

            /*
            하나라도 없는 항목이있으면 이력서 없다고 말함
             */
            for (int i = 1; i < columns.length - 2; i++) {
                if (c.getString(i).trim().length() == 0) {
                    isResume = 0; //이력서없음
                    break;
                }
            }

            String[] s = c.getString(2).split(",");

            if (s[0].contains("대"))
                resume.setGrade("대학교");
            else
                resume.setGrade("고등학교");

            s = c.getString(1).split(",");

            resume.setObjective(s[0]);
            resume.setAddress(c.getString(3));
            resume.setLicense(c.getString(4));
            resume.setMisscellaneous(c.getString(5));
        }
        c.close();

        url = new String[]{"http://img.jobkorea.kr/trans/c/200x80/c/o/JK_Co_coset1647.png",
                "http://img.jobkorea.kr/trans/c/200x80/k/n/JK_Co_knlsystem.png",
                "http://img.jobkorea.kr/trans/c/200x80/d/k/JK_Co_dkvascom1.png",
                "http://img.jobkorea.kr/trans/c/200x80/a/c/JK_Co_acegluer.png",
                "http://img.jobkorea.kr/trans/c/200x80/w/n/JK_Co_wnwpdldostl.png",
                "http://img.jobkorea.kr/trans/c/200x80/n/a/JK_Co_nava007.png"};
    }
}
