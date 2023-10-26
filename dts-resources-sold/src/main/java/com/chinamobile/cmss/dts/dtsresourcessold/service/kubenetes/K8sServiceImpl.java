package com.chinamobile.cmss.dts.dtsresourcessold.service.kubenetes;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class K8sServiceImpl implements K8sService{

    @Override
    public int getResidualInstanceForEveryZone(KubernetesClient client, double cpu, double memory) {
        //每个可用区可以创建的实例 = min(c/CPU, m/Memory)
        //遍历zone的每个node节点
        int unit = 0;
        NodeList nodeList = client.nodes().list();
        for (Node node:nodeList.getItems()){
            //判断哪些node节点是dts工作节点(TODO)
            //获取每个node节点的全部CPU数量和内存
            double totalCpuOnSpecificNode = Double.parseDouble(node.getStatus().getCapacity().get("cpu").getAmount());
            double totalMemoryOnSpecificNode = Double.parseDouble(node.getStatus().getCapacity().get("memory").getAmount());
            //获得每个node节点当前的资源的total limits
            Map<String, Double> map = getTotalCpuAndMemoryLimitsOnSpecificNode(client, node);
            double totalCpuLimitsOnSpecificNode = map.get("cpu");
            double totalMemoryLimitsOnSpecificNode = map.get("memory");
            double residualCpuOnSpecificNode = totalCpuOnSpecificNode * 1000 - totalCpuLimitsOnSpecificNode;
            double residualMemoryOnSpecificNode = totalMemoryOnSpecificNode / 1024  - totalMemoryLimitsOnSpecificNode;
            unit += Math.min(Math.floor(residualCpuOnSpecificNode / cpu / 1000), Math.floor(residualMemoryOnSpecificNode / memory / 1024));
        }
        return unit;
    }

    @Override
    public KubernetesClient getClient() {
//        System.setProperty("KUBECONFIG","./conf/config");
//        KubernetesClient client = new DefaultKubernetesClient();
//        return client;
        File configFile = new File("./conf/config");
        final String configYaml;
        try {
            configYaml = String.join("\n", Files.readAllLines(configFile.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Config config = io.fabric8.kubernetes.client.Config.fromKubeconfig(configYaml);
        KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build();
        return client;
    }

    @Override
    public Map<String,Double> getTotalCpuAndMemoryLimitsOnSpecificNode(KubernetesClient client, Node node) {
        String nodeName = node.getMetadata().getName();
        PodList podList = client.pods().withField("spec.nodeName", nodeName).list();
        //遍历当前Node节点下的所有Pod
        double totalCpuLimits = 0;
        double totalMemoryLimits = 0;
        for (Pod pod:podList.getItems()){
            //遍历当前Pod下的所有Container
            for (Container container:pod.getSpec().getContainers()){
                Map<String, Quantity> limits = container.getResources().getLimits();
                //container没有设置limits属性
                if (limits != null){
                    totalCpuLimits += (limits.get("cpu") == null ? 0 : getCpuLimits(limits.get("cpu").getAmount(), limits.get("cpu").getFormat()));
                    totalMemoryLimits += (limits.get("memory") == null ? 0 : getMemoryLimits(limits.get("memory").getAmount(), limits.get("memory").getFormat()));
                }
            }

        }
        Map<String, Double> map = new HashMap<>();
        map.put("cpu", totalCpuLimits);
        map.put("memory", totalMemoryLimits);
        return map;
    }

    @Override
    public double getCpuLimits(String value, String format) {
        double limit = Double.parseDouble(value);
        switch(format){
            case "n":
                limit /= 1000;
                break;
            case "m":
                break;
            case "":
                limit *= 1000;
                break;
            default:
                throw new RuntimeException("没有对应的CPU格式");

        }
        return limit;
    }

    @Override
    public double getMemoryLimits(String value, String format) {
        double limit = Double.parseDouble(value);
        switch (format){
            case "Ki":
                limit /= 1024;
                break;
            case "K":
                limit /= 1000;
            case "Mi":
                break;
            case "M":
                //当一个node中出现M和Mi时,kubectl describe node 中allocated resources返回的单位为B
                //为了方便把M和Mi当成相同的单位
                break;
            case "Gi":
            case "G":
                limit *= 1024;
                break;
            default:
                throw new RuntimeException("没有对应的内存单位");
        }
        return limit;
    }


    @Override
    public void closeClient(Client client) {
        client.close();
    }
}
