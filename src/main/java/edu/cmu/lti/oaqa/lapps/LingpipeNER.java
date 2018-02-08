/*
 * Copyright 2014 The Language Application Grid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package edu.cmu.lti.oaqa.lapps;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.util.Streams;
import org.lappsgrid.core.DataFactory;
import org.lappsgrid.discriminator.Discriminators;
import static org.lappsgrid.discriminator.Discriminators.Alias;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.LifException;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.lappsgrid.discriminator.Discriminators.Uri;

public class LingpipeNER extends AbstractLingpipeService {
    private Chunker chunker;

    public LingpipeNER() throws IOException, ClassNotFoundException {
        super();

        //load models file
        loadChucker();

         metadata.setDescription("Lingpipe Named Entity Recognizer with model \"English News: MUC-6\"");

        // JSON for input information
        IOSpecification requires = new IOSpecification();
        requires.addFormats(Uri.TEXT, Uri.LAPPS);
        requires.addLanguage("en");             // Source language

        // JSON for output information
        IOSpecification produces = new IOSpecification();
        produces.addFormat(Uri.LAPPS);          // LIF (form)
        produces.addAnnotation(Uri.NE);         // Named Entity
        requires.addLanguage("en");             // Target language

        // Embed I/O metadata JSON objects
        metadata.setRequires(requires);
        metadata.setProduces(produces);
    }

    protected void loadChucker() throws IOException, ClassNotFoundException {
        URL url = getClass().getResource("/models/ne-en-news-muc6.AbstractCharLmRescoringChunker");
        loadChucker(url);
    }

    protected void loadChucker(URL url) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(url.openStream());
        chunker = (Chunker) ois.readObject();
        Streams.closeQuietly(ois);
    }

    @Override
    public String execute(String input) {
        // Step #1: Parse the input.
        Data data = Serializer.parse(input, Data.class);

        // Step #2: Check the discriminator
        final String discriminator = data.getDiscriminator();
        if (discriminator.equals(Discriminators.Uri.ERROR)) {
            // Return the input unchanged.
            return input;
        }

        // Step #3: Extract the text.
        Container container = null;
        if (discriminator.equals(Discriminators.Uri.TEXT)) {
            container = new Container();
            container.setText(data.getPayload().toString());
        } else if (accept(discriminator)) {
            container = new Container((Map) data.getPayload());
        } else {
            // This is a format we don't accept.
            String message = String.format("Unsupported discriminator type: %s", discriminator);
            return new Data<String>(Discriminators.Uri.ERROR, message).asJson();
        }

        // Step #4: Check the text
        String text = container.getText();

        if (text == null || text.isEmpty()) {
            return input;
        }

        // Step #5: Create a new View
        View view = null;
        try
        {
            view = container.newView();
        }
        catch (LifException e)
        {
            return DataFactory.error("Unable to create a new view.", e);
        }

        // Step #6 GO.
        Chunking chunking = chunker.chunk(text);
        int i = 1;
        for (Chunk chunk : chunking.chunkSet()) {
            Annotation a = view.newAnnotation("lingpipe-chunk-" + i, Uri.NE, chunk.start(), chunk.end());
            a.setLabel(Alias.NE);
            a.addFeature(Features.Token.WORD, text.substring(chunk.start(), chunk.end()));

            //TODO Features.NamedEntity.CATEGORY should likely be defined as a feature type.
            a.addFeature(Features.NamedEntity.CATEGORY, chunk.type());
            a.addFeature("score", String.valueOf(chunk.score()));
            i++;
        }

        // Step #7: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view.addContains(Discriminators.Uri.NE, this.getClass().getName(), "ner:lingpipe:en-news-muc-6");

        // Step #8: Create a DataContainer with the result.
        data = new DataContainer(container);

        // Step #9: Serialize the data object and return the JSON.
        return data.asJson();

    }

}
