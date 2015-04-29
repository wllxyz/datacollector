/**
 * (c) 2014 StreamSets, Inc. All rights reserved. May not
 * be copied, modified, or distributed in whole or part without
 * written consent of StreamSets, Inc.
 */
package com.streamsets.pipeline.restapi;

import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.lib.util.ThreadUtil;
import com.streamsets.pipeline.main.RuntimeInfo;
import com.streamsets.pipeline.store.PipelineStoreException;
import com.streamsets.pipeline.util.AuthzRole;

import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/v1/admin")
@DenyAll
public class AdminResource {

  private final RuntimeInfo runtimeInfo;

  @Inject
  public AdminResource(RuntimeInfo runtimeInfo) {
    this.runtimeInfo = runtimeInfo;
  }

  @POST
  @Path("/shutdown")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(AuthzRole.ADMIN)
  public Response shutdown() throws PipelineStoreException {
    Utils.checkState(runtimeInfo.getExecutionMode() != RuntimeInfo.ExecutionMode.SLAVE,
      "This operation is not supported in SLAVE mode");

    Thread thread = new Thread("Shutdown Request") {
      @Override
      public void run() {
        // sleeping  500ms to allow the HTTP response to go back
        ThreadUtil.sleep(500);
        runtimeInfo.shutdown();
      }
    };
    thread.setDaemon(true);
    thread.start();
    return Response.ok().build();
  }

  @GET
  @Path("/threadsDump")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(AuthzRole.ADMIN)
  public Response getThreadsDump() throws IOException {
    ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] threads = threadMXBean.dumpAllThreads(true, true);
    List<Map> augmented = new ArrayList<>(threads.length);
    for (ThreadInfo thread : threads) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("threadInfo", thread);
      map.put("userTimeNanosecs", threadMXBean.getThreadUserTime(thread.getThreadId()));
      map.put("cpuTimeNanosecs", threadMXBean.getThreadCpuTime(thread.getThreadId()));
      augmented.add(map);
    }
    return Response.ok(augmented).build();
  }


  @GET
  @Path("/sdcDirectories")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(AuthzRole.ADMIN)
  public Response getSDCDirectories() throws IOException {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("runtimeDir", runtimeInfo.getRuntimeDir());
    map.put("configDir", runtimeInfo.getConfigDir());
    map.put("dataDir", runtimeInfo.getDataDir());
    map.put("logDir", runtimeInfo.getLogDir());
    map.put("staticWebDir", runtimeInfo.getStaticWebDir());
    return Response.ok(map).build();
  }
}
