package rs.raf.javaproject.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rs.raf.javaproject.model.Job;
import rs.raf.javaproject.model.Point;
import rs.raf.javaproject.response.RegionStatusResponse;
import rs.raf.javaproject.response.ResultResponse;
import rs.raf.javaproject.response.StatusResponse;
import rs.raf.javaproject.service.JobService;

import java.util.Collection;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/jobs")
public class JobController {
    // TODO: Vidi gde treba nodeID da se prosledjuje i dodaj, promeni i u doc-u

    @Autowired
    private JobService service;

    @GetMapping("/status")
    @ResponseBody
    public Collection<StatusResponse> status(){
        return service.status();
    }

    @GetMapping("/status/{jobID}")
    @ResponseBody
    public StatusResponse status(@PathVariable String jobID){
        return service.status(jobID);
    }

    @GetMapping("/status/{jobID}/{regionID}")
    @ResponseBody
    public StatusResponse status(@PathVariable String jobID, @PathVariable String regionID){
        return service.status(jobID, regionID);
    }

    @PutMapping("/start")
    public void start(@RequestBody Job job){
        service.start(job);
    }

    @GetMapping("/result/{jobID}")
    @ResponseBody
    public ResultResponse result(@PathVariable String jobID){
        return service.result(jobID);
    }

    @GetMapping("/result/{jobID}/{regionID}")
    @ResponseBody
    public ResultResponse result(@PathVariable String jobID, @PathVariable String regionID){
        return service.result(jobID, regionID);
    }


//    @GetMapping("/stopAll/{jobID}")
//    public void stopAll(@PathVariable String jobID){
//        service.stopAll(jobID);
//    }

    @DeleteMapping("/{jobID}")
    public void deleteJob(@PathVariable String jobID){
        service.deleteJob(jobID);
    }
}
