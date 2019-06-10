//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5-2 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// �nderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2014.02.04 um 12:22:03 PM CET 
//

package org.onvif.ver10.device.wsdl;

import org.onvif.ver10.schema.Dot11Capabilities;

import javax.xml.bind.annotation.*;

/**
 * <p>
 * Java-Klasse f�r anonymous complex type.
 * 
 * <p>
 * Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * <complexType>
 *   <complexContent>
 *     <restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       <sequence>
 *         <element name="Capabilities" type="{http://www.onvif.org/ver10/schema}Dot11Capabilities"/>
 *       </sequence>
 *     </restriction>
 *   </complexContent>
 * </complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "capabilities" })
@XmlRootElement(name = "GetDot11CapabilitiesResponse")
public class GetDot11CapabilitiesResponse {

	@XmlElement(name = "Capabilities", required = true)
	protected Dot11Capabilities capabilities;

	/**
	 * Ruft den Wert der capabilities-Eigenschaft ab.
	 * 
	 * @return possible object is {@link Dot11Capabilities }
	 * 
	 */
	public Dot11Capabilities getCapabilities() {
		return capabilities;
	}

	/**
	 * Legt den Wert der capabilities-Eigenschaft fest.
	 * 
	 * @param value
	 *            allowed object is {@link Dot11Capabilities }
	 * 
	 */
	public void setCapabilities(Dot11Capabilities value) {
		this.capabilities = value;
	}

}
