package com.abidi.broker;

import com.abidi.marketdata.model.MarketDataCons;
import com.abidi.queue.CircularMMFQueue;
import com.abidi.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static com.abidi.consumer.UDPQueueConsumer.QUEUE_SIZE;
import static java.net.InetAddress.getLocalHost;

public class UDPConsumerBroker {

    private static final Logger LOG = LoggerFactory.getLogger(UDPConsumerBroker.class);
    private final CircularMMFQueue circularMMFQueue;
    private final DatagramSocket socket;
    private volatile DatagramPacket mdPacket;
    private final byte[] bytes;
    private final byte[] ackMsgSeq = new byte[8];
    private final DatagramPacket ackPacket;
    private MarketDataCons marketDataCons = new MarketDataCons();
    private long msgCount = 0;


    public UDPConsumerBroker() throws IOException {

        this.circularMMFQueue = new CircularMMFQueue(marketDataCons.size(), QUEUE_SIZE, "/tmp/consumer");
        socket = new DatagramSocket(5000, InetAddress.getLocalHost());
        bytes = new byte[marketDataCons.size()];
        mdPacket = new DatagramPacket(bytes, marketDataCons.size(), InetAddress.getLocalHost(), 5001);
        ackPacket = new DatagramPacket(ackMsgSeq, 8, getLocalHost(), 5001);
    }

    public static void main(String[] args) throws IOException {
        UDPConsumerBroker udpConsumerBroker = new UDPConsumerBroker();
        udpConsumerBroker.process();
    }

    public void process() {

        while (true) {
            try {
                socket.receive(mdPacket);
                if (mdPacket.getData() != null && mdPacket.getData().length == marketDataCons.size()) {
                    marketDataCons.setData(mdPacket.getData());
                    ackPacket.setData(ByteUtils.longToBytes(marketDataCons.getId()));

                    if (circularMMFQueue.add(mdPacket.getData())) {
                        LOG.info("{} Msg enqueued {}, sending ack", ++msgCount, marketDataCons);
                        socket.send(ackPacket);
                    } else {
                        LOG.info("Can't accept msg, queue is full");
                    }
                }
            } catch (Exception exp) {
                LOG.error("Failed to receive msg");
            }
        }
    }
}
