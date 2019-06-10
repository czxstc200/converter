package cn.edu.bupt.soap.devices;

import cn.edu.bupt.soap.OnvifDevice;
import cn.edu.bupt.soap.SOAP;
import org.onvif.ver10.schema.AbsoluteFocus;
import org.onvif.ver10.schema.FocusMove;
import org.onvif.ver10.schema.ImagingOptions20;
import org.onvif.ver10.schema.ImagingSettings20;
import org.onvif.ver20.imaging.wsdl.*;

import javax.xml.soap.SOAPException;
import java.net.ConnectException;

public class ImagingDevices {
	@SuppressWarnings("unused")
	private OnvifDevice onvifDevice;
	private SOAP soap;

	public ImagingDevices(OnvifDevice onvifDevice) {
		this.onvifDevice = onvifDevice;
		this.soap = onvifDevice.getSoap();
	}

	public ImagingOptions20 getOptions(String videoSourceToken) {
		if (videoSourceToken == null) {
			return null;
		}

		GetOptions request = new GetOptions();
		GetOptionsResponse response = new GetOptionsResponse();

		request.setVideoSourceToken(videoSourceToken);

		try {
			response = (GetOptionsResponse) soap.createSOAPImagingRequest(request, response, false);
		}
		catch (SOAPException | ConnectException e) {
			e.printStackTrace();
			return null;
		}

		if (response == null) {
			return null;
		}

		return response.getImagingOptions();
	}

	public boolean moveFocus(String videoSourceToken, float absoluteFocusValue) {
		if (videoSourceToken == null) {
			return false;
		}

		Move request = new Move();
		MoveResponse response = new MoveResponse();

		AbsoluteFocus absoluteFocus = new AbsoluteFocus();
		absoluteFocus.setPosition(absoluteFocusValue);

		FocusMove focusMove = new FocusMove();
		focusMove.setAbsolute(absoluteFocus);

		request.setVideoSourceToken(videoSourceToken);
		request.setFocus(focusMove);

		try {
			response = (MoveResponse) soap.createSOAPImagingRequest(request, response, true);
		}
		catch (SOAPException | ConnectException e) {
			e.printStackTrace();
			return false;
		}

		if (response == null) {
			return false;
		}

		return true;
	}

	public ImagingSettings20 getImagingSettings(String videoSourceToken) {
		if (videoSourceToken == null) {
			return null;
		}

		GetImagingSettings request = new GetImagingSettings();
		GetImagingSettingsResponse response = new GetImagingSettingsResponse();

		request.setVideoSourceToken(videoSourceToken);

		try {
			response = (GetImagingSettingsResponse) soap.createSOAPImagingRequest(request, response, true);
		}
		catch (SOAPException | ConnectException e) {
			e.printStackTrace();
			return null;
		}

		if (response == null) {
			return null;
		}

		return response.getImagingSettings();
	}

	public boolean setImagingSettings(String videoSourceToken, ImagingSettings20 imagingSettings) {
		if (videoSourceToken == null) {
			return false;
		}

		SetImagingSettings request = new SetImagingSettings();
		SetImagingSettingsResponse response = new SetImagingSettingsResponse();

		request.setVideoSourceToken(videoSourceToken);
		request.setImagingSettings(imagingSettings);

		try {
			response = (SetImagingSettingsResponse) soap.createSOAPImagingRequest(request, response, true);
		}
		catch (SOAPException | ConnectException e) {
			e.printStackTrace();
			return false;
		}

		if (response == null) {
			return false;
		}

		return true;
	}
}
