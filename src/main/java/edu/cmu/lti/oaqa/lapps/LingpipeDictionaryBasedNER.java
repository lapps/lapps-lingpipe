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
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.MapDictionary;
import com.aliasi.dict.TrieDictionary;
import com.aliasi.dict.Dictionary;
import com.aliasi.dict.ExactDictionaryChunker;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;

import static org.lappsgrid.discriminator.Discriminators.Alias;
import org.lappsgrid.discriminator.Discriminators;
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
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.Map;

import static org.lappsgrid.discriminator.Discriminators.Uri;

public class LingpipeDictionaryBasedNER extends AbstractLingpipeService {

    static final double CHUNK_SCORE = 1.0;

    public LingpipeDictionaryBasedNER() throws IOException, ClassNotFoundException {
        super();

         metadata.setDescription("Exact Dictionary-based Lingpipe Named Entity Recognizer");

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
        } else if (discriminator.equals(Discriminators.Uri.LAPPS)) {
            container = new Container((Map) data.getPayload());
        } else {
            // This is a format we don't accept.
            String message = String.format("Unsupported discriminator type: %s", discriminator);
            return new Data<String>(Discriminators.Uri.ERROR, message).asJson();
        }

        // Step #4: Create a new View
        View view = container.newView();

        // Step #5: Chuck the text and add annotations.
        String text = container.getText();

        if (text == null || text.isEmpty()) {
            return input;
        }

        MapDictionary<String> dictionary = new MapDictionary<String>();
        String[] wordlist = ((String) data.getParameter("dictionary")).split("\\r?\\n");
        for (String entry: wordlist) {
            // example entry: Obama, PERSON
            String fields[] = entry.trim().split("[,]+");
            dictionary.addEntry(new DictionaryEntry<String>(fields[0].trim(), fields[1].trim(), CHUNK_SCORE));
        }

        ExactDictionaryChunker chunker = new ExactDictionaryChunker(dictionary, IndoEuropeanTokenizerFactory.INSTANCE, true, true);
        Chunking chunking = chunker.chunk(text);
        int i = 1;
        for (Chunk chunk : chunking.chunkSet()) {
            String type = mapNE(chunk.type());
            Annotation a = view.newAnnotation("lingpipe-chunk-" + i, type, chunk.start(), chunk.end());
            a.setLabel(Alias.NE);
            a.addFeature(Features.Token.WORD, text.substring(chunk.start(), chunk.end()));
            //a.addFeature(Features.Token.TYPE, chunk.type());
            a.addFeature(Features.NamedEntity.CATEGORY, chunk.type());
            a.addFeature("score", String.valueOf(chunk.score()));
        }

        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view.addContains(Discriminators.Uri.NE, this.getClass().getName(), "");

        // Step #7: Create a DataContainer with the result.
        data = new DataContainer(container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asJson();

    }

    private String mapNE(String type) {
        switch (type) {
            case "PERSON":
                return Uri.PERSON;
            case "LOCATION":
                return Uri.LOCATION;
            case "ORGANIZATION":
                return Uri.ORGANIZATION;
            case "DATE":
                return Uri.DATE;
        }
        return type;
    }
}
