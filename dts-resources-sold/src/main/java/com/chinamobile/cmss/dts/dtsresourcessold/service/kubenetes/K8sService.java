package com.chinamobile.cmss.dts.dtsresourcessold.service.kubenetes;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.client.Client;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Map;

public interface K8sService {
    public int getResidualInstanceForEveryZone(KubernetesClient client,  double cpu, double memory);
    public KubernetesClient getClient();
    Map<String,Double> getTotalCpuAndMemoryLimitsOnSpecificNode(KubernetesClient client, Node node);

    double getCpuLimits(String value, String format);
    double getMemoryLimits(String value, String format);
    public void closeClient(Client client);
}
