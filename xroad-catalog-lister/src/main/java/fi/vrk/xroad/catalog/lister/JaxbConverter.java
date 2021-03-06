/**
 * The MIT License
 * Copyright (c) 2016, Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vrk.xroad.catalog.lister;

import fi.vrk.xroad.catalog.persistence.entity.Service;
import fi.vrk.xroad.catalog.persistence.entity.Subsystem;
import fi.vrk.xroad.catalog.persistence.entity.Wsdl;
import fi.vrk.xroad.xroad_catalog_lister.Member;
import fi.vrk.xroad.xroad_catalog_lister.ServiceList;
import fi.vrk.xroad.xroad_catalog_lister.SubsystemList;
import fi.vrk.xroad.xroad_catalog_lister.WSDL;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Utility for converting JAXB classes & entities
 */
@Component
public class JaxbConverter {

    /**
     * Convert entities to XML objects
     * @param members Iterable of Member entities
     * @param onlyActiveChildren if true, convert only active subsystems
     * @return Collection of Members (JAXB generated)
     */
    public Collection<Member> convertMembers(Iterable<fi.vrk.xroad.catalog.persistence.entity.Member> members,
                                             boolean onlyActiveChildren)  {
        List<Member> converted = new ArrayList<>();
        for (fi.vrk.xroad.catalog.persistence.entity.Member member: members) {
            Member cm = new Member();
            cm.setChanged(toXmlGregorianCalendar(member.getStatusInfo().getChanged()));
            cm.setCreated(toXmlGregorianCalendar(member.getStatusInfo().getCreated()));
            cm.setFetched(toXmlGregorianCalendar(member.getStatusInfo().getFetched()));
            cm.setRemoved(toXmlGregorianCalendar(member.getStatusInfo().getRemoved()));
            cm.setMemberCode(member.getMemberCode());
            cm.setMemberClass(member.getMemberClass());
            cm.setName(member.getName());
            cm.setXRoadInstance(member.getXRoadInstance());
            cm.setSubsystems(new SubsystemList());
            Iterable<Subsystem> subsystems;
            if (onlyActiveChildren) {
                subsystems = member.getActiveSubsystems();
            } else {
                subsystems = member.getAllSubsystems();
            }
            cm.getSubsystems().getSubsystem().addAll(convertSubsystems(subsystems, onlyActiveChildren));
            converted.add(cm);
        }
        return converted;
    }

    /**
     * Convert entities to XML objects
     * @param subsystems Iterable of Subsystem entities
     * @param onlyActiveChildren if true, convert only active subsystems
     * @return collection of XML objects
     */
    public Collection<fi.vrk.xroad.xroad_catalog_lister.Subsystem> convertSubsystems(Iterable<Subsystem> subsystems,
                                                                                     boolean onlyActiveChildren) {
        List<fi.vrk.xroad.xroad_catalog_lister.Subsystem> converted = new ArrayList<>();
        for (Subsystem subsystem: subsystems) {
            fi.vrk.xroad.xroad_catalog_lister.Subsystem cs = new fi.vrk.xroad.xroad_catalog_lister.Subsystem();
            cs.setChanged(toXmlGregorianCalendar(subsystem.getStatusInfo().getChanged()));
            cs.setCreated(toXmlGregorianCalendar(subsystem.getStatusInfo().getCreated()));
            cs.setFetched(toXmlGregorianCalendar(subsystem.getStatusInfo().getFetched()));
            cs.setRemoved(toXmlGregorianCalendar(subsystem.getStatusInfo().getRemoved()));
            cs.setSubsystemCode(subsystem.getSubsystemCode());
            cs.setServices(new ServiceList());
            Iterable<Service> services;
            if (onlyActiveChildren) {
                services = subsystem.getActiveServices();
            } else {
                services = subsystem.getAllServices();
            }
            cs.getServices().getService().addAll(convertServices(services, onlyActiveChildren));
            converted.add(cs);
        }
        return converted;
    }

    /**
     * Convert entities to XML objects
     * @param services Iterable of Service entities
     * @param onlyActiveChildren if true, convert only active subsystems
     * @return collection of XML objects
     */
    public Collection<fi.vrk.xroad.xroad_catalog_lister.Service> convertServices(Iterable<Service> services,
                                                                                 boolean onlyActiveChildren) {
        List<fi.vrk.xroad.xroad_catalog_lister.Service> converted = new ArrayList<>();
        for (Service service: services) {
            fi.vrk.xroad.xroad_catalog_lister.Service cs = new fi.vrk.xroad.xroad_catalog_lister.Service();
            cs.setChanged(toXmlGregorianCalendar(service.getStatusInfo().getChanged()));
            cs.setCreated(toXmlGregorianCalendar(service.getStatusInfo().getCreated()));
            cs.setFetched(toXmlGregorianCalendar(service.getStatusInfo().getFetched()));
            cs.setRemoved(toXmlGregorianCalendar(service.getStatusInfo().getRemoved()));
            cs.setServiceCode(service.getServiceCode());
            cs.setServiceVersion(service.getServiceVersion());
            Wsdl wsdl = null;
            if (onlyActiveChildren) {
                if (service.getWsdl() != null && !service.getWsdl().getStatusInfo().isRemoved()) {
                    wsdl = service.getWsdl();
                }
            } else {
                wsdl = service.getWsdl();
            }
            if (wsdl != null) {
                cs.setWsdl(convertWsdl(service.getWsdl()));
            }
            converted.add(cs);
        }
        return converted;
    }

    private WSDL convertWsdl(Wsdl wsdl) {
        WSDL cw = new WSDL();
        cw.setChanged(toXmlGregorianCalendar(wsdl.getStatusInfo().getChanged()));
        cw.setCreated(toXmlGregorianCalendar(wsdl.getStatusInfo().getCreated()));
        cw.setFetched(toXmlGregorianCalendar(wsdl.getStatusInfo().getFetched()));
        cw.setRemoved(toXmlGregorianCalendar(wsdl.getStatusInfo().getRemoved()));
        cw.setExternalId(wsdl.getExternalId());
        return cw;
    }

    public LocalDateTime toLocalDateTime(XMLGregorianCalendar calendar) {
        return calendar.toGregorianCalendar().toZonedDateTime().toLocalDateTime();
    }

    protected XMLGregorianCalendar toXmlGregorianCalendar(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        } else {
            GregorianCalendar cal = GregorianCalendar.from(localDateTime.atZone(ZoneId.systemDefault()));
            XMLGregorianCalendar xc = null;
            try {
                xc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            } catch (DatatypeConfigurationException e) {
                throw new CatalogListerRuntimeException("Cannot instantiate DatatypeFactory", e);
            }
            return xc;
        }
    }


}
