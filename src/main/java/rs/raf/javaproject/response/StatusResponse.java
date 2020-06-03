package rs.raf.javaproject.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import rs.raf.javaproject.model.Job;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusResponse {

    private int numberOfJobs;
    private List<Job> allJobs;
}
