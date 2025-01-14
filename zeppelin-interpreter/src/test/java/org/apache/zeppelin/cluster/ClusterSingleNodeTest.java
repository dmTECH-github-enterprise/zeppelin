/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zeppelin.cluster;

import org.apache.zeppelin.cluster.meta.ClusterMeta;
import org.apache.zeppelin.cluster.meta.ClusterMetaType;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.remote.RemoteInterpreterUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


class ClusterSingleNodeTest {
  private static Logger LOGGER = LoggerFactory.getLogger(ClusterSingleNodeTest.class);
  private static ZeppelinConfiguration zConf;

  private static ClusterManagerServer clusterServer = null;
  private static ClusterManagerClient clusterClient = null;

  static String zServerHost;
  static int zServerPort;
  static final String metaKey = "ClusterSingleNodeTestKey";

  @BeforeAll
  static void startCluster() throws IOException, InterruptedException {
    LOGGER.info("startCluster >>>");

    zConf = ZeppelinConfiguration.load("zeppelin-site-test.xml");

    // Set the cluster IP and port
    zServerHost = RemoteInterpreterUtils.findAvailableHostAddress();
    zServerPort = RemoteInterpreterUtils.findRandomAvailablePortOnAllLocalInterfaces();
    zConf.setClusterAddress(zServerHost + ":" + zServerPort);

    clusterServer = ClusterManagerServer.getInstance(zConf);
    clusterServer.start();

    // mock cluster manager client
    clusterClient = ClusterManagerClient.getInstance(zConf);
    clusterClient.start(metaKey);

    // Waiting for cluster startup
    int wait = 0;
    while(wait++ < 100) {
      if (clusterServer.isClusterLeader()
          && clusterServer.raftInitialized()
          && clusterClient.raftInitialized()) {
        LOGGER.info("wait {}(ms) found cluster leader", wait*3000);
        break;
      }
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Thread.sleep(3000);
    assertEquals(true, clusterServer.isClusterLeader());
    LOGGER.info("startCluster <<<");
  }

  @AfterAll
  static void stopCluster() {
    if (null != clusterClient) {
      clusterClient.shutdown();
    }
    if (null != clusterClient) {
      clusterServer.shutdown();
    }
    LOGGER.info("stopCluster");
  }

  @Test
  void getServerMeta() {
    LOGGER.info("getServerMeta >>>");

    // Get metadata for all services
    Map<String, Map<String, Object>> meta = clusterClient.getClusterMeta(ClusterMetaType.SERVER_META, "");
    LOGGER.info(meta.toString());

    Map<String, Map<String, Object>> intpMeta = clusterClient.getClusterMeta(ClusterMetaType.INTP_PROCESS_META, "");
    LOGGER.info(intpMeta.toString());

    assertNotNull(meta);
    assertTrue(meta instanceof Map);

    // Get metadata for the current service
    Map<String, Object> values = meta.get(clusterClient.getClusterNodeName());
    assertTrue(values instanceof Map);
    assertTrue(values.size() > 0);

    LOGGER.info("getServerMeta <<<");
  }

  @Test
  void putIntpProcessMeta() {
    // mock IntpProcess Meta
    Map<String, Object> meta = new HashMap<>();
    meta.put(ClusterMeta.SERVER_HOST, zServerHost);
    meta.put(ClusterMeta.SERVER_PORT, zServerPort);
    meta.put(ClusterMeta.INTP_TSERVER_HOST, "INTP_TSERVER_HOST");
    meta.put(ClusterMeta.INTP_TSERVER_PORT, "INTP_TSERVER_PORT");
    meta.put(ClusterMeta.CPU_CAPACITY, "CPU_CAPACITY");
    meta.put(ClusterMeta.CPU_USED, "CPU_USED");
    meta.put(ClusterMeta.MEMORY_CAPACITY, "MEMORY_CAPACITY");
    meta.put(ClusterMeta.MEMORY_USED, "MEMORY_USED");

    // put IntpProcess Meta
    clusterClient.putClusterMeta(ClusterMetaType.INTP_PROCESS_META, metaKey, meta);

    // get IntpProcess Meta
    Map<String, Map<String, Object>> check
        = clusterClient.getClusterMeta(ClusterMetaType.INTP_PROCESS_META, metaKey);

    LOGGER.info(check.toString());

    assertNotNull(check);
    assertNotNull(check.get(metaKey));
    assertEquals(true, check.get(metaKey).size()>0);
  }
}
