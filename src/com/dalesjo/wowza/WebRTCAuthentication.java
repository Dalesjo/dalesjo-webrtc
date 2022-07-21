package com.dalesjo.wowza;

import java.util.Date;
import java.util.Map;

import com.wowza.wms.application.*;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.module.*;
import com.wowza.wms.rtp.model.*;
import com.wowza.wms.webrtc.http.HTTPWebRTCExchangeSessionInfo;
import com.wowza.wms.webrtc.model.WebRTCSession;
import com.wowza.wms.websocket.model.IWebSocketSession;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

public class WebRTCAuthentication extends HTTPWebRTCExchangeSessionInfo
{

	private static final Class<WebRTCAuthentication> CLASS = WebRTCAuthentication.class;
	private static final String secretProperty = "dalesjo-webrtc-secret";
	private static final String parameterProperty = "dalesjo-webrtc-parameter";
	private static final String subjectProperty = "dalesjo-webrtc-subject-only";

	private String parameter = null;
	private Boolean subjectOnly = false;
	private String secret = null;

	@Override
	protected void websocketSessionCreate(IWebSocketSession webSocketSession)
	{
		super.websocketSessionCreate(webSocketSession);
	}

	@Override
	protected void websocketSessionDestroy(IWebSocketSession webSocketSession)
	{
		super.websocketSessionDestroy(webSocketSession);
	}

	@Override
	protected void authenticateRequest(CommandContext commandContext, CommandControl commandControl)
	{
		super.authenticateRequest(commandContext, commandControl);

		getProperties(commandContext);
		if (secret == null)
		{
			return;
		}

		validateToken(commandContext, commandControl);
	}

	private void allowPlayer(CommandControl commandControl)
	{
		commandControl.canPlay = true;
		commandControl.canPublish = false;
		commandControl.canQuery = true;
	}
	
	private void allowPublisher(CommandControl commandControl)
	{
		commandControl.canPlay = true;
		commandControl.canPublish = true;
		commandControl.canQuery = true;
	}

	private void denyConnection(CommandControl commandControl)
	{
		commandControl.canPlay = false;
		commandControl.canPublish = false;
		commandControl.canQuery = false;
	}

	private Claims getClaims(String token)
	{
		return Jwts.parser().setSigningKey(secret.getBytes()).parseClaimsJws(token).getBody();
	}

	private void getProperties(CommandContext commandContext)
	{
		try
		{
			var instance = commandContext.commandRequest.getApplicationInstance(commandContext.vhost);
			var name = instance.getApplication().getName();
			var properties = instance.getProperties();

			parameter = getValueOrDefault((String) properties.get(parameterProperty), "token");
			subjectOnly = getValueOrDefault((Boolean) properties.get(subjectProperty), false);
			secret = (String) properties.get(secretProperty);

			if (secret == null)
			{
				warning("No secret have been set for application " + name + ", we will not validate any tokens.");
			}
		} catch (Exception e)
		{
			warning("Failed to getProperties: " + e.getMessage());
		}
	}

	private String getStream(CommandContext commandContext)
	{
		return commandContext.commandRequest.getStreamName();
	}
	
	private String getToken(CommandContext commandContext)
	{
		var json = commandContext.commandRequest.getJSONEntries();
		var userData = (Map<String, Object>) json.get("userData");
		var token = (String) userData.get(parameter);
		return token;
	}

	public static <T> T getValueOrDefault(T value, T defaultValue)
	{
		return value == null ? defaultValue : value;
	}
	
	private void validateToken(CommandContext commandContext, CommandControl commandControl)
	{
		try
		{
			var stream = getStream(commandContext);
			var token = getToken(commandContext);
			var claims = getClaims(token);

			if (!subjectOnly)
			{
				trace("subjectOnly not set, allowPublisher");
				allowPublisher(commandControl);
				return;
			}

			var subject = claims.getSubject();
			trace("compare subject:'" + subject + "' with stream: '" + stream + "'");
			if (stream.equals(subject))
			{
				trace("stream name equals to token subject, allowPublisher");
				allowPublisher(commandControl);
				return;
			}

			allowPlayer(commandControl);
		}
		catch (Exception e)
		{
			trace("Exception found, denyConnection: " + e.getMessage());
			denyConnection(commandControl);
		}
	}

	private void warning(String message)
	{
		WMSLoggerFactory.getLogger(CLASS).warn("WebRTCAuthentication: " + message);
	}

	private void trace(String message)
	{
		WMSLoggerFactory.getLogger(CLASS).info("WebRTCAuthentication: " + message);
	}
}
