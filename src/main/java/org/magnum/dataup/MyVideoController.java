/*
 * 
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package org.magnum.dataup;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.magnum.dataup.VideoFileManager;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.sun.xml.internal.ws.client.ResponseContextReceiver;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Streaming;
import retrofit.mime.TypedFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.*;

@Controller
public class MyVideoController {

    private static final AtomicLong currentId = new AtomicLong(0L);
	
	private Map<Long,Video> videos = new HashMap<Long, Video>();

	// getVideoList()
	@RequestMapping(value="/video", method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return videos.values();
	}

	// addVideos ()
	@RequestMapping(value="/video", method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		checkAndSetId(v);		
		v.setDataUrl(getMyDataUrl(v.getId()));		
		videos.put(v.getId(),v);
		return v;
	}	

	
	// setVideoData()
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(@PathVariable("id") long id, @RequestParam("data") MultipartFile videoData, HttpServletResponse response) {
			
		if(id == 0 || videoData == null) { response.setStatus(404); return (new VideoStatus(VideoState.READY)); }
		
		Video v = videos.get(id);
		
		if(v == null) { response.setStatus(404); return (new VideoStatus(VideoState.READY)); }
		
			
			try {
				VideoFileManager vfm = VideoFileManager.get();
				vfm.saveVideoData(v, videoData.getInputStream());
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		return (new VideoStatus(VideoState.READY));
	}
	
	// getData()
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.GET)
    @Streaming Response getData(@PathVariable("id") long id, HttpServletResponse response)  {
		Video v = videos.get(id);
		if(v == null) { response.setStatus(404); return null; }
		try{
			VideoFileManager vfm = VideoFileManager.get();
			vfm.copyVideoData(v,  response.getOutputStream());
		}
		catch(Exception e) { e.printStackTrace();}
		
		return null;		
	}
	
    private String getMyDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }
    
    private String getUrlBaseForLocalServer() {
		   HttpServletRequest request = 
		       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		   String base = 
		      "http://"+request.getServerName() 
		      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
		   return base;
		}
	
    private void checkAndSetId(Video entity) {
		if(entity.getId() == 0){
			entity.setId(currentId.incrementAndGet());
		}
	}


}
