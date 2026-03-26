package com.local.caejobservice.job.api;

import com.local.caejobservice.job.api.request.JobCreateRequest;
import com.local.caejobservice.job.api.response.JobActionResponse;
import com.local.caejobservice.job.api.response.JobDetailResponse;
import com.local.caejobservice.job.application.JobApplicationService;
import com.local.caejobservice.job.application.assembler.JobResponseAssembler;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {

  private final JobApplicationService jobApplicationService;
  private final JobResponseAssembler jobResponseAssembler;

  public JobController(
      JobApplicationService jobApplicationService, JobResponseAssembler jobResponseAssembler) {
    this.jobApplicationService = jobApplicationService;
    this.jobResponseAssembler = jobResponseAssembler;
  }

  @GetMapping("/")
  public Map<String, Object> healthcheck() {
    return jobApplicationService.healthPayload();
  }

  @PostMapping("/jobs")
  public JobActionResponse createJob(@Valid @RequestBody JobCreateRequest request) {
    return jobApplicationService.createJob(request);
  }

  @GetMapping("/jobs/{jobId}")
  public JobDetailResponse getJob(@PathVariable String jobId) {
    return jobResponseAssembler.toDetailResponse(jobApplicationService.getJob(jobId));
  }

  @PostMapping("/jobs/{jobId}/cancel")
  public JobActionResponse cancelJob(@PathVariable String jobId) {
    return jobApplicationService.cancelJob(jobId);
  }

  @GetMapping("/jobs")
  public List<JobDetailResponse> listJobs(@RequestParam(required = false) String status) {
    return jobApplicationService.listJobs(status).stream()
        .map(jobResponseAssembler::toDetailResponse)
        .toList();
  }
}
