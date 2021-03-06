package tools;

import info.BlogInfo;
import info.ProfileInfo;
import info.ShareInfo;
import info.StatusInfo;
import info.UserSample;
import info.WordInfo;
import info.photoInfo;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import srt.photo_feature;

public class DataProcessor {
	BufferedReader br;
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	Pattern emoPattern = Pattern.compile("\\([0-9a-z\u4e00-\u9fa5]+\\)");
	Pattern imgPattern = Pattern.compile("(jpg)|(gif)");
	Pattern chiPattern = Pattern.compile("[\u4e00-\u9fa5]");
	Pattern doublePattern = Pattern.compile("\\d\\.(\\d)+");
	Splitter splitter = new Splitter();
	WordCounter wc;
	Matcher emoMatcher, imgMatcher, chiMatcher;
	JSONParser parser = new JSONParser();
	
	public static void main(String[] args) {
		File data = new File("./data_json");
		String[] files = data.list();
		StatusInfo statusInfo;
		BlogInfo blogInfo;
		ShareInfo shareInfo;
		ProfileInfo profileInfo;
		photoInfo PhotoInfo;
		WordInfo wordInfo;
		DataProcessor dp = new DataProcessor();
		ArrayList<UserSample> samples = new ArrayList<UserSample>();
		BufferedReader reader;
		
		for (String personFile : files ) {
			UserSample sample = new UserSample();
			statusInfo = dp.getStatusInfo("./data_json/"+personFile);
			if (statusInfo!=null) System.out.println(statusInfo.toString());
			sample.addInfo(0, statusInfo);
			blogInfo = dp.getBlogInfo("./data_json/"+personFile);
			if (blogInfo!=null) System.out.println(blogInfo.toString());
			sample.addInfo(1, blogInfo);
			shareInfo = dp.getShareInfo("./data_json/"+personFile);
			if (shareInfo!=null) System.out.println(shareInfo.toString());
			sample.addInfo(2, shareInfo);
			profileInfo = dp.getProfileInfo("./data_json/"+personFile);
			if (profileInfo!=null) System.out.println(profileInfo.toString());
			sample.addInfo(3, profileInfo);
			wordInfo = dp.getWordInfo("./data_json/"+personFile);
			if (wordInfo!=null) System.out.println(wordInfo.toString());
			sample.addInfo(4, wordInfo);
			//PhotoInfo = dp.getPhotoInfo("./data/"+personFile);
	        //if (PhotoInfo!=null) System.out.println(PhotoInfo.toString());
	        //sample.addInfo(4, PhotoInfo);
			try {
				reader = new BufferedReader(new InputStreamReader(
						new FileInputStream("./result/"+personFile+".txt")));
				String result = reader.readLine();
				Matcher resultMatcher = dp.doublePattern.matcher(result);
				double[] scores = new double[5];
				int i = 0;
				while (resultMatcher.find()) {
					scores[i] = Double.parseDouble(resultMatcher.group());
					i++;
				}
				sample.setBigfive(scores[0], scores[1], scores[2], scores[3], scores[4]);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			samples.add(sample);
		}
		ARFFGenerator.generateARFF(samples);		
	}
	
	public StatusInfo getStatusInfo(String filename) {
		File status = new File(filename+"/status.dat");
		System.out.println(status.getPath());
		if (!status.exists()) return new StatusInfo();
		int num = 0;
		int sharesum = 0, maxshare = 0;
		int commentsum = 0, maxcmt = 0;
		int timediff = 0, mintimediff = Integer.MAX_VALUE;
		int textlong = 0;
		int emoticonsum = 0;
		JSONObject obj;
		try {
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream(status), "utf-8"));
			String content, temp;
			Date date, preDate = null;
			while ((temp = br.readLine())!=null) {
				obj = (JSONObject) parser.parse(temp);
				content = (String) obj.get("text");
				
				date = df.parse((String) obj.get("time"));
				if (num > 0) {
					long diff = (preDate.getTime()-date.getTime())/1000;
					if (diff > 0) {
						if (mintimediff > diff)
							mintimediff = (int) diff;
						timediff += diff;
						preDate = df.parse((String) obj.get("time"));
					}
				} else {
					preDate = df.parse((String) obj.get("time"));
				}
				
				int share = Integer.parseInt((String) obj.get("share"));
				if (maxshare < share)
					maxshare = share;
				sharesum += share;
				
				int cmt = Integer.parseInt((String) obj.get("comment"));
				if (maxcmt < cmt)
					maxcmt = cmt;
				commentsum += cmt;
				
				if (content.contains("���������"))
					content = content.substring(0, content.indexOf("���������"));
				textlong += content.length();
				emoMatcher = emoPattern.matcher(content);
				while (emoMatcher.find()) {
					if (Emoticon.contains(emoMatcher.group()))
						emoticonsum += 1;
				}
				num ++;
			}
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException | ParseException  e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
		StatusInfo statusInfo = new StatusInfo();
		if (num > 0) {
			statusInfo.setNum(num);
			statusInfo.setAvgshare((double)sharesum/num);
			statusInfo.setMaxshare(maxshare);
			statusInfo.setAvgcmt((double)commentsum/num);
			statusInfo.setMaxcmt(maxcmt);
			statusInfo.setTimediff((double)timediff/(Math.max(num-1, 1)));
			statusInfo.setMintimediff(mintimediff);
			statusInfo.setTextlength((double)textlong/num);
			statusInfo.setAvgemoticon((double)emoticonsum/num);
		} else {
			statusInfo.setNum(0);
			statusInfo.setMintimediff(Integer.MAX_VALUE);
		}
		return statusInfo;
	}
	
