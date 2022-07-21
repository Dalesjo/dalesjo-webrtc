package com.dalesjo.wowza;

import java.util.Date;
import java.util.Map;

import com.wowza.wms.application.*;
import com.wowza.wms.module.*;
import com.wowza.wms.rtp.model.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class WebRTCAuthentication extends ModuleBase {

	public void onAppStart(IApplicationInstance appInstance) {

		String fullname = appInstance.getApplication().getName() + "/" + appInstance.getName();

		info("onAppStart: " + fullname);

		var properties = appInstance.getProperties();

		parameter = getValueOrDefault((String) properties.get(parameterProperty), "token");
		secret = (String) properties.get(secretProperty);
		subjectOnly = (Boolean) properties.get(subjectProperty);
		
		if (secret == null) {
			warning("No secret have been set application will not validate any tokens.");
		}
	}

	private static final String secretProperty = "dalesjo-webrtc-secret";
	private static final String parameterProperty = "dalesjo-webrtc-parameter";
	private static final String subjectProperty = "dalesjo-webrtc-subject-only";

	private String parameter = null;
	private Boolean subjectOnly = false;
	private String secret = null;

	public void onRTPSessionCreate(RTPSession rtpSession) {
		info("onRTPSessionCreate: " + rtpSession.getSessionId());

		try {

			if (!rtpSession.isWebRTC()) {
				return;
			}

			if (secret == null) {
				return;
			}

			var userData = (Map<String, Object>) rtpSession.getWebRTCSession().getCommandRequest().getJSONEntries().get("userData");
			var token = (String) userData.get(parameter);
			var claims = Jwts.parser().setSigningKey(secret.getBytes()).parseClaimsJws(token).getBody();
			
			//checkStreamName(rtpSession,claims);
			
		} catch (Exception e) {
			info("Rejecting connection, exception:" + e.getMessage());
			rtpSession.rejectSession();
		}
	}

	private void checkStreamName(RTPSession rtpSession,Claims claims) throws Exception
	{
		var streamName = rtpSession.getRTSPStream().getStreamName();
		var subject = claims.getSubject();
		
		if(streamName != subject)
		{
			throw new Exception("Subject "+ subject + " is trying to stream "+ streamName +" which is forbidden");
		}
		
	}
	
	public void onRTPSessionDestroy(RTPSession rtpSession) {
		info("onRTPSessionDestroy: " + rtpSession.getSessionId());
	}

	private void info(String message) {
		getLogger().info("WebRTCAuthentication: " + message);
	}

	private void warning(String message) {
		getLogger().warn("WebRTCAuthentication: " + message);
	}

	public static <T> T getValueOrDefault(T value, T defaultValue) {
		return value == null ? defaultValue : value;
	}
}
