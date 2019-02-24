package edu.cmu.lti.oaqa.lapps
import org.lappsgrid.api.WebService
import org.lappsgrid.serialization.Data
//import groovy.json.JsonOutput

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author Keith Suderman
 */
class YemenProcessor {
    void run() {
        Queue<Packet> loaded = new ConcurrentLinkedQueue<Packet>()
        Queue<Packet> tokenized = new ConcurrentLinkedQueue<>()
        Queue<Packet> split = new ConcurrentLinkedQueue<>()
        Queue<Packet> tagged = new ConcurrentLinkedQueue<>()
        Queue<Packet> nerq = new ConcurrentLinkedQueue<>()

        List<Thread> threads = [
                new DataReader(new File("/var/corpora/Yemen-lif"), loaded),
                new TokenizerWorker(loaded, tokenized),
                new SplitterWorker(tokenized, split),
                new TaggerWorker(split, tagged),
                new NERWorker(tagged, nerq),
                new DataWriter('/var/corpora/Yemen-Lingpipe', nerq)
        ]
        threads*.start()
        threads*.join()
        println "All threads have terminated."
    }

    static void main(String[] args) {
        new YemenProcessor().run()
    }
}

abstract class Worker extends Thread {
    WebService service
    ConcurrentLinkedQueue<Packet> input
    ConcurrentLinkedQueue<Packet> output

    Worker(WebService service, ConcurrentLinkedQueue<Packet> input, ConcurrentLinkedQueue<Packet> output) {
        this.service = service
        this.input = input
        this.output = output
    }

    abstract String name();

    void run() {
        Packet packet = input.poll()
        while (packet != Packet.POISON) {
            if (packet == null) {
                sleep(100)
            }
            else {
                println "${packet.count}. ${name()} : ${packet.filename}"
                packet.data = service.execute(packet.data)
                output.add(packet)
            }
            packet = input.poll()
        }
        output.add(Packet.POISON)
        println "${name()} terminating"
    }
}

class DataReader extends Thread {
    File directory
    Queue<Packet> output

    DataReader(String path, Queue<Packet> output) {
        this(new File(path), output)
    }

    DataReader(File directory, Queue<Packet> output) {
        this.directory = directory
        this.output = output
    }

    public void run() {
        FileFilter filter = { File f ->
            return f.name.endsWith('.lif')
        }
        int counter = 0
//        File[] files = directory.listFiles(filter)
        directory.listFiles(filter).each { File file ->
//        for (int i = 0; i < 100; ++i) {
//            File file = files[i]
            ++counter
            println "Loading ${counter} : ${file.path}"
            Packet packet = new Packet()
            packet.filename = file.name
            packet.data = file.text
            packet.count = counter
            output.add(packet)
        }
        output.add(Packet.POISON)
        println "All files have been loaded."
    }
}

class TokenizerWorker extends Worker {
    TokenizerWorker(ConcurrentLinkedQueue<Packet> input, ConcurrentLinkedQueue<Packet> output) {
        super(new LingpipeTokenizer(), input, output)
    }
    String name() { 'tokenizer' }
}

class TaggerWorker extends Worker {
    TaggerWorker(ConcurrentLinkedQueue<Packet> input, ConcurrentLinkedQueue<Packet> output) {
        super(new LingpipeTagger(), input, output)
    }
    String name() { 'tagger' }
}

class SplitterWorker extends Worker {
    SplitterWorker(ConcurrentLinkedQueue<Packet> input, ConcurrentLinkedQueue<Packet> output) {
        super(new LingpipeSentenceSplitter(), input, output)
    }
    String name() { ' splitter' }
}

class NERWorker extends Worker {
    NERWorker(ConcurrentLinkedQueue<Packet> input, ConcurrentLinkedQueue<Packet> output) {
        super(new LingpipeNER(), input, output)
    }
    String name() { 'ner' }
}

class DataWriter extends Thread {
    File destination
    ConcurrentLinkedQueue<Packet> input

    DataWriter(String path, ConcurrentLinkedQueue<Packet> input) {
        this(new File(path), input)
    }

    DataWriter(File destination, ConcurrentLinkedQueue<Packet> input) {
        this.destination = destination
        this.input = input
        if (!destination.exists()) {
            if (!destination.mkdirs()) {
                throw new IOException("Unable to create directory: ${destination.path}")
            }
        }
    }

    void run() {
        Packet packet = input.poll()
        while (packet != Packet.POISON) {
            if (packet == null) {
                sleep(100)
            }
            else {
                File file = new File(destination, packet.filename)
                println "Writing ${file.path}"
                file.text = JsonOutput.prettyPrint(packet.data)
            }
            packet = input.poll()
        }
        println "Wrote all files."
    }
}

class Packet {
    static final Packet POISON = new Packet()
    String filename
    String data
    int count
}
