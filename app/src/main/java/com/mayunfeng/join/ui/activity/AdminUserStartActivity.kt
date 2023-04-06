package com.mayunfeng.join.ui.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.mayunfeng.join.api.SignApi
import com.mayunfeng.join.base.AppBaseActivity
import com.mayunfeng.join.bean.BaseBean
import com.mayunfeng.join.bean.StartSignBean
import com.mayunfeng.join.bean.UserSignTable
import com.mayunfeng.join.databinding.ActivityAdminUserStartBinding
import com.mayunfeng.join.service.CLOSE_ALL_NOTIFY_KEY
import com.mayunfeng.join.service.WebSocketService
import com.mayunfeng.join.ui.adapter.AdminUserStartAdapter
import com.mayunfeng.join.ui.adapter.MyStartSignInfoAdapter
import com.mayunfeng.join.utils.MyRetrofitObserver.Companion.mySubscribeMainThread
import com.mayunfeng.join.utils.retrofit.QuickRtObserverListener
import com.mayunfeng.join.utils.retrofit.RetrofitManager
import com.pikachu.utils.utils.TimeUtils
import java.io.Serializable

class AdminUserStartActivity : AppBaseActivity<ActivityAdminUserStartBinding, String>(),
    QuickRtObserverListener<BaseBean<ArrayList<UserSignTable>>> {
    private var signId: Long = -1

    private val signApi: SignApi = RetrofitManager.getInstance().create(SignApi::class.java)


    private val myStartSignInfoAdapter = AdminUserStartAdapter({
        // 签到
        this.signId = it.signId
        // 0 无密码打卡   1 签到码打卡    2 二维码打卡     3 手势打卡
        when(it.startSignInfo.signType){
            0 -> startSign(signId)
            1 -> PwsNumActivity.startActivity(this@AdminUserStartActivity, it.startSignInfo.signKey)
            2 -> QRCodeActivity.startActivity(this@AdminUserStartActivity, it.startSignInfo.signKey)
            3 -> PwsGestureActivity.startActivity(this@AdminUserStartActivity, it.startSignInfo.signKey)
        }
    }, {
        UserInfoActivity.startUserInfoActivity(this, it)
    }, {
        startActivity(GroupInfoActivity::class.java, it.id)
    })

    override fun onAppCreate(savedInstanceState: Bundle?) {
        binding.recycler.adapter = myStartSignInfoAdapter
        binding.smartRefreshLayout.setOnRefreshListener {
            startLoad()
        }
        // 加载
        binding.smartRefreshLayout.setOnLoadMoreListener {
            TimeUtils.timing(200) {
                binding.smartRefreshLayout.finishRefreshWithNoMoreData()
            }
        }
        startLoad()
    }


    private fun startLoad() {
        signId = -1
        binding.appNul.root.visibility = View.GONE
        binding.smartRefreshLayout.autoRefresh()
        signApi.sendMySignInfo().mySubscribeMainThread(this, this, -1)
    }


    override fun onSendError(t: BaseBean<ArrayList<UserSignTable>>?, e: Throwable) {
        binding.smartRefreshLayout.finishRefresh()
        binding.appNul.root.visibility = View.VISIBLE
        showToast(t?.reason ?: e.message)
    }


    override fun onSendComplete(t: BaseBean<ArrayList<UserSignTable>>) {
        binding.smartRefreshLayout.finishRefresh()
        if (t.result.isNullOrEmpty()) {
            binding.appNul.root.visibility = View.VISIBLE
            return
        }
        binding.appNul.root.visibility = View.GONE
        myStartSignInfoAdapter.refreshData(t.result)
    }

    override fun onEventBus(event: String?, key: Int?, msg: String?) {
        if (event != null && signId != -1L) startSign(signId, event)
    }


    private fun startSign(signId: Long, key: String? = null) {
        signApi.sendStartSign(signId, key).mySubscribeMainThread(this, object : QuickRtObserverListener<BaseBean<Boolean>>{
            override fun onSendError(t: BaseBean<Boolean>?, e: Throwable) {
                this@AdminUserStartActivity.signId = -1
                showToast(t?.reason ?: e.message)
            }

            override fun onSendComplete(t: BaseBean<Boolean>) {
                startLoad()
                showToast("签到完成")
            }
        })

    }

}