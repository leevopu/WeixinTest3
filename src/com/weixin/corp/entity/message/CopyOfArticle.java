package com.weixin.corp.entity.message;

/**
 * ͼ����Ϣ
 * sss
 */
public class CopyOfArticle {
	/**
	 * ͼ����Ϣ����
	 */
	private String Title;

	/**
	 * ͼ����Ϣ����
	 */
	private String Description;

	/**
	 * ͼƬ���ӣ�֧��JPG��PNG��ʽ��<br>
	 * �Ϻõ�Ч��Ϊ��ͼ640*320��Сͼ80*80
	 */
	private String PicUrl;

	/**
	 * ���ͼ����Ϣ��ת����
	 */
	private String Url;

	public String getTitle() {
		return Title;
	}

	public void setTitle(String title) {
		Title = title;
	}

	public String getDescription() {
		return null == Description ? "" : Description;
	}

	public void setDescription(String description) {
		Description = description;
	}

	public String getPicUrl() {
		return null == PicUrl ? "" : PicUrl;
	}

	public void setPicUrl(String picUrl) {
		PicUrl = picUrl;
	}

	public String getUrl() {
		return null == Url ? "" : Url;
	}

	public void setUrl(String url) {
		Url = url;
	}

	public CopyOfArticle(String title, String description, String picUrl, String url) {
		super();
		Title = title;
		Description = description;
		PicUrl = picUrl;
		Url = url;
	}

	public CopyOfArticle() {
		super();
	}
	
}