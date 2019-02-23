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

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;
import com.aliasi.util.Streams;
import org.lappsgrid.core.DataFactory;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.lappsgrid.discriminator.Discriminators.Uri;

public class LingpipeTagger extends AbstractLingpipeService {

    HmmDecoder decoder;

    public LingpipeTagger() throws IOException, ClassNotFoundException {
        super();

        //load the model
        loadTagger();

        metadata.setDescription("Lingpipe Brown-HMM pos tagger");
        // JSON for input information
        IOSpecification requires = metadata.getRequires();
        requires.addAnnotation(Uri.TOKEN);

        // JSON for output information
        IOSpecification produces = metadata.getProduces();
        produces.addAnnotation(Uri.POS);
        produces.addTagSet(Uri.POS, Uri.TAGS_POS_BROWN);

    }

    protected void loadTagger() throws IOException, ClassNotFoundException {
        URL url = getClass().getResource("/models/pos-en-general-brown.HiddenMarkovModel");
        loadTagger(url);
    }

    protected void loadTagger(URL url) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(url.openStream());
        HiddenMarkovModel hmm = (HiddenMarkovModel) ois.readObject();
        Streams.closeQuietly(ois);
        decoder = new HmmDecoder(hmm);
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

        // Step #3: Extract the token.
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

        List<View> views = container.findViewsThatContain(Uri.TOKEN);
        if (views == null || views.size() == 0) {
            return new Data<String>(Uri.ERROR, "Unable to process input: no tokens found").asJson();
        }
        // Work around for the Serializer Bug #23. The Serializer creates HashMap objects instead of
        // view objects when initialized from a Map.
        Object object = views.get(0);
        View tokenStep = null;
        if (object instanceof Map)
        {
            tokenStep = new View((Map)object);
        }
        else
        {
            tokenStep = (View) object;
        }
//        View tokenStep = views.get(0);
        List<Annotation> annotations = tokenStep.getAnnotations();

        String text = container.getText();
        if (text == null || text.isEmpty()) {
            return input;
        }

        // Step #4: Create a new View
        View view = null;
        view = container.newView();

        // Step #5: Chuck the text and add annotations.
        ArrayList<String> tokens = new ArrayList<>();
        for (Annotation annotation : annotations) {
            String token = text.substring(annotation.getStart().intValue(), annotation.getEnd().intValue());
            tokens.add(token);
        }
        Tagging<String> tagging = decoder.tag(tokens);
        if (tagging.size() != annotations.size()) {
            return new Data<String>(Uri.ERROR, "Tagger error: input/output sizes are different.").asJson();
        } else {
            Iterator<Annotation> annoIter = annotations.iterator();
            Iterator<String> tagIter = tagging.tags().iterator();
            while (annoIter.hasNext() && tagIter.hasNext()) {
                Annotation anno = annoIter.next();
                anno.setAtType(Uri.POS);
                anno.setLabel(Discriminators.Alias.POS);
                anno.addFeature(Features.Token.PART_OF_SPEECH, tagIter.next());
                view.add(anno);
            }
        }

        // Step #6: Update the view's metadata. Each view contains metadata about the
        // annotations it contains, in particular the name of the tool that produced the annotations.
        view.addContains(Uri.POS, this.getClass().getName(), "tagger:lingpipe:brown-hmm-tagger");

        // Step #7: Create a DataContainer with the result.
        data = new DataContainer(container);

        // Step #8: Serialize the data object and return the JSON.
        return data.asJson();

    }

}