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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.magnum.dataup.VideoFileManager;
import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.magnum.dataup.model.VideoStatus.VideoState;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.sun.xml.internal.ws.client.ResponseContextReceiver;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Streaming;
import retrofit.mime.TypedFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.*;

@SuppressWarnings("unused")
@Controller
public class CopyOfMyVideoController {

	private List<Video> videos = new CopyOnWriteArrayList<Video>();
	//private Map<Long, Video> videos = new HashMap<Long,Video>();
	private long no_ids = 0;
	
	// Shiv's Get method
	@RequestMapping(value="/video", method=RequestMethod.GET)
	public @ResponseBody List<Video> getVideoList(){
		System.out.println("getVideoList()");
		return videos;
	}
	
	@RequestMapping(value="/video", method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		System.out.println("addVideo()");

		v.setId(++no_ids);
		
		v.setDataUrl(getMyDataUrl(no_ids));
		
		videos.add(v);
		System.out.println(v.getDataUrl());
		System.out.println(v.getId());
		return v;
	}	

	@RequestMapping(value="/video/{id}/data", method=RequestMethod.POST)
	public VideoStatus setVideoData(Long id, TypedFile videoData, HttpServletResponse response) throws IOException {
		
		System.out.println("setVideoData() id - " + id);
		if(id < 1 || id > no_ids || videoData.length() < 1) { response.sendError(404); return (new VideoStatus(VideoState.READY)); }
		
		Video v = videos.get((int) (id-1));
		
		if(v == null) { response.sendError(404); return (new VideoStatus(VideoState.READY)); }
		
			VideoFileManager vfm = VideoFileManager.get();
			vfm.saveVideoData(v, videoData.in());
		
		return (new VideoStatus(VideoState.READY));
	}
	
	@RequestMapping(value="/video/{id}/data", method=RequestMethod.GET)
    @Streaming Response getData(Long id, HttpServletResponse response) throws IOException {
		System.out.println("getData() id - " + id);
		if(id < 1 || id > no_ids) { response.sendError(404); return null; }
		Video v = videos.get((int) (id-1));
		if(v == null) response.sendError(404);

		VideoFileManager vfm = VideoFileManager.get();
			
		vfm.copyVideoData(v,  response.getOutputStream());

		return null;		
	}
	
    private String getMyDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        //System.out.println(url);
        return url;
    }
    
    private String getUrlBaseForLocalServer() {
		   HttpServletRequest request = 
		       ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		   String base = 
		      "http://"+request.getServerName() 
		      + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
		   //System.out.println(base);
		   return base;
		}


}