	public BlogInfo getBlogInfo(String filename) {
		File blog = new File(filename+"/blog.dat");
		System.out.println(blog.getPath());
		if (!blog.exists()) return new BlogInfo();
		int num = 0;
		int sharesum = 0, maxshare = 0;
		int commentsum = 0, maxcmt = 0;
		int viewsum = 0, maxview = 0;
		int timediff = 0, mintimediff = Integer.MAX_VALUE;
		int titlelong = 0;
		int textlong = 0;
		int publicnum = 0;
		int imgnum = 0, maximg = 0;
		JSONObject obj;
		try {
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream(blog), "utf-8"));
			String content, temp;
			Date date, preDate = null;
			while ((temp = br.readLine())!=null) {
				obj = (JSONObject) parser.parse(temp);
				titlelong += ((String) obj.get("title")).length();
				textlong += Integer.parseInt((String) obj.get("textlength"));
				content = (String) obj.get("text");
				
				int curimg = 0;
				imgMatcher = imgPattern.matcher(content);
				while (imgMatcher.find())
					curimg += 1;
				if (maximg < curimg)
					maximg = curimg;
				imgnum += curimg;
				
				date = df.parse((String) obj.get("time"));
				if (num > 0) {
					long diff = (preDate.getTime()-date.getTime())/1000;
					if (diff > 0) {
						if (mintimediff > diff)
							mintimediff = (int) diff;
						timediff += diff;
						preDate = df.parse((String) obj.get("time"));
					}
				} else {
					preDate = df.parse((String) obj.get("time"));
				}
				
				int share = Integer.parseInt((String) obj.get("share"));
				if (maxshare < share)
					maxshare = share;
				sharesum += share;
				
				int cmt = Integer.parseInt((String) obj.get("comment"));
				if (maxcmt < cmt)
					maxcmt = cmt;
				commentsum += cmt;
				
				if (Integer.parseInt((String)obj.get("access"))==0)
					publicnum += 1;
				
				int view = Integer.parseInt((String)obj.get("view"));
				if (maxview < view)
					maxview = view;
				viewsum += view;
				num += 1;
			}
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
		
