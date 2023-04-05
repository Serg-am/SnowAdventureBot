package com.bots.snowadventurebot.repositories;

import com.bots.snowadventurebot.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
