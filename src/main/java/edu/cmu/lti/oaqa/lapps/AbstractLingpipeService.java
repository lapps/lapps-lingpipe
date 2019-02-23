package edu.cmu.lti.oaqa.lapps;

import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import static  org.lappsgrid.discriminator.Discriminators.*;

/**
 * @author Di Wang.
 * @author Keith Suderman
 */
abstract public class AbstractLingpipeService implements ProcessingService {

    //TODO: add to vocab
    protected static final String AGPL_LICENCE = "http://vocab.lappsgrid.org/ns/license#agpl-3.0";

    protected ServiceMetadata metadata;

    public AbstractLingpipeService() {
        // Create a metadata object
        metadata = new ServiceMetadata();

        // Populate metadata using setX() methods
        metadata.setName(this.getClass().getName());
        metadata.setVersion(Version.getVersion());
        metadata.setVendor("http://lti.cs.cmu.edu");

        IOSpecification io = metadata.getRequires();
        io.addFormats(Uri.TEXT, Uri.LIF, Uri.LAPPS);
        io.addLanguage("en");

        io = metadata.getProduces();
        io.addFormat(Uri.LIF);
        io.addLanguage("en");

        //TODO I'm not sure this is the correct way to do this.
        // The markdown should likely be in a separate field. Doing it this way
        // loses the discriminator...
        // NOTE It is not as the metadata fails schema validation.
//        metadata.setLicense("Lingpipe is released under the `GNU Affero General Public License version 3.0 <https://www.gnu.org/licenses/agpl-3.0.en.html>`_");
        metadata.setLicense(AGPL_LICENCE);
    }

    @Override
    public String getMetadata() {
        // Create Data instance and populate it
        Data<ServiceMetadata> data = new Data<>(Uri.META, this.metadata);
        return data.asJson();
    }

    public boolean accept(String discriminator) {
    	return Uri.LIF.equals(discriminator); // || Uri.LAPPS.equals(discriminator);
	}
}
