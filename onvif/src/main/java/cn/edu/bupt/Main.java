package cn.edu.bupt;

import cn.edu.bupt.discovery.DeviceDiscovery;
import cn.edu.bupt.soap.OnvifDevice;
import cn.edu.bupt.soap.devices.MediaDevices;
import cn.edu.bupt.util.URLClassifier;
import org.onvif.ver10.schema.Profile;

import javax.xml.soap.SOAPException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.URL;
import java.util.List;
import java.util.Set;

public class Main {

	private static final String INFO = "Commands:\n  \n  url: Get snapshort URL.\n  info: Get information about each valid command.\n  profiles: Get all profiles.\n  exit: Exit this application.\n  rtsp: get the rtsp path.";

	public static void main(String args[]) throws Exception{

		Set<String> ips = DeviceDiscovery.discoverIpv4DevicesWithoutProxy();
		for(String ip : ips){
			System.out.println(ip);
		}
		System.out.println("--------");

		InputStreamReader inputStream = new InputStreamReader(System.in);
		BufferedReader keyboardInput = new BufferedReader(inputStream);
		String input, cameraAddress, user, password;

		cameraAddress = "192.168.1.107";
		user = "czx";
		password = "zx19950529";
//
//		try {
//			System.out.println("Please enter camera IP (with port if not 80):");
//			cameraAddress = keyboardInput.readLine();
//			System.out.println("Please enter camera username:");
//			user = keyboardInput.readLine();
//			System.out.println("Please enter camera password:");
//			password = keyboardInput.readLine();
//		}
//		catch (IOException e1) {
//			e1.printStackTrace();
//			return;
//		}

		System.out.println("Connect to camera, please wait ...");
		OnvifDevice cam;
		MediaDevices mediaDevices;
		try {
			cam = new OnvifDevice(cameraAddress, user, password,false);
			mediaDevices = new MediaDevices(cam);

		}
		catch (ConnectException | SOAPException e1) {
			System.err.println("No connection to camera, please try again.");
			return;
		}
		System.out.println("Connection to camera successful!");

		while (true) {
			try {
				System.out.println();
				System.out.println("Enter a command (type \"info\" to get commands):");
				input = keyboardInput.readLine();

				switch (input) {
				case "url": {
					List<Profile> profiles = cam.getDevices().getProfiles();
					for (Profile p : profiles) {
						try {
							System.out.println("URL from Profile \'" + p.getName() + "\': " + cam.getMedia().getSnapshotUri(p.getToken()));
						}
						catch (SOAPException e) {
							System.err.println("Cannot grap snapshot URL, got Exception "+e.getMessage());
						}
					}
					break;
				}
				case "profiles":
					List<Profile> profiles = cam.getDevices().getProfiles();
					System.out.println("Number of profiles: " + profiles.size());
					for (Profile p : profiles) {
						System.out.println("  Profile "+p.getName()+" token is: "+p.getToken());
					}
					break;
				case "info":
					System.out.println(INFO);
					break;
				case "quit":
				case "exit":
				case "end":
					return;
				case "rtsp":
					try {
						System.out.println(mediaDevices.getRTSPStreamUri("Profile_1"));
					}catch (Exception e){

					}
					break;
				case "name":
					System.out.println(cam.getDevices().getDeviceInformation());
					break;
				case "capacity":
					System.out.println(cam.getDevices().getCapabilities());
					break;
				default:
					System.out.println("Unknown command!");
					System.out.println();
					System.out.println(INFO);
					break;
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}