package com.abidi.producer;

import com.abidi.marketdata.model.MarketData;
import com.abidi.queue.CircularMMFQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.abidi.consumer.UDPQueueConsumer.QUEUE_SIZE;

public class UDPQueueProducer {

    private static final Logger LOG = LoggerFactory.getLogger(UDPQueueProducer.class);
    private final CircularMMFQueue mmfQueue;
    private final MarketData md;

    public UDPQueueProducer() throws IOException {
        md = new MarketData();
        mmfQueue = new CircularMMFQueue(md.size(), QUEUE_SIZE, "/tmp/producer");
    }

    public static void main(String[] args) throws IOException {

        LOG.info("Starting Market Data Generator...");
        UDPQueueProducer udpQueueProducer = new UDPQueueProducer();
        udpQueueProducer.produce();
    }

    public void produce() {
        LOG.info("Sending MD messages...");
        int msgId = 1;
        md.set("GB00BJLR0J16", 1d + msgId, 0, true, (byte) 1, "BRC", "2023-02-14:22:10:13", msgId);
        while (true) {
            md.setPrice(1d + msgId);
            md.side(msgId % 2 == 0 ? 0 : 1);
            md.setFirm(msgId % 2 == 0);
            md.setId(msgId);
            if (mmfQueue.add(md.getData())) {
                msgId++;
                LOG.debug("Msg {} sent", msgId);
            }
        }
    }
}