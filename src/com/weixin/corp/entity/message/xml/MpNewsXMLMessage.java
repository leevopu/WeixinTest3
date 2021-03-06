package com.weixin.corp.entity.message.xml;

import com.weixin.corp.entity.message.pojo.MpArticle;
import com.weixin.corp.entity.message.pojo.MpNews;


/**
 * 图文消息
 * 
 */
public class MpNewsXMLMessage extends CorpBaseXMLMessage {
	/**
	 * 多条图文消息信息
	 */
	private MpNews mpnews;

	public MpNews getMpnews() {
		return mpnews;
	}

	public MpNewsXMLMessage(String mediaId, String title,String thumb_media_id,String content) {
		super();
		MpArticle[] articles = {new MpArticle(title,thumb_media_id,content)};
		this.mpnews = new MpNews("", articles);
		this.setMsgType("mpnews");
	}

	public MpNewsXMLMessage(String mediaId) {
		super();
		this.mpnews = new MpNews(mediaId, new MpArticle[0]);
		this.setMsgType("mpnews");
	}
}
