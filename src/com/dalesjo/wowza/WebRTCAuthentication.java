package com.dalesjo.wowza;

import java.util.Date;
import java.util.Map;

import com.wowza.wms.application.*;
import com.wowza.wms.module.*;
import com.wowza.wms.rtp.model.*;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class WebRTCAuthentication extends ModuleBase {

	public void onAppStart(IApplicationInstance appInstance) {
		String fullname = appInstance.getApplication().getName() + "/" + appInstance.getName();
		info("onAppStart: " + fullname);
	}

	public void onRTPSessionCreate(RTPSession rtpSession) {
		info("onRTPSessionCreate: " + rtpSession.getSessionId());
		
		try {
			
			if(!rtpSession.isWebRTC())
			{
				return;
			}
			
			
			
			var key = "token";
			var secret = "secret";
			var streamName = rtpSession.getRTSPStream().getStreamName();
			
			info("streamname:"+ streamName);
			
			Map<String, Object> userData = (Map<String, Object>)rtpSession.getWebRTCSession().getCommandRequest().getJSONEntries().get("userData");
			String token = (String)userData.get(key);
			
			var claims = Jwts.parser().setSigningKey(secret.getBytes()).parseClaimsJws(token).getBody();
			

			
		} catch (Exception e) {
			info("Rejecting connection, exception:" + e.getMessage());
			rtpSession.rejectSession();
		}
	}

	public void onRTPSessionDestroy(RTPSession rtpSession) {
		info("onRTPSessionDestroy: " + rtpSession.getSessionId());
	}
	
	  private void info(String message)
	  {
		  getLogger().info("WebRTCAuthentication: " + message);
	  }
}
