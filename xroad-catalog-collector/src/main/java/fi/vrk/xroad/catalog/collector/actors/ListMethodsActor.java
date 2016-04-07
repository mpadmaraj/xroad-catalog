package fi.vrk.xroad.catalog.collector.actors;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import fi.vrk.xroad.catalog.collector.extension.SpringExtension;
import fi.vrk.xroad.catalog.collector.util.ClientTypeUtil;
import fi.vrk.xroad.catalog.collector.util.XRoadClient;
import fi.vrk.xroad.catalog.collector.wsimport.ClientType;
import fi.vrk.xroad.catalog.collector.wsimport.XRoadClientIdentifierType;
import fi.vrk.xroad.catalog.collector.wsimport.XRoadObjectType;
import fi.vrk.xroad.catalog.collector.wsimport.XRoadServiceIdentifierType;
import fi.vrk.xroad.catalog.persistence.CatalogService;
import fi.vrk.xroad.catalog.persistence.entity.Member;
import fi.vrk.xroad.catalog.persistence.entity.Service;
import fi.vrk.xroad.catalog.persistence.entity.Subsystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Actor which fetches all clients, and delegates listing
 * their methods to ListMethodsActors
 */
@Component
@Scope("prototype")
@Slf4j
public class ListMethodsActor extends XRoadCatalogActor {

    private static AtomicInteger COUNTER = new AtomicInteger(0);
    // to test fault handling
    private static boolean FORCE_FAILURES = false;

    @Value("${xroad-catalog.xroad-instance}")
    private String xroadInstance;

    @Value("${xroad-catalog.member-code}")
    private String memberCode;

    @Value("${xroad-catalog.member-class}")
    private String memberClass;

    @Value("${xroad-catalog.subsystem-code}")
    private String subsystemCode;

    @Value("${xroad-catalog.webservices-endpoint}")
    private String webservicesEndpoint;

    @Autowired
    private SpringExtension springExtension;

    @Autowired
    protected CatalogService catalogService;

    // supervisor-created pool of list methods actors
    private ActorRef fetchWsdlPoolRef;

    @Override
    public void preStart() throws Exception {
        log.info("preStart {}", this.hashCode());
        fetchWsdlPoolRef = new RelativeActorRefUtil(getContext())
                .resolvePoolRef(Supervisor.FETCH_WSDL_ACTOR_ROUTER);
        super.preStart();
    }

    @Override
    protected boolean handleMessage(Object message) throws Exception {

        if (message instanceof ClientType) {

            log.info("{} onReceive {}", COUNTER.addAndGet(1), this.hashCode());
            ClientType clientType = (ClientType) message;

            Subsystem subsystem = new Subsystem(new Member(clientType.getId().getXRoadInstance(), clientType.getId()
                    .getMemberClass(),
                    clientType.getId().getMemberCode(), clientType.getName()), clientType.getId()
                    .getSubsystemCode());

            log.info("{} Handling subsystem {} ", COUNTER, subsystem);
            log.info("Fetching methods for the client with listMethods -service...");

            XRoadClientIdentifierType xroadId = new XRoadClientIdentifierType();
            xroadId.setXRoadInstance(xroadInstance);
            xroadId.setMemberClass(memberClass);
            xroadId.setMemberCode(memberCode);
            xroadId.setSubsystemCode(subsystemCode);
            xroadId.setObjectType(XRoadObjectType.SUBSYSTEM);

            // fetch the methods
            try {
                log.info("calling web service at {}", webservicesEndpoint);
                List<XRoadServiceIdentifierType> result = XRoadClient.getMethods(webservicesEndpoint, xroadId,
                        clientType);
                log.info("Received all methods for client {} ", ClientTypeUtil.toString(clientType));

                // Save services for subsystems
                List<Service> services = new ArrayList<>();
                for (XRoadServiceIdentifierType service : result) {
                    services.add(new Service(subsystem, service.getServiceCode(), service.getServiceVersion()));
                }
                catalogService.saveServices(subsystem.createKey(), services);

                // get wsdls
                for (XRoadServiceIdentifierType service : result) {
                    log.info("{} Sending service {} to new MethodActor ", COUNTER, service.getServiceCode());
                    fetchWsdlPoolRef.tell(service, getSender());
                }
            } catch (Exception e) {
                log.error("Failed to get methods for subsystem {} \n {}", subsystem, e.toString());
                throw e;
            }
            return true;

        } else {
            return false;
        }
    }
}
