package me.jcala.xmarket.mvp.user.trades.add;

import android.content.Context;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import me.jcala.xmarket.R;
import me.jcala.xmarket.conf.Api;
import me.jcala.xmarket.data.dto.Result;
import me.jcala.xmarket.data.pojo.Trade;
import me.jcala.xmarket.data.pojo.User;
import me.jcala.xmarket.data.storage.UserIntermediate;
import me.jcala.xmarket.util.FileUtils;
import me.jcala.xmarket.util.RetrofitUtils;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class TradeAddPresenterImpl
        implements TradeAddPresenter,TradeAddModel.onTradeAddListener{
    private TradeAddModel model;
    private TradeAddView view;
    private Context context;

    public TradeAddPresenterImpl(Context context, TradeAddView view) {
        this.context = context;
        this.view = view;
        this.model=new TradeAddModelImpl();
    }

    @Override
    public void gainTagList() {
       model.executeGetTagsReq(this);
    }

    @Override
    public void hasGotAddTradeResult(Result<String> result) {
        view.whenStopProgress();
        if (result==null){
            view.whenFail(Api.SERVER_ERROR.msg());
            return;
        }

        switch (result.getCode()) {
            case 100:
                view.whenAddSuccess();break;
            case 99:
                view.whenFail(Api.SERVER_ERROR.msg());break;
            default:
        }
    }

    @Override
    public void hasGoTagsResult(Result<List<String>> result) {
        if (result==null || result.getData() == null){
            view.whenFail(Api.SERVER_ERROR.msg());
            return;
        }

        switch (result.getCode()) {
            case 100:
                view.whenGetTagListSuccess(result.getData());break;
            case 99:
                view.whenFail(result.getMsg());break;
            default:
        }
    }

    @Override
    public void releaseTrade(LinkedList<String> picUrls, EditText title,
                             EditText price, EditText desc, TextView tag) {
        Trade trade=checkForm(picUrls,title,price,desc,tag);

        if (!trade.isReleaseCheck()){
            return;
        }

        List<File> files= FileUtils.compressMultiFilesExceptLast(context,picUrls);
        List<MultipartBody.Part> pics= RetrofitUtils.filesToMultipartBodyParts(files);
        String tradeJsonStr=new Gson().toJson(trade);
        RequestBody tradeJson=RetrofitUtils.createPartFromString(tradeJsonStr);
        model.executeAddTradeReq(tradeJson,trade.getAuthor().getId(),pics,this);
        view.whenStartProgress();
    }

    private Trade checkForm(List<String> picUrls,EditText title,EditText price,EditText desc,TextView tag){
        Trade trade=new Trade();
        trade.setReleaseCheck(false);
        if (picUrls.size() < 2){
            view.whenFail("请选择至少一张配图");
            return trade;
        }
        String titleData=title.getText().toString().trim();
        if (titleData.isEmpty()){
            view.whenFail("标题不可以为空");
            return trade;
        }
        trade.setTitle(titleData);
        String priceData=price.getText().toString().trim();
        if (titleData.isEmpty()){
            view.whenFail("价格不可以为空");
            return trade;
        }
        long priceValue=Long.parseLong(priceData);
        trade.setPrice(priceValue);
        String descData=desc.getText().toString().trim();
        if (descData.isEmpty()){
            view.whenFail("描述不可以为空");
            return trade;
        }
        trade.setDesc(descData);
        String tagData=tag.getText().toString().trim();
        String textViewValue=context.getResources().getString(R.string.trade_add_tag);
        if (tagData.equals(textViewValue)){
            view.whenFail("请选择商品分类");
            return trade;
        }
        trade.setTagName(tagData);
        User author= UserIntermediate.instance.getUser(context);
        if (author==null || author.getId()==null){
            return trade;
        }
        trade.setAuthor(author);
        trade.setReleaseCheck(true);
        return trade;
    }
}
