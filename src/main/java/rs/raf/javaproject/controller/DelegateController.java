package rs.raf.javaproject.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rs.raf.javaproject.model.BackupInfo;
import rs.raf.javaproject.model.Point;
import rs.raf.javaproject.response.RegionStatusResponse;
import rs.raf.javaproject.response.ResultResponse;
import rs.raf.javaproject.service.JobService;
import rs.raf.javaproject.service.NodeService;

import java.util.Collection;

@RestController
@CrossOrigin("*")
@RequestMapping("api/delegate/{nodeID}")
public class DelegateController {

    @Autowired
    JobService jobService;

    @Autowired
    NodeService nodeService;

    @GetMapping("/jobs/{jobID}")
    @ResponseBody
    public Collection<Point> getJobResultFromNode(@PathVariable String nodeID, @PathVariable String jobID){
        return jobService.getJobResultFromNode(nodeID, jobID);
    }

    @GetMapping("/jobs/{jobID}/{regionID}")
    @ResponseBody
    public Collection<Point> getRegionResultFromNode(@PathVariable String nodeID, @PathVariable String jobID, @PathVariable String regionID){
        return jobService.getRegionResultFromNode(nodeID, jobID, regionID);
    }

    @GetMapping("/status/{jobID}")
    @ResponseBody
    public RegionStatusResponse myStatus(@PathVariable String nodeID, @PathVariable String jobID){
        return jobService.myStatus(nodeID, jobID);
    }

    @GetMapping("/status/{jobID}/{regionID}")
    @ResponseBody
    public RegionStatusResponse status(@PathVariable String nodeID, @PathVariable String jobID, @PathVariable String regionID){
        return jobService.myStatus(nodeID, jobID, regionID);
    }

    @GetMapping("/backup/{jobID}/{regionID}")
    @ResponseBody
    public BackupInfo getBackup(@PathVariable String nodeID, @PathVariable String jobID, @PathVariable String regionID){
        return nodeService.getBackup(nodeID, jobID, regionID);
    }
}
