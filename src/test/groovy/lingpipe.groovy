#!/usr/bin/env lsd
import org.lappsgrid.client.ServiceClient
import org.lappsgrid.serialization.Data

/*
 * Copyright (c) 2016 The American National Corpus
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

String root = "http://grid.anc.org:9080/LingpipeServices/1.1.0/services/"

def factory = { String name ->
    return new ServiceClient("$root/Lingpipe$name")
}

def tokenizer = factory('Tokenizer')
def splitter = factory('SentenceSplitter')
def tagger = factory('Tagger')
def ner = factory('DictionaryBasedNER')

String dictionary = '''Barack Obama | Person
United States | LOCATION'''
Data data = new Data(Uri.TEXT, "Barack Obama is the 44th President of the United States")

String json = data.asJson()
[tokenizer, splitter, tagger].each { service ->
    println groovy.json.JsonOutput.prettyPrint(json)
    json = service.execute(json)
    println()
}

data = Serializer.parse(json, Data)
data.setParameter('dictionary', dictionary)
json = ner.execute(data.asJson())

println groovy.json.JsonOutput.prettyPrint(json)