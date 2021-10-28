package com.event.discovery.agent.apps.mapper;

import com.event.discovery.agent.job.model.JobBO;
import com.event.discovery.agent.rest.model.JobDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventDiscoveryOperationRequestMapper {

    JobDTO map(JobBO job);

}