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

import org.lappsgrid.core.DataFactory;
import org.lappsgrid.discriminator.Discriminators;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

import static org.lappsgrid.discriminator.Discriminators.Uri;

public class LingpipeDictionaryBasedNER extends AbstractLingpipeService {

    static final double CHUNK_SCORE = 1.0;

    public LingpipeDictionaryBasedNER() throws IOException, ClassNotFoundException {
        super();

         metadata.setDescription("Exact Dictionary-based Lingpipe Named Entity Recognizer");

        // JSON for output information
        IOSpecification produces = metadata.getProduces();
        produces.addAnnotation(Uri.NE);         // Named Entity
        produces.addTagSet(Uri.NE, Uri.TAGS_NER + "#lingpipe");
    }

    //@Override
    public String current_execute(String input) {
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
            // FIXME Setting the @context is a workaround for a bug in the serialization API.
            // When that bug is fixed this workaround can be removed.
            // https://github.com/lapps/org.lappsgrid.serialization/issues/25
            Map map = (Map) data.getPayload();
            if (map.get("@context") == null) {
                map.put("@context", "http://vocab.lappsgrid.org/context-1.0.0.jsonld");
            }
            container = new Container(map);
//            container = new Container((Map)data.getPayload());
            // END OF WORKAROUND.
        } else {
            // This is a format we don't accept.
            String message = String.format("Unsupported discriminator type: %s", discriminator);
            return new Data<String>(Discriminators.Uri.ERROR, message).asJson();
        }

        // Step #4: Create a new View
		View view = null;
        view = container.newView();

		// Step #5: Chuck the text and add annotations.
        String text = container.getText();

        if (text == null || text.isEmpty()) {
            return input;
        }

        MapDictionary<String> dictionary = new MapDictionary<String>();
        String param = (String) data.getParameter("dictionary");
        String[] wordlist = param.split("\\r?\\n");
        for (String entry: wordlist) {
            // example entry: Obama, PERSON
            String fields[] = entry.trim().split("[|]+");
            if (fields.length != 2) {
                String message = String.format("Invalid dictionary format. Found %d fields in %s", fields.length, entry);
                return new Data(Uri.ERROR, message).asPrettyJson();
            }
            dictionary.addEntry(new DictionaryEntry<String>(fields[0].trim(), fields[1].trim(), CHUNK_SCORE));
        }

        ExactDictionaryChunker chunker = new ExactDictionaryChunker(dictionary, IndoEuropeanTokenizerFactory.INSTANCE, true, true);
        Chunking chunking = chunker.chunk(text);
        int i = 1;
        for (Chunk chunk : chunking.chunkSet()) {
            Annotation a = view.newAnnotation("lingpipe-chunk-" + i, Uri.NE, chunk.start(), chunk.end());
            a.setLabel(Alias.NE);
            a.addFeature(Features.Token.WORD, text.substring(chunk.start(), chunk.end()));
            a.addFeature(Features.NamedEntity.CATEGORY, chunk.type());
            a.addFeature("score", String.valueOf(chunk.score()));
            i++;
        }

        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view.addContains(Discriminators.Uri.NE, this.getClass().getName(), "ner:lingpipe:dictionary");

        // Step #7: Create a DataContainer with the result.
        data = new DataContainer(container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asJson();

    }

    @Override
    public String execute(String json) {
        try {
            return current_execute(json);
        }
        catch (Exception e) {
            StringWriter swriter = new StringWriter();
            PrintWriter pwriter = new PrintWriter(swriter);
            e.printStackTrace(pwriter);
            return (new Data(Uri.ERROR, swriter.toString())).asJson();
        }
    }

}
