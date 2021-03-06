package com.weixin.corp.main;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.weixin.aes.AesException;
import com.weixin.aes.WXBizMsgCrypt;
import com.weixin.corp.service.MessageService;
import com.weixin.corp.utils.WeixinUtil;

/**
 * 核心请求处理类（微信服务器发过来的请求）
 * 
 */
public class CorpWeixinServlet extends HttpServlet {

	public void init() throws ServletException {
		// 获取web.xml中配置的参数
		// appid第三方用户唯一凭证
		String appid = getInitParameter("appid");
		// appsecret第三方用户唯一凭证密钥
		String appsecret = getInitParameter("appsecret");
		// aeskey第三方用户加密密钥
		String aeskey = getInitParameter("aeskey");
		// agentid第三方用户应用ID
		String agentid = getInitParameter("agentid");
		
		String httpsRequestHostUrl = getInitParameter("httpsRequestHostUrl");
		String httpsRequestMethod = getInitParameter("httpsRequestMethod");
		String httpsRequestQName = getInitParameter("httpsRequestQName");

		// 未配置appid、appsecret、aeskey时给出提示
		if ("".equals(appid) || "".equals(appsecret) || "".equals(aeskey)
				|| aeskey.length() != 43 || "".equals(agentid)) {
			log.error("appid, appsecret, aeskey or agentid configuration error in web.xml, please check carefully.");
			System.exit(-1);
		} else {
			// token第三方用户验证口令
			String token = getInitParameter("token");
			if (null != token) {
				WeixinUtil.init(token, appid, appsecret, aeskey, agentid, httpsRequestHostUrl, httpsRequestMethod, httpsRequestQName);
			}
		}
	}

	private static Log log = LogFactory.getLog(CorpWeixinServlet.class);

	private static final long serialVersionUID = -5021188348833856475L;

	private static Map<String, Map<String, String>> requestCachePool = new HashMap<String, Map<String,String>>();

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// 微信加密签名
		String signature = request.getParameter("msg_signature");
		// 时间戳
		String timestamp = request.getParameter("timestamp");
		// 随机数
		String nonce = request.getParameter("nonce");
		// 随机字符串
		String echostr = request.getParameter("echostr");

		if (null == signature && null == timestamp && null == nonce) {
			System.out.println("signatures all null");
			return;
		}

		String sEchoStr; // 需要返回的明文
		PrintWriter out = response.getWriter();
		WXBizMsgCrypt wxcpt;
		try {
			wxcpt = new WXBizMsgCrypt(WeixinUtil.getToken(),
					WeixinUtil.getAeskey(), WeixinUtil.getAppid());
			sEchoStr = wxcpt.VerifyURL(signature, timestamp, nonce, echostr);
			// 验证URL成功，将sEchoStr返回
			out.print(sEchoStr);
		} catch (AesException e1) {
			e1.printStackTrace();
		}

		// 通过检验signature对请求进行校验，若校验成功则原样返回echostr，表示接入成功，否则接入失败
		if (WXBizMsgCrypt.checkSignature(WeixinUtil.getToken(), signature,
				timestamp, nonce)) {
			out.print(echostr);
		}
		out.close();
		out = null;
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		long startDoPostTime = System.currentTimeMillis();
		System.out.println("doPost");
		System.out.println("start doPost Time = " + startDoPostTime);

		// 获得请求参数
		String signature = request.getParameter("msg_signature");
		System.out.println("signature: " + signature);
		String timestamp = request.getParameter("timestamp");
		String nonce = request.getParameter("nonce");

		String requestId = signature + timestamp + nonce;

		if (requestCachePool.containsKey(requestId)) {
			return;
		}

		// 获得post提交的数据
		BufferedReader br = new BufferedReader(new InputStreamReader(
				request.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line = null;
		while (null != (line = br.readLine())) {
			sb.append(line);
		}
		String requestMsg = sb.toString();
		System.out.println("requestCryptMsg: " + requestMsg);
		String requestDecryptMsg = null;
		String aesErrorInfo = null;
		WXBizMsgCrypt wxcpt = null;
		try {
			wxcpt = new WXBizMsgCrypt(WeixinUtil.getToken(),
					WeixinUtil.getAeskey(), WeixinUtil.getAppid());
			requestDecryptMsg = wxcpt.DecryptMsg(signature, timestamp, nonce,
					requestMsg);
			System.out.println("requestDecryptMsg: " + requestDecryptMsg);
		} catch (AesException e1) {
			aesErrorInfo = e1.getMessage();
			e1.printStackTrace();
		}
		if (null == requestDecryptMsg) {
			log.error("DecryptMsg Error: " + aesErrorInfo);
			return;
		}
		log.info("requestDecryptMsg: " + requestDecryptMsg);

		ByteArrayInputStream bais = new ByteArrayInputStream(
				requestDecryptMsg.getBytes("UTF-8"));
		response.setCharacterEncoding("UTF-8");

		Map<String, String> requestMap = null;
		try {
			requestMap = MessageService.parseXml(bais);
		} catch (Exception e1) {
			e1.printStackTrace();
			log.error("ParseXml Error: " + e1.getMessage());
			return;
		}
		requestCachePool.put(requestId, requestMap);
		// 处理
		String responseMsg = MessageService.processRequest(requestMap);
		if (null == responseMsg) {
			return;
		}

		System.out.println("responseMsg before encrypt: " + responseMsg);
		log.info("responseMsg before encrypt: " + responseMsg);

		long endDoPostTime = System.currentTimeMillis();
		System.out.println("end doPost Time = " + endDoPostTime);
		System.out.println("本次Post响应耗时: " + (endDoPostTime - startDoPostTime)
				/ 1000f + "秒");
		// if (endDoPostTime - startDoPostTime > 5) {
		// // System.out.println("超时响应, 改为模板推送");
		// // MessageUtil.sendTemplateMessage(requestMap);
		// } else {
		// 响应消息
		try {
			responseMsg = wxcpt.EncryptMsg(responseMsg, timestamp, nonce);
		} catch (AesException e1) {
			aesErrorInfo = e1.getMessage();
			e1.printStackTrace();
		}
		if (null == responseMsg) {
			log.error("EncryptMsg Error: " + aesErrorInfo);
			return;
		}
		log.info("responseMsg after encrypt: " + responseMsg);

		PrintWriter out = response.getWriter();
		out.print(responseMsg);
		out.close();

		requestCachePool.remove(requestId);
	}

}