package com.esprit.task.client;

import com.esprit.task.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user", url = "${user.service.url:http://localhost:8090}", path = "/api/users")
public interface UserClient {

    @GetMapping("/{id}")
    UserDto getUserById(@PathVariable("id") Long id);
}
