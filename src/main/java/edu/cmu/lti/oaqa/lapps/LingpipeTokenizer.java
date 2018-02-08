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

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import org.lappsgrid.core.DataFactory;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.LifException;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

import java.util.Map;

import static org.lappsgrid.discriminator.Discriminators.Uri;
import static org.lappsgrid.discriminator.Discriminators.Alias;

public class LingpipeTokenizer extends AbstractLingpipeService {

    static final TokenizerFactory TOKENIZER_FACTORY
            = IndoEuropeanTokenizerFactory.INSTANCE;

    public LingpipeTokenizer(){
        super();

        metadata.setDescription("Lingpipe IndoEuropean Tokenizer");

        // JSON for input information
        IOSpecification requires = new IOSpecification();
        requires.addFormats(Uri.TEXT, Uri.LAPPS);
        requires.addLanguage("en");             // Source language

        // JSON for output information
        IOSpecification produces = new IOSpecification();
        produces.addFormat(Uri.LAPPS);          // LIF (form)
        produces.addAnnotation(Uri.TOKEN);         // Token
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
        if (discriminator.equals(Uri.ERROR)) {
            // Return the input unchanged.
            return input;
        }

        // Step #3: Extract the text.
        Container container = null;
        if (discriminator.equals(Uri.TEXT)) {
            container = new Container();
            container.setText(data.getPayload().toString());
        } else if (accept(discriminator)) {
            container = new Container((Map) data.getPayload());
        } else {
            // This is a format we don't accept.
            String message = String.format("Unsupported discriminator type: %s", discriminator);
            return new Data<String>(Uri.ERROR, message).asJson();
        }

        // Step #4: Create a new View
        View view = null;
        try
        {
            view = container.newView();
        }
        catch (LifException e)
        {
            return DataFactory.error("Unable to create a new view.", e);
        }

        // Step #5: Chuck the text and add annotations.
        String text = container.getText();

        if (text == null || text.isEmpty()) {
            return input;
        }

        Tokenizer tokenizer = TOKENIZER_FACTORY.tokenizer(text.toCharArray(), 0, text.length());
        String token = null;
        int i = 1;
        while ((token = tokenizer.nextToken()) != null) {
            Annotation a = view.newAnnotation("lingpipe-token-" + i, Uri.TOKEN,
                    tokenizer.lastTokenStartPosition(), tokenizer.lastTokenEndPosition());
            a.setLabel(Alias.TOKEN );
            a.addFeature(Features.Token.WORD, token);
        }

        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the
        // annotations.
        view.addContains(Uri.TOKEN, this.getClass().getName(), "token:lingpipe:indo-european-tokenizer");

        // Step #7: Create a DataContainer with the result.
        //data = new DataContainer(container);
        data = new Data<Container>(Uri.LIF, container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asJson();

    }

}