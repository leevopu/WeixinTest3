package com.weixin.corp.main;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.weixin.aes.AesException;
import com.weixin.aes.WXBizMsgCrypt;
import com.weixin.corp.utils.CommonUtil;
import com.weixin.corp.utils.MessageUtil;
import com.weixin.corp.utils.UploadUtil;
import com.weixin.corp.utils.WeixinUtil;

/**
 * ������������
 * 
 */
public class UploadServlet extends HttpServlet {
	private static final long serialVersionUID = 5941583433272362854L;
	private static Log log = LogFactory.getLog(UploadServlet.class);
	private static ConcurrentMap<String, Map<String, String>> requestCachePool = new ConcurrentHashMap<>();

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
//		final int MXA_SEGSIZE = 1000 * 1024 * 20;// ����ÿ������������
		long startDoPostTime = System.currentTimeMillis();
		System.out.println("doPost");
		System.out.println("start doPost Time = " + startDoPostTime);
		System.out.println("ContentType: " + request.getContentType());
		System.out.println(request.getHeader("Content-Disposition"));
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			System.out.println(headerNames.nextElement());
		}
		long contentLength = request.getContentLength();
		System.out.println("lenth: " + contentLength); //�ж��ļ�����
		// ����20M������ʾ
		String size = CommonUtil.convertFileSize(contentLength);
		System.out.println("===================="+size);
		//=================================================================
		// �ж��ļ���С
		//=================================================================
		String x = StringUtils.substringBefore(size, " ");
		System.out.println(x);
		System.out.println(+Float.parseFloat(x)>10);
		/*if(Float.parseFloat(x)>10){
			System.out.println("�ļ���С����20M�������²�����������");
			return ;
		}*/
		
		File uploadFile = parseUpload(request);
		// �ж��ļ��Ƿ��ϴ��ɹ�
		System.out.println(uploadFile.exists());
	}

	private File parseUpload(HttpServletRequest request) throws IOException {

		final int NONE = 0;
		final int DATAHEADER = 1;
		final int FILEDATA = 2;

		String contentType = request.getContentType();// ������Ϣ����
		String filename = ""; // �ļ���
		String boundary = ""; // �ֽ��
		String lastboundary = ""; // ������

		int pos = contentType.indexOf("boundary=");

		if (pos != -1) { // ȡ�÷ֽ���ͽ�����
			pos += "boundary=".length();
			boundary = "--" + contentType.substring(pos);
			lastboundary = boundary + "--";
		}
		int state = NONE;
		// �õ�����������
		DataInputStream in = new DataInputStream(request.getInputStream());
		
		// ��������Ϣ��ʵ���͵�b������
		int totalBytes = request.getContentLength();
		byte[] b = new byte[totalBytes];
		in.readFully(b);
		in.close();
		String reqContent = new String(b, "UTF-8");
		BufferedReader br = new BufferedReader(new StringReader(reqContent));

		String line = null;
		while (null != (line = br.readLine())) {
			switch (state) {
			case NONE:
				if (line.startsWith(boundary)) {
					state = DATAHEADER;
				}
				break;
			case DATAHEADER:
				pos = line.indexOf("filename=");
				if (pos != -1) {
					String temp = line;
					pos = line.indexOf("filename=");
					pos += "filename=".length() + 1;
					line = line.substring(pos);
					int l = line.length();
					line = line.substring(0, l - 1);// ȥ������Ǹ����š�
					pos = line.lastIndexOf("\\");
					line = line.substring(pos + 1);
					filename = line;
					//=======================================================================
					//        ��ȡ�ļ���׺   �ж��ļ�����  ����ͼƬ���ͣ������CommonUtil.compressPic() ѹ��
					//=======================================================================
					String x = StringUtils.substringAfterLast(filename, ".");
					System.out.println("--------------------------"+x+"-----------------------------");
					// ���ֽ�������ȡ���ļ�����
					pos = byteIndexOf(b, temp, 0);
					b = subBytes(b, pos + temp.getBytes().length + 2, b.length);// ȥ��ǰ��Ĳ���
					System.out.println(CommonUtil.convertFileSize(b.length));
					
					int n = 0;
					/**
					 * ����boundary������ Content-Disposition: form-data; name="bin";
					 * filename="12.pdf" Content-Type: application/octet-stream
					 * Content-Transfer-Encoding: binary ���ַ���
					 */
					while ((line = br.readLine()) != null) {
						if (n == 1)
							break;
						if (line.equals(""))
							n++;

						b = subBytes(b, line.getBytes().length + 2, b.length);
					}
					pos = byteIndexOf(b, boundary, 0);
					if (pos != -1)
						b = subBytes(b, 0, pos - 1);
					state = FILEDATA;
				}
				break;
			case FILEDATA:
				while ((!line.startsWith(boundary))
						&& (!line.startsWith(lastboundary))) {
					line = br.readLine();
					if (line.startsWith(boundary)) {
						state = DATAHEADER;
						break;
					}
				}
				break;
			}
		}
		File uploadFolder = new File(UploadUtil.TEMP_URL + CommonUtil.getDateStr(new Date(), "yyyy-MM-dd"));
		if(!uploadFolder.exists()){
			uploadFolder.mkdir();
		}
		System.out.println(uploadFolder.getAbsolutePath());
		System.out.println(uploadFolder.getAbsolutePath() + File.separator +  filename);
		File uploadFile = new File(uploadFolder.getAbsolutePath() + File.separator +  filename);
		// ���������
		FileOutputStream outStream = new FileOutputStream(uploadFile);
		// д������
		outStream.write(b, 0, b.length - 1);
		// �ر������
		outStream.close();
		return uploadFile;
	}

	// �ֽ������е�INDEXOF��������STRING���е�INDEXOF����
	public static int byteIndexOf(byte[] b, String s, int start) {
		return byteIndexOf(b, s.getBytes(), start);
	}

	// �ֽ������е�INDEXOF��������STRING���е�INDEXOF����
	public static int byteIndexOf(byte[] b, byte[] s, int start) {
		int i;
		if (s.length == 0) {
			return 0;
		}
		int max = b.length - s.length;
		if (max < 0)
			return -1;
		if (start > max)
			return -1;
		if (start < 0)
			start = 0;
		search: for (i = start; i <= max; i++) {
			if (b[i] == s[0]) {
				int k = 1;
				while (k < s.length) {
					if (b[k + i] != s[k]) {
						continue search;
					}
					k++;
				}
				return i;
			}
		}
		return -1;
	}

	// ���ڴ�һ���ֽ���������ȡһ���ֽ�����
	public static byte[] subBytes(byte[] b, int from, int end) {
		byte[] result = new byte[end - from];
		System.arraycopy(b, from, result, 0, end - from);
		return result;
	}

	// ���ڴ�һ���ֽ���������ȡһ���ַ���
	public static String subBytesString(byte[] b, int from, int end) {
		return new String(subBytes(b, from, end));
	}

}