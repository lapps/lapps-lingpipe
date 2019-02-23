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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.api.WebService;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;

import java.io.IOException;

import static org.lappsgrid.discriminator.Discriminators.*;

/**
 *
 */
public class LingpipeDictionaryNERTest
{
	public static final String text = "Barack Obama is the 44th President of the United States.";

	private WebService service;

	@Before
	public void setup() throws IOException, ClassNotFoundException
	{
		service = new LingpipeDictionaryBasedNER();
	}

	@After
	public void teardown() {
		service = null;
	}

	@Test
	public void testExecute() {
		String dictionary = "Barack Obama|PERSON\nUnited States|LOCATION";
		Data data = new Data(Uri.TEXT, text);
		data.setParameter("dictionary", dictionary);

		String json = service.execute(data.asJson());
		System.out.println(Serializer.parse(json).asPrettyJson());

	}
}
