package rs.raf.javaproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.raf.javaproject.config.MyConfig;
import rs.raf.javaproject.handler.ExecutorPool;
import rs.raf.javaproject.model.*;

import rs.raf.javaproject.model.SuccessorTable;
import rs.raf.javaproject.requests.bootstrap.Hail;
import rs.raf.javaproject.requests.bootstrap.Left;
import rs.raf.javaproject.requests.bootstrap.New;
import rs.raf.javaproject.requests.job.JobResult;
import rs.raf.javaproject.requests.job.NewJob;
import rs.raf.javaproject.requests.job.RegionResult;
import rs.raf.javaproject.requests.job.Status;
import rs.raf.javaproject.requests.node.*;
import rs.raf.javaproject.response.RegionStatusResponse;
import rs.raf.javaproject.response.ResultResponse;
import rs.raf.javaproject.response.StatusResponse;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class MessageService {

    @Autowired
    private MyConfig config;

    @Autowired
    private SuccessorTable successorTable;

    @Autowired
    private PredecessorTable predecessorTable;

    @Autowired
    private ExecutorPool executorPool;

    private String getBootstrapHailUrl(){
        return "http://" + config.getBootstrap() + "/api/bootstrap/hail";
    }

    private String getBootstrapNewUrl(){
        return "http://" + config.getBootstrap() + "/api/bootstrap/new";
    }

    private String getBootstrapLeftUrl(){
        return "http://" + config.getBootstrap() + "/api/bootstrap/left";
    }

    private String getAllNodesUrl(Node node){
        return "http://" + node.getAddress() + "/api/node/allNodes";
    }

    private String getPingNodesUrl(Node node, Node ping){
        return "http://" + node.getAddress() + "/api/node/ping/" + ping.getAddress();
    }
    private String getNewNodeUrl(Node receiver, Node newNode){
        return "http://" + receiver.getId() + "/api/node/new/" +newNode.getId();
    }

    private String getSaveBackupUrl(Node receiver){
        return "http://" + receiver.getId() + "/api/node/backup";
    }

    private String getAllJobsUrl(Node receiver){
        return "http://" + receiver.getId() + "/api/node/allJobs";
    }

    private String getNewJobUrl(Node receiver){
        return  "http://" + receiver.getId() + "/api/jobs/start";
    }

    private String getJobWorkUrl(String receiver, String forWho, String jobID){
        return "http://" + receiver + "/api/delegate/" + forWho + "/jobs/" + jobID;
    }
    
    private String getRegionWorkUlr(String receiver, String forWho, String jobID, String regionID){
        return "http://" + receiver + "/api/delegate/" + forWho + "/jobs/" + jobID + "/" + regionID;
    }

    private String getLeftUrl(Node receiver, Node nodeThatLeft){
        return "http://" + receiver.getId() + "/api/node/left/" + nodeThatLeft.getId();
    }

    private String getStatusUrl(Node receiver, String forWho, String jobID){
        return "http://" + receiver.getId() + "/api/delegate/" + forWho + "/status/" + jobID;
    }


    private String getStatusUrl(Node receiver, String forWho, String jobID, String regionID){
        return "http://" + receiver.getId() + "/api/delegate/" + forWho + "/status/" + jobID + "/" + regionID;
    }

    private String getGetBackupUrl(String delegateID, String nodeID, String jobID, String regionID) {
        return "http://" + delegateID + "/api/delegate/" + nodeID +"/backup/" + jobID + "/" + regionID;
    }


    public Node sendBootstrapHail(){
        Hail hail = new Hail(getBootstrapHailUrl());
        return hail.execute();
    }

    public void sendBootstrapLeft(Node node){

        executorPool.submit(new Runnable() {
            @Override
            public void run() {
                Left left = new Left(getBootstrapLeftUrl(), node);
                left.execute();
            }
        });
    }

    public void sendBootstrapNew(){
        executorPool.submit(new Runnable() {
            @Override
            public void run() {
                New n = new New(getBootstrapNewUrl(), config.getMe());
                n.execute();
            }
        });
    }


    public Collection<Node> sendGetAllNodes(Node node){
        AllNodes allNodes = new AllNodes(getAllNodesUrl(node));
        Collection<Node> ret =  allNodes.execute();
        if(ret == null){
            ret = new ArrayList<>();
        }
        return ret;
    }

    public Boolean sendPing(Node posrednik, Node destinacija, Integer timeout){
        Ping ping = new Ping(getPingNodesUrl(posrednik, destinacija), timeout);
        return ping.execute();
    }

    public void sendNewNode(Node newNode){

        for(Node node: successorTable.broadcastingNodes()){
            executorPool.submit(new Runnable() {
                @Override
                public void run() {
                    NotifyNewNode notifyNewNode = new NotifyNewNode(getNewNodeUrl(node, newNode));
                    notifyNewNode.execute();
                }
            });
        }

        for(Node node: predecessorTable.broadcastingNodes()){
            executorPool.submit(new Runnable() {
                @Override
                public void run() {
                    NotifyNewNode notifyNewNode = new NotifyNewNode(getNewNodeUrl(node, newNode));
                    notifyNewNode.execute();
                }
            });
        }
    }

    public void sendSaveBackup(Node receiver, BackupInfo backupInfo){
        executorPool.submit(new Runnable() {
            @Override
            public void run() {
                SaveBackup saveBackup = new SaveBackup(getSaveBackupUrl(receiver), backupInfo);
                saveBackup.execute();
            }
        });
    }

    public Collection<Job> sendGetAllJobs(Node node) {
        AllJobs allJobs = new AllJobs(getAllJobsUrl(node));
        Collection<Job> jobs = allJobs.execute();

        if(jobs == null){
            jobs = new ArrayList<>();
        }

        return jobs;
    }

    public void broadcastNewJob(Job job){
        for (Node node: successorTable.broadcastingNodes()){
            executorPool.submit(new Runnable() {
                @Override
                public void run() {
                    NewJob newJob = new NewJob(getNewJobUrl(node), job);
                    newJob.execute();
                }
            });
        }
    }

    public ResultResponse sendGetResult(String jobID, List<String> recipients) {
        ResultResponse resultResponse = new ResultResponse();
        resultResponse.setJobID(jobID);

        resultResponse.setData(new ArrayList<>());

        for(String nodeID: recipients){

            if(nodeID.equals(config.getMe().getId())){
                // Salje sebi ukoliko je on na tom job-u
                JobResult jobResult = new JobResult(getJobWorkUrl(nodeID, nodeID, jobID));
                Collection<Point> points = jobResult.execute();
                if(points == null){
                    points = new ArrayList<>();
                }
                resultResponse.getData().addAll(points);
            }else{
                // Koristimo delegate za slanje poruke
                Node delegator = successorTable.getDelegator(successorTable.getDatabase().getAllNodes().get(nodeID));
                System.out.println(successorTable.getTable());
                System.out.println(delegator.getId() + " za " + nodeID);
                JobResult jobResult = new JobResult(getJobWorkUrl(delegator.getId(), nodeID, jobID));                               // Ne saljemo direktno cvoru nego delegatoru
                // Ako nam je cvor u successor tabeli vratice njega
                Collection<Point> jobs = jobResult.execute();
                if(jobs == null){
                    jobs = new ArrayList<>();
                }
                resultResponse.getData().addAll(jobs);
            }
        }

        return resultResponse;
    }

    public ResultResponse sendGetResult(String jobID, String regionID, List<String> recipients) {
        ResultResponse resultResponse = new ResultResponse();
        resultResponse.setJobID(jobID);

        resultResponse.setData(new ArrayList<>());

        for(String nodeID: recipients){

            if(nodeID.equals(config.getMe().getId())){
                // Salje sebi ukoliko je on na tom job-u
                RegionResult regionResult = new RegionResult(getRegionWorkUlr(nodeID, nodeID, jobID, regionID));
                Collection<Point> points = regionResult.execute();
                if(points == null){
                    points = new ArrayList<>();
                }
                resultResponse.getData().addAll(points);
            }else{
                // Koristimo delegate za slanje poruke
                Node delegator = successorTable.getDelegator(successorTable.getDatabase().getAllNodes().get(nodeID));

                RegionResult regionResult = new RegionResult(getRegionWorkUlr(delegator.getId(), nodeID, jobID, regionID));
                Collection<Point> points = regionResult.execute();
                if(points == null){
                    points = new ArrayList<>();
                }
                resultResponse.getData().addAll(points);
            }
        }

        return resultResponse;
    }


    public void broadcastLeaveMessage(Node nodeThatLeft){
        for (Node node: successorTable.broadcastingNodes()){
            executorPool.submit(new Runnable() {
                @Override
                public void run() {
                    NodeLeft left = new NodeLeft(getLeftUrl(node, nodeThatLeft));
                    left.execute();
                }
            });
        }
    }

    @PreDestroy
    public void leave(){
        sendBootstrapLeft(config.getMe());
    }

    public StatusResponse sendGetStatus(String jobID, List<String> receiverIDs) {

        StatusResponse statusResponse = new StatusResponse();
        statusResponse.setJobID(jobID);

        for(String nodeID: receiverIDs){
            RegionStatusResponse regionStatusResponse = new RegionStatusResponse();

            if(nodeID.equals(config.getMe().getId())){

                Status status = new Status(getStatusUrl(config.getMe(), nodeID, jobID));                               // Ne saljemo direktno cvoru nego delegatoru

                regionStatusResponse = status.execute();

            }else{
                Node delegator = successorTable.getDelegator(successorTable.getDatabase().getAllNodes().get(nodeID));

                Status status = new Status(getStatusUrl(delegator, nodeID, jobID));                               // Ne saljemo direktno cvoru nego delegatoru

                regionStatusResponse = status.execute();
            }

            if(regionStatusResponse == null){
//                String region =
                regionStatusResponse = new RegionStatusResponse("nema region id", nodeID, 0);
            }
            statusResponse.getAllJobs().add(regionStatusResponse);
        }

        return statusResponse;
    }

    public StatusResponse sendGetStatus(String jobID, String regionID, List<String> receiverIDs) {

        StatusResponse statusResponse = new StatusResponse();
        statusResponse.setJobID(jobID);
        statusResponse.setSubRegion(regionID);

        for(String nodeID: receiverIDs){
            RegionStatusResponse regionStatusResponse = new RegionStatusResponse();

            if(nodeID.equals(config.getMe().getId())){

                Status status = new Status(getStatusUrl(config.getMe(), nodeID, jobID, regionID));                               // Ne saljemo direktno cvoru nego delegatoru

                regionStatusResponse = status.execute();
            }else{
                Node delegator = successorTable.getDelegator(successorTable.getDatabase().getAllNodes().get(nodeID));

                Status status = new Status(getStatusUrl(delegator, nodeID, jobID, regionID));                               // Ne saljemo direktno cvoru nego delegatoru

                regionStatusResponse = status.execute();
            }
            statusResponse.getAllJobs().add(regionStatusResponse);
        }

        return statusResponse;
    }

    public RegionStatusResponse sendGetRegionStatus(String jobID, String nodeID) {

        Node delegator = successorTable.getDelegator(new Node(nodeID));
        Status status = new Status(getStatusUrl(delegator, nodeID, jobID));                               // Ne saljemo direktno cvoru nego delegatoru
        RegionStatusResponse regionStatusResponse = status.execute();
        if(regionStatusResponse == null){
            regionStatusResponse = new RegionStatusResponse("Treba mi region", nodeID, 0);
        }
        return regionStatusResponse;
    }

    public RegionStatusResponse sendGetRegionStatus(String jobID, String nodeID, String regionID) {

        Node delegator = successorTable.getDelegator(new Node(nodeID));
        Status status = new Status(getStatusUrl(delegator, nodeID, jobID, regionID));                               // Ne saljemo direktno cvoru nego delegatoru

        RegionStatusResponse regionStatusResponse = status.execute();
        if(regionStatusResponse == null){
            regionStatusResponse = new RegionStatusResponse("Treba mi region1", nodeID, 0);
        }
        return regionStatusResponse;
    }

    public BackupInfo sendGetData(String nodeID, String jobID, String regionID) {
        Node delegat = successorTable.getDelegator(new Node(nodeID));
        GetBackup getBackup = new GetBackup(getGetBackupUrl(delegat.getId(), nodeID, jobID, regionID));
        return getBackup.execute();
    }


}









