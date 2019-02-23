/*
 * Copyright (c) 2018 The American National Corpus
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
 */

package edu.cmu.lti.oaqa.lapps;

import org.junit.Test;
import org.lappsgrid.api.WebService;
//import org.lappsgrid.client.ServiceClient;
import org.lappsgrid.client.ServiceClient;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.vocabulary.Features;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.util.Map;

import static org.lappsgrid.discriminator.Discriminators.*;

/**
 *
 */
public class NerTest
{
	private String lingpipeUrl = "http://vassar.lappsgrid.org/invoker/anc:lingpipe.ner_1.1.1-SNAPSHOT";
	private String lingpipeDictUrl = "http://vassar.lappsgrid.org/invoker/anc:lingpipe.dictionary_ner_1.1.1-SNAPSHOT";

	@Test
	public void lingpipe() throws ServiceException
	{
		run(lingpipeUrl);
	}

	@Test
	public void lingpipeDict() throws ServiceException, IOException, ClassNotFoundException
	{
		run(lingpipeDictUrl);
	}

	public void run(String url) throws ServiceException
	{
		WebService ner = new ServiceClient(url, "tester", "tester");

		String text = "Barack Obama was the 44th President of the United States.";
		Data data = new Data(Uri.TEXT, text);
		String dictionary = "Barack Obama|PERSON\nUnited States|LOCATION";
		data.setParameter("dictionary", dictionary);

		String json = ner.execute(data.asJson());

		data = Serializer.parse(json);
		System.out.println(data.asPrettyJson());

		if (Uri.ERROR.equals(data.getDiscriminator())) {
			System.out.println(data.getPayload().toString());
			return;

		}
		Container container = new Container((Map)data.getPayload());
		container.getViews().forEach( view -> {
			System.out.println("View " + view.getId());
			view.getAnnotations().forEach( annotation -> {
				System.out.printf("%d-%d %s : %s\n", annotation.getStart(), annotation.getEnd(), annotation.getFeature(Features.NamedEntity.CATEGORY), annotation.getFeature("word"));
			});
		});
	}
}
