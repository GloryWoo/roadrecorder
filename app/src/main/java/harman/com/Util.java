package harman.com;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import harman.com.monitor.models.FileModel;

public class Util {
	private static final char HEX_DIGITS[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
			'A', 'B', 'C', 'D', 'E', 'F'};

	public static String toHexString(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
			sb.append(HEX_DIGITS[b[i] & 0x0f]);
		}
		return sb.toString();
	}

	public static String md5sum(String filePath) {
		InputStream fis;
		byte[] buffer = new byte[1024];
		int numRead = 0;
		MessageDigest md5;
		try {
			fis = new FileInputStream(filePath);
			md5 = MessageDigest.getInstance("MD5");
			while ((numRead = fis.read(buffer)) > 0) {
				md5.update(buffer, 0, numRead);
			}
			fis.close();
			return toHexString(md5.digest());
		} catch (Exception e) {
			System.out.println("error");
			return null;
		}
	}

	public static byte[] unGzip(byte[] buf) throws IOException {
		GZIPInputStream gzi = null;
		ByteArrayOutputStream bos = null;
		try {
			gzi = new GZIPInputStream(new ByteArrayInputStream(buf));
			bos = new ByteArrayOutputStream(buf.length);
			int count = 0;
			byte[] tmp = new byte[2048];
			while ((count = gzi.read(tmp)) != -1) {
				bos.write(tmp, 0, count);
			}
			buf = bos.toByteArray();
		} finally {
			if (bos != null) {
				bos.flush();
				bos.close();
			}
			if (gzi != null)
				gzi.close();
		}
		return buf;
	}

	/**
	 * Member cache
	 *
	 * @param val
	 * @return
	 * @throws IOException
	 */
	public static byte[] gzip(byte[] val) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(val.length);
		GZIPOutputStream gos = null;
		try {
			gos = new GZIPOutputStream(bos);
			gos.write(val, 0, val.length);
			gos.finish();
			gos.flush();
			bos.flush();
			val = bos.toByteArray();
		} finally {
			if (gos != null)
				gos.close();
			if (bos != null)
				bos.close();
		}
		return val;
	}

	/**
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	public static void zipFile(String source, String target) throws IOException {
		FileInputStream fin = null;
		FileOutputStream fout = null;
		GZIPOutputStream gzout = null;
		try {
			fin = new FileInputStream(source);
			fout = new FileOutputStream(target);
			gzout = new GZIPOutputStream(fout);
			byte[] buf = new byte[1024];
			int num;
			while ((num = fin.read(buf)) != -1) {
				gzout.write(buf, 0, num);
			}
		} finally {
			if (gzout != null)
				gzout.close();
			if (fout != null)
				fout.close();
			if (fin != null)
				fin.close();
		}
	}

	/**
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	public static void unZipFile(String source, String target)
			throws IOException {
		FileInputStream fin = null;
		GZIPInputStream gzin = null;
		FileOutputStream fout = null;
		try {
			fin = new FileInputStream(source);
			gzin = new GZIPInputStream(fin);
			fout = new FileOutputStream(target);
			byte[] buf = new byte[1024];
			int num;
			while ((num = gzin.read(buf, 0, buf.length)) != -1) {
				fout.write(buf, 0, num);
			}
		} finally {
			if (fout != null)
				fout.close();
			if (gzin != null)
				gzin.close();
			if (fin != null)
				fin.close();
		}
	}

	public static String makeMD5(String password) {
		MessageDigest md;
		try {
			// 
			md = MessageDigest.getInstance("MD5");
			// 
			md.update(password.getBytes());
			// digest()
			// BigInteger
			String pwd = new BigInteger(1, md.digest()).toString(16);
			System.err.println(pwd);
			return pwd;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return password;
	}

	public static String makeMD5(byte[] data) {
		MessageDigest md;
		try {
			//
			md = MessageDigest.getInstance("MD5");
			//
			md.update(data);
			// digest()
			// BigInteger
			//String pwd = new BigInteger(1, md.digest()).toString(16);
			String pwd = byteArrayToHex(md.digest());
			System.err.println(pwd);
			return pwd;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for (byte b : a) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public static byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(x);
		return buffer.array();
	}

	public static long bytesToLong(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
		buffer.put(bytes);
		buffer.flip();//need flip
		return buffer.getLong();
	}

	public static int versionCompare(String ver1, String ver2) {
		String verSz1[] = ver1.split("\\.");
		String verSz2[] = ver2.split("\\.");
		int idx = 0;
		String vItem2;

		for (String vItem1 : verSz1) {
			if (idx < verSz2.length) {
				vItem2 = verSz2[idx];
				if (Integer.parseInt(vItem1) > Integer.parseInt(vItem2))
					return 1;
				else if (Integer.parseInt(vItem1) < Integer.parseInt(vItem2))
					return -1;
			} else
				return 1;

			idx++;
		}
		if (idx == verSz2.length)
			return 0;
		else
			return -1;
	}

	public static String getDate() {
		Calendar c = Calendar.getInstance();

		String year = String.valueOf(c.get(Calendar.YEAR));
		String month = String.valueOf(c.get(Calendar.MONTH));
		String day = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
		String hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
		String mins = String.valueOf(c.get(Calendar.MINUTE));

		StringBuffer sbBuffer = new StringBuffer();
		sbBuffer.append(year + "-" + month + "-" + day + " " + hour + ":"
				+ mins);

		return sbBuffer.toString();
	}

	public static String DateToString(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
		return sdf.format(date);
	}

	public static byte[] StringToByteArray(String str) {
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		for (char a : str.toCharArray()) {
			if (a <= '9' && a >= '0') {
				bo.write(a-'0');
			}
		}
		try {
			bo.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bo.toByteArray();
	}

	public static boolean ByteArrayContains(byte[] a, byte b) {
		int s = a.length;

		for (int i = 0; i < s; i++) {
			if (a[i] == b) {
				return true;
			}
		}
		return false;
	}

	public static byte[] ByteArrayStrim0(byte[] a) {
		int s = a.length;
		int cnt0 = 0;
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		for (int i = 0; i < s; i++) {
			if (a[i] != 0)
				bo.write(a[i]);
		}
		try {
			bo.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bo.toByteArray();
	}

	public static FileModel FileModelClone(FileModel origin){
		FileModel dest = new FileModel();

		dest.setSubdir(origin.getSubdir());
		dest.setFilename(origin.getFilename());
		dest.setDate(origin.getDate());
		dest.setStopDate(origin.getStopDate());
		dest.setLocked(origin.getLocked());
		dest.setUploadStat(origin.getUploadStat());
		dest.setUploadSz(origin.getUploadSz());
		return dest;
	}
}