		BlogInfo blogInfo = new BlogInfo();
		if (num > 0) {
			blogInfo.setNum(num);
			blogInfo.setAvgshare((double)sharesum/num);
			blogInfo.setMaxshare(maxshare);
			blogInfo.setAvgcmt((double)commentsum/num);
			blogInfo.setMaxcmt(maxcmt);
			blogInfo.setTimediff((double)timediff/(Math.max(num-1, 1)));
			blogInfo.setMintimediff(mintimediff);
			blogInfo.setTextlength((double)textlong/num);
			blogInfo.setAvgview((double)viewsum/num);
			blogInfo.setMaxview(maxview);
			blogInfo.setTitlelength((double)titlelong/num);
			blogInfo.setPublicratio((double)publicnum/num);
			blogInfo.setAvgimg((double)imgnum/num);
			blogInfo.setMaximg(maximg);
		} else {
			blogInfo.setNum(0);
			blogInfo.setMintimediff(Integer.MAX_VALUE);
		}
		return blogInfo;
	}
	
	public ShareInfo getShareInfo(String filename) {
	    File share = new File(filename+"/share.dat");
		System.out.println(share.getPath());
		if (!share.exists()) return new ShareInfo();
		int num = 0;
		int photosum = 0;
		int blogsum = 0;
		int videosum = 0;
		int othersum = 0;
		int commentsum = 0, maxcmt = 0;
		int timediff = 0, mintimediff = Integer.MAX_VALUE;
		JSONObject obj;
		try {
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream(share), "utf-8"));
			String content, temp, url;
			Date date, preDate = null;
			while ((temp = br.readLine())!=null) {
				obj = (JSONObject) parser.parse(temp);
				content = (String) obj.get("text");
				url = (String) obj.get("url");
				
				int cmt = Integer.parseInt(((String) obj.get("comment")));
				if (maxcmt < cmt)
					maxcmt = cmt;
				commentsum += cmt;
				
				if (url.contains("photo")) photosum += 1;
				else if (url.contains("blog")) blogsum += 1;
				else if (url.contains("youku")||url.contains("www.56.com")
						||url.contains("tudou")||url.contains("video")||
						url.contains("youtube")||url.contains("acfun")||
						url.contains("iqiyi")) videosum += 1;
				else othersum += 1;

				date = df.parse((String) obj.get("time"));
				if (num > 0) {
					long diff = (preDate.getTime()-date.getTime())/1000;
					if (diff > 0) {
						if (mintimediff > diff)
							mintimediff = (int) diff;
						timediff += diff;
						preDate = df.parse((String) obj.get("time"));
					}
				} else {
					preDate = df.parse((String) obj.get("time"));
				}
				num ++;
			}
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
		
		ShareInfo shareInfo = new ShareInfo();
		if (num > 0) {
			shareInfo.setNum(num);
			shareInfo.setAvgcmt((double)commentsum/num);
			shareInfo.setMaxcmt(maxcmt);
			shareInfo.setTimediff((double)timediff/(Math.max(num-1, 1)));
			shareInfo.setMintimediff(mintimediff);
			shareInfo.setPhotoratio((double)photosum/num);
			shareInfo.setBlogratio((double)blogsum/num);
			shareInfo.setVideoratio((double)videosum/num);
			shareInfo.setOtherratio((double)othersum/num);
		} else {
			shareInfo.setNum(0);
			shareInfo.setMintimediff(Integer.MAX_VALUE);
		}
		return shareInfo;
	}
	
	public ProfileInfo getProfileInfo(String filename) {
		File profile = new File(filename+"/profile.dat");
		System.out.println(profile.getPath());
		if (!profile.exists()) return new ProfileInfo();
		ProfileInfo profileInfo = new ProfileInfo();
		int hanzinum = 0;
		try {
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream(profile), "utf-8"));
			JSONObject obj = (JSONObject) parser.parse(br.readLine());
			String name = (String) obj.get("name");
			chiMatcher = chiPattern.matcher(name);
			while (chiMatcher.find()) hanzinum += 1;
			
			profileInfo.setHanziratio((double)hanzinum/name.length());
			profileInfo.setStar((int)(long) obj.get("star"));
			if ((JSONObject) obj.get("basicInformation") != null)
				profileInfo.setBasicinfo(((JSONObject) obj.get("basicInformation")).size());
			if ((JSONArray) obj.get("education") != null)
				profileInfo.setEducation(((JSONArray) obj.get("education")).size());
			if ((JSONArray) obj.get("work") != null)
				profileInfo.setWorknum(((JSONArray) obj.get("work")).size());
			if ((JSONArray) obj.get("like") != null)
				profileInfo.setLikenum(((JSONArray) obj.get("like")).size());
			profileInfo.setAppcount((int)(long) obj.get("appCount"));
			profileInfo.setVisitorcount((int)(long) obj.get("visitorCount"));
			profileInfo.setPagecount((int)(long) obj.get("pageCount"));
			profileInfo.setZhancount((int)(long) obj.get("zhanCount"));
			profileInfo.setMusiccount((int)(long) obj.get("musicCount"));
			profileInfo.setMoviecount((int)(long) obj.get("movieCount"));
			profileInfo.setFriendcount((int)(long) obj.get("friendCount"));
			//profileInfo.setFriendDensity();
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException | org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}
		return profileInfo;
	}
	
	public WordInfo getWordInfo(String filename) {
		wc = new WordCounter("emoticon_lexicon/positive.txt",
				"emoticon_lexicon/negative.txt");
		File status = new File(filename+"/status.dat");
		if (!status.exists()) return new WordInfo();
		int num = 0;
		int posWordSum = 0, negWordSum = 0;
		double posStatusNum = 0, negStatusNum = 0;
		JSONObject obj;
		try {
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream(status), "utf-8"));
			String temp;
			while((temp = br.readLine()) != null) {
				num++;
				obj = (JSONObject) parser.parse(temp);
				String text = (String) obj.get("text");
				if (text.contains("转自"))
					text = text.substring(0, text.indexOf("转自"));
				String splittedText = splitter.splitString(text);
				
				int pos = wc.countPosWord(splittedText);
				posWordSum += pos;
				int neg = wc.countNegWord(splittedText);
				negWordSum += neg;
				if (pos > neg) posStatusNum += 1;
				else if (pos < neg) negStatusNum += 1;
				else {
					posStatusNum += 0.5;
					negStatusNum += 0.5;
				}
				if (num == 100) break;
			}			
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		WordInfo wordInfo = new WordInfo();
		if (num == 0) {
			wordInfo.setPosWordNum(0);
			wordInfo.setNegWordNum(0);
			wordInfo.setPosStatusRatio(0.5);
			wordInfo.setNegStatusRatio(0.5);
		} else {
			wordInfo.setPosWordNum((double)posWordSum/num);
			wordInfo.setNegWordNum((double)negWordSum/num);
			wordInfo.setPosStatusRatio(posStatusNum/num);
			wordInfo.setNegStatusRatio(negStatusNum/num);
		}
		return wordInfo;
	}
	
	public photoInfo getPhotoInfo(String filename) {
	     File dir = new File(filename);
	     String[] dirFiles = dir.list();
	     int albumnum = 0;
	     int num = 0;
	     int facenum = 0;
	     double f1=0,f2= 0,f3 = 0;
	     double colors[] = new double[256];
	     if (dirFiles.length > 0) {
	     for (String albumName : dirFiles ) {
	       if (albumName.endsWith("dat")) continue;
	       albumnum++;
	       File dir1 = new File(filename + "/" + albumName);
	       String[] photoFiles = dir1.list();
	       for (String photoName : photoFiles) {
	         num ++;
	         String photo = filename+"/"+albumName+"/"+photoName;
	         JSONObject obj;
	         try {
	           Object[] result = null;
	           photo_feature a = new photo_feature();
	           result = a.face_detection(1, photo.toString());
	           facenum += Integer.parseInt(result[0].toString());
	           //System.out.println(facenum);
	           result = a.color_f(1, photo);
	           String data[]=result[0].toString().split(" ");
	           int k = 0;
	           for(String i : data) {
	             if (i.startsWith("0")) {
	               colors[k]+=Double.parseDouble(i);
	               k++;
	             }
	           }
	           
	           /*result = a.texture_f(1, photo);
               data=result[0].toString().split(" ");
               k = 0;
               for(String i : data) {
                 if (i.startsWith("0")) {
                   double x = Double.parseDouble(i);
                   if (k == 0) f1= f1 +x;
                   else if (k == 1) f2 =f2 +x;
                   else f3= f3 +x;
                   k++;
                 }
               }*/
               //System.out.println(photo);
	         }
	         catch (Exception e) {
	         }
	       }
	     }
	     }
	     photoInfo PhotoInfo = new photoInfo();
	        if (num > 0) {
	            for (int i=0; i < 256; ++i)
	              colors[i] /= num;
	            
	            PhotoInfo.setNum(num);
	            PhotoInfo.setAlbumNum(albumnum);
	            PhotoInfo.setAvgPhotoNum((double)num/(double)albumnum);
	            PhotoInfo.setFaceNum(facenum);
	            PhotoInfo.setAvgFaceNum((double)facenum/(double)num);
	            PhotoInfo.setAvgColorInfo(colors);
	            PhotoInfo.setF1(0);
	            PhotoInfo.setF2(0);
	            PhotoInfo.setF3(0);
                
	            //PhotoInfo.setF1((double)f1/(double)num);
	            //PhotoInfo.setF2((double)f2/(double)num);
	            //PhotoInfo.setF3((double)f3/(double)num);
	        } else {
	            PhotoInfo.setNum(0);
	        }
	        return PhotoInfo;
	    }

}
