package cn.edu.bupt.discovery;

/**
 * @Description: DeviceDiscovery
 * @Author: czx
 * @CreateDate: 2019-06-06 15:37
 * @Version: 1.0
 */

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import javax.xml.soap.*;

import cn.edu.bupt.soap.OnvifDevice;
import cn.edu.bupt.util.URLClassifier;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Node;
import org.w3c.dom.*;

@Slf4j
public class DeviceDiscovery {

    public static final String WS_DISCOVERY_SOAP_VERSION = "SOAP 1.2 Protocol";
    public static final String WS_DISCOVERY_CONTENT_TYPE = "application/soap+xml";
    public static final int WS_DISCOVERY_TIMEOUT = 6000;
    public static final int WS_DISCOVERY_PORT = 3702;
    public static final String WS_DISCOVERY_ADDRESS_IPv4 = "239.255.255.250";
    public static String WS_DISCOVERY_PROBE_MESSAGE = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:tns=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\"><soap:Header><wsa:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action><wsa:MessageID>urn:uuid:c032cfdd-c3ca-49dc-820e-ee6696ad63e2</wsa:MessageID><wsa:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To></soap:Header><soap:Body><tns:Probe/></soap:Body></soap:Envelope>";
    private static final Random random = new SecureRandom();

    public static void main(String[] args) {
        for (URL url : discoverWsDevicesAsUrls()) {
            System.out.println("Device discovered: " + url.toString());
        }
    }

    /**
     * 找到所有的设备，设备的地址为Ipv4且没有使用代理，不使用代理确保了同一个摄像头只会出现一次
     * @return 找到的摄像头
     */
    public static Set<String> discoverIpv4DevicesWithoutProxy(){
        Set<URL> urls = discoverWsDevicesAsUrls();
        Set<String> ips = new HashSet<>();
        for(URL url : urls){
            try {
                if (URLClassifier.isIPv4Address(url.getHost())) {
                    OnvifDevice device = new OnvifDevice(url.getHost(), "", "", false);
                    if(!device.isProxy()){
                        ips.add(url.getHost());
                    }
                }
            }catch (Exception e){
                log.warn("discovery exception.");
                e.printStackTrace();
            }
        }
        return ips;
    }


    /**
     * 获取所有ONVIF设备
     * @return 设备URL
     */
    public static Set<URL> discoverWsDevicesAsUrls() {
        return discoverWsDevicesAsUrls("", "");
    }

    /**
     * 以获取URL的方式获得所有的ONVIF设备
     * @param regexpProtocol 找到的设备所使用的协议通过这个正则表达式来匹配
     * @param regexpPath 找到的设备所使用的path通过这个正则表达式来匹配
     * @return 符合要求的URL
     */
    public static Set<URL> discoverWsDevicesAsUrls(String regexpProtocol, String regexpPath) {
        final Set<URL> urls = new HashSet<>();
        try{
            for (String key : discoverWsDevices()) {
                final URL url = new URL(key);
                if ((regexpProtocol.length() > 0 && !url.getProtocol().matches(regexpProtocol))
                    ||(regexpPath.length() > 0 && !url.getPath().matches(regexpPath))) {
                    continue;
                }
                urls.add(url);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return urls;
    }

    /**
     * 通过广播数据报文的方式，获取ONVIF设备
     * @return ONVIF的地址
     */
    public static Set<String> discoverWsDevices() {
        Set<String> addresses = new HashSet<>();
        List<InetAddress> addressList = findInterfaceAddress();
        try {
            for (InetAddress address : addressList) {
                String uuid = UUID.randomUUID().toString();
                String probe = WS_DISCOVERY_PROBE_MESSAGE.replaceAll("<wsa:MessageID>urn:uuid:.*</wsa:MessageID>", "<wsa:MessageID>urn:uuid:" + uuid + "</wsa:MessageID>");
                int port = random.nextInt(20000) + 40000;

                DatagramSocket server = new DatagramSocket(port, address);
                server.send(new DatagramPacket(probe.getBytes(), probe.length(), InetAddress.getByName(WS_DISCOVERY_ADDRESS_IPv4), WS_DISCOVERY_PORT));
                long startTime = System.currentTimeMillis();
                DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
                server.setSoTimeout(WS_DISCOVERY_TIMEOUT/2);
                // TODO：将parse过程移到获取了所有回应（即for循环之外）之后。
                while (System.currentTimeMillis() - startTime < WS_DISCOVERY_TIMEOUT) {
                    try {
                        server.receive(packet);
                    }catch (SocketTimeoutException ignored){

                    }
                    List<String> urls = parseSoapResponseForUrls(Arrays.copyOf(packet.getData(), packet.getLength()));
                    addresses.addAll(urls);
                }
                server.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return addresses;
    }

    private static List<Node> getNodeMatching(Node body, String regexp) {
        final List<Node> nodes = new ArrayList<>();
        if (body.getNodeName().matches(regexp)) nodes.add(body);
        if (body.getChildNodes().getLength() == 0) return nodes;
        NodeList returnList = body.getChildNodes();
        for (int k = 0; k < returnList.getLength(); k++) {
            final Node node = returnList.item(k);
            nodes.addAll(getNodeMatching(node, regexp));
        }
        return nodes;
    }


    private static List<String> parseSoapResponseForUrls(byte[] data) throws SOAPException, IOException {
        List<String> urls = new ArrayList<>();
        MessageFactory factory = MessageFactory.newInstance(WS_DISCOVERY_SOAP_VERSION);
        MimeHeaders headers = new MimeHeaders();
        headers.addHeader("Content-type", WS_DISCOVERY_CONTENT_TYPE);
        SOAPMessage message = factory.createMessage(headers, new ByteArrayInputStream(data));
        SOAPBody body = message.getSOAPBody();
        for (Node node : getNodeMatching(body, ".*:XAddrs")) {
            if (node.getTextContent().length() > 0) {
                urls.addAll(Arrays.asList(node.getTextContent().split(" ")));
            }
        }
        return urls;
    }

    private static List<InetAddress> findInterfaceAddress(){
        List<InetAddress> addressList = new ArrayList<>();
        try {
            final Enumeration<NetworkInterface> interfaces =
                    NetworkInterface.getNetworkInterfaces();
            if(interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface anInterface = interfaces.nextElement();
                    if(!anInterface.isLoopback() ) {
                        final List<InterfaceAddress> interfaceAddresses =
                                anInterface.getInterfaceAddresses();
                        for (InterfaceAddress address : interfaceAddresses) {
                            if(address.getAddress() instanceof Inet4Address) {
                                addressList.add(address.getAddress());
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return addressList;
    }
}

