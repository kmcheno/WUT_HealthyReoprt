package com.ming.healthyreport.service.Impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.mail.MailUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.ming.healthyreport.model.PostDTO;
import com.ming.healthyreport.model.User;
import com.ming.healthyreport.service.ReportService;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {
    private static final List<String> uaList;
    private static final HashMap<String, String> urls;
    private static final HashMap<String, List<String>> header;

    static {
        uaList = CollUtil.newArrayList(
            "Mozilla/5.0 (Linux; Android 11; POCO F2 Pro Build/RKQ1.200826.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/74.0.3729.136 Mobile Safari/537.36 MMWEBID/1230 MicroMessenger/8.0.17.2040(0x28001133) Process/toolsmp WeChat/arm64 Weixin NetType/WIFI Language/zh_CN ABI/arm64",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.20(0x1800142f) NetType/WIFI Language/zh_CN",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36 MicroMessenger/7.0.9.501 NetType/WIFI MiniProgramEnv/Windows WindowsWechat"
        );
        urls = new HashMap<>(4);
        urls.put("checkBind", "https://zhxg.whut.edu.cn/yqtjwx/api/login/checkBind");
        urls.put("bindUserInfo", "https://zhxg.whut.edu.cn/yqtjwx/api/login/bindUserInfo");
        urls.put("monitorRegister", "https://zhxg.whut.edu.cn/yqtjwx/./monitorRegister");
        urls.put("cancelBind", "https://zhxg.whut.edu.cn/yqtjwx/api/login/cancelBind");

        header = new HashMap<>(9);
        header.put("Host", List.of("zhxg.whut.edu.cn"));
        header.put("Connection", List.of("keep-alive"));
        header.put("X-Tag", List.of("flyio"));
        header.put("content-type", List.of("application/json"));
        header.put("encode", List.of("true"));
        header.put(
            "Referer", List.of("https://servicewechat.com/wxa0738e54aae84423/21/page-frame.html"
            ));
        header.put("Accept-Encoding", List.of("gzip, deflate, br"));
    }

    @Override
    public boolean report(User user, int waitTime, int attemptNum) {
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
            //抛出runtime异常、checked异常时都会重试，但是抛出error不会重试。
            .retryIfException()
            //返回false也需要重试
            .retryIfResult(aBoolean -> Objects.equals(aBoolean, false))
            //重调策略
            .withWaitStrategy(WaitStrategies.fixedWait(waitTime, TimeUnit.SECONDS))
            //尝试次数
            .withStopStrategy(StopStrategies.stopAfterAttempt(attemptNum))
            .build();

        StringBuilder sb = new StringBuilder();
        try {
            retryer.call(() -> {
                PostDTO postDTO;
                try {
                    postDTO = checkBind();
                    if (!postDTO.isStatus()) {
                        sb.append(postDTO.getData()).append("\n");
                        return false;
                    }
                    postDTO = bindUserInfo(user);
                    if (!postDTO.isStatus()) {
                        sb.append(postDTO.getData()).append("\n");
                        return false;
                    }
                    postDTO = monitorRegister();
                    if (!postDTO.isStatus()) {
                        sb.append(postDTO.getData()).append("\n");
                        return false;
                    }
                    return true;
                } finally {
                    cancelBind();
                }
            });
        } catch (ExecutionException | RetryException | IllegalStateException e) {
            log.warn("填报异常：{}", e.toString());
            MailUtil.send(
                user.getMail(),
                "填报失败",
                StrUtil.format("今日填报失败，请手动填报！\n各次失败原因：\n{} \n 异常状态：{}", sb, e.toString()),
                false
            );
            return false;
        }
        MailUtil.send(user.getMail(), "填报成功", "今日填报成功！", false);
        return true;
    }

    private PostDTO checkBind() {
        Set<String> userAgent = RandomUtil.randomEleSet(uaList, 1);
        header.put("User-Agent", new ArrayList<>(userAgent));
        JSONObject data = JSONUtil.createObj().set("sn", "").set("idCard", "");
        PostDTO postDTO = postMethod(urls.get("checkBind"), data.toString());
        if (postDTO.isStatus()) {
            JSONObject decodeDataJson = JSONUtil.parseObj(postDTO.getData());
            if (decodeDataJson.getBool("bind")) {
                postDTO.setStatus(false);
                return postDTO;
            }
            header.put(
                "Cookie",
                List.of(StrUtil.format("JSESSIONID={}", decodeDataJson.getStr("sessionId")))
            );
        }
        return postDTO;
    }

    private PostDTO bindUserInfo(User user) {
        JSONObject data = JSONUtil.createObj()
            .set("sn", user.getUserId()).set("idCard", user.getUserPassword());
        return postMethod(urls.get("bindUserInfo"), data.toString());
    }

    private PostDTO monitorRegister() {
        String province = "湖北省";
        String city = "武汉市";
        String county = "洪山区";
        String street = "工大路";
        String temperature = "36.5°C~36.9°C";
        StringBuilder address = new StringBuilder(province)
            .append(city)
            .append(county)
            .append(street);

        JSONObject data = JSONUtil.createObj()
            .set("diagnosisName", "")
            .set("relationWithOwn", "")
            .set("currentAddress", address)
            .set("remark", "无")
            .set("healthInfo", "正常")
            .set("isDiagnosis", "0")
            .set("isFever", "0")
            .set("isInSchool", "1")
            .set("isLeaveChengdu", "0")
            .set("isSymptom", "0")
            .set("temperature", temperature)
            .set("province", province)
            .set("city", city)
            .set("county", county);
        return postMethod(urls.get("monitorRegister"), data.toString());
    }

    private void cancelBind() {
        postMethod(urls.get("cancelBind"), "");
    }

    private PostDTO postMethod(String url, String data) {
        data = Base64.encode(data);
        String decodeData;
        boolean status;
        HttpRequest request = HttpRequest.post(url).header(header).body(data);
        try (HttpResponse response = request.execute()) {
            JSONObject responseData = JSONUtil.parseObj(response.body());
            status = responseData.getBool("status");
            if (status) {
                byte[] decode = Base64.decode(responseData.getStr("data"));
                try {
                    decodeData = JSONUtil.parseObj(decode).toString();
                } catch (Exception e) {
                    decodeData = StrUtil.str(decode, StandardCharsets.UTF_8);
                }
            } else {
                decodeData = responseData.getStr("message");
                log.info("请求url:{}, 请求message：{}", url, decodeData);
            }
            log.info("返回消息:{}", decodeData);
        }
        return PostDTO.builder().status(status).data(decodeData).build();
    }
}
