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

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.lappsgrid.discriminator.Discriminators.Uri;

/**
 * @author Di Wang.
 */
public class LingpipeSentenceSplitterTest
{

    protected WebService service;

    @Before
    public void setUp() {
        service = new LingpipeSentenceSplitter();
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
        assertEquals("Name is not correct", LingpipeSentenceSplitter.class.getName(), metadata.getName());


        List<String> list = requires.getFormat();
        assertEquals("Too many formats accepted", 3, list.size());
        assertTrue("Text not accepted", list.contains(Uri.TEXT));
        assertTrue("LIF not accepted", list.contains(Uri.LIF));

        assertEquals("Too many annotation types produced", 1, produces.getAnnotations().size());
        assertEquals("Tokens not produced", Uri.SENTENCE, produces.getAnnotations().get(0));

    }

    @Test
    public void testExecute() throws Exception {
        // set up test material
        final String text = "Barack Obama is the 44th President of the United States. Who is next?";

        // call `execute()`, store returned a JSON string into a `Container` datastructure, the main wrapper for LIF
        Container container = execute(text);
        assertEquals("Text not set correctly", text, container.getText());

        // Now, see all annotations in current view is correct
        List<View> views = container.getViews();
        if (views.size() != 1) {
            fail(String.format("Expected 1 view. Found: %d", views.size()));
        }
        View view = views.get(0);
        assertTrue("View does not contain Tokens", view.contains(Uri.SENTENCE));
        List<Annotation> annotations = view.getAnnotations();
        if (annotations.size() != 2) {
            fail(String.format("Expected 2 Sentences. Found %d", annotations.size()));
        }
        Annotation ne1 = annotations.get(1);
        assertEquals("Token 1: wrong type", Uri.SENTENCE, ne1.getAtType());
        assertEquals("Token 1: wrong start", 57L, ne1.getStart().longValue());

        Annotation ne2 = annotations.get(1);
        assertEquals("Token 2: wrong end", 69L, ne2.getEnd().longValue());
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
        assertEquals("Returned format is not LIF", Uri.LIF, dc.getDiscriminator());
        return dc.getPayload();
    }
}