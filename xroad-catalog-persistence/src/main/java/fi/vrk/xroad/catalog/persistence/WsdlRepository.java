package fi.vrk.xroad.catalog.persistence;

import fi.vrk.xroad.catalog.persistence.entity.Subsystem;
import fi.vrk.xroad.catalog.persistence.entity.Wsdl;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WsdlRepository extends CrudRepository<Wsdl, Long> {
    /**
     * Returns also removed items
     */
    List<Wsdl> findAnyByExternalId(String externalId);
}
