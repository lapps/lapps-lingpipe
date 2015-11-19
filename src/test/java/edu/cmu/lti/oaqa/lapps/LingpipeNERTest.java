package edu.cmu.lti.oaqa.lapps;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.api.WebService;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.lappsgrid.discriminator.Discriminators.Uri;

/**
 * @author Di Wang.
 */
public class LingpipeNERTest {

    protected WebService service;

    @Before
    public void setUp() throws IOException, ClassNotFoundException {
        service = new LingpipeNER();
    }

    @After
    public void tearDown() {
        service = null;
    }

    @Test
    public void testGetMetadata() throws Exception {
        // Retrieve metadata, remember `getMetadata()` returns a serialized JSON string
        String json = service.getMetadata();
        assertNotNull("service.getMetadata() returned null", json);

        // Instantiate `Data` object with returned JSON string
        Data data = Serializer.parse(json, Data.class);
        assertNotNull("Unable to parse metadata json.", data);
        assertNotSame(data.getPayload().toString(), Uri.ERROR, data.getDiscriminator());

        // Then, convert it into `Metadata` datastructure
        ServiceMetadata metadata = new ServiceMetadata((Map) data.getPayload());
        IOSpecification produces = metadata.getProduces();
        IOSpecification requires = metadata.getRequires();

        // Now, see each field has correct value
        assertEquals("Name is not correct", LingpipeNER.class.getName(), metadata.getName());


        List<String> list = requires.getFormat();
        assertEquals("Too many formats accepted", 2, list.size());
        assertTrue("Text not accepted", list.contains(Uri.TEXT));

        assertEquals("Too many annotation types produced", 1, produces.getAnnotations().size());
        assertEquals("Tokens not produced", Uri.NE, produces.getAnnotations().get(0));

    }

    @Test
    public void testExecute() throws Exception {
        // set up test material
        final String text = "Barack Obama is the 44th President of the United States.";

        // call `execute()`, store returned a JSON string into a `Container` datastructure, the main wrapper for LIF
        Container container = execute(text);
        assertEquals("Text not set correctly", text, container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain NEs", view.contains(Uri.NE));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 2) {
            fail(String.format("Expected 2 NEs. Found %d", annotations.size()));
        }
        Annotation ne1 = annotations.get(0);
        assertEquals("Token 1: wrong type", Uri.NE, ne1.getAtType());
        assertEquals("Token 1: wrong start", 0L, ne1.getStart().longValue());
        assertEquals("Token 1: wrong word", "Barack Obama", ne1.getFeature(Features.Token.WORD));

        Annotation ne2 = annotations.get(1);
        assertEquals("Token 2: wrong end", 55L, ne2.getEnd().longValue());
        assertEquals("Token 2: wrong word", "United States", ne2.getFeature(Features.Token.WORD));
    }


    protected Container execute(String input) {
        return execute(new Data<>(Uri.TEXT, input));
    }

    protected Container execute(Container container) {
        return execute(new DataContainer(container));
    }

    protected Container execute(Data data) {
        String json = service.execute(data.asJson());
        assertNotNull("Service returned null", json);
        DataContainer dc = Serializer.parse(json, DataContainer.class);
        assertEquals("Returned format is not LIF", Uri.LAPPS, dc.getDiscriminator());
        return dc.getPayload();
    }
}