package com.varun.leaderelection;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

public class LeaderElection implements Watcher {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final String ELECTION_NAMESPACE = "/election";

    private final ZooKeeper zooKeeper;
    private String currentZNodeName;

    public LeaderElection() throws IOException {
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        LeaderElection leaderElection = new LeaderElection();
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();
        leaderElection.run();
        leaderElection.close();
        System.out.println("Disconnected from Zookeeper, exiting application");
    }

    private void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    private void close() throws InterruptedException {
        zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from Zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
                break;
            case NodeDeleted:
                try {
                    reelectLeader();
                } catch (InterruptedException | KeeperException e) {
                    throw new RuntimeException(e);
                }
        }
    }

    /**
     * Volunteer for leadership by creating an ephemeral sequential zNode
     * @throws InterruptedException if the current thread is interrupted
     * @throws KeeperException     if the Zookeeper operation fails
     */
    private void volunteerForLeadership() throws InterruptedException, KeeperException {
        String zNodePrefix = ELECTION_NAMESPACE + "/c_";
        String zNodeFullPath = zooKeeper.create(zNodePrefix,
                new byte[]{},
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Created zNode: " + zNodeFullPath);
        this.currentZNodeName = zNodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    /**
     * Re-elect the leader by watching the predecessor zNode
     * @throws InterruptedException if the current thread is interrupted
     * @throws KeeperException    if the Zookeeper operation fails
     */
    private void reelectLeader() throws InterruptedException, KeeperException {
        Stat predecessorStat = null;
        String predecessorZNode = "";
        while (predecessorStat == null) {
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);
            children.sort(String::compareTo);
            String smallestChild = children.getFirst();
            if (smallestChild.equals(currentZNodeName)) {
                System.out.println("I am the leader");
                return;
            } else {
                System.out.println("I am not the leader");
                int predecessorIndex = children.indexOf(currentZNodeName) - 1;
                predecessorZNode = children.get(predecessorIndex);
                predecessorStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZNode, this);
            }
        }
        System.out.println("Watching zNode: " + predecessorZNode);
    }
}
