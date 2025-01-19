package org.example.clouddatastorage.Service;

import org.example.clouddatastorage.Entity.User;
import org.example.clouddatastorage.Repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public void registerUser(Long id, String username){

        User user = new User();
        user.setId(id);
        user.setUsername(username);

        userRepository.save(user);
    }

    public User findUserById(Long id){
        return userRepository.findById(id).orElse(null); //null вернуть Optional?
    }
}
