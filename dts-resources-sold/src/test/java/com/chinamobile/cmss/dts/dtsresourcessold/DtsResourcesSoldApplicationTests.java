package com.chinamobile.cmss.dts.dtsresourcessold;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chinamobile.cmss.dts.dtsresourcessold.dto.QryOfferDto;
import com.chinamobile.cmss.dts.dtsresourcessold.service.capacity.CapacityService;
import com.chinamobile.cmss.dts.dtsresourcessold.service.capacity.KafkaProducerService;
import com.chinamobile.cmss.dts.dtsresourcessold.service.kubenetes.K8sService;
import com.chinamobile.cmss.dts.dtsresourcessold.service.mop.MopService;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
class DtsResourcesSoldApplicationTests {

    @Value("${mop.url}")
    private String URL;

    @Value("${mop.publicKey}")
    private String PUBLIC_KEY;

    @Value("${mop.privateKey}")
    private String PRIVATE_KEY;

    @Value("${mop.appId}")
    private String APP_ID;

    @Value("${mop.status:1}")
    private String STATUS;

//    @Value("${grayConfig}")
//    private String BgGray;

    @Value("${mop.order.enable}")
    private boolean mopOrderEnable;

    @Value("${mop.user.enable}")
    private boolean mopUserEnable;

    @Autowired
    private MopService mopService;

    @Autowired
    private CapacityService capacityService;

    @Autowired
    private KafkaProducerService kafkaService;

    @Autowired
    private K8sService k8sService;

    private KubernetesClient client;






    @Test
    void contextLoads() {

    }


    @Test
    public void testGetTotalCpuAndMemoryLimitsOnSpecificNode(){
        KubernetesClient client = k8sService.getClient();
        NodeList nodeList = client.nodes().list();
        for (Node node:nodeList.getItems()){
            Map<String, Double> map = k8sService.getTotalCpuAndMemoryLimitsOnSpecificNode(client, node);
            System.out.println("node:" + node.getMetadata().getName()
            + ", totalCpuLimits:" + map.get("cpu") + ",totalMemoryLimits:" + map.get("memory"));
        }

    }

    @Test
    public void testGetPodRequest(){
        KubernetesClient client = k8sService.getClient();
        NodeList nodeList = client.nodes().list();
        for (Node node:nodeList.getItems()){
            String nodeName = node.getMetadata().getName();
            PodList podList = client.pods().withField("spec.nodeName", nodeName).list();
            for (Pod pod:podList.getItems()){
                int cpuTotalLimits = 0;
                int memoryTotalLimits = 0;
                for (Container container:pod.getSpec().getContainers()){
                    Map<String, Quantity> limits = container.getResources().getLimits();
                    if (limits != null){
                        cpuTotalLimits += Integer.parseInt(limits.get("cpu") == null ? "0" : limits.get("cpu").getAmount());
                        memoryTotalLimits += Integer.parseInt(limits.get("memoty") == null ? "0" : limits.get("memory").getAmount());
                        System.out.println(nodeName + "," + pod.getMetadata().getName() + ",container:" + container.getName());
                    }

                }

            }

        }
    }
    @Test
    public void testGetAllPodsOnSpecificNode(){
        KubernetesClient client = k8sService.getClient();
        NodeList nodeList = client.nodes().list();
        for (Node node:nodeList.getItems()){
            String nodeName = node.getMetadata().getName();
            PodList podList = client.pods().withField("spec.nodeName", nodeName).list();
            for (Pod pod:podList.getItems()){
                System.out.println(nodeName + "," + pod.getMetadata().getName());
            }

        }
    }
    @Test
    public void testGetAllPodsOnCluster(){
        KubernetesClient client = k8sService.getClient();
        PodList podList = client.pods().list();
        for (Pod pod:podList.getItems()){
            System.out.println(pod.getMetadata().getName());
        }
    }

    @Test
    public void testGetAllRunningPodsOnCluster(){
        KubernetesClient client = k8sService.getClient();
        PodList podList = client.pods()
                .withField("status.phase", "Running")
                .list();
        for (Pod pod:podList.getItems()){
            System.out.println(pod.getMetadata().getName());
        }
    }
    /**
     * 测试k8s获取client
     */
    @Test
    public void testGetKubernetesClient(){
        KubernetesClient client = k8sService.getClient();
        System.out.println(client);

        NodeList nodeList = client.nodes().list();
        System.out.println(nodeList);
        for (Node node:nodeList.getItems()){
            //判断哪些node节点是dts工作节点
            //获取每个node节点的全部CPU数量和内存
            int totalCPU = Integer.parseInt(node.getStatus().getAllocatable().get("cpu").getAmount());
            int totalMemory = Integer.parseInt(node.getStatus().getAllocatable().get("memory").getAmount());
            //获得每个node节点当前的资源的total request
            System.out.println(node.getMetadata().getName() + totalCPU);
            System.out.println(node.getMetadata().getName() + totalMemory);

            NodeStatus nodeStatus = node.getStatus();
            List<NodeCondition> conditions = nodeStatus.getConditions();
            System.out.println(conditions);

        }
    }
    @Test
    public void testSendMessage(){
        kafkaService.sendMessage("test");
    }
    @Test
    public void testParseJsonToQryOfferDto(){

        String qryOfferFromFile = capacityService.getQryOfferFromFile("./conf/qryOffer.json");
        List<QryOfferDto> qryOfferDtoList = capacityService.parseJSONToQryOfferDto(qryOfferFromFile);
        System.out.println(qryOfferDtoList);
    }
    @Test
    public void testParseJson(){
        String qryOfferFromFile = capacityService.getQryOfferFromFile("./conf/qryOffer.json");
        JSONObject jsonObject = JSON.parseObject(qryOfferFromFile);
        JSONArray jsonArray = jsonObject.getJSONObject("result").getJSONArray("body");
        System.out.println(jsonArray);
    }

    @Test
    public void testGetQryOffer() {
        String qryOffer = mopService.getQryOffer("CIDC-RP-25", "redis");
        System.out.println(qryOffer);


    }

    @Test
    public void testGetQryOfferFromFile() throws IOException {
        String qryOfferFromFile = capacityService.getQryOfferFromFile("./conf/qryOffer.json");
        System.out.println(qryOfferFromFile);

        File file = new File("./conf/qryOffer-test.json");
        if (!file.exists()){
            file.createNewFile();
        }
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(qryOfferFromFile);
        fileWriter.close();
    }






}
