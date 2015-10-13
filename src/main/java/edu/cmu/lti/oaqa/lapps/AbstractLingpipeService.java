package edu.cmu.lti.oaqa.lapps;

import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;

/**
 * @author Di Wang.
 */
abstract public class AbstractLingpipeService implements ProcessingService {

    //TODO: add to vocab
    protected static final String AGPL_LICENCE = "http://vocab.lappsgrid.org/ns/license#agpl-3.0";

    ServiceMetadata metadata;

    public AbstractLingpipeService() {
        // Create a metadata object
        metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setVersion("1.0.0-SNAPSHOT");
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(AGPL_LICENCE);
    }

    @Override
    public String getMetadata() {
        // Create Data instance and populate it
        Data<ServiceMetadata> data = new Data<>(Discriminators.Uri.META, this.metadata);
        return data.asJson();
    }
}
