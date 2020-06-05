package org.example;


import org.example.config.ServentConfig;
import org.example.model.Job;
import org.example.model.Node;
import org.example.service.DatabaseService;
import org.example.service.MessageService;
import org.example.service.ReconstructionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PostConstruct;
import java.util.Collection;

@SpringBootApplication
public class JavaServlet {

    @Autowired
    ServentConfig config;

    @Autowired
    DatabaseService databaseService;

    @Autowired
    MessageService messageService;

    @Autowired
    ReconstructionService reconstructionService;

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(JavaServlet.class, args);
    }

    public static void exitThread() {
        SpringApplication.exit(context,() -> 0);
    }

    @PostConstruct
    public void init() {

        config.setServent(new Node(config.getAddress(), config.getPort()));

        databaseService.saveNode(databaseService.getInfo()); // Dodaje me u kolekciju svih nodova

        Node enteringNode = messageService.sendBootstrapHail();

        if(enteringNode.getIp() == null){
            messageService.sendBootstrapNew(config.getServent());
        }else{

            databaseService.saveNodes(messageService.sendGetAllNodes(enteringNode));
            databaseService.saveJobs(messageService.sendGetAllJobs(enteringNode));

            reconstructionService.reconstruct();

            messageService.sendUpdateNewNode(databaseService.getPredecessor(), databaseService.getInfo());
            messageService.broadcastNewNode(databaseService.getMyBroadcastingNodes(), databaseService.getInfo());

            messageService.sendBootstrapNew(config.getServent());

        }

    }
}
